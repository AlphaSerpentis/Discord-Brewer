package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TranslateContext extends BotCommand<MessageEmbed, MessageContextInteractionEvent>
        implements AcknowledgeableCommand<MessageContextInteractionEvent> {

    public TranslateContext() {
        super(
                new BotCommandOptions()
                        .setName("Translate Attachments")
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
        EmbedBuilder serverCheckEmbed;
        EmbedBuilder userCheckEmbed;
        List<Message.Attachment> attachments = tryToGetAudioFiles(event);
        var description = new StringBuilder();
        CommandResponse<MessageEmbed> rateLimitResponse;
        MessageEmbed[] embedsArray;
        long guildId;

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

        // Check if user/guild is restricted
        guildId = event.getGuild() == null ? 0 : event.getGuild().getIdLong();

        serverCheckEmbed = ModerationHandler.isRestricted(guildId, true);
        if(serverCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), serverCheckEmbed.build());

        userCheckEmbed = ModerationHandler.isRestricted(event.getUser().getIdLong(), false);
        if(userCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), userCheckEmbed.build());

        workingEmbed = new EmbedBuilder();

        for(Message.Attachment attachment: attachments) {
            AudioTranslationResponse response = OpenAIHandler.getAudioTranslation(attachment.getUrl());

            if(response.isCached()) {
                description.append("# Cached Translation of ").append(attachment.getFileName()).append("\n");
            } else {
                description.append("# Translation of ").append(attachment.getFileName()).append("\n");
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

        AnalyticsHandler.addUsage(event.getGuild(), ServiceType.TRANSLATE_ATTACHMENT);

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
        ArrayList<Message.Attachment> attachments = new ArrayList<>(event.getTarget().getAttachments());

        attachments.removeIf(
                attachment -> attachment.getDuration() == 0 && CustomOpenAiService.SUPPORTED_EXTENSIONS.stream().noneMatch(
                        extension -> attachment.getFileName().endsWith(extension)
                )
        );

        return attachments;
    }
}
