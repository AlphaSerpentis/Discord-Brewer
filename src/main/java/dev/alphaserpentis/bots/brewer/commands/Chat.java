package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Chat extends BotCommand<MessageEmbed, SlashCommandInteractionEvent>
        implements AcknowledgeableCommand<SlashCommandInteractionEvent> {

    public Chat() {
        super(
                new BotCommandOptions("chat", "Initiate a conversation with Brew(r)!")
                        .setDeferReplies(true)
                        .setOnlyEmbed(true)
                        .setUseRatelimits(true)
                        .setRatelimitLength(60)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        return null;
    }
}
