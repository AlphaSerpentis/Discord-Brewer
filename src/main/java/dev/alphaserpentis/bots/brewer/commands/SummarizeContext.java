package dev.alphaserpentis.bots.brewer.commands;

import com.theokanning.openai.completion.chat.ChatCompletionResult;
import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.data.openai.ChatCompletionModels;
import dev.alphaserpentis.bots.brewer.data.openai.Prompts;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SummarizeContext extends BotCommand<MessageEmbed, MessageContextInteractionEvent>
    implements AcknowledgeableCommand<MessageContextInteractionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SummarizeContext.class);
    private static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of(".txt", ".json", ".md");

    public SummarizeContext() {
        super(
                new BotCommandOptions()
                        .setName("Summarize Message")
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
            embedsArray = checkAndHandleAcknowledgement(event);
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

        AnalyticsHandler.addUsage(
                event.getGuild(),
                ServiceType.SUMMARIZE_CONTEXT
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

    private void summarize(@NonNull MessageContextInteractionEvent event, @NonNull EmbedBuilder eb) {
        ChatCompletionResult result;

        eb.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");

        // Check if there's text in the message
        if(event.getTarget().getContentRaw().isBlank()) {
            // Check if there are any attachments
            List<Message.Attachment> attachments = new ArrayList<>(event.getTarget().getAttachments());

            if(attachments.isEmpty()) {
                eb.setDescription("No text or attachments found!");
                eb.setColor(0xff0000);
            } else {
                Message.Attachment attachment = attachments.get(0);

                if(SUPPORTED_FILE_EXTENSIONS.stream().anyMatch(attachment.getFileName()::endsWith)) {
                    try {
                        InputStream inputStream = attachment.getProxy().download().get();
                        result = OpenAIHandler.getCompletion(
                                ChatCompletionModels.GPT_3_5_TURBO.getName(),
                                Prompts.SETUP_SYSTEM_PROMPT_SUMMARIZE_TEXT,
                                new String(inputStream.readAllBytes()),
                                0.25,
                                0.1,
                                0.1
                        );

                        eb.setDescription(cleanOutput(result.getChoices().get(0).getMessage().getContent()));
                        eb.setColor(0x00ff00);
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        eb.setDescription("Unable to read attachment!");
                        eb.setColor(0xff0000);

                        logger.error(e.getMessage(), e);
                    }
                } else {
                    eb.setDescription("""
                                No text or attachments found!
                                
                                **Note:** Only .txt, .md, and .json files are supported.
                                """);
                    eb.setColor(0xff0000);
                }
            }
        } else {
            result = OpenAIHandler.getCompletion(
                    ChatCompletionModels.GPT_3_5_TURBO.getName(),
                    Prompts.SETUP_SYSTEM_PROMPT_SUMMARIZE_TEXT,
                    event.getTarget().getContentRaw(),
                    0.25,
                    0.1,
                    0.1
            );

            eb.setDescription(cleanOutput(result.getChoices().get(0).getMessage().getContent()));
            eb.setColor(0x00ff00);
        }
    }

    private String cleanOutput(@NonNull String input) {
        if(input.startsWith("\"")) {
            input = input.substring(1);
        }

        return input;
    }
}
