package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class Vote extends BotCommand<MessageEmbed> {

    public Vote() {
        super(
                new BotCommandOptions(
                        "vote",
                        "Support Brewer by voting for it!",
                        true,
                        false,
                        TypeOfEphemeral.DEFAULT
                )
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Vote for Brewer!");
        eb.setDescription("By voting for Brewer, you can help share the power of Brewer with others! Your vote is greatly appreciated!");
        eb.addField("Top.gg", "https://top.gg/bot/1097362340468502548/vote", false);
        eb.addField("Discords.com", "https://discords.com/bots/bot/1097362340468502548", false);
        eb.addField("Discord Bot List", "https://discordbotlist.com/bots/brewer", false);
        eb.setFooter("Thank you for voting for Brewer!", event.getUser().getAvatarUrl());
        eb.setColor(Color.CYAN);

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }
}
