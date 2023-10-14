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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Brew extends ButtonCommand<MessageEmbed, SlashCommandInteractionEvent>
        implements AcknowledgeableCommand<SlashCommandInteractionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Brew.class);
    private final EmbedBuilder NO_PERMISSIONS = new EmbedBuilder()
            .setTitle("No Permissions")
            .setDescription("""
                    You do not have the required permissions to run this command.

                    Must be the server owner or have `Administrator` permissions.""")
            .setColor(Color.RED);
    private final EmbedBuilder PROMPT_REJECTED = new EmbedBuilder()
            .setTitle("Prompt Rejected")
            .setDescription(
                    """
                    The prompt you provided was rejected. Ensure that it does not violate the usage policies of OpenAI.
                    
                    **Continued violations may result in termination from our services.**

                    You can read more about the usage policies of OpenAI here: https://openai.com/policies/usage-policies"""
            )
            .setColor(Color.RED);
    private final EmbedBuilder BREWING_UP = new EmbedBuilder()
            .setTitle("Brewing Up")
            .setDescription("""
            Brewing up your server...
            
            **This may take a while due to rate limits.**""")
            .setColor(Color.ORANGE);
    private final EmbedBuilder GENERATING_NEW_BREW = new EmbedBuilder()
            .setTitle("Generating New Brew")
            .setDescription("Generating a new brew...")
            .setColor(Color.ORANGE);
    private final EmbedBuilder GENERATING_ERROR = new EmbedBuilder()
            .setTitle("Error Generating Brew")
            .setDescription("""
                    An error occurred while generating a brew.
                    
                    You can try again by pressing the "Brew" button below.
                    
                    Report this over at https://brewr.ai/discord
                    
                    Error Message:
                    %s""")
            .setColor(Color.RED);
    private final EmbedBuilder CANCELLED = new EmbedBuilder()
            .setTitle("Cancelled")
            .setDescription("Cancelled the current session.")
            .setColor(Color.RED);
    private final EmbedBuilder REVERTING = new EmbedBuilder()
            .setTitle("Reverting")
            .setDescription("""
            Reverting the changes made to the server...
            
            **This may take a while due to Discord rate limits.**""")
            .setColor(Color.ORANGE);
    private final EmbedBuilder POST_EXECUTION_NO_ERROR = new EmbedBuilder()
            .setTitle("Server Brewed Up!")
            .setDescription("The server has been successfully brewed up!%s")
            .setColor(Color.GREEN);
    private final EmbedBuilder POST_EXECUTION_ERROR = new EmbedBuilder()
            .setTitle("Server Brew Attempted")
            .setDescription("""
                    A brew was attempted, but there were errors.
                    
                    You can revert any changes made by pressing the "Revert" button below.
                    
                    Report this over at https://brewr.ai/discord
                    
                    Error Message:
                    %s""")
            .setColor(Color.ORANGE);
    private final EmbedBuilder REVERTED_NO_ERROR = new EmbedBuilder()
            .setTitle("Reverted")
            .setDescription("Reverted the changes made to the server.")
            .setColor(Color.GREEN);
    private final EmbedBuilder REVERTED_ERROR = new EmbedBuilder()
            .setTitle("Reverted?")
            .setDescription("""
                    Brew(r) attempted to revert the changes made, but might not have been able to. See below for more details.
                    
                    Report this over at https://brewr.ai/discord
                    
                    Error Message:
                    %s""")
            .setColor(Color.ORANGE);
    private final EmbedBuilder USER_SESSION_NOT_FOUND = new EmbedBuilder()
            .setTitle("User Session Not Found")
            .setDescription("You do not have an active session. Run </brew create:%d> or </brew rename:%d> to start a new session.")
            .setColor(Color.RED);

    public Brew() {
        super(
                new BotCommandOptions("brew", "Setup your Discord server with a prompt!")
                        .setOnlyEmbed(true)
                        .setOnlyEphemeral(false)
                        .setDeferReplies(true)
                        .setUseRatelimits(true)
                        .setRatelimitLength(180)
                        .setCommandVisibility(CommandVisibility.GUILD)
        );

        addButton("brew", ButtonStyle.PRIMARY, "New Brew", Emoji.fromUnicode("☕"),false);
        addButton("confirm", ButtonStyle.SUCCESS, "Confirm", Emoji.fromUnicode("✅"), false);
        addButton("cancel", ButtonStyle.DANGER, "Cancel", Emoji.fromUnicode("✖️"), false);
        addButton("revert", ButtonStyle.DANGER, "Revert", Emoji.fromUnicode("↩️"), false);
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        final var userSession = BrewHandler.getUserSession(event.getUser().getIdLong());
        final var buttonId = event.getComponentId().substring(getName().length() + 1);
        var hook = event.deferEdit().complete();

        if(userSession == null) {
            var eb = new EmbedBuilder(USER_SESSION_NOT_FOUND);
            long guildCommandId = getGuildCommandId(event.getGuild());

            eb.setDescription(
                    String.format(
                            USER_SESSION_NOT_FOUND.getDescriptionBuilder().toString(),
                            guildCommandId,
                            guildCommandId
                    )
            );

            hook.editOriginalComponents().setEmbeds(eb.build()).queue();
            return;
        }

        switch(buttonId) {
            case "brew" -> onBrewButtonClick(userSession, hook);
            case "confirm" -> onConfirmButtonClick(userSession, hook, event);
            case "cancel" -> onCancelButtonClick(hook, event);
            case "revert" -> onRevertButtonClick(userSession, hook, event);
            default -> throw new IllegalStateException("Unexpected value: " + buttonId);
        }
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull SlashCommandInteractionEvent event) {
        final UserSession userSession = BrewHandler.getUserSession(event.getUser().getIdLong());

        if(userSession == null)
            return List.of();
        else if(event.getName().equals(getName()) && userSession.getInteractionToken().equals(event.getToken())) {
            return List.of(
                    getButton("brew"),
                    getButton("confirm"),
                    getButton("cancel")
            );
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder workingEmbed;
        String prompt;
        CommandResponse<MessageEmbed> rateLimitResponse;
        MessageEmbed[] embedsArray;

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch(IOException e) {
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

        // Although this *should* be enforced by Discord as it is configured to only show up for users with Administrator
        // permissions, this is just a fallback *just in case*
        if(!isUserAllowedToRunCommand(event.getMember()))
            return new CommandResponse<>(isOnlyEphemeral(), NO_PERMISSIONS.build());

        // Check if the prompt doesn't get flagged by OpenAI
        prompt = event.getOption("prompt").getAsString();

        if(OpenAIHandler.isContentFlagged(
                prompt,
                userId,
                event.getGuild() != null ? event.getGuild().getIdLong() : 0, true
        ))
            return new CommandResponse<>(isOnlyEphemeral(), PROMPT_REJECTED.build());

        try {
            switch(event.getSubcommandName()) {
                case "create" -> BrewHandler.generateCreatePrompt(workingEmbed, prompt, event);
                case "rename" -> BrewHandler.generateRenamePrompt(workingEmbed, prompt, event);
            }
        } catch(GenerationException e) {
            workingEmbed = new EmbedBuilder(GENERATING_ERROR);

            workingEmbed.setDescription(
                    String.format(GENERATING_ERROR.getDescriptionBuilder().toString(), e.getMessage())
            );
            BrewHandler.removeUserSession(userId);
            ratelimitMap.remove(userId);

            return new CommandResponse<>(isOnlyEphemeral(), true, workingEmbed.build());
        } catch(Exception e) {
            BrewHandler.removeUserSession(userId);
            ratelimitMap.remove(userId);

            throw e;
        }

        return new CommandResponse<>(isOnlyEphemeral(), workingEmbed.build());
    }

    @Override
    public void updateCommand(@NonNull Guild guild) {
        var promptOptionData = new OptionData(
                OptionType.STRING,
                "prompt",
                "Describe a theme, style, or the specifics of what you want!",
                true
        );
        var create = new SubcommandData(
                "create",
                "Create new roles/categories/channels with a prompt!"
        ).addOptions(promptOptionData);
        var rename = new SubcommandData(
                "rename",
                "Rename your preexisting server's channels, roles, and categories!"
        ).addOptions(promptOptionData);
        var cmdData = ((SlashCommandData) getJDACommandData(getCommandType(), getName(), getDescription()))
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(create, rename);

        guild
                .upsertCommand(cmdData)
                .queue();
    }

    private boolean isUserAllowedToRunCommand(@NonNull Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }

    private void onBrewButtonClick(@NonNull UserSession userSession, @NonNull InteractionHook hook) {
        ChatMessage chatMessage;
        var eb = new EmbedBuilder();

        if(userSession.getType() == UserSession.UserSessionType.NEW_BREW) {
            chatMessage = Prompts.SETUP_SYSTEM_PROMPT_CREATE;
        } else {
            chatMessage = Prompts.SETUP_SYSTEM_PROMPT_RENAME;
        }

        hook.editOriginalComponents()
                .setEmbeds(GENERATING_NEW_BREW.build())
                .queue();

        try {
            userSession.setActionsToExecute(
                    BrewHandler.generateActions(
                            chatMessage,
                            userSession.getPrompt(),
                            userSession.getAction()
                    )
            );
        } catch(JsonSyntaxException | IOException | GenerationException e) {
            eb = new EmbedBuilder(GENERATING_ERROR);
            eb.setDescription(String.format(GENERATING_ERROR.getDescriptionBuilder().toString(), e.getMessage()));

            hook
                    .editOriginalComponents()
                    .setEmbeds(eb.build())
                    .setActionRow(getButton("brew"))
                    .queue();

            LOGGER.error("Error while generating brew", e);
        }

        BrewHandler.previewChangesPage(eb, userSession.getActionsToExecute());

        if(userSession.getBrewCount() == 3) {
            hook
                    .editOriginalComponents()
                    .setEmbeds(eb.build())
                    .setActionRow(getButton("confirm"), getButton("cancel"))
                    .queue();
        } else {
            hook
                    .editOriginalComponents()
                    .setEmbeds(eb.build())
                    .setActionRow(getButton("brew"), getButton("confirm"), getButton("cancel"))
                    .queue();
        }

        userSession.setBrewCount((short) (userSession.getBrewCount() + 1));
    }

    private void onConfirmButtonClick(
            @NonNull UserSession userSession,
            @NonNull InteractionHook hook,
            @NonNull ButtonInteractionEvent event
    ) {
        hook.editOriginalComponents().setEmbeds(BREWING_UP.build()).complete();

        Interpreter.InterpreterResult result = Interpreter.interpretAndExecute(
                userSession.getActionsToExecute(),
                userSession.getAction(),
                event.getGuild()
        );

        userSession.setInterpreterResult(result);

        try {
            if(result.completeSuccess()) {
                var eb = new EmbedBuilder(POST_EXECUTION_NO_ERROR);

                if(!VoteHandler.isUserInRemindedMap(event.getUser().getIdLong())) {
                    long voteCommandId = core.getCommandsHandler().getCommand("vote").getGlobalCommandId();

                    eb.setDescription(
                            String.format(
                                    POST_EXECUTION_NO_ERROR.getDescriptionBuilder().toString(),
                                    "\n\nIf you're enjoying the bot, do please vote for Brew(r)! You can do so by clicking [here](https://top.gg/bot/819650039680575488/vote) or by running </vote:" + voteCommandId + ">."
                            )
                    );

                    VoteHandler.addUserToRemindedMap(event.getUser().getIdLong());
                } else {
                    eb.setDescription(
                            String.format(POST_EXECUTION_NO_ERROR.getDescriptionBuilder().toString(), "")
                    );
                }

                hook.editOriginalEmbeds(
                        eb.build()
                ).setActionRow(
                        getButton("revert")
                ).queue();
            } else {
                var errorMessages = String.join("\n", result.messages());
                var errorEmbed = new EmbedBuilder(POST_EXECUTION_ERROR)
                        .setDescription(
                                String.format(POST_EXECUTION_ERROR.getDescriptionBuilder().toString(), errorMessages)
                        )
                        .build();

                hook.editOriginalEmbeds(
                        errorEmbed
                ).setActionRow(
                        getButton("revert")
                ).queue();
            }
        } catch (Exception ignored) {
            BrewHandler.removeUserSession(event.getUser().getIdLong());
        }
    }

    private void onCancelButtonClick(@NonNull InteractionHook hook, @NonNull ButtonInteractionEvent event) {
        hook.editOriginalComponents().setEmbeds(CANCELLED.build()).queue();
        BrewHandler.removeUserSession(event.getUser().getIdLong());
    }

    private void onRevertButtonClick(
            @NonNull UserSession userSession,
            @NonNull InteractionHook hook,
            @NonNull ButtonInteractionEvent event
    ) {
        Interpreter.InterpreterResult result;
        hook.editOriginalComponents().setEmbeds(REVERTING.build()).complete();

        try {
            result = Interpreter.deleteAllChanges(userSession);
        } catch(Exception e) {
            var errorEmbed = new EmbedBuilder(REVERTED_ERROR)
                    .setDescription(String.format(REVERTED_ERROR.getDescriptionBuilder().toString(), e.getMessage()))
                    .build();

            LOGGER.error("Error while reverting changes", e);
            hook.editOriginalComponents().setEmbeds(errorEmbed).queue();
            return;
        }

        if(result.completeSuccess()) {
            hook.editOriginalEmbeds(REVERTED_NO_ERROR.build()).queue();
        } else {
            var errorMessages = String.join("\n", result.messages());
            var errorEmbed = new EmbedBuilder(REVERTED_ERROR)
                    .setDescription(String.format(REVERTED_ERROR.getDescriptionBuilder().toString(), errorMessages))
                    .build();

            LOGGER.error("Error while reverting changes\n\n%s".formatted(errorMessages));
            hook.editOriginalEmbeds(errorEmbed).queue();
        }

        BrewHandler.removeUserSession(event.getUser().getIdLong());
    }
}
