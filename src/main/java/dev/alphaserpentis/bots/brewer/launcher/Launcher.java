package dev.alphaserpentis.bots.brewer.launcher;

import dev.alphaserpentis.bots.brewer.commands.Brew;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.core.CoffeeCoreBuilder;
import dev.alphaserpentis.coffeecore.data.bot.BotSettings;
import io.github.cdimascio.dotenv.Dotenv;

public class Launcher {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        CoffeeCoreBuilder<?> builder = new CoffeeCoreBuilder<>();
        Brew brew = new Brew();

        builder
                .setSettings(
                        new BotSettings(
                                Long.parseLong(dotenv.get("BOT_OWNER_ID")),
                                dotenv.get("SERVER_DATA_PATH"),
                                Boolean.parseBoolean(dotenv.get("UPDATE_COMMANDS_AT_LAUNCH")),
                                Boolean.parseBoolean(dotenv.get("REGISTER_DEFAULT_COMMANDS"))
                        )
                )
                .enableSharding(true);

        builder.build(dotenv.get("DISCORD_BOT_TOKEN")).registerCommands(
                brew
        );

        OpenAIHandler.init(dotenv.get("OPENAI_API_KEY"));
    }
}
