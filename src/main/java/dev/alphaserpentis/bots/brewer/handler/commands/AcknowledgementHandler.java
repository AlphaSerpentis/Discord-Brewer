package dev.alphaserpentis.bots.brewer.handler.commands;

import com.google.gson.Gson;
import dev.alphaserpentis.bots.brewer.data.brewer.AcknowledgeThis;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Handles acknowledgements of TOS, Privacy Policy, and Updates
 */
public class AcknowledgementHandler {
    private static Path pathToAcknowledgementsDirectory;

    public static void init(@NonNull Path pathToAcknowledgementsDirectory) {
        AcknowledgementHandler.pathToAcknowledgementsDirectory = pathToAcknowledgementsDirectory;
    }

    public static MessageEmbed[] getAcknowledgementEmbeds(@NonNull ArrayList<AcknowledgeThis.Type> types) throws IOException {
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

    private static AcknowledgeThis tryToReadAcknowledgementFile(@NonNull final AcknowledgeThis.Type type) throws IOException {
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
                case TOS -> inputStream = classLoader.getResourceAsStream("default/acknowledgement/tos.json");
                case PRIVACY_POLICY -> inputStream = classLoader.getResourceAsStream("default/acknowledgement/privacy_policy.json");
                case UPDATES -> inputStream = classLoader.getResourceAsStream("default/acknowledgement/updates.json");
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
        } else {
            reader = new BufferedReader(new FileReader(file));
        }

        return gson.fromJson(reader, AcknowledgeThis.class);
    }
}
