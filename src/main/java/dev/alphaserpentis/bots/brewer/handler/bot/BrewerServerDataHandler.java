package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class BrewerServerDataHandler extends ServerDataHandler<BrewerServerData> {
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
        boolean updateFile = false;

        if(resetAcknowledgementsForToS) {
            resetAcknowledgementsForToS();
            updateFile = true;
        }
        if(resetAcknowledgementsForPrivacyPolicy) {
            resetAcknowledgementsForPrivacyPolicy();
            updateFile = true;
        }
        if(resetAcknowledgementsForNewUpdates) {
            resetAcknowledgementsForNewUpdates();
            updateFile = true;
        }

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
            throw new RuntimeException(e);
        }
    }

    public void resetAcknowledgementsForToS() {
        for(BrewerServerData serverData: serverDataHashMap.values()) {
            serverData.setAcknowledgedNewTos(false);
        }
    }

    public void resetAcknowledgementsForPrivacyPolicy() {
        for(BrewerServerData serverData: serverDataHashMap.values()) {
            serverData.setAcknowledgedNewPrivacyPolicy(false);
        }
    }

    public void resetAcknowledgementsForNewUpdates() {
        for(BrewerServerData serverData: serverDataHashMap.values()) {
            serverData.setAcknowledgedNewUpdates(false);
        }
    }
}
