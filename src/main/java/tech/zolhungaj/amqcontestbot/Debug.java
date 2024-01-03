package tech.zolhungaj.amqcontestbot;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.ErrorParsingCommand;
import tech.zolhungaj.amqapi.servercommands.UnregisteredCommand;
import tech.zolhungaj.amqapi.servercommands.expandlibrary.ExpandLibraryEntryList;
import tech.zolhungaj.amqapi.servercommands.expandlibrary.ExpandLibraryEntryUpdated;
import tech.zolhungaj.amqapi.servercommands.gameroom.*;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.*;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.*;
import tech.zolhungaj.amqapi.servercommands.globalstate.*;
import tech.zolhungaj.amqapi.servercommands.social.*;
import tech.zolhungaj.amqapi.servercommands.store.TicketRollResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class Debug {
    private static final List<Class<?>> registerList = List.of(
            ExpandLibraryEntryList.class,
            ExpandLibraryEntryUpdated.class,
            AnswerResults.class,
            AnswerReveal.class,
            GameStarting.class,
            GuessPhaseOver.class,
            NextVideoInfo.class,
            PlayerRejoin.class,
            PlayersAnswered.class,
            PlayNextSong.class,
            QuizEndResult.class,
            QuizFatalError.class,
            QuizNoSongs.class,
            QuizOver.class,
            QuizReady.class,
            QuizSkipMessage.class,
            SongFeedbackRequest.class,
            WaitingForBuffering.class,
            GameHosted.class,
            NewPlayer.class,
            PlayerChangedAvatar.class,
            PlayerChangedToSpectator.class,
            PlayerReadyChange.class,
            SpectatorChangedToPlayer.class,
            GameChatMessage.class,
            GameChatSystemMessage.class,
            GameChatUpdate.class,
            PlayerLeft.class,
            SpectatorJoined.class,
            SpectatorLeft.class,
            Alert.class,
            AllOnlineUsers.class,
            AvatarDriveUpdate.class,
            FileServerStatus.class,
            ForcedLogoff.class,
            FriendAdded.class,
            FriendNameChange.class,
            FriendOnlineChange.class,
            FriendProfileImageChange.class,
            HtmlAlert.class,
            LoginComplete.class,
            NewDonation.class,
            NewQuestEvents.class,
            OnlinePlayerCountChange.class,
            OnlineUserChange.class,
            PopoutMessage.class,
            RankedChampionsUpdate.class,
            RankedGameStateChanged.class,
            RankedLeaderboardUpdate.class,
            RankedScoreUpdate.class,
            SelfNameChange.class,
            ServerRestartWarning.class,
            ServerUnknownError.class,
            DirectMessage.class,
            DirectMessageResponse.class,
            FriendRemoved.class,
            FriendRequestReceived.class,
            FriendRequestResponse.class,
            FriendSocialStatusUpdate.class,
            GameInvite.class,
            PlayerProfile.class,
            TicketRollResult.class,
            PlayerJoinedQueue.class,
            PlayerLeftQueue.class,
            DirectMessageAlert.class,
            SkipVotePassed.class,
            QuizSkippingToNextPhase.class
    );
    private final ApiManager api;
    private final Path debugJsonFile = Path.of(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".json");
    private final Path debugCommandFile = Path.of(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
    @PostConstruct
    public void init() throws IOException {
        Files.createFile(debugJsonFile);
        Files.createFile(debugCommandFile);
        Files.writeString(debugJsonFile, "[\n");
        api.onAllJsonCommands(json -> {
            try {
                Files.writeString(debugJsonFile, json.toString(4) + ",\n", StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("Failed to write to json file", e);
            }
        });
        api.onAllCommands(command -> {
            try {
                Files.writeString(debugCommandFile, command.toString()+ "\n", StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("Failed to write to file", e);
            }
            if (command instanceof ErrorParsingCommand errorParsingCommand){
                var path = Path.of("ERROR-" + errorParsingCommand.commandName().replace(" ", "-") + ".txt");
                try{
                    log.error("Something is wrong with the input data, writing to {} for inspection", path, errorParsingCommand.error());
                    Files.writeString(path, "\n\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                    Files.writeString(path, errorParsingCommand.data().toString(4), StandardOpenOption.APPEND);
                    Files.writeString(path, "\n\n", StandardOpenOption.APPEND);
                    Files.writeString(path, errorParsingCommand.error().toString(), StandardOpenOption.APPEND);
                }catch (IOException e){
                    log.error("ERROR file write error", e);
                }
            } else if (command instanceof UnregisteredCommand unregisteredCommand){
                log.info("""
                    Unregistered command:
                        command: {}
                        data: {}
                    """, unregisteredCommand.commandName(), unregisteredCommand.data());
                var path = Path.of("UNREGISTERED-" + unregisteredCommand.commandName().replace(" ", "-") + ".txt");
                try{
                    Files.writeString(path, "\n\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                    Files.writeString(path, unregisteredCommand.data().toString(4), StandardOpenOption.APPEND);
                }catch (IOException e){
                    log.error("UNREGISTERED file write error", e);
                }
            }
        });
        registerList.forEach(api::registerCommand);
    }

    @PreDestroy
    public void destroy() throws IOException {
        Files.writeString(debugJsonFile, "\n]", StandardOpenOption.APPEND);
    }

}
