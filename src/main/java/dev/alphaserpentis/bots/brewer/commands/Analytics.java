package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class Analytics extends BotCommand<MessageEmbed, SlashCommandInteractionEvent> {

    public Analytics() {
        super(
                new BotCommandOptions("analytics", "Get analytics")
                        .setDeferReplies(true)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        var eb = new EmbedBuilder();

        return new CommandResponse<>(isOnlyEphemeral(), eb.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        var guild = new SubcommandData("guild", "Get the analytics for this guild");
        var bot = new SubcommandData("bot", "Get the analytics for the bot");

        jda.upsertCommand(getName(), getDescription()).addSubcommands(guild, bot).queue(
                cmd -> setGlobalCommandId(cmd.getIdLong())
        );
    }

    public String getAnonymizedGuildAnalytics(long guildId) {
        return null;
    }
}
