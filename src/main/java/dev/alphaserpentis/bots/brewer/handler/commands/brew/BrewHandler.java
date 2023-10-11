package dev.alphaserpentis.bots.brewer.handler.commands.brew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatMessage;
import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.data.brewer.UserSession;
import dev.alphaserpentis.bots.brewer.data.discord.DiscordConfig;
import dev.alphaserpentis.bots.brewer.data.openai.Prompts;
import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.parser.ParseActions;
import dev.alphaserpentis.bots.brewer.launcher.Launcher;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.entities.channel.concrete.CategoryImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.ForumChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.MediaChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.StageChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.VoiceChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.NewsChannelImpl;
import net.dv8tion.jda.internal.entities.channel.middleman.AbstractStandardGuildMessageChannelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BrewHandler {

    public static Logger logger = LoggerFactory.getLogger(BrewHandler.class);

    private static final HashMap<Long, UserSession> userSessions = new HashMap<>();

    public static UserSession getUserSession(@NonNull long userId) {
        return userSessions.get(userId);
    }

    public static void removeUserSession(@NonNull long userId) {
        userSessions.remove(userId);
    }

    public static void generateCreatePrompt(
            @NonNull EmbedBuilder eb,
            @NonNull String prompt,
            @NonNull SlashCommandInteractionEvent event
    ) {
        ArrayList<ParseActions.ExecutableAction> actions;
        UserSession session;

        AnalyticsHandler.addUsage(event.getGuild(), ServiceType.CREATE);

        try {
            actions = generateActions(
                    Prompts.SETUP_SYSTEM_PROMPT_CREATE,
                    prompt,
                    ParseActions.ValidAction.CREATE
            );
        } catch(JsonSyntaxException e) {
            logger.error("JSONSyntaxException thrown in generateCreatePrompt", e);

            throw new GenerationException(GenerationException.Type.JSON_EXCEPTION.getDescriptions(), e.getCause());
        } catch(OpenAiHttpException e) {
            logger.error("OpenAiHttpException thrown in generateCreatePrompt", e);

            throw new GenerationException(GenerationException.Type.OVERLOADED_EXCEPTION.getDescriptions(), e.getCause());
        } catch(SocketTimeoutException e) {
            logger.error("SocketTimeoutException thrown in generateCreatePrompt", e);

            throw new GenerationException(GenerationException.Type.TIMEOUT_EXCEPTION.getDescriptions(), e.getCause());
        }

        previewChangesPage(eb, actions);
        session = new UserSession(
                prompt,
                UserSession.UserSessionType.NEW_BREW,
                ParseActions.ValidAction.CREATE,
                actions
        );
        session
                .setJDA(event.getJDA())
                .setInteractionToken(event.getInteraction().getToken())
                .setGuildId(event.getGuild().getIdLong());
        userSessions.put(event.getUser().getIdLong(), session);
    }

    public static void generateRenamePrompt(
            @NonNull EmbedBuilder eb,
            @NonNull String prompt,
            @NonNull SlashCommandInteractionEvent event
    ) {
        boolean allowNsfwChannelRenames = (
                (BrewerServerData) Launcher.core.getServerDataHandler().getServerData(event.getGuild().getIdLong())
        ).getTryRenamingNsfwChannels();
        ArrayList<ParseActions.ExecutableAction> actions;
        UserSession session;

        try {
            actions = generateActions(
                    Prompts.SETUP_SYSTEM_PROMPT_RENAME,
                    new GsonBuilder().setPrettyPrinting().create().toJson(
                            getGuildData(event.getGuild(), prompt, allowNsfwChannelRenames)
                    ),
                    ParseActions.ValidAction.EDIT
            );

            optimizeRenameActions(actions);
        } catch(JsonSyntaxException e) {
            logger.error("JSONSyntaxException thrown in generateRenamePrompt", e);

            throw new GenerationException(GenerationException.Type.JSON_EXCEPTION.getDescriptions(), e.getCause());
        } catch(OpenAiHttpException e) {
            logger.error("OpenAiHttpException thrown in generateRenamePrompt", e);

            throw new GenerationException(GenerationException.Type.OVERLOADED_EXCEPTION.getDescriptions(), e.getCause());
        } catch(SocketTimeoutException e) {
            logger.error("SocketTimeoutException thrown in generateRenamePrompt", e);

            throw new GenerationException(GenerationException.Type.TIMEOUT_EXCEPTION.getDescriptions(), e.getCause());
        } finally {
            AnalyticsHandler.addUsage(event.getGuild(), ServiceType.RENAME);
        }

        previewChangesPage(eb, actions);
        session = new UserSession(
                prompt,
                UserSession.UserSessionType.RENAME,
                ParseActions.ValidAction.EDIT,
                actions
        );
        session
                .setJDA(event.getJDA())
                .setInteractionToken(event.getInteraction().getToken())
                .setGuildId(event.getGuild().getIdLong());
        userSessions.put(event.getUser().getIdLong(), session);
    }

    @NonNull
    public static ArrayList<ParseActions.ExecutableAction> generateActions(
            @NonNull ChatMessage system,
            @NonNull String prompt,
            @NonNull ParseActions.ValidAction action
    ) throws SocketTimeoutException {
        Gson gson = new Gson();
        String result = OpenAIHandler.getCompletion(
                system,
                prompt
        ).getChoices().get(0).getMessage().getContent();
        DiscordConfig config;
        ArrayList<ParseActions.ExecutableAction> actions;

        try {
            config = gson.fromJson(result, DiscordConfig.class);
        } catch(JsonSyntaxException e) {
            config = gson.fromJson(tryToFixJSON(result), DiscordConfig.class);
        }

        actions = ParseActions.parseActions(config, action);

        return actions;
    }

    public static void previewChangesPage(
            @NonNull EmbedBuilder eb,
            @NonNull ArrayList<ParseActions.ExecutableAction> actions
    ) {
        final String TOO_LONG = "... (too long to show)";
        String categoriesVal, channelsVal, rolesVal;

        eb.setTitle("Preview Changes");
        eb.setDescription("""
                Here is a preview of the changes that will be made to your server. Please confirm that you want to make these changes.

                If you want to regenerate with the same prompt, click on the "☕ New Brew" button.
                
                **Notice**: Permissions are omitted from this preview. If the bot doesn't have sufficient permissions to make the changes, it will not set permissions!
                """);
        eb.setFooter("Have questions or feedback? Join our Discord @ brewr.ai/discord");
        eb.setColor(Color.GREEN);

        // Categories
        categoriesVal = actions.stream().filter(
                action -> action.targetType() == ParseActions.ValidTarget.CATEGORY
        ).map(
                BrewHandler::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Categories",
                categoriesVal.length() > 1024 ? categoriesVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : categoriesVal,
                false
        );

        // Channels
        channelsVal = actions.stream().filter(
                action -> action.targetType() == ParseActions.ValidTarget.TEXT_CHANNEL
                        || action.targetType() == ParseActions.ValidTarget.VOICE_CHANNEL
                        || action.targetType() == ParseActions.ValidTarget.FORUM_CHANNEL
                        || action.targetType() == ParseActions.ValidTarget.STAGE_CHANNEL
        ).map(
                BrewHandler::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Channels",
                channelsVal.length() > 1024 ? channelsVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : channelsVal,
                false
        );

        // Roles
        rolesVal = actions.stream().filter(
                action -> action.targetType() == ParseActions.ValidTarget.ROLE
        ).map(
                BrewHandler::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Roles",
                rolesVal.length() > 1024 ? rolesVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : rolesVal,
                false
        );
    }

    private static String generateReadablePreview(@NonNull ParseActions.ExecutableAction action) {
        StringBuilder readableData = getData(action);

        switch(action.action()) {
            case CREATE -> {
                return String.format(
                        action.action().editableText + "\n%s",
                        action.target(),
                        readableData
                );
            }
            case EDIT -> {
                return String.format(
                        action.action().editableText + "\n%s",
                        action.target(),
                        action.data().get(ParseActions.ValidDataNames.NAME),
                        readableData
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + action.action());
        }
    }

    private static void optimizeRenameActions(@NonNull ArrayList<ParseActions.ExecutableAction> actions) {
        actions.removeIf(action -> {
            String name = (String) action.data().get(ParseActions.ValidDataNames.NAME);
            String desc = (String) action.data().get(ParseActions.ValidDataNames.DESCRIPTION);
            String color = (String) action.data().get(ParseActions.ValidDataNames.COLOR);

            boolean isNameSameOrEmpty = name == null || name.isEmpty() || name.equals(action.target());
            boolean isDescAndColorEmpty = (desc == null || desc.isEmpty()) && (color == null || color.isEmpty());

            return isNameSameOrEmpty && isDescAndColorEmpty;
        });
    }


    private static StringBuilder getData(@NonNull ParseActions.ExecutableAction action) {
        StringBuilder readableData = new StringBuilder();

        for(Map.Entry<ParseActions.ValidDataNames, ?> entry : action.data().entrySet()) {
            if(
                    entry.getKey() == ParseActions.ValidDataNames.NAME
                            || entry.getKey() == ParseActions.ValidDataNames.PERMISSIONS
                            || entry.getValue().equals("")
            )
                continue;

            readableData.append(String.format(
                    " - **%s**: %s\n",
                    entry.getKey().readable,
                    entry.getValue()
            ));
        }
        return readableData;
    }

    private static DiscordConfig getGuildData(
            @NonNull Guild guild,
            @NonNull String prompt,
            boolean getNsfwChannels
    ) {
        HashMap<String, DiscordConfig.ConfigItem> categories = new HashMap<>();
        HashMap<String, DiscordConfig.ConfigItem> channels = new HashMap<>();
        HashMap<String, DiscordConfig.ConfigItem> roles = new HashMap<>();

        guild.getCategories().forEach(category -> {
            // Verify the category's name won't get us yeeted
            if(OpenAIHandler.isContentFlagged(category.getName(), -1, -1, false))
                return;

            categories.put(category.getName(), new DiscordConfig.ConfigItem(
                    category.getName(),
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        });
        guild.getChannels(false).forEach(channel -> {
            String type;

            if(channel instanceof CategoryImpl)
                return;

            if(channel instanceof AbstractStandardGuildMessageChannelImpl<?> chn && chn.isNSFW() && !getNsfwChannels)
                return;

            if(channel instanceof TextChannelImpl || channel instanceof NewsChannelImpl) {
                type = "txt";
            } else if(channel instanceof VoiceChannelImpl) {
                type = "vc";
            } else if(channel instanceof ForumChannelImpl || channel instanceof MediaChannelImpl) {
                type = "forum";
            } else if(channel instanceof StageChannelImpl) {
                type = "stage";
            } else {
                return;
            }

            // Verify the channel's name won't get us yeeted
            if(OpenAIHandler.isContentFlagged(channel.getName(), -1, -1, false))
                return;

            channels.put(channel.getName(), new DiscordConfig.ConfigItem(
                    channel.getName(),
                    type,
                    null,
                    (channel instanceof TextChannelImpl) ? ((TextChannelImpl) channel).getTopic() : null,
                    null,
                    null
            ));
        });
        guild.getRoles().forEach(role -> {
            if(role.isPublicRole() || role.isManaged())
                return;

            // Verify the role's name won't get us yeeted
            if(OpenAIHandler.isContentFlagged(role.getName(), -1, -1, false))
                return;

            roles.put(role.getName(), new DiscordConfig.ConfigItem(
                    role.getName(),
                    null,
                    null,
                    null,
                    null,
                    String.format("#%06x", role.getColorRaw())
            ));
        });

        return new DiscordConfig(
                categories,
                channels,
                roles,
                prompt
        );
    }

    @NonNull
    private static String tryToFixJSON(@NonNull String json) {
        // Remove extra commas
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");

        // Remove any text before the beginning of the JSON
        json = json.substring(json.indexOf("{"));

        // Remove any text after the end of the JSON
        json = json.substring(0, json.lastIndexOf("}") + 1);

        return json;
    }
}
