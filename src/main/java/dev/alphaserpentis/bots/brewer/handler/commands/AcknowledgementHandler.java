package dev.alphaserpentis.bots.brewer.handler.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.AcknowledgeThis;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;

import java.nio.file.Path;

/**
 * Handles acknowledgements of TOS, Privacy Policy, and Updates
 */
public class AcknowledgementHandler {
    private static Path pathToAcknowledgementsDirectory;
    private static boolean tos, privacyPolicy, updates;

    public static void init(
            Path pathToAcknowledgementsDirectory,
            boolean resetAcknowledgementsForToS,
            boolean resetAcknowledgementsForPrivacyPolicy,
            boolean resetAcknowledgementsForNewUpdates
    ) {
        AcknowledgementHandler.pathToAcknowledgementsDirectory = pathToAcknowledgementsDirectory;
        AcknowledgementHandler.tos = resetAcknowledgementsForToS;
        AcknowledgementHandler.privacyPolicy = resetAcknowledgementsForPrivacyPolicy;
        AcknowledgementHandler.updates = resetAcknowledgementsForNewUpdates;
    }

    public EmbedBuilder getAcknowledgementEmbed(@NonNull AcknowledgeThis.Type type) {
        EmbedBuilder eb = new EmbedBuilder();

        return eb;
    }
}
