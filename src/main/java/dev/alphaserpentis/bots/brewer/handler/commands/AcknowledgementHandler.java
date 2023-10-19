package dev.alphaserpentis.bots.brewer.handler.commands;

import com.google.gson.Gson;
import dev.alphaserpentis.bots.brewer.data.brewer.AcknowledgeThis;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles acknowledgements of TOS, Privacy Policy, and Updates
 */
public class AcknowledgementHandler {
    private static Path pathToAcknowledgementsDirectory;
    private static final String DEFAULT_TOS = "default/acknowledgement/tos.json";
    private static final String DEFAULT_PRIVACY_POLICY = "default/acknowledgement/privacy_policy.json";
    private static final String DEFAULT_UPDATES = "default/acknowledgement/updates.json";

    public static void init(@NonNull Path pathToAcknowledgementsDirectory) {
        AcknowledgementHandler.pathToAcknowledgementsDirectory = pathToAcknowledgementsDirectory;
    }

    public static MessageEmbed[] getAcknowledgementEmbeds(
            @NonNull List<AcknowledgeThis.Type> types
    ) throws IOException {
        MessageEmbed[] embeds = new MessageEmbed[types.size()];

        for(int i = 0; i < types.size(); i++) {
            AcknowledgeThis acknowledgement = tryToReadAcknowledgementFile(types.get(i));
            var embedBuilder = new EmbedBuilder();

            embedBuilder.setTitle(acknowledgement.title());
            embedBuilder.setDescription(acknowledgement.description());
            embedBuilder.setColor(Color.decode(acknowledgement.color()));
            embedBuilder.setFooter(acknowledgement.footer());

            embeds[i] = embedBuilder.build();
        }

        return embeds;
    }

    private static AcknowledgeThis tryToReadAcknowledgementFile(
            @NonNull final AcknowledgeThis.Type type
    ) throws IOException {
        File file;
        BufferedReader reader;
        Gson gson = new Gson();
        InputStream inputStream;

        switch(type) {
            case TOS -> file = new File(pathToAcknowledgementsDirectory + "/tos.json");
            case PRIVACY_POLICY -> file = new File(pathToAcknowledgementsDirectory + "/privacy_policy.json");
            case UPDATES -> file = new File(pathToAcknowledgementsDirectory + "/updates.json");
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        if(!file.exists()) {
            ClassLoader classLoader = AcknowledgementHandler.class.getClassLoader();
            switch(type) {
                case TOS -> inputStream = classLoader.getResourceAsStream(DEFAULT_TOS);
                case PRIVACY_POLICY -> inputStream = classLoader.getResourceAsStream(DEFAULT_PRIVACY_POLICY);
                case UPDATES -> inputStream = classLoader.getResourceAsStream(DEFAULT_UPDATES);
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
        } else {
            reader = new BufferedReader(new FileReader(file));
        }

        return gson.fromJson(reader, AcknowledgeThis.class);
    }
}
