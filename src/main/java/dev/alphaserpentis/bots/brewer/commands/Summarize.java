package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.IOException;

public class Summarize extends BotCommand<MessageEmbed, SlashCommandInteractionEvent>
    implements AcknowledgeableCommand<SlashCommandInteractionEvent> {

    public Summarize() {
        super(
                new BotCommandOptions()
                        .setName("summarize")
                        .setDescription("Summarize a message!")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
                        .setRatelimitLength(60)
                        .setUseRatelimits(true)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder workingEmbed;
        CommandResponse<MessageEmbed> rateLimitResponse;
        MessageEmbed[] embedsArray;

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(embedsArray != null) {
            return new CommandResponse<>(isOnlyEphemeral(), true, embedsArray);
        }

        // Check rate limit
        rateLimitResponse = (CommandResponse<MessageEmbed>) checkAndHandleRateLimitedUser(userId);

        if(rateLimitResponse != null)
            return rateLimitResponse;

        workingEmbed = new EmbedBuilder();

        return null;
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {

    }
}
