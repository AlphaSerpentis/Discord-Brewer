package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.alphaserpentis.bots.brewer.data.brewer.Analytics;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.entities.Guild;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyticsHandler {
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final HashMap<Long, Analytics> serverAnalytics = new HashMap<>();
    private static Path analyticsDirectory;

    public static void init(@NonNull Path analyticsDirectory) {
        AnalyticsHandler.analyticsDirectory = analyticsDirectory;
        scheduledExecutor.scheduleAtFixedRate(AnalyticsHandler::dumpAnalytics, 1, 24, TimeUnit.HOURS);
    }

    public static void generateAnalytics(long guildId) {
        BrewerServerData serverData = (BrewerServerData) Launcher.core.getServerDataHandler().getServerData(guildId);
        Guild guild = Launcher.core.getJda().getGuildById(guildId);
        Analytics analytics;
        int snapshotUserCount;
        long snapshotAgeOfServer;

        // Take snapshot of the server
        snapshotUserCount = guild.retrieveMetaData().complete().getApproximateMembers();
        snapshotAgeOfServer = (System.currentTimeMillis() - guild.getTimeCreated().toInstant().toEpochMilli());

        // Set analytics
        analytics = new Analytics(snapshotUserCount, snapshotAgeOfServer, serverData.getPaidTier());

        serverAnalytics.put(guildId, analytics);
    }

    /**
     * Dumps analytics to the analytics directory.
     */
    public static void dumpAnalytics() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
    }
}
