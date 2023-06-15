package dev.alphaserpentis.bots.brewer.commands;

import com.google.gson.JsonSyntaxException;
import com.theokanning.openai.completion.chat.ChatMessage;
import dev.alphaserpentis.bots.brewer.data.brewer.UserSession;
import dev.alphaserpentis.bots.brewer.data.openai.Prompts;
import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import dev.alphaserpentis.bots.brewer.handler.commands.brew.BrewHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.vote.VoteHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.bots.brewer.handler.parser.Interpreter;
import dev.alphaserpentis.coffeecore.commands.ButtonCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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

public class Brew extends ButtonCommand<MessageEmbed, SlashCommandInteractionEvent> {

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
                    
                    You can try again by pressing the "Brew" button below.
                    
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
            .setDescription("The server has been successfully brewed up!%s")
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
        final UserSession userSession = BrewHandler.getUserSession(event.getUser().getIdLong());
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

                if(userSession.getType() == UserSession.UserSessionType.NEW_BREW) {
                    chatMessage = Prompts.SETUP_SYSTEM_PROMPT_CREATE;
                } else {
                    chatMessage = Prompts.SETUP_SYSTEM_PROMPT_RENAME;
                }

                hook.editOriginalComponents()
                        .setEmbeds(
                                GENERATING_NEW_BREW.build()
                        )
                        .queue();

                try {
                    userSession.setActionsToExecute(
                            BrewHandler.generateActions(
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

                BrewHandler.previewChangesPage(eb, userSession.getActionsToExecute());

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
                        EmbedBuilder eb = new EmbedBuilder(POST_EXECUTION_NO_ERROR);

                        if(!VoteHandler.isUserInRemindedMap(event.getUser().getIdLong())) {
                            long voteCommandId = core.getCommandsHandler().getCommand("vote").getCommandId();

                            eb.setDescription(
                                    String.format(
                                            POST_EXECUTION_NO_ERROR.getDescriptionBuilder().toString(),
                                            "\n\nIf you're enjoying the bot, do please vote for Brewer! You can do so by clicking [here](https://top.gg/bot/819650039680575488/vote) or by running </vote:" + voteCommandId + ">."
                                    )
                            );

                            VoteHandler.addUserToRemindedMap(event.getUser().getIdLong());
                        }

                        hook.editOriginalEmbeds(
                                eb.build()
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

                    BrewHandler.removeUserSession(event.getUser().getIdLong());
                }
            }
            case "cancel" -> {
                hook.editOriginalComponents().setEmbeds(CANCELLED.build()).queue();
                BrewHandler.removeUserSession(event.getUser().getIdLong());
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

                BrewHandler.removeUserSession(event.getUser().getIdLong());
            }
        }
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull SlashCommandInteractionEvent event) {
        final UserSession userSession = BrewHandler.getUserSession(event.getUser().getIdLong());

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

        if(OpenAIHandler.isContentFlagged(prompt, userId, event.getGuild() != null ? event.getGuild().getIdLong() : 0))
            return new CommandResponse<>(PROMPT_REJECTED.build(), isOnlyEphemeral());

        try {
            switch(event.getSubcommandName()) {
                case "create" -> BrewHandler.generateCreatePrompt(eb, prompt, event);
                case "rename" -> BrewHandler.generateRenamePrompt(eb, prompt, event);
            }
        } catch(GenerationException e) {
            eb = new EmbedBuilder(GENERATING_ERROR);
            eb.setDescription(String.format(GENERATING_ERROR.getDescriptionBuilder().toString(), e.getMessage()));

            BrewHandler.removeUserSession(userId);
            ratelimitMap.remove(userId);
            return new CommandResponse<>(eb.build(), isOnlyEphemeral());
        } catch(Exception e) {
            BrewHandler.removeUserSession(userId);
            ratelimitMap.remove(userId);
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
}
