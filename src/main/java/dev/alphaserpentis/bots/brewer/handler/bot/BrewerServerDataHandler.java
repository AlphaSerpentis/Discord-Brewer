package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class BrewerServerDataHandler extends ServerDataHandler<BrewerServerData> {

    final Logger logger = LoggerFactory.getLogger(BrewerServerDataHandler.class);

    /**
     * Initializes the server data handler.
     *
     * @param path             The path to the server data file.
     * @param typeToken        The {@link TypeToken} of the mapping of user IDs to {@link BrewerServerData}.
     * @param jsonDeserializer The {@link JsonDeserializer} to deserialize the server data.
     * @throws IOException If the bot fails to read the server data file.
     */
    public BrewerServerDataHandler(
            @NonNull Path path,
            @NonNull TypeToken<Map<Long, BrewerServerData>> typeToken,
            @NonNull JsonDeserializer<Map<Long, BrewerServerData>> jsonDeserializer,
            boolean resetAcknowledgementsForToS,
            boolean resetAcknowledgementsForPrivacyPolicy,
            boolean resetAcknowledgementsForNewUpdates
    ) throws IOException {
        super(path, typeToken, jsonDeserializer);
        boolean updateFile = resetAcknowledgementsForToS || resetAcknowledgementsForPrivacyPolicy || resetAcknowledgementsForNewUpdates;

        resetAcknowledgements(
                resetAcknowledgementsForToS,
                resetAcknowledgementsForPrivacyPolicy,
                resetAcknowledgementsForNewUpdates
        );

        if(updateFile) {
            updateServerData();
        }
    }

    @Override
    protected BrewerServerData createNewServerData() {
        return new BrewerServerData();
    }

    @Override
    public void onGuildJoin(@NonNull GuildJoinEvent event) {
        serverDataHashMap.put(event.getGuild().getIdLong(), createNewServerData());
        try {
            updateServerData();
        } catch (IOException e) {
            logger.error("Failed to update server data file.", e);
        }
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        serverDataHashMap.remove(event.getGuild().getIdLong());
        AnalyticsHandler.stopTrackingGuild(event.getGuild().getIdLong());
        try {
            updateServerData();
        } catch (IOException e) {
            logger.error("Failed to update server data file.", e);
        }
    }

    public void resetAcknowledgements(
            boolean tos,
            boolean privacyPolicy,
            boolean newUpdates
    ) {
        for(BrewerServerData serverData: serverDataHashMap.values()) {
            if(tos) {
                serverData.setAcknowledgedNewTos(false);
            }
            if(privacyPolicy) {
                serverData.setAcknowledgedNewPrivacyPolicy(false);
            }
            if(newUpdates) {
                serverData.setAcknowledgedNewUpdates(false);
            }
        }
    }
}
