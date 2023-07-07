package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.handler.openai.CustomOpenAiService;
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
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TranscribeContext extends BotCommand<MessageEmbed[], MessageContextInteractionEvent> implements AcknowledgeableCommand<MessageContextInteractionEvent> {

    public TranscribeContext() {
        super(
                new BotCommandOptions()
                        .setName("Transcribe Attachments")
                        .setCommandType(Command.Type.MESSAGE)
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed[]> runCommand(long userId, @NonNull MessageContextInteractionEvent event) {
        MessageEmbed[] embedsArray;
        EmbedBuilder workingEmbed;
        List<Message.Attachment> attachments = tryToGetAudioFiles(event);
        StringBuilder description = new StringBuilder();

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        if(embedsArray == null) {
            workingEmbed = new EmbedBuilder();
        } else {
            return new CommandResponse<>(isOnlyEphemeral(), embedsArray);
        }

        for(Message.Attachment attachment: attachments) {
            AudioTranscriptionResponse response = OpenAIHandler.getAudioTranscription(attachment.getUrl());

            if(response.isCached()) {
                description.append("# Cached Transcription of ").append(attachment.getFileName()).append("\n");
            } else {
                description.append("# Transcription of ").append(attachment.getFileName()).append("\n");
            }

            description.append(response.text()).append("\n\n");
            workingEmbed.setColor(Color.GREEN);
        }
        if(description.length() == 0) {
            description.append("No audio files found!");
            workingEmbed.setColor(Color.RED);
        }

        workingEmbed.setDescription(description.toString());
        workingEmbed.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");

        return new CommandResponse<>(isOnlyEphemeral(), workingEmbed.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        CommandData cmdData = getJDACommandData(getCommandType(), getName(), getDescription());

        jda
                .upsertCommand(cmdData)
                .queue(r -> setCommandId(r.getIdLong()));
    }

    @NonNull
    private List<Message.Attachment> tryToGetAudioFiles(@NonNull MessageContextInteractionEvent event) {
        ArrayList<Message.Attachment> attachments = new ArrayList<>(event.getTarget().getAttachments());

        attachments.removeIf(
                attachment -> attachment.getDuration() == 0 && CustomOpenAiService.SUPPORTED_EXTENSIONS.stream().noneMatch(
                        extension -> attachment.getFileName().endsWith(extension)
                )
        );

        return attachments;
    }
}
