package dev.alphaserpentis.bots.brewer.launcher;

import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.brewer.commands.*;
import dev.alphaserpentis.bots.brewer.data.serialization.BrewerServerDataDeserializer;
import dev.alphaserpentis.bots.brewer.executor.CustomExecutors;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.BrewerServerDataHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.AcknowledgementHandler;
import dev.alphaserpentis.bots.brewer.handler.discord.StatusHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.other.TopGgHandler;
import dev.alphaserpentis.coffeecore.commands.defaultcommands.About;
import dev.alphaserpentis.coffeecore.commands.defaultcommands.Help;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.AboutInformation;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import dev.alphaserpentis.coffeecore.handler.api.discord.commands.CommandsHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Launcher {

    public static CoffeeCore core;

    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();

        buildAndConfigureCore(dotenv);
        initializeHandlers(dotenv);
        setEnvAcknowledgementsBackToFalse();
    }

    private static void buildAndConfigureCore(@NonNull Dotenv dotenv) throws IOException {
        AboutInformation about = new AboutInformation(
                """
                Brewer is your open-source assistant, stirring up innovation in your Discord server with OpenAI's ChatGPT. Just prompt it, and watch as it brews new roles, categories, and channels, or even renames the existing ones!
                
                [**Invite Brewer to Your Server!**](https://brewr.ai/invite)
                [**GitHub**](https://github.com/AlphaSerpentis/Discord-Brewer)
                [**Discord Server**](https://brewr.ai/discord)
                """,
                "asrp.dev/#bots",
                Color.ORANGE,
                null,
                true,
                true
        );
        CoffeeCoreBuilder<?> builder = new CoffeeCoreBuilder<>()
                .setSettings(
                        new BotSettings(
                                Long.parseLong(dotenv.get("BOT_OWNER_ID")),
                                dotenv.get("SERVER_DATA_PATH"),
                                Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                                Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS")),
                                about
                        )
                ).setEnabledGatewayIntents(
                        List.of(
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_VOICE_STATES
                        )
                )
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                .setEnabledCacheFlags(List.of(CacheFlag.VOICE_STATE))
                .setDisabledCacheFlags(List.of())
                .setServerDataHandler(
                        new BrewerServerDataHandler(
                                Path.of(dotenv.get("SERVER_DATA_PATH")),
                                new TypeToken<>() {},
                                new BrewerServerDataDeserializer(),
                                Boolean.parseBoolean(dotenv.get("RESET_ACKNOWLEDGEMENT_TOS")),
                                Boolean.parseBoolean(dotenv.get("RESET_ACKNOWLEDGEMENT_PRIVACY")),
                                Boolean.parseBoolean(dotenv.get("RESET_ACKNOWLEDGEMENT_UPDATES"))
                        )
                )
                .setCommandsHandler(new CommandsHandler(CustomExecutors.newCachedThreadPool(2)))
                .enableSharding(true);

        core = builder.build(dotenv.get("DISCORD_BOT_TOKEN"));

        core.registerCommands(
                new CustomSettings(),
                new Help(),
                new About(),
                new Brew(),
                new Vote(),
                new Transcribe(),
                new TranscribeContext()
        );
    }

    private static void initializeHandlers(@NonNull Dotenv dotenv) {
        OpenAIHandler.init(
                dotenv.get("OPENAI_API_KEY"),
                Path.of(dotenv.get("FLAGGED_CONTENT_DIRECTORY")),
                Path.of(dotenv.get("TRANSCRIPTION_CACHE_PATH")),
                Path.of(dotenv.get("TRANSLATION_CACHE_PATH"))
        );
        StatusHandler.init(core);
        TopGgHandler.init(dotenv.get("TOPGG_API_KEY"), core.getSelfUser().getId());
        AcknowledgementHandler.init(
                Path.of(dotenv.get("ACKNOWLEDGEMENTS_PATH"))
        );
        AnalyticsHandler.init(
                Path.of(dotenv.get("ANALYTICS_PATH"))
        );
    }

    private static void setEnvAcknowledgementsBackToFalse() {
        try {
            FileReader fileReader = new FileReader(".env");
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("RESET_ACKNOWLEDGEMENT_TOS")) {
                    lines.add("RESET_ACKNOWLEDGEMENT_TOS=false");
                } else if (line.startsWith("RESET_ACKNOWLEDGEMENT_PRIVACY")) {
                    lines.add("RESET_ACKNOWLEDGEMENT_PRIVACY=false");
                } else if (line.startsWith("RESET_ACKNOWLEDGEMENT_UPDATES")) {
                    lines.add("RESET_ACKNOWLEDGEMENT_UPDATES=false");
                } else {
                    lines.add(line);
                }
            }

            fileReader.close();

            FileWriter fileWriter = new FileWriter(".env");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (String outputLine : lines) {
                bufferedWriter.write(outputLine);
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            fileWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
