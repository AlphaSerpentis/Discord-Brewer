package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.AcknowledgeThis;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.handler.bot.BrewerServerDataHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.AcknowledgementHandler;
import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @param <E> The type of the event
 */
public interface AcknowledgeableCommand<E extends GenericCommandInteractionEvent> {

    /**
     * Checks if a guild has acknowledged any changes to the TOS/Privacy Policy/bot updates.
     *
     * @param event The event to check.
     * @return
     */
    default MessageEmbed[] checkAndHandleAcknowledgement(@NonNull E event) throws IOException {
        BrewerServerData serverData = getServerData(event.getGuild().getIdLong());

        if(serverData == null) {
            return null;
        }

        List<AcknowledgeThis.Type> typesToAcknowledge = getTypesToAcknowledge(serverData);

        if(typesToAcknowledge.isEmpty()) {
            return null;
        }

        Launcher.core.getServerDataHandler().updateServerData();
        return AcknowledgementHandler.getAcknowledgementEmbeds((ArrayList<AcknowledgeThis.Type>) typesToAcknowledge);
    }

    /**
     * Retrieves the server data.
     *
     * @param guildId The ID of the guild.
     * @return The BrewerServerData for the guild, or null if not found.
     */
    default BrewerServerData getServerData(long guildId) {
        return ((BrewerServerDataHandler) Launcher.core.getServerDataHandler()).getServerData(guildId);
    }

    /**
     * Determines the types of acknowledgements needed for the given server data.
     *
     * @param serverData The server data to check.
     * @return A list of types that need acknowledgement.
     */
    default List<AcknowledgeThis.Type> getTypesToAcknowledge(BrewerServerData serverData) {
        List<AcknowledgeThis.Type> typesToAcknowledge = new ArrayList<>(3);

        if(!serverData.getAcknowledgedNewTos()) {
            typesToAcknowledge.add(AcknowledgeThis.Type.TOS);
            serverData.setAcknowledgedNewTos(true);
        }
        if(!serverData.getAcknowledgedNewPrivacyPolicy()) {
            typesToAcknowledge.add(AcknowledgeThis.Type.PRIVACY_POLICY);
            serverData.setAcknowledgedNewPrivacyPolicy(true);
        }
        if(!serverData.getAcknowledgedNewUpdates()) {
            typesToAcknowledge.add(AcknowledgeThis.Type.UPDATES);
            serverData.setAcknowledgedNewUpdates(true);
        }

        return typesToAcknowledge;
    }
}

