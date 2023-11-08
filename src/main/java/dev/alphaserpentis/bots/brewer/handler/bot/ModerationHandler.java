package dev.alphaserpentis.bots.brewer.handler.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.brewer.data.brewer.FlaggedContent;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;

/**
 * Handler that moderates content and activity pertaining to the usage of the bot
 */
public class ModerationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ModerationHandler.class);
    private static HashSet<Long> restrictedIds = new HashSet<>();
    private static Path flaggedContentDirectory;
    private static Path restrictedIdsDirectory;

    public static void init(@NonNull Path flaggedContentDirectory, @NonNull Path restrictedIdsDirectory) {
        ModerationHandler.flaggedContentDirectory = flaggedContentDirectory;
        ModerationHandler.restrictedIdsDirectory = restrictedIdsDirectory;

        try {
            if(!Files.exists(flaggedContentDirectory)) {
                Files.createDirectories(flaggedContentDirectory);
            }
            if(!Files.exists(restrictedIdsDirectory)) {
                Files.createFile(restrictedIdsDirectory);
            } else {
                tryToReadRestrictedIds();
            }
        } catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void writeFlaggedContentToDirectory(@NonNull FlaggedContent content) {
        var fileName = content.userId() + "-" + content.guildId() + "-" + Instant.now().getEpochSecond() + ".txt";

        try(var writer = new BufferedWriter(new FileWriter(flaggedContentDirectory.resolve(fileName).toFile()))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(content, writer);
        } catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void addRestrictedId(long id) {
        try {
            if(restrictedIds.add(id))
                tryToWriteToRestrictedIds();
        } catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void removeRestrictedId(long id) {
        try {
            if(restrictedIds.remove(id))
                tryToWriteToRestrictedIds();
        } catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Nullable
    public static EmbedBuilder isRestricted(long id, boolean isGuild) {
        if(id != 0 && isRestricted(id)) {
            var eb = new EmbedBuilder();
            var type = isGuild ? "Guild" : "User";
            eb.setTitle("Restricted " + type).setDescription("This " + type + " is restricted from using the bot");

            return eb;
        }
        return null;
    }

    public static boolean isRestricted(long id) {
        return restrictedIds.contains(id);
    }

    private static void tryToReadRestrictedIds() {
        try(var reader = Files.newBufferedReader(restrictedIdsDirectory)) {
            restrictedIds = new Gson().fromJson(reader, new TypeToken<HashSet<Long>>(){}.getType());
        } catch(IOException | JsonSyntaxException e) {
            logger.error(e.getMessage(), e);
            restrictedIds = new HashSet<>();
        }
    }

    private static void tryToWriteToRestrictedIds() throws IOException {
        Files.write(
                restrictedIdsDirectory,
                new GsonBuilder().setPrettyPrinting().create().toJson(restrictedIds).getBytes()
        );
    }
}
