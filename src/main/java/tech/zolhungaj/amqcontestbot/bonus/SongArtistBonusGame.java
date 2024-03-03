package tech.zolhungaj.amqcontestbot.bonus;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.AnswerResults;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.PlayNextSong;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizOver;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizReady;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.commands.ChatCommands;
import tech.zolhungaj.amqcontestbot.commands.DirectMessageCommands;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongArtistBonusGame {
    private static final LevenshteinDetailedDistance TEXT_COMPARER = new LevenshteinDetailedDistance();
    private static final Predicate<Double> scoreCutoff = score -> score > 0.8;
    private static final Answer NO_ANSWER = new Answer(Optional.empty(), Optional.empty(), Optional.empty());
    private final ApiManager apiManager;
    private final ChatCommands chatCommands;
    private final DirectMessageCommands directMessageCommands;
    private final ChatController chatController;

    private final Map<String, Answer> answers = new HashMap<>();
    private final Map<String, Score> scores = new HashMap<>();
    private final Map<String, Score> lastScores = new HashMap<>();


    @PostConstruct
    private void init(){
        apiManager.on(PlayNextSong.class, songStart -> answers.clear());
        apiManager.on(QuizReady.class, quizReady -> {
            answers.clear();
            scores.clear();
        });
        apiManager.on(AnswerResults.class, this::score);
        apiManager.on(QuizOver.class, quizOver -> {
            chatFinalScore();
            answers.clear();
            scores.clear();
            lastScores.clear();
        });
        registerChatCommands();
    }

    private void score(AnswerResults answerResults){
        lastScores.clear();
        List<String> validAnimeNames = new ArrayList<>();
        validAnimeNames.add(answerResults.songInfo().mainAnimeNames().english());
        validAnimeNames.add(answerResults.songInfo().mainAnimeNames().romaji());
        validAnimeNames.addAll(answerResults.songInfo().alternativeAnimeNames());
        validAnimeNames.addAll(answerResults.songInfo().alternativeAnimeNamesAnswers());
        answers.forEach((sender, answer) -> {
            Score previousScore = scores.getOrDefault(sender, new Score(0, 0, 0));
            double animeScore = answer.anime.map(
                    anime -> validAnimeNames.stream()
                        .mapToDouble(validAnime -> compareText(anime, validAnime))
                        .max()
                        .orElse(1.0) //case where there are no anime titles
                    )
                    .filter(scoreCutoff)
                    .orElse(0.0);
            double songScore = answer.song
                    .map(song -> compareText(song, answerResults.songInfo().songName()))
                    .filter(scoreCutoff)
                    .orElse(0.0);
            double artistScore = answer.artist
                    .map(artist -> compareText(artist, answerResults.songInfo().artist()))
                    .filter(scoreCutoff)
                    .orElse(0.0);
            Score scoreToAdd = new Score(animeScore, songScore, artistScore);
            lastScores.put(sender, scoreToAdd);
            scores.put(sender, previousScore.add(scoreToAdd));
            log.info("Score for {}: anime: {}, song: {}, artist: {}", sender, animeScore, songScore, artistScore);
        });
        chatLastScore();
    }

    private static double compareText(@NonNull String original, @NonNull String comparison){
        original = original.toLowerCase();
        comparison = comparison.toLowerCase();
        LevenshteinResults results = TEXT_COMPARER.apply(original, comparison);
        double currentScore = 1.0;
        if(results.getDeleteCount() > 0){
            currentScore -= (Math.pow(1.030, results.getDeleteCount()) - 1.027);
        }
        if(results.getInsertCount() > 0){
            currentScore -= (Math.pow(1.031, results.getInsertCount()) - 1.027);
        }
        if(results.getSubstituteCount() > 0){
            currentScore -= (Math.pow(1.028, results.getSubstituteCount()) - 1.027);
        }
        return currentScore;
    }

    private void chatFinalScore(){
        chatScore(scores, "%s:%.2f"::formatted);
    }

    private void chatLastScore(){
        chatScore(lastScores, "%s:+%.2f"::formatted);
    }

    private void chatScore(Map<String, Score> map, BiFunction<String, Double, String> formatter){
        Function<Map.Entry<String,Score>, String> extractSender = Map.Entry::getKey;
        Map<String, ToDoubleFunction<Map.Entry<String,Score>>> extractScores = Map.of(
                "anime", entry -> entry.getValue().anime,
                "song", entry -> entry.getValue().song,
                "artist", entry -> entry.getValue().artist
        );
        for(Map.Entry<String, ToDoubleFunction<Map.Entry<String,Score>>> extractionFunction : extractScores.entrySet()){
            String scoreType = extractionFunction.getKey();
            ToDoubleFunction<Map.Entry<String,Score>> extractScore = extractionFunction.getValue();
            String scoresString = map.entrySet()
                    .stream()
                    .filter(entry -> extractScore.applyAsDouble(entry) > 0)
                    .sorted(Comparator.comparingDouble(extractScore).reversed())
                    .map(entry -> {
                        String sender = extractSender.apply(entry);
                        Double score = extractScore.applyAsDouble(entry);
                        return formatter.apply(sender, score);
                    })
                    .collect(Collectors.joining(" "));
            if(!scoresString.isBlank()){
                chatController.send("bonus.score." + scoreType, scoresString);
            }
        }
    }

    private void registerChatCommands(){
        //note the high amount of aliases, this is to make it easier for people to use the command quickly
        //and the more weird ones are legacy from the old bot
        chatCommands.register(this::answerCommand, "answer", "a");
        chatCommands.register(this::songArtistCommand, "songartist", "sa", "answersongartist", "answersa");
        chatCommands.register(this::animeCommand, "anime", "answeranime");
        chatCommands.register(this::songCommand, "song", "answersong", "answers", "s", "as");
        chatCommands.register(this::artistCommand, "artist", "answerartist", "answera", "aa");

        directMessageCommands.register(this::answerCommand, "answer", "a");
        directMessageCommands.register(this::songArtistCommand, "songartist", "sa");
        directMessageCommands.register(this::animeCommand, "anime"); //alias covered by answer
        directMessageCommands.register(this::songCommand, "song", "s");
        directMessageCommands.register(this::artistCommand, "artist", "aa");
    }

    private void answerCommand(String sender, List<String> arguments){
        if(arguments.isEmpty()){
            throw new IncorrectArgumentCountException(1, 3);
        }
        String argument = String.join(" ", arguments);
        List<String> rawAnswers = List.of(argument.split("\\|"));
        if(rawAnswers.size() > 3){
            throw new IncorrectArgumentCountException(1, 3);
        }
        String anime = !rawAnswers.isEmpty() ? rawAnswers.get(0) : null;
        String song = rawAnswers.size() > 1 ? rawAnswers.get(1) : null;
        String artist = rawAnswers.size() > 2 ? rawAnswers.get(2) : null;
        answer(sender, anime, song, artist);
    }

    private void songArtistCommand(String sender, List<String> arguments){
        if(arguments.isEmpty()){
            throw new IncorrectArgumentCountException(1, 2);
        }
        String argument = String.join(" ", arguments);
        List<String> rawAnswers = List.of(argument.split("\\|"));
        if(rawAnswers.size() > 3){
            throw new IncorrectArgumentCountException(1, 2);
        }
        String song = !rawAnswers.isEmpty() ? rawAnswers.get(0) : null;
        String artist = rawAnswers.size() > 1 ? rawAnswers.get(1) : null;
        answer(sender, null, song, artist);
    }

    private void animeCommand(String sender, List<String> arguments){
        if(arguments.isEmpty()){
            throw new IncorrectArgumentCountException(1);
        }
        String argument = String.join(" ", arguments);
        answer(sender, argument, null, null);
    }
    private void songCommand(String sender, List<String> arguments){
        if(arguments.isEmpty()){
            throw new IncorrectArgumentCountException(1);
        }
        String argument = String.join(" ", arguments);
        answer(sender, null, argument, null);
    }

    private void artistCommand(String sender, List<String> arguments){
        if(arguments.isEmpty()){
            throw new IncorrectArgumentCountException(1);
        }
        String argument = String.join(" ", arguments);
        answer(sender, null, null, argument);
    }

    private void answer(String sender, @Nullable String anime, @Nullable String song, @Nullable String artist){
        answers.putIfAbsent(sender, NO_ANSWER);
        if(anime != null && !anime.isBlank()){
            answers.computeIfPresent(sender, (s, answer) -> answer.withAnime(Optional.of(anime.trim())));
        }
        if(song != null && !song.isBlank()){
            answers.computeIfPresent(sender, (s, answer) -> answer.withSong(Optional.of(song.trim())));
        }
        if(artist != null && !artist.isBlank()){
            answers.computeIfPresent(sender, (s, answer) -> answer.withArtist(Optional.of(artist.trim())));
        }
    }

    @With
    private record Answer(Optional<String> anime, Optional<String> song, Optional<String> artist){}
    private record Score(double anime, double song, double artist){
        public Score add(Score other){
            return new Score(anime + other.anime, song + other.song, artist + other.artist);
        }
    }
}
