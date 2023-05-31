package dev.alphaserpentis.bots.brewer.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.theokanning.openai.completion.chat.ChatMessage;
import dev.alphaserpentis.bots.brewer.data.DiscordConfig;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.parser.Interpreter;
import dev.alphaserpentis.bots.brewer.handler.parser.ParseActions;
import dev.alphaserpentis.coffeecore.commands.ButtonCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.entities.channel.concrete.CategoryImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Brew extends ButtonCommand<MessageEmbed> {

    private enum UserSessionType {
        NEW_BREW,
        RENAME
    }

    public static class UserSession {
        private final String prompt;
        private final UserSessionType type;
        private final ParseActions.ValidAction action;
        private JDA jda;
        private ArrayList<ParseActions.ExecutableAction> actionsToExecute;
        private Interpreter.InterpreterResult interpreterResult;
        private short brewCount;
        private String interactionToken = "";
        private long guildId;

        public UserSession(
                @NonNull String prompt,
                @NonNull UserSessionType type,
                @NonNull ParseActions.ValidAction action,
                @NonNull ArrayList<ParseActions.ExecutableAction> actionsToExecute,
                short brewCount
        ) {
            this.prompt = prompt;
            this.type = type;
            this.action = action;
            this.actionsToExecute = actionsToExecute;
            this.brewCount = brewCount;
        }

        public String getPrompt() {
            return prompt;
        }
        public UserSessionType getType() {
            return type;
        }
        public ParseActions.ValidAction getAction() {
            return action;
        }
        public JDA getJDA() {
            return jda;
        }
        public ArrayList<ParseActions.ExecutableAction> getActionsToExecute() {
            return actionsToExecute;
        }
        public Interpreter.InterpreterResult getInterpreterResult() {
            return interpreterResult;
        }
        public short getBrewCount() {
            return brewCount;
        }
        public String getInteractionToken() {
            return interactionToken;
        }
        public long getGuildId() {
            return guildId;
        }

        public void setActionsToExecute(ArrayList<ParseActions.ExecutableAction> actionsToExecute) {
            this.actionsToExecute = actionsToExecute;
        }
        public void setJDA(JDA jda) {
            this.jda = jda;
        }
        public void setInterpreterResult(@NonNull Interpreter.InterpreterResult interpreterResult) {
            this.interpreterResult = interpreterResult;
        }
        public void setBrewCount(short brewCount) {
            this.brewCount = brewCount;
        }
        public void setInteractionToken(String interactionToken) {
            this.interactionToken = interactionToken;
        }
        public void setGuildId(long guildId) {
            this.guildId = guildId;
        }
    }

    private final HashMap<Long, UserSession> userSessions = new HashMap<>();
    private static final EmbedBuilder DMS_NOT_SUPPORTED = new EmbedBuilder()
            .setTitle("DMs Not Supported")
            .setDescription("This command does not support DMs.")
            .setColor(Color.RED);
    private static final EmbedBuilder NO_PERMISSIONS = new EmbedBuilder()
            .setTitle("No Permissions")
            .setDescription("""
                    You do not have the required permissions to run this command.

                    Must be the server owner or have `Administrator` permissions.""")
            .setColor(Color.RED);
    private static final EmbedBuilder PROMPT_REJECTED = new EmbedBuilder()
            .setTitle("Prompt Rejected")
            .setDescription(
                    """
                    The prompt you provided was rejected. Ensure that it does not violate the usage policies of OpenAI.

                    You can read more about the usage policies of OpenAI here: https://openai.com/policies/usage-policies"""
            )
            .setColor(Color.RED);
    private static final EmbedBuilder BREWING_UP = new EmbedBuilder()
            .setTitle("Brewing Up")
            .setDescription("""
            Brewing up your server...
            
            **This may take a while due to rate limits.**""")
            .setColor(Color.ORANGE);
    private static final EmbedBuilder GENERATING_NEW_BREW = new EmbedBuilder()
            .setTitle("Generating New Brew")
            .setDescription("Generating a new brew...")
            .setColor(Color.ORANGE);
    private static final EmbedBuilder GENERATING_ERROR = new EmbedBuilder()
            .setTitle("Error Generating Brew")
            .setDescription("""
                    An error occurred while generating a brew.
                    
                    You can try again by pressing the "Try Again" button below.
                    
                    Report this over at https://asrp.dev/discord
                    
                    Error Message: %s""")
            .setColor(Color.RED);
    private static final EmbedBuilder CANCELLED = new EmbedBuilder()
            .setTitle("Cancelled")
            .setDescription("Cancelled the current session.")
            .setColor(Color.RED);
    private static final EmbedBuilder REVERTING = new EmbedBuilder()
            .setTitle("Reverting")
            .setDescription("""
            Reverting the changes made to the server...
            
            **This may take a while due to rate limits.**""")
            .setColor(Color.ORANGE);
    private static final EmbedBuilder POST_EXECUTION_NO_ERROR = new EmbedBuilder()
            .setTitle("Server Brewed Up!")
            .setDescription("The server has been successfully brewed up!")
            .setColor(Color.GREEN);
    private static final EmbedBuilder POST_EXECUTION_ERROR = new EmbedBuilder()
            .setTitle("Server Brew Attempted")
            .setDescription("""
                    A brew was attempted, but there were errors.
                    
                    You can revert any changes made by pressing the "Revert" button below.
                    
                    Report this over at https://asrp.dev/discord
                    
                    Error Message: %s""")
            .setColor(Color.ORANGE);
    private static final EmbedBuilder REVERTED_NO_ERROR = new EmbedBuilder()
            .setTitle("Reverted")
            .setDescription("Reverted the changes made to the server.")
            .setColor(Color.GREEN);

    private static final EmbedBuilder REVERTED_ERROR = new EmbedBuilder()
            .setTitle("Reverted?")
            .setDescription("""
                    Reverted the changes made to the server (maybe), but there were errors.
                    
                    Report this over at https://asrp.dev/discord
                    
                    Error Message: %s""")
            .setColor(Color.ORANGE);

    private final EmbedBuilder USER_SESSION_NOT_FOUND = new EmbedBuilder()
            .setTitle("User Session Not Found")
            .setDescription("You do not have an active session. Run </brew server:" + getCommandId() + "> or </brew rename:" + getCommandId() + "> to start a new session.")
            .setColor(Color.RED);

    public Brew() {
        super(
                new BotCommandOptions(
                        "brew",
                        "Setup your Discord server with a prompt!",
                        180,
                        0,
                        true,
                        false,
                        TypeOfEphemeral.DEFAULT,
                        true,
                        true,
                        true,
                        false
                )
        );

        addButton("brew", ButtonStyle.PRIMARY, "New Brew", Emoji.fromUnicode("☕"),false);
        addButton("confirm", ButtonStyle.SUCCESS, "Confirm", Emoji.fromUnicode("✅"), false);
        addButton("cancel", ButtonStyle.DANGER, "Cancel", Emoji.fromUnicode("✖️"), false);
        addButton("revert", ButtonStyle.DANGER, "Revert", Emoji.fromUnicode("↩️"), false);
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        final UserSession userSession = userSessions.get(event.getUser().getIdLong());
        final String buttonId = event.getComponentId().substring(getName().length() + 1);
        InteractionHook hook = event.deferEdit().complete();

        if(userSession == null) {
            hook.editOriginalComponents().setEmbeds(USER_SESSION_NOT_FOUND.build()).queue();
            return;
        }

        switch(buttonId) {
            case "brew" -> {
                ChatMessage chatMessage;
                EmbedBuilder eb = new EmbedBuilder();

                if(userSession.getType() == UserSessionType.NEW_BREW) {
                    chatMessage = OpenAIHandler.SETUP_SYSTEM_PROMPT_SETUP;
                } else {
                    chatMessage = OpenAIHandler.SETUP_SYSTEM_PROMPT_RENAME;
                }

                hook.editOriginalComponents()
                        .setEmbeds(
                                GENERATING_NEW_BREW.build()
                        )
                        .queue();

                try {
                    userSession.setActionsToExecute(
                            generateActions(
                                    chatMessage,
                                    userSession.getPrompt(),
                                    userSession.getAction()
                            )
                    );
                } catch(JsonSyntaxException e) {
                    eb = new EmbedBuilder(GENERATING_ERROR);
                    eb.setDescription(String.format(GENERATING_ERROR.getDescriptionBuilder().toString(), e.getMessage()));

                    hook
                            .editOriginalComponents()
                            .setEmbeds(eb.build())
                            .setActionRow(
                                    getButton("brew")
                            )
                            .queue();
                }

                previewChangesPage(eb, userSession.getActionsToExecute());

                if(userSession.getBrewCount() == 3) {
                    userSession.setBrewCount((short) (userSession.getBrewCount() + 1));

                    hook
                            .editOriginalComponents()
                            .setEmbeds(eb.build())
                            .setActionRow(
                                    getButton("confirm"),
                                    getButton("cancel")
                            )
                            .queue();
                } else {
                    hook
                            .editOriginalComponents()
                            .setEmbeds(eb.build())
                            .setActionRow(
                                    getButton("brew"),
                                    getButton("confirm"),
                                    getButton("cancel")
                            )
                            .queue();
                }

            }
            case "confirm" -> {
                hook.editOriginalComponents().setEmbeds(BREWING_UP.build()).complete();

                Interpreter.InterpreterResult result = Interpreter.interpretAndExecute(
                        userSession.getActionsToExecute(),
                        userSession.getAction(),
                        event.getGuild()
                );

                userSession.setInterpreterResult(result);

                try {
                    if(result.completeSuccess()) {
                        hook.editOriginalEmbeds(
                                POST_EXECUTION_NO_ERROR.build()
                        ).setActionRow(
                                getButton("revert")
                        ).queue();
                    } else {
                        String errorMessages = String.join("\n", result.messages());
                        EmbedBuilder eb = new EmbedBuilder(POST_EXECUTION_ERROR);

                        MessageEmbed errorEmbed = eb
                                .setDescription(
                                        String.format(
                                                POST_EXECUTION_ERROR.getDescriptionBuilder().toString(),
                                                errorMessages
                                        )
                                )
                                .build();

                        hook.editOriginalEmbeds(
                                errorEmbed
                        ).setActionRow(
                                getButton("revert")
                        ).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    userSessions.remove(event.getUser().getIdLong());
                }
            }
            case "cancel" -> {
                hook.editOriginalComponents().setEmbeds(CANCELLED.build()).queue();
                userSessions.remove(event.getUser().getIdLong());
            }
            case "revert" -> {
                hook.editOriginalComponents().setEmbeds(REVERTING.build()).complete();

                Interpreter.InterpreterResult result = Interpreter.deleteAllChanges(userSession);

                if(result.completeSuccess()) {
                    hook.editOriginalEmbeds(
                            REVERTED_NO_ERROR.build()
                    ).queue();
                } else {
                    String errorMessages = String.join("\n", result.messages());
                    EmbedBuilder eb = new EmbedBuilder(REVERTED_ERROR);

                    MessageEmbed errorEmbed = eb
                            .setDescription(
                                    String.format(
                                            REVERTED_ERROR.getDescriptionBuilder().toString(),
                                            errorMessages
                                    )
                            )
                            .build();

                    hook.editOriginalEmbeds(
                            errorEmbed
                    ).queue();
                }

                userSessions.remove(event.getUser().getIdLong());
            }
        }
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull GenericCommandInteractionEvent event) {
        final UserSession userSession = userSessions.get(event.getUser().getIdLong());

        if(userSession == null)
            return new ArrayList<>();
        else if(event.getName().equals(getName()) && userSession.getInteractionToken().equals(event.getToken())) {
            return new ArrayList<>() {{
                add(getButton("brew"));
                add(getButton("confirm"));
                add(getButton("cancel"));
            }};
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        String prompt;
        CommandResponse<MessageEmbed> response;

        // Check rate limit
        response = (CommandResponse<MessageEmbed>) checkAndHandleRateLimitedUser(userId);

        if(response != null)
            return response;

        // Check if the command was ran in the DMs
        if(!event.isFromGuild())
            return new CommandResponse<>(DMS_NOT_SUPPORTED.build(), isOnlyEphemeral());

        // Check if the user is allowed to run the command
        if(!isUserAllowedToRunCommand(event.getMember()))
            return new CommandResponse<>(NO_PERMISSIONS.build(), isOnlyEphemeral());

        // Check if the prompt doesn't get flagged by OpenAI
        prompt = event.getOption("prompt").getAsString();

        if(!OpenAIHandler.isPromptSafeToUse(prompt))
            return new CommandResponse<>(PROMPT_REJECTED.build(), isOnlyEphemeral());

        try {
            switch(event.getSubcommandName()) {
                case "create" -> runCreatePrompt(eb, prompt, event);
                case "rename" -> runRenamePrompt(eb, prompt, event);
            }
        } catch(Exception e) {
            userSessions.remove(userId);
            throw e;
        }

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData create = new SubcommandData("create", "Create new roles/categories/channels with a prompt!")
                .addOption(OptionType.STRING, "prompt", "The prompt to use for the brew.", true);
        SubcommandData rename = new SubcommandData("rename", "Rename your roles/categories/channels with a prompt!")
                .addOption(OptionType.STRING, "prompt", "The prompt to use for the brew.", true);

        jda
                .upsertCommand(name, description)
                .addSubcommands(create, rename)
                .queue(r -> setCommandId(r.getIdLong()));
    }

    private boolean isUserAllowedToRunCommand(@NonNull Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }

    @NonNull
    private ArrayList<ParseActions.ExecutableAction> generateActions(
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

//        System.out.println(result);

        try {
            config = gson.fromJson(result, DiscordConfig.class);
        } catch(JsonSyntaxException e) {
//            System.out.println("Invalid JSON, trying to fix it...");
            config = gson.fromJson(tryToFixJSON(result), DiscordConfig.class);
        }

        actions = ParseActions.parseActions(config, action);

        return actions;
    }

    @NonNull
    private String tryToFixJSON(@NonNull String json) {
        // Remove extra commas
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");

        return json;
    }

    private void runCreatePrompt(@NonNull EmbedBuilder eb, @NonNull String prompt, @NonNull SlashCommandInteractionEvent event) {
        ArrayList<ParseActions.ExecutableAction> actions = generateActions(
                OpenAIHandler.SETUP_SYSTEM_PROMPT_SETUP,
                prompt,
                ParseActions.ValidAction.CREATE
        );
        UserSession session;
        previewChangesPage(eb, actions);

        session = new UserSession(prompt, UserSessionType.NEW_BREW, ParseActions.ValidAction.CREATE, actions, (short) 1);
        session.setInteractionToken(event.getToken());
        session.setJDA(event.getJDA());
        session.setGuildId(event.getGuild().getIdLong());
        userSessions.put(
                event.getUser().getIdLong(),
                session
        );
    }

    private void runRenamePrompt(@NonNull EmbedBuilder eb, @NonNull String prompt, @NonNull SlashCommandInteractionEvent event) {
        ArrayList<ParseActions.ExecutableAction> actions = generateActions(
                OpenAIHandler.SETUP_SYSTEM_PROMPT_RENAME,
                new GsonBuilder().setPrettyPrinting().create().toJson(getGuildData(event.getGuild(), "Rename name, desc, and color based on " + prompt)),
                ParseActions.ValidAction.EDIT
        );
        UserSession session;
        previewChangesPage(eb, actions);

        session = new UserSession(prompt, UserSessionType.RENAME, ParseActions.ValidAction.EDIT, actions, (short) 1);
        session.setInteractionToken(event.getToken());
        session.setJDA(event.getJDA());
        session.setGuildId(event.getGuild().getIdLong());
        userSessions.put(
                event.getUser().getIdLong(),
                session
        );
    }

    @NonNull
    private DiscordConfig getGuildData(@NonNull Guild guild, @NonNull String prompt) {
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
                    null,
                    null,
                    null,
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
                    null
            ));
        });

        return new DiscordConfig(
                categories,
                channels,
                roles,
                prompt
        );
    }

    private void previewChangesPage(@NonNull EmbedBuilder eb, @NonNull ArrayList<ParseActions.ExecutableAction> actions) {
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
                this::generateReadablePreview
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
                this::generateReadablePreview
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
                this::generateReadablePreview
        ).reduce(
                (a, b) -> String.format("%s\n%s", a, b)
        ).orElse("No changes");

        eb.addField(
                "Roles",
                rolesVal.length() > 1024 ? rolesVal.substring(0, 1024 - TOO_LONG.length()) + TOO_LONG : rolesVal,
                false
        );
    }

    @NonNull
    private String generateReadablePreview(@NonNull ParseActions.ExecutableAction action) {
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

    @NonNull
    private String addSuggestion() {
        final String SUGGESTION = """
                If you're enjoying this bot, consider supporting Brewer by voting for us!
                    
                Run </vote:%s> to vote for us!""";
        return String.format(SUGGESTION, core.getCommandsHandler().mappingOfCommands.get("vote").getCommandId());
    }
}
