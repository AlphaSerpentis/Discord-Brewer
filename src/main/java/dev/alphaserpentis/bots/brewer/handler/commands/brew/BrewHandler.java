package dev.alphaserpentis.bots.brewer.handler.commands.brew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatMessage;
import dev.alphaserpentis.bots.brewer.data.DiscordConfig;
import dev.alphaserpentis.bots.brewer.data.Prompts;
import dev.alphaserpentis.bots.brewer.data.UserSession;
import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.parser.ParseActions;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.entities.channel.concrete.CategoryImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BrewHandler {
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

        try {
            actions = generateActions(
                    Prompts.SETUP_SYSTEM_PROMPT_CREATE,
                    prompt,
                    ParseActions.ValidAction.CREATE
            );
        } catch(JsonSyntaxException e) {
            throw new GenerationException(GenerationException.ExceptionType.JSON_EXCEPTION.getDescriptions(), e.getCause());
        } catch(OpenAiHttpException e) {
            throw new GenerationException(GenerationException.ExceptionType.OVERLOADED_EXCEPTION.getDescriptions(), e.getCause());
        }

        previewChangesPage(eb, actions);
        session = new UserSession(
                prompt,
                UserSession.UserSessionType.NEW_BREW,
                ParseActions.ValidAction.CREATE,
                actions,
                (short) 0
        );
        session.setJDA(event.getJDA());
        session.setInteractionToken(event.getInteraction().getToken());
        session.setGuildId(event.getGuild().getIdLong());
        userSessions.put(event.getUser().getIdLong(), session);
    }

    public static void generateRenamePrompt(
            @NonNull EmbedBuilder eb,
            @NonNull String prompt,
            @NonNull SlashCommandInteractionEvent event
    ) {
        ArrayList<ParseActions.ExecutableAction> actions;
        UserSession session;

        try {
            actions = generateActions(
                    Prompts.SETUP_SYSTEM_PROMPT_RENAME,
                    new GsonBuilder().setPrettyPrinting().create().toJson(getGuildData(event.getGuild(), "Rename name, desc, and color based on " + prompt)),
                    ParseActions.ValidAction.EDIT
            );
        } catch(JsonSyntaxException e) {
            throw new GenerationException(GenerationException.ExceptionType.JSON_EXCEPTION.getDescriptions(), e.getCause());
        } catch(OpenAiHttpException e) {
            throw new GenerationException(GenerationException.ExceptionType.OVERLOADED_EXCEPTION.getDescriptions(), e.getCause());
        }
        previewChangesPage(eb, actions);
        session = new UserSession(
                prompt,
                UserSession.UserSessionType.RENAME,
                ParseActions.ValidAction.EDIT,
                actions,
                (short) 0
        );
        session.setJDA(event.getJDA());
        session.setInteractionToken(event.getInteraction().getToken());
        session.setGuildId(event.getGuild().getIdLong());
        userSessions.put(event.getUser().getIdLong(), session);
    }

    @NonNull
    public static ArrayList<ParseActions.ExecutableAction> generateActions(
            @NonNull ChatMessage system,
            @NonNull String prompt,
            @NonNull ParseActions.ValidAction action
    ) {
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

    public static void previewChangesPage(@NonNull EmbedBuilder eb, @NonNull ArrayList<ParseActions.ExecutableAction> actions) {
        final String TOO_LONG = "... (too long to show)";
        String categoriesVal, channelsVal, rolesVal;

        eb.setTitle("Preview Changes");
        eb.setDescription("""
                Here is a preview of the changes that will be made to your server. Please confirm that you want to make these changes.

                If you want to regenerate with the same prompt, click on the "☕ New Brew" button.
                
                **Notice**: Permissions are omitted from this preview. If the bot doesn't have sufficient permissions to make the changes, it will not set permissions!
                """);
        eb.setColor(Color.GREEN);

        // Categories
        categoriesVal = actions.stream().filter(
                action -> action.targetType() == ParseActions.ValidTarget.CATEGORY
        ).map(
                BrewHandler::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s\n%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Categories",
                categoriesVal.length() > 1024 ? categoriesVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : categoriesVal,
                false
        );

        // Channels
        channelsVal = actions.stream().filter(
                action -> (action.targetType() == ParseActions.ValidTarget.TEXT_CHANNEL)
                        || (action.targetType() == ParseActions.ValidTarget.VOICE_CHANNEL)
        ).map(
                BrewHandler::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s\n%s", a, b)
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
                (a, b) -> String.format("%s\n%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Roles",
                rolesVal.length() > 1024 ? rolesVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : rolesVal,
                false
        );
    }

    private static String generateReadablePreview(@NonNull ParseActions.ExecutableAction action) {
        StringBuilder readableData = new StringBuilder();

        for(Map.Entry<ParseActions.ValidDataNames, Object> entry : action.data().entrySet()) {
            if(
                    entry.getKey() == ParseActions.ValidDataNames.NAME
                            || entry.getKey() == ParseActions.ValidDataNames.PERMISSIONS
                            || entry.getValue().equals("")
            )
                continue;

            readableData.append(String.format(
                    "└ **%s**: %s\n",
                    entry.getKey().readable,
                    entry.getValue()
            ));
        }

        if(action.action() == ParseActions.ValidAction.CREATE) {
            return String.format(
                    action.action().editableText + "\n%s",
                    action.target(),
                    readableData
            );
        } else if(action.action() == ParseActions.ValidAction.EDIT) {
            return String.format(
                    action.action().editableText + "\n%s",
                    action.target(),
                    action.data().get(ParseActions.ValidDataNames.NAME),
                    readableData
            );
        } else {
            throw new IllegalStateException("Unexpected value: " + action.action());
        }
    }

    private static DiscordConfig getGuildData(@NonNull Guild guild, @NonNull String prompt) {
        HashMap<String, DiscordConfig.ConfigItem> categories = new HashMap<>();
        HashMap<String, DiscordConfig.ConfigItem> channels = new HashMap<>();
        HashMap<String, DiscordConfig.ConfigItem> roles = new HashMap<>();

        guild.getCategories().forEach(category -> categories.put(category.getName(), new DiscordConfig.ConfigItem(
                category.getName(),
                null,
                null,
                null,
                null,
                null
        )));
        guild.getChannels(false).forEach(channel -> {
            if(channel instanceof CategoryImpl)
                return;

            channels.put(channel.getName(), new DiscordConfig.ConfigItem(
                    channel.getName(),
                    (channel instanceof TextChannelImpl) ? null : "vc",
                    null,
                    (channel instanceof TextChannelImpl) ? ((TextChannelImpl) channel).getTopic() : null,
                    null,
                    null
            ));
        });
        guild.getRoles().forEach(role -> {
            if(role.isPublicRole() || role.isManaged())
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

        // Remove any text befor

        return json;
    }
}
