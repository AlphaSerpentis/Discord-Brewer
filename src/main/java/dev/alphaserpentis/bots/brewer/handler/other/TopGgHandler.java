package dev.alphaserpentis.bots.brewer.handler.other;

import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import dev.alphaserpentis.coffeecore.helper.ContainerHelper;
import io.reactivex.rxjava3.annotations.NonNull;
import org.discordbots.api.client.DiscordBotListAPI;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TopGgHandler {
    private static DiscordBotListAPI dblApi;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void init(@NonNull String token, @NonNull String botId) {
        dblApi = new DiscordBotListAPI.Builder()
                .token(token)
                .botId(botId)
                .build();

        scheduler.scheduleAtFixedRate(
                TopGgHandler::update,
                0,
                60,
                TimeUnit.MINUTES
        );
    }

    public static void update() {
        setServerCount();
    }

    public static void setServerCount() {
        final ContainerHelper container = new ContainerHelper(Launcher.core.getActiveContainer());
        dblApi.setStats(container.getGuilds().size());
    }

    public static boolean didUserVote(@NonNull String userId) throws ExecutionException, InterruptedException {
        return dblApi.hasVoted(userId).toCompletableFuture().get();
    }
}
