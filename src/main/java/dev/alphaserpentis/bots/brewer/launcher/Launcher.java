package dev.alphaserpentis.bots.brewer.launcher;

import dev.alphaserpentis.bots.brewer.commands.Brew;
import dev.alphaserpentis.bots.brewer.commands.Vote;
import dev.alphaserpentis.bots.brewer.handler.discord.StatusHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.AboutInformation;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.*;

public class Launcher {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder<?> builder = new CoffeeCoreBuilder<>();
        CoffeeCore core;
        Brew brew = new Brew();
        Vote vote = new Vote();
        AboutInformation about;

        about = new AboutInformation(
                """
                Brewer is an open-source bot that uses OpenAI's ChatGPT to rename or generate new roles/categories/channels with the touch of a prompt!
                
                [**GitHub**](https://github.com/AlphaSerpentis/Discord-Brewer)
                [**Discord Server**](https://asrp.dev/discord)
                """,
                "asrp.dev/#bots",
                Color.ORANGE,
                null,
                true,
                true
        );

        builder
                .setSettings(
                        new BotSettings(
                                Long.parseLong(dotenv.get("BOT_OWNER_ID")),
                                dotenv.get("SERVER_DATA_PATH"),
                                Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                                Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS")),
                                about
                        )
                )
                .enableSharding(true);

        core = builder.build(dotenv.get("DISCORD_BOT_TOKEN"));
        core.registerCommands(
                brew,
                vote
        );

        OpenAIHandler.init(dotenv.get("OPENAI_API_KEY"));
        StatusHandler.init(core);
    }
}
