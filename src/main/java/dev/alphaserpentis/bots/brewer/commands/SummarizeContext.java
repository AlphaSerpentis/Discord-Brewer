package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.summarize.SummarizeHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SummarizeContext extends BotCommand<MessageEmbed, MessageContextInteractionEvent>
    implements AcknowledgeableCommand<MessageContextInteractionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SummarizeContext.class);
    private static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of(".txt", ".json", ".md");
    private static final String NOTHING_FOUND = """
            No text or attachments found!
            
            **Note:** Only .txt, .md, and .json files are supported.""";

    public SummarizeContext() {
        super(
                new BotCommandOptions()
                        .setName("Summarize Message")
                        .setHelpDescription("Summarizes a text message")
                        .setCommandType(Command.Type.MESSAGE)
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
                        .setRatelimitLength(60)
                        .setUseRatelimits(true)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull MessageContextInteractionEvent event) {
        EmbedBuilder workingEmbed;
        EmbedBuilder serverCheckEmbed;
        EmbedBuilder userCheckEmbed;
        CommandResponse<MessageEmbed> rateLimitResponse;
        MessageEmbed[] embedsArray;
        long guildId;

        try {
            embedsArray = checkAndHandleAcknowledgement(event, false);
        } catch(IOException e) {
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

        // Check if user/guild is restricted
        guildId = event.getGuild() == null ? 0 : event.getGuild().getIdLong();

        serverCheckEmbed = ModerationHandler.isRestricted(guildId, true);
        if(serverCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), serverCheckEmbed.build());

        userCheckEmbed = ModerationHandler.isRestricted(event.getUser().getIdLong(), false);
        if(userCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), userCheckEmbed.build());

        workingEmbed = new EmbedBuilder();

        // Pull the message and summarize it
        summarize(event, workingEmbed);

        AnalyticsHandler.addUsage(event.getGuild(), ServiceType.SUMMARIZE_CONTEXT);

        return new CommandResponse<>(isOnlyEphemeral(), workingEmbed.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        jda
                .upsertCommand(getJDACommandData(getCommandType(), getName(), getDescription()))
                .queue(r -> setGlobalCommandId(r.getIdLong()));
    }

    private void summarize(@NonNull MessageContextInteractionEvent event, @NonNull EmbedBuilder eb) {
        eb.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");

        // Check if there's text in the message
        if(event.getTarget().getContentRaw().isBlank()) {
            // Check if there are any attachments
            List<Message.Attachment> attachments = new ArrayList<>(event.getTarget().getAttachments());

            if(attachments.isEmpty()) {
                eb.setDescription(NOTHING_FOUND);
                eb.setColor(0xff0000);
            } else {
                Message.Attachment attachment = attachments.get(0);

                if(SUPPORTED_FILE_EXTENSIONS.stream().anyMatch(attachment.getFileName()::endsWith)) {
                    try {
                        eb.setDescription(
                                SummarizeHandler.generateSummarization(
                                        new String(attachment.getProxy().download().get().readAllBytes())
                                )
                        );
                        eb.setColor(0x00ff00);
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        eb.setDescription("Unable to read attachment!");
                        eb.setColor(0xff0000);

                        logger.error(e.getMessage(), e);
                    }
                } else {
                    eb.setDescription(NOTHING_FOUND);
                    eb.setColor(0xff0000);
                }
            }
        } else {
            eb.setDescription(SummarizeHandler.generateSummarization(event.getTarget().getContentRaw()));
            eb.setColor(0x00ff00);
        }
    }
}
