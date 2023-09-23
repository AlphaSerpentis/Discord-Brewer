package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.handler.other.TopGgHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;

public class Vote extends BotCommand<MessageEmbed, SlashCommandInteractionEvent> {

    public Vote() {
        super(
                new BotCommandOptions()
                        .setName("vote")
                        .setDescription("Support Brewer by voting for it!")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        String description = """
        By voting for Brewer, you can help share the power of Brewer with others! Your vote is greatly appreciated!
        
        ### [Top.gg](https://top.gg/bot/1097362340468502548/vote)
        Voted: %s
        ### [Discords.com](https://discords.com/bots/bot/1097362340468502548)
        Voted: Unable to Check
        ### [Discord Bot List](https://discordbotlist.com/bots/brewer)
        Voted: Unable to Check
        
        """;
        eb.setTitle("Vote for Brewer!");
        try {
            eb.setDescription(description.formatted(TopGgHandler.didUserVote(event.getUser().getId()) ? "✅" : "❌"));
        } catch (Exception e) {
            eb.setDescription(description.formatted("Unable to Check"));
        }
        eb.setColor(Color.CYAN);
        eb.setFooter("Thank you for voting for Brewer!", event.getUser().getAvatarUrl());

        return new CommandResponse<>(isOnlyEphemeral(), eb.build());
    }
}
