package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.coffeecore.data.entity.EntityData;
import dev.alphaserpentis.coffeecore.handler.api.discord.entities.DataHandler;
import dev.alphaserpentis.coffeecore.serialization.EntityDataDeserializer;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class BrewerServerDataHandler<T extends EntityData> extends DataHandler<T> {

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
            @NonNull TypeToken<Map<String, Map<Long, T>>> typeToken,
            @NonNull EntityDataDeserializer<T> jsonDeserializer,
            boolean resetTosAcknowledgement,
            boolean resetPrivacyPolicyAcknowledgement,
            boolean resetUpdateAcknowledgement
    ) throws IOException {
        super(path, typeToken, jsonDeserializer);

        resetAcknowledgements(
                resetTosAcknowledgement,
                resetPrivacyPolicyAcknowledgement,
                resetUpdateAcknowledgement
        );
    }

    @Override
    protected void handleEntityDataException(@NonNull Exception e) {
        logger.error("Failed to update server data file.", e);
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        super.onGuildLeave(event);
        AnalyticsHandler.stopTrackingGuild(event.getGuild().getIdLong());
    }

    public void resetAcknowledgements(boolean tos, boolean privacyPolicy, boolean newUpdates) {
        entityDataHashMap
                .get("guild")
                .forEach((id, serverData) -> {
                    BrewerServerData castedData = (BrewerServerData) serverData;

                    if(tos)
                        castedData.setAcknowledgedNewTos(false);
                    if(privacyPolicy)
                        castedData.setAcknowledgedNewPrivacyPolicy(false);
                    if(newUpdates)
                        castedData.setAcknowledgedNewUpdates(false);
                });
    }
}
