package dev.alphaserpentis.bots.brewer.handler.bot;

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
import java.util.Objects;
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
        }, 1, 60, TimeUnit.MINUTES);

        Launcher.core.getShardManager().getGuilds().forEach(guild -> generateAnalytics(guild.getIdLong()));
    }

    public static void addUsage(@Nullable Guild guild, @NonNull ServiceType type) {
        if(guild == null || (serverAnalytics.get(guild.getIdLong()) == null && !generateAnalytics(guild.getIdLong()))) {
            return;
        }

        var analytics = serverAnalytics.get(guild.getIdLong());

        analytics.getUsagePerServiceType().putIfAbsent(type, 0);
        analytics.getUsagePerServiceType().put(type, analytics.getUsagePerServiceType().get(type) + 1);
    }

    /**
     * Generates analytics for a guild
     * @param guildId ID of the guild to generate analytics for
     * @return true if analytics were generated, false if the guild opted out of analytics or guild was not found
     */
    public static boolean generateAnalytics(long guildId) {
        var serverData = (BrewerServerData) Launcher.core.getServerDataHandler().getServerData(guildId);
        Guild guild;
        Analytics analytics;
        int snapshotUserCount;
        long epochSecond;
        long snapshotAgeOfServer;
        long snapshotBotAgeInServer;

        if(serverData.getServerWideOptOutOfAnalytics()) {
            return false;
        }

        try {
            guild = Objects.requireNonNull(Launcher.core.getShardManager().getGuildById(guildId));
        } catch(NullPointerException e) {
            logger.error("Failed to get guild with ID " + guildId, e);
            return false;
        }

        // Take snapshot of the server
        epochSecond = Instant.now().getEpochSecond();
        snapshotUserCount = guild.retrieveMetaData().complete().getApproximateMembers();
        snapshotAgeOfServer = epochSecond - guild.getTimeCreated().toInstant().getEpochSecond();
        snapshotBotAgeInServer = epochSecond  - guild.getSelfMember().getTimeJoined().toInstant().getEpochSecond();

        // Set analytics
        analytics = new Analytics(
                snapshotUserCount,
                snapshotAgeOfServer,
                snapshotBotAgeInServer,
                serverData.getPaidTier()
        );

        serverAnalytics.put(guildId, analytics);

        return true;
    }

    /**
     * Dumps analytics to the analytics directory.
     */
    public static void dumpAnalytics() throws IOException {
        var writer = new BufferedWriter(new FileWriter(analyticsDirectory.toFile()));
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var anonymizedAnalytics = new ArrayList<>(serverAnalytics.values());

        gson.toJson(anonymizedAnalytics, writer);

        writer.close();
    }

    public static void stopTrackingGuild(long guildId) {
        serverAnalytics.remove(guildId);
    }
}
