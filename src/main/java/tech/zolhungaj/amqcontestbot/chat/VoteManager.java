package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class VoteManager {
    private static final int VOTE_TIME = 20;
    private final Vote noVote = new Vote(List.of(), () -> {});
    private final ChatCommands chatCommands;
    private final ChatController chatController;

    private Vote currentVote = noVote;
    private int counter = 0;
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void count(){
        if(currentVote == noVote){
            counter = 0;
            return;
        }
        counter++;
        if(counter >= VOTE_TIME){
            currentVote.finaliseVote();
        }
    }

    @PostConstruct
    private void init(){
        chatCommands.register((sender, unused) -> currentVote.registerVote(sender, true), "yes", "y");
        chatCommands.register((sender, unused) -> currentVote.registerVote(sender, false), "no", "n");
        chatCommands.register((sender, unused) -> adminReset(), ChatCommands.Grant.ADMIN, "resetvote");
    }

    private void adminReset(){
        chatController.send("vote.reset");
        reset();
    }

    private void reset(){
        currentVote = noVote;
        counter = 0;
    }

    public void startVote(List<String> voters, Runnable runnable){
        startVote(voters, runnable, null);
    }

    public void startVote(List<String> voters, Runnable runnable, String initialVoter){
        if(currentVote != noVote){
            chatController.send("vote.already-running");
            return;
        }
        if(voters.isEmpty()){
            chatController.send("vote.no-voters");
            return;
        }
        currentVote = new Vote(voters, runnable);
        if(initialVoter != null){
            //register initial vote, if present
            currentVote.registerVote(initialVoter, true);
        }
    }



    private class Vote{
        private final Map<String, Optional<Boolean>> votes = new HashMap<>();
        private final Runnable runnable;
        private Vote(List<String> voters, Runnable runnable){
            voters.forEach(voter -> votes.put(voter, Optional.empty()));
            this.runnable = runnable;
        }

        private void registerVote(String voter, boolean vote){
            if(!votes.containsKey(voter)){
                chatController.send("vote.not-registered", voter);
                return;
            }
            if(votes.get(voter).isPresent()){
                chatController.send("vote.already-voted", voter);
                return;
            }
            votes.replace(voter, Optional.of(vote));
            calculateVote();
        }

        private void calculateVote(){
            int yes = (int) votes.values().stream().filter(Optional::isPresent).filter(Optional::get).count();
            int no = (int) votes.values().stream().filter(Optional::isPresent).filter(vote -> !vote.get()).count();
            int missing = (int) votes.values().stream().filter(Optional::isEmpty).count();
            int total = yes + no + missing;
            if(yes > no + missing){
                chatController.send("vote.passed", yes, total);
                run();
                reset();
            }else if(no >= yes + missing){
                chatController.send("vote.failed", no, total);
                reset();
            }else{
                chatController.send("vote.status", yes, no, total, missing);
            }
        }

        private void finaliseVote(){
            //remove all voters that did not vote, then calculate result
            chatController.send("vote.finalise");
            List<String> noVoters = votes.keySet().stream().filter(voter -> votes.get(voter).isEmpty()).toList();
            noVoters.forEach(votes::remove);
            calculateVote();
        }

        private void run(){
            runnable.run();
        }
    }
}
