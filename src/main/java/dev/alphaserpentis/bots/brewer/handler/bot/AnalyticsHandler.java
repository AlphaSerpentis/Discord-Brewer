package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.alphaserpentis.bots.brewer.data.brewer.Analytics;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyticsHandler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsHandler.class);
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final HashMap<Long, Analytics> serverAnalytics = new HashMap<>();
    private static Path analyticsDirectory;

    public static void init(@NonNull Path analyticsDirectory) {
        AnalyticsHandler.analyticsDirectory = analyticsDirectory;
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                dumpAnalytics();
            } catch (IOException e) {
               logger.error("Failed to dump analytics", e);
            }
        }, 1, 1440, TimeUnit.MINUTES);

        Launcher.core.getShardManager().getGuilds().forEach(guild -> {
            BrewerServerData serverData = (BrewerServerData) Launcher.core.getServerDataHandler().getServerData(guild.getIdLong());
            if(!serverData.getServerWideOptOutOfAnalytics()) {
                generateAnalytics(guild.getIdLong());
            }
        });
    }

    public static void addUsage(@Nullable Guild guild, @NonNull ServiceType type) {
        if(guild == null) {
            return;
        }

        if(serverAnalytics.get(guild.getIdLong()) == null) {
            generateAnalytics(guild.getIdLong());
        }

        Analytics analytics = serverAnalytics.get(guild.getIdLong());

        analytics.getUsagePerServiceType().putIfAbsent(type, 0);
        analytics.getUsagePerServiceType().put(type, analytics.getUsagePerServiceType().get(type) + 1);
    }

    public static void generateAnalytics(long guildId) {
        BrewerServerData serverData = (BrewerServerData) Launcher.core.getServerDataHandler().getServerData(guildId);

        if(serverData.getServerWideOptOutOfAnalytics()) {
            return;
        }

        Guild guild = Launcher.core.getShardManager().getGuildById(guildId);
        Analytics analytics;
        int snapshotUserCount;
        long snapshotAgeOfServer;
        long snapshotBotAgeInServer;

        // Take snapshot of the server
        snapshotUserCount = guild.retrieveMetaData().complete().getApproximateMembers();
        snapshotAgeOfServer = (Instant.now().getEpochSecond() - guild.getTimeCreated().toInstant().getEpochSecond());
        snapshotBotAgeInServer = Instant.now().getEpochSecond() - guild.getSelfMember().getTimeJoined().toInstant().getEpochSecond();

        // Set analytics
        analytics = new Analytics(snapshotUserCount, snapshotAgeOfServer, snapshotBotAgeInServer, serverData.getPaidTier());

        serverAnalytics.put(guildId, analytics);
    }

    /**
     * Dumps analytics to the analytics directory.
     */
    public static void dumpAnalytics() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ArrayList<Analytics> anonymizedAnalytics = new ArrayList<>(serverAnalytics.values());
        BufferedWriter writer = new BufferedWriter(new FileWriter(analyticsDirectory.toFile()));

        gson.toJson(anonymizedAnalytics, writer);

        writer.close();
    }

    public static void stopTrackingGuild(long guildId) {
        serverAnalytics.remove(guildId);
    }
}
