package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranscribeContext extends BotCommand<MessageEmbed, MessageContextInteractionEvent> {
    private static final String[] SUPPORTED_EXTENSIONS = new String[] {
            "mp3",
            "mp4",
            "mpeg",
            "mpga",
            "m4a",
            "wav",
            "webm"
    };

    public TranscribeContext() {
        super(
                new BotCommandOptions()
                        .setName("Transcribe Attachments")
                        .setDescription("")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull MessageContextInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        List<Message.Attachment> attachments = tryToGetAudioFiles(event);
        StringBuilder description = new StringBuilder();

        for(Message.Attachment attachment: attachments) {
            AudioTranscriptionResponse response = OpenAIHandler.getAudioTranscription(attachment.getUrl());

            if(response.isCached()) {
                description.append("# Cached Transcription of ").append(attachment.getFileName()).append("\n");
            } else {
                description.append("# Transcription of ").append(attachment.getFileName()).append("\n");
            }

            description.append(response.text()).append("\n\n");
            eb.setColor(Color.GREEN);
        }
        if(description.length() == 0) {
            description.append("No audio files found!");
            eb.setColor(Color.RED);
        }

        eb.setDescription(description.toString());
        eb.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        CommandDataImpl cmdData = new CommandDataImpl(
                Command.Type.MESSAGE,
                getName()
        );

        jda
                .upsertCommand(cmdData)
                .queue(r -> setCommandId(r.getIdLong()));
    }

    @NonNull
    private List<Message.Attachment> tryToGetAudioFiles(@NonNull MessageContextInteractionEvent event) {
        ArrayList<Message.Attachment> attachments = new ArrayList<>(event.getTarget().getAttachments());

        attachments.removeIf(
                attachment -> attachment.getDuration() == 0 && Arrays.stream(SUPPORTED_EXTENSIONS).noneMatch(
                        extension -> attachment.getFileName().endsWith(extension)
                )
        );

        return attachments;
    }
}
