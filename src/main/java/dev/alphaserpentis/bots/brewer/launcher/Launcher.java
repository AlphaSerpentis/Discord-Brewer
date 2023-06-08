package dev.alphaserpentis.bots.brewer.launcher;

import dev.alphaserpentis.bots.brewer.commands.Brew;
import dev.alphaserpentis.bots.brewer.commands.Vote;
import dev.alphaserpentis.bots.brewer.handler.discord.StatusHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.other.TopGgHandler;
import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.AboutInformation;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.*;

public class Launcher {

    public static CoffeeCore core;

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder<?> builder = new CoffeeCoreBuilder<>();
        Brew brew = new Brew();
        Vote vote = new Vote();
        AboutInformation about;

        about = new AboutInformation(
                """
                Brewer is your open-source assistant, stirring up innovation in your Discord server with OpenAI's ChatGPT. Just prompt it, and watch as it brews new roles, categories, and channels, or even renames the existing ones!
                
                [**Invite Brewer to Your Server!**](https://asrp.dev/brewer-invite)
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
        TopGgHandler.init(dotenv.get("TOPGG_API_KEY"), core.getSelfUser().getId());
    }
}
