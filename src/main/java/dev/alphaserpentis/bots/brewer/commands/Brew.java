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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

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

    public class UserSession {
        private final String prompt;
        private final UserSessionType type;
        private ArrayList<ParseActions.ExecutableAction> actionsToExecute;
        private Interpreter.InterpreterResult interpreterResult;
        private short brewCount;
        private String interactionToken = "";

        public UserSession(
                @NonNull String prompt,
                @NonNull UserSessionType type,
                @NonNull ArrayList<ParseActions.ExecutableAction> actionsToExecute,
                short brewCount
        ) {
            this.prompt = prompt;
            this.type = type;
            this.actionsToExecute = actionsToExecute;
            this.brewCount = brewCount;
        }

        public String getPrompt() {
            return prompt;
        }
        public UserSessionType getType() {
            return type;
        }
        public ArrayList<ParseActions.ExecutableAction> getActionsToExecute() {
            return actionsToExecute;
        }
        public short getBrewCount() {
            return brewCount;
        }
        public String getInteractionToken() {
            return interactionToken;
        }

        public void setActionsToExecute(ArrayList<ParseActions.ExecutableAction> actionsToExecute) {
            this.actionsToExecute = actionsToExecute;
        }
        public void setInterpreterResult(@NonNull Interpreter.InterpreterResult interpreterResult) {
            this.interpreterResult = interpreterResult;
        }
        public void setInteractionToken(String interactionToken) {
            this.interactionToken = interactionToken;
        }
    }

    private final HashMap<Long, UserSession> userSessions = new HashMap<>();
    private static final MessageEmbed DMS_NOT_SUPPORTED = new EmbedBuilder()
            .setTitle("DMs Not Supported")
            .setDescription("This command does not support DMs.")
            .setColor(Color.RED)
            .build();
    private static final MessageEmbed NO_PERMISSIONS = new EmbedBuilder()
            .setTitle("No Permissions")
            .setDescription("""
                    You do not have the required permissions to run this command.

                    Must be the server owner or have `Administrator` permissions.""")
            .setColor(Color.RED)
            .build();
    private static final MessageEmbed PROMPT_REJECTED = new EmbedBuilder()
            .setTitle("Prompt Rejected")
            .setDescription(
                    """
                    The prompt you provided was rejected. Ensure that it does not violate the usage policies of OpenAI.

                    You can read more about the usage policies of OpenAI here: https://openai.com/policies/usage-policies"""
            )
            .setColor(Color.RED)
            .build();
    private static final MessageEmbed BREWING_UP = new EmbedBuilder()
            .setTitle("Brewing Up")
            .setDescription("Brewing up your server...")
            .setColor(Color.ORANGE)
            .build();
    private static final MessageEmbed CANCELLED = new EmbedBuilder()
            .setTitle("Cancelled")
            .setDescription("Cancelled the current session.")
            .setColor(Color.RED)
            .build();
    private static final MessageEmbed REVERTING = new EmbedBuilder()
            .setTitle("Reverting")
            .setDescription("Reverting the changes made to the server...")
            .setColor(Color.ORANGE)
            .build();
    private static final MessageEmbed POST_EXECUTION_NO_ERROR = new EmbedBuilder()
            .setTitle("Server Brewed Up!")
            .setDescription("The server has been successfully brewed up!")
            .setColor(Color.GREEN)
            .build();
    private static final EmbedBuilder POST_EXECUTION_ERROR = new EmbedBuilder()
            .setTitle("Server Brewed Up!")
            .setDescription("""
                    The server has been successfully brewed up, but there were some errors.
                    
                    You can revert this change by pressing the "Revert" button below.
                    
                    Error Message: %s""")
            .setColor(Color.ORANGE);
    private static final MessageEmbed REVERTED_NO_ERROR = new EmbedBuilder()
            .setTitle("Reverted")
            .setDescription("Reverted the changes made to the server.")
            .setColor(Color.GREEN)
            .build();

    private static final EmbedBuilder REVERTED_ERROR = new EmbedBuilder()
            .setTitle("Reverted?")
            .setDescription("""
                    Reverted the changes made to the server (maybe), but there were some errors.
                    
                    Error Message: %s""")
            .setColor(Color.ORANGE);

    private final MessageEmbed USER_SESSION_NOT_FOUND = new EmbedBuilder()
            .setTitle("User Session Not Found")
            .setDescription("You do not have an active session. Run </brew server:" + getCommandId() + "> or </brew rename:" + getCommandId() + "> to start a new session.")
            .setColor(Color.RED)
            .build();

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
            hook.editOriginalComponents().setEmbeds(USER_SESSION_NOT_FOUND).queue();
            return;
        }

        switch(buttonId) {
            case "brew" -> {
                ChatMessage chatMessage;
                EmbedBuilder eb = new EmbedBuilder();

                if(userSession.type == UserSessionType.NEW_BREW) {
                    chatMessage = OpenAIHandler.SETUP_SYSTEM_PROMPT_SETUP;
                } else {
                    chatMessage = OpenAIHandler.SETUP_SYSTEM_PROMPT_RENAME;
                }

                hook.editOriginalComponents()
                        .setEmbeds(BREWING_UP)
                        .queue();

                userSession.actionsToExecute = generateActions(
                        chatMessage,
                        userSession.getPrompt()
                );

                previewChangesPage(eb, userSession.actionsToExecute);

                if(userSession.brewCount++ == 3) {
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
                                    getButton("confirm"),
                                    getButton("cancel"),
                                    getButton("brew")
                            )
                            .queue();
                }

            }
            case "confirm" -> {
                Message msg = hook.editOriginalComponents().setEmbeds(BREWING_UP).complete();

                Interpreter.InterpreterResult result = Interpreter.interpretAndExecute(
                        userSession.actionsToExecute,
                        event.getGuild()
                );

                userSession.setInterpreterResult(result);

                try {
                    if(result.completeSuccess()) {
                        msg.editMessageEmbeds(
                                POST_EXECUTION_NO_ERROR
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

                        msg.editMessageEmbeds(
                                errorEmbed
                        ).setActionRow(
                                getButton("revert")
                        ).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case "cancel" -> {
                hook.editOriginalComponents().setEmbeds(CANCELLED).queue();
                userSessions.remove(event.getUser().getIdLong());
            }
            case "revert" -> {
                Message msg = hook.editOriginalComponents().setEmbeds(REVERTING).complete();

                try {
                    Interpreter.deleteAllChanges(
                            userSession.interpreterResult.channels(),
                            userSession.interpreterResult.roles()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    EmbedBuilder eb = new EmbedBuilder(REVERTED_ERROR);

                    msg.editMessageEmbeds(
                            eb
                                    .setDescription(
                                            String.format(
                                                    REVERTED_ERROR.getDescriptionBuilder().toString(),
                                                    e.getMessage()
                                            )
                                    )
                                    .build()
                    ).queue();

                    return;
                } finally {
                    userSessions.remove(event.getUser().getIdLong());
                }

                msg.editMessageEmbeds(
                        REVERTED_NO_ERROR
                ).queue();
            }
        }
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull GenericCommandInteractionEvent event) {
        final UserSession userSession = userSessions.get(event.getUser().getIdLong());

        if(userSession == null)
            return new ArrayList<>();
        else if(event.getName().equals(getName()) && userSession.interactionToken.equals(event.getToken())) {
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
            return new CommandResponse<>(DMS_NOT_SUPPORTED, isOnlyEphemeral());

        // Check if the user is allowed to run the command
        if(!isUserAllowedToRunCommand(event.getMember()))
            return new CommandResponse<>(NO_PERMISSIONS, isOnlyEphemeral());

        // Check if the prompt doesn't get flagged by OpenAI
        prompt = String.valueOf(event.getOption("prompt"));

        if(!OpenAIHandler.isPromptSafeToUse(prompt))
            return new CommandResponse<>(PROMPT_REJECTED, isOnlyEphemeral());

        switch(event.getSubcommandName()) {
            case "server" -> runServerPrompt(eb, prompt, event);
//            case "rename" -> runRenamePrompt(eb, prompt, event);
            case "rename" -> {
                return new CommandResponse<>(
                        eb
                                .setDescription("Soon™")
                                .setTitle("Rename")
                                .setColor(Color.RED)
                                .build(),
                        isOnlyEphemeral()
                );
            }
        }

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData server = new SubcommandData("server", "Setup your Discord server with a prompt!")
                .addOption(OptionType.STRING, "prompt", "The prompt to use for the brew.", true);
        SubcommandData rename = new SubcommandData("rename", "Rename your channels, roles, and categories with a prompt!")
                .addOption(OptionType.STRING, "prompt", "The prompt to use for the brew.", true);

        jda
                .upsertCommand(name, description)
                .addSubcommands(server, rename)
                .queue(r -> setCommandId(r.getIdLong()));
    }

    private boolean isUserAllowedToRunCommand(@NonNull Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }

    @NonNull
    private ArrayList<ParseActions.ExecutableAction> generateActions(
            @NonNull ChatMessage system,
            @NonNull String prompt
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

        actions = ParseActions.parseActions(config);

        return actions;
    }

    @NonNull
    private String tryToFixJSON(@NonNull String json) {
        // Remove extra commas
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");

        return json;
    }

    private void runServerPrompt(@NonNull EmbedBuilder eb, @NonNull String prompt, @NonNull SlashCommandInteractionEvent event) {
        ArrayList<ParseActions.ExecutableAction> actions = generateActions(
                OpenAIHandler.SETUP_SYSTEM_PROMPT_SETUP,
                prompt
        );
        previewChangesPage(eb, actions);

        userSessions.put(event.getUser().getIdLong(), new UserSession(prompt, UserSessionType.NEW_BREW, actions, (short) 1));
        userSessions.get(event.getUser().getIdLong()).setInteractionToken(event.getToken());
    }

    private void runRenamePrompt(@NonNull EmbedBuilder eb, @NonNull String prompt, @NonNull SlashCommandInteractionEvent event) {
        ArrayList<ParseActions.ExecutableAction> actions = generateActions(
                OpenAIHandler.SETUP_SYSTEM_PROMPT_RENAME,
                new GsonBuilder().setPrettyPrinting().create().toJson(getGuildData(event.getGuild(), prompt))
        );
        previewChangesPage(eb, actions);

        userSessions.put(event.getUser().getIdLong(), new UserSession(prompt, UserSessionType.RENAME, actions, (short) 1));
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
        guild.getChannels().forEach(channel -> {
            String type = "";
            String desc = null;
            if(channel.getType().isMessage()) {
                type = "txt";
                desc = ((TextChannel) channel).getTopic();
            } else if(channel.getType().isAudio())
                type = "vc";

            channels.put(channel.getName(), new DiscordConfig.ConfigItem(
                    channel.getName(),
                    type,
                    ((TextChannel) channel).getParentCategory().getId(),
                    desc,
                    null,
                    null
            ));
        });
        guild.getRoles().forEach(role -> roles.put(role.getName(), new DiscordConfig.ConfigItem(
                role.getName(),
                null,
                null,
                null,
                null,
                null
        )));

        return new DiscordConfig(
                categories,
                channels,
                roles,
                prompt
        );
    }

    private void previewChangesPage(@NonNull EmbedBuilder eb, @NonNull ArrayList<ParseActions.ExecutableAction> actions) {
        eb.setTitle("Preview Changes");
        eb.setDescription("""
                Here is a preview of the changes that will be made to your server. Please confirm that you want to make these changes.

                If you want to regenerate with the same prompt, click on the "☕ New Brew" button.
                
                **Notice**: Permissions are omitted from this preview. If the bot doesn't have sufficient permissions to make the changes, it will not set permissions!
                """);
        eb.setColor(Color.GREEN);

        // Categories
        eb.addField(
                "Categories",
                actions.stream().filter(
                        action -> action.target() == ParseActions.ValidTarget.CATEGORY
                ).map(
                        action -> {
                            StringBuilder readableData = new StringBuilder();

                            for(Map.Entry<String, Object> entry : action.data().entrySet()) {
                                if(entry.getKey().equals("name") || entry.getKey().equals("perms"))
                                    continue;

                                readableData.append(String.format(
                                        "└ **%s**: %s\n",
                                        entry.getKey(),
                                        entry.getValue()
                                ));
                            }

                            return String.format(
                                    "%s %s\n%s",
                                    action.action().readable,
                                    action.data().get("name"),
                                    readableData
                            );
                        }
                ).reduce(
                        (a, b) -> String.format("%s\n%s", a, b)
                ).orElse("No changes"),
                false
        );

        // Channels
        eb.addField(
                "Channels",
                actions.stream().filter(
                        action -> (action.target() == ParseActions.ValidTarget.TEXT_CHANNEL)
                                || (action.target() == ParseActions.ValidTarget.VOICE_CHANNEL)
                ).map(
                        action -> {
                            StringBuilder readableData = new StringBuilder();

                            for(Map.Entry<String, Object> entry : action.data().entrySet()) {
                                if(entry.getKey().equals("name") || entry.getKey().equals("perms"))
                                    continue;

                                readableData.append(String.format(
                                        "└ **%s**: %s\n",
                                        entry.getKey(),
                                        entry.getValue()
                                ));
                            }

                            return String.format(
                                    "%s %s\n%s",
                                    action.action().readable,
                                    action.data().get("name"),
                                    readableData
                            );
                        }
                ).reduce(
                        (a, b) -> String.format("%s\n%s", a, b)
                ).orElse("No changes"),
                false
        );

        // Roles
        eb.addField(
                "Roles",
                actions.stream().filter(
                        action -> action.target() == ParseActions.ValidTarget.ROLE
                ).map(
                        action -> {
                            StringBuilder readableData = new StringBuilder();

                            for(Map.Entry<String, Object> entry : action.data().entrySet()) {
                                if(entry.getKey().equals("name") || entry.getKey().equals("perms"))
                                    continue;

                                readableData.append(String.format(
                                        "└ **%s**: %s\n",
                                        entry.getKey(),
                                        entry.getValue()
                                ));
                            }

                            return String.format(
                                    "%s %s\n%s",
                                    action.action().readable,
                                    action.data().get("name"),
                                    readableData
                            );
                        }
                ).reduce(
                        (a, b) -> String.format("%s\n%s", a, b)
                ).orElse("No changes"),
                false
        );

        // Other Server Modifications
//        eb.addField(
//                "Other Server Modifications",
//                actions.stream().filter(
//                        action -> (action.target() == ParseActions.ValidTarget.SERVER)
//                ).map(
//                        action -> {
//                            StringBuilder readableData = new StringBuilder();
//
//                            for(Map.Entry<String, Object> entry : action.data().entrySet()) {
//                                if(entry.getKey().equals("name") || entry.getKey().equals("perm"))
//                                    continue;
//
//                                readableData.append(String.format(
//                                        "└ **%s**: %s\n",
//                                        entry.getKey(),
//                                        entry.getValue()
//                                ));
//                            }
//
//                            return String.format(
//                                    "%s %s\n%s",
//                                    action.action().readable,
//                                    action.data().get("name"),
//                                    readableData
//                            );
//                        }
//                ).reduce(
//                        (a, b) -> String.format("%s\n%s", a, b)
//                ).orElse("No changes"),
//                false
//        );
    }
}
