package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class Transcribe extends BotCommand<MessageEmbed, SlashCommandInteractionEvent> {
    public Transcribe() {
        super(
                new BotCommandOptions()
                        .setName("transcribe")
                        .setDescription("Transcribes an audio file")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
        );
    }

    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Transcribe");
        eb.setDescription(
                OpenAIHandler.getAudioTranscription(
                        event.getOption("url").getAsString()
                ).getText()
        );

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        jda
                .upsertCommand(getName(), getDescription())
                .addOption(OptionType.STRING, "url", "The URL of the audio file to transcribe", true)
                .queue(r -> setCommandId(r.getIdLong()));
    }
}
