package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TranscribeContext extends BotCommand<MessageEmbed, MessageContextInteractionEvent>
        implements AcknowledgeableCommand<MessageContextInteractionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeContext.class);

    public TranscribeContext() {
        super(
                new BotCommandOptions()
                        .setName("Transcribe Attachments")
                        .setCommandType(Command.Type.MESSAGE)
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
                        .setRatelimitLength(60)
                        .setUseRatelimits(true)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull MessageContextInteractionEvent event) {
        EmbedBuilder workingEmbed;
        CommandResponse<MessageEmbed> rateLimitResponse;
        var description = new StringBuilder();
        MessageEmbed[] embedsArray;

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch (IOException e) {
            logger.error("Failed to check and handle acknowledgement", e);

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

        for(Message.Attachment attachment: tryToGetAudioFiles(event)) {
            var response = OpenAIHandler.getAudioTranscription(attachment.getUrl());

            if(response.isCached()) {
                description.append("# Cached Transcription of ").append(attachment.getFileName()).append("\n");
            } else {
                description.append("# Transcription of ").append(attachment.getFileName()).append("\n");
            }

            description.append(response.text()).append("\n\n");
            workingEmbed.setColor(Color.GREEN);
        }
        if(description.isEmpty()) {
            description.append("No audio files found!");
            workingEmbed.setColor(Color.RED);
        }

        workingEmbed.setDescription(description.toString());
        workingEmbed.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");

        AnalyticsHandler.addUsage(
                event.getGuild(),
                ServiceType.TRANSCRIBE_ATTACHMENT
        );

        return new CommandResponse<>(isOnlyEphemeral(), workingEmbed.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        CommandData cmdData = getJDACommandData(getCommandType(), getName(), getDescription());

        jda
                .upsertCommand(cmdData)
                .queue(r -> setGlobalCommandId(r.getIdLong()));
    }

    @NonNull
    private List<Message.Attachment> tryToGetAudioFiles(@NonNull MessageContextInteractionEvent event) {
        var attachments = new ArrayList<>(event.getTarget().getAttachments());

        attachments.removeIf(
                attachment -> attachment.getDuration() == 0 && CustomOpenAiService.SUPPORTED_EXTENSIONS.stream()
                        .noneMatch(extension -> attachment.getFileName().endsWith(extension))
        );

        return attachments;
    }
}
