package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.AcknowledgeThis;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.handler.bot.BrewerServerDataHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.AcknowledgementHandler;
import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;

public interface AcknowledgeableCommand<E extends GenericCommandInteractionEvent> {

    /**
     * Checks if a guild has acknowledged any changes to the TOS/Privacy Policy/bot updates, and then provides an
     * embed builder with the acknowledgement stuff
     * @param event The event to check
     * @return An array of embeds to send, or null if no acknowledgement is needed
     */
    @Nullable
    default MessageEmbed[] checkAndHandleAcknowledgement(@NonNull E event) throws IOException {
        BrewerServerData serverData;

        try {
            serverData = ((BrewerServerDataHandler) Launcher.core.getServerDataHandler()).getServerData(
                    event.getGuild().getIdLong()
            );
        } catch(NullPointerException ignored) {
            return null;
        }

        if(serverData != null) {
            ArrayList<AcknowledgeThis.Type> typesToAcknowledge = new ArrayList<>(3);

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

            if(!typesToAcknowledge.isEmpty()) {
                Launcher.core.getServerDataHandler().updateServerData();

                return AcknowledgementHandler.getAcknowledgementEmbeds(typesToAcknowledge);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
