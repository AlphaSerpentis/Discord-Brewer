package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.data.brewer.UserSession;
import dev.alphaserpentis.bots.brewer.data.discord.DiscordConfig;
import dev.alphaserpentis.bots.brewer.exception.DiscordEntityException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.channel.ChannelManager;
import net.dv8tion.jda.api.managers.channel.middleman.StandardGuildMessageChannelManager;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.CATEGORY;
import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.COLOR;
import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.DESCRIPTION;
import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.NAME;
import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.PERMISSIONS;

public class Interpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Interpreter.class);

    public record InterpreterResult(
            @NonNull ArrayList<String> messages,
            @Nullable ArrayList<GuildChannel> channels,
            @Nullable ArrayList<Role> roles,
            @Nullable OriginalState originalState
    ) {
        public InterpreterResult(@NonNull ArrayList<String> messages) {
            this(messages, null, null, null);
        }

        public InterpreterResult(@NonNull ArrayList<String> messages, @NonNull OriginalState originalState) {
            this(messages, null, null, originalState);
        }

        public InterpreterResult(
                @NonNull ArrayList<String> messages,
                @Nullable ArrayList<GuildChannel> channels,
                @Nullable ArrayList<Role> roles
        ) {
            this(messages, channels, roles, null);
        }
    }

    /**
     * Stores the original state of the guild before edits were made
     * @param categoryData The original data of the categories
     * @param textChannelData The original data of the text channels
     * @param voiceChannelData The original data of the voice channels
     * @param roleData The original data of the roles.
     */
    public record OriginalState(
            @NonNull HashMap<Long, HashMap<String, String>> categoryData,
            @NonNull HashMap<Long, HashMap<String, String>> textChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> voiceChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> forumChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> stageChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> roleData
    ) {
        public OriginalState() {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    /**
     * Interprets and executes the actions
     * @param actions The actions to execute
     * @param validAction The type of action to execute (THIS MUST MATCH THE ACTIONS' ACTION TYPE)
     * @param guild The guild to execute the actions on
     * @return The result of the execution
     */
    @NonNull
    public static InterpreterResult interpretAndExecute(
            @NonNull ArrayList<ParseActions.ExecutableAction> actions,
            @NonNull ParseActions.ValidAction validAction,
            @NonNull Guild guild
    ) {
        ArrayList<GuildChannel> channels = null;
        ArrayList<Role> roles = null;
        OriginalState originalState = null;
        final var messages = new ArrayList<String>();

        if(validAction == ParseActions.ValidAction.CREATE) {
            channels = new ArrayList<>();
            roles = new ArrayList<>();
        } else if(validAction == ParseActions.ValidAction.EDIT) {
            originalState = new OriginalState();
        }

        for(ParseActions.ExecutableAction action : actions) {
            try {
                if(action.action() != validAction)
                    throw new IllegalArgumentException("validAction does not match action.action()");

                switch(action.targetType()) {
                    case CATEGORY -> {
                        switch(validAction) {
                            case CREATE -> channels.add(createCategory(action, guild));
                            case EDIT -> originalState.categoryData.putAll(editCategory(
                                    action,
                                    guild.getCategoriesByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    case TEXT_CHANNEL -> {
                        switch(validAction) {
                            case CREATE -> channels.add(createTextChannel(action, guild));
                            case EDIT -> originalState.textChannelData.putAll(editTextChannel(
                                    action,
                                    guild.getTextChannelsByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    case VOICE_CHANNEL -> {
                        switch(validAction) {
                            case CREATE -> channels.add(createVoiceChannel(action, guild));
                            case EDIT -> originalState.voiceChannelData.putAll(editVoiceChannel(
                                    action,
                                    guild.getVoiceChannelsByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    case FORUM_CHANNEL -> {
                        switch(validAction) {
                            case CREATE -> channels.add(createForumChannel(action, guild));
                            case EDIT -> originalState.forumChannelData.putAll(editForumChannel(
                                    action,
                                    guild.getForumChannelsByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    case STAGE_CHANNEL -> {
                        switch(validAction) {
                            case CREATE -> channels.add(createStageChannel(action, guild));
                            case EDIT -> originalState.stageChannelData.putAll(editStageChannel(
                                    action,
                                    guild.getStageChannelsByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    case ROLE -> {
                        switch(validAction) {
                            case CREATE -> roles.add(createRole(action, guild));
                            case EDIT -> originalState.roleData.putAll(editRole(
                                    action,
                                    guild.getRolesByName(action.target(), true).get(0)
                            ));
                            default -> throw new IllegalArgumentException("Invalid action type");
                        }
                    }
                    default -> throw new IllegalArgumentException("Invalid target type");
                }
            } catch(Exception e) {
                captureError(e, messages);
                LOGGER.error("Error while executing action", e);
            }
        }

        switch(validAction) {
            case CREATE -> {
                return new InterpreterResult(messages, channels, roles);
            }
            case EDIT -> {
                return new InterpreterResult(messages, originalState);
            }
            default -> throw new IllegalArgumentException("Invalid action type");
        }
    }

    @NonNull
    public static InterpreterResult deleteAllChanges(@NonNull UserSession session) {
        var messages = new ArrayList<String>();
        var action = session.getAction();

        switch(action) {
            case CREATE -> {
                var channels = session.getInterpreterResult().channels();
                var roles = session.getInterpreterResult().roles();
                var restActions = new ArrayList<RestAction<?>>();

                for(var channel: channels)
                    restActions.add(channel.delete().onErrorMap(e -> captureError(e, messages)));
                for(var role: roles)
                    restActions.add(role.delete().onErrorMap(e -> captureError(e, messages)));

                if(!restActions.isEmpty())
                    RestAction.allOf(restActions).complete();

                return new InterpreterResult(messages, null, null, null);
            }
            case EDIT -> {
                var guild = session.getJDA().getGuildById(session.getGuildId());
                var originalState = session.getInterpreterResult().originalState();
                var restActions = new ArrayList<RestAction<?>>();

                if(guild == null) {
                    return new InterpreterResult(
                            new ArrayList<>(List.of("Guild not found. Guild ID: " + session.getGuildId()))
                    );
                }

                for(long catId: originalState.categoryData().keySet()) { // Categories
                    try {
                        var cat = checkDiscordEntity(guild.getCategoryById(catId), "Category", catId);

                        restActions.add(
                                cat.getManager().setName(
                                        originalState.categoryData().get(catId).get("name")
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                    } catch(Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long txtChnlId: originalState.textChannelData().keySet()) { // Text channels
                    try {
                        var chnl = checkDiscordEntity(guild.getTextChannelById(txtChnlId), "Text channel", txtChnlId);

                        restActions.add(
                                chnl.getManager().setName(
                                        originalState.textChannelData().get(txtChnlId).get("name")
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                        restActions.add(
                                chnl.getManager().setTopic(
                                        originalState.textChannelData().get(txtChnlId).get("desc")
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                    } catch(Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long vcChnlId: originalState.voiceChannelData().keySet()) { // Voice channels
                    try {
                        var chnl = checkDiscordEntity(guild.getVoiceChannelById(vcChnlId), "Voice channel", vcChnlId);

                        restActions.add(
                                chnl.getManager().setName(
                                        originalState.voiceChannelData().get(vcChnlId).get("name")
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                    } catch(Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long roleId: originalState.roleData().keySet()) { // Roles
                    try {
                        var role = checkDiscordEntity(guild.getRoleById(roleId), "Role", roleId);

                        restActions.add(
                                role.getManager().setName(
                                        originalState.roleData().get(roleId).get("name")
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                        restActions.add(
                                role.getManager().setColor(
                                        Color.decode(originalState.roleData().get(roleId).get("color"))
                                ).onErrorMap(e -> captureError(e, messages))
                        );
                    } catch(Exception e) {
                        messages.add(e.getMessage());
                    }
                }

                if(!restActions.isEmpty())
                    RestAction.allOf(restActions).complete();

                return new InterpreterResult(messages);
            }
            default -> throw new IllegalArgumentException("Invalid action type");
        }
    }

    @SuppressWarnings("unchecked")
    private static Category createCategory(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var data = action.data();
        var category = guild.createCategory(data.get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
        var permsData = data.get(PERMISSIONS);

        if(permsData != null && !permsData.equals(""))
            assignRolesPermissions((ArrayList<DiscordConfig.ConfigItem.Permission>) permsData, guild, category);

        return category;
    }

    @SuppressWarnings("unchecked")
    private static TextChannel createTextChannel(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        var data = action.data();
        var channel = guild.createTextChannel(data.get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
        var permsData = data.get(PERMISSIONS);

        if(permsData != null && !permsData.equals(""))
            assignRolesPermissions((ArrayList<DiscordConfig.ConfigItem.Permission>) permsData, guild, channel);

        assignCategory((String) data.get(CATEGORY), guild, channel);
        assignDescription((String) data.get(DESCRIPTION), channel.getManager());

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static VoiceChannel createVoiceChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var data = action.data();
        var channel = guild.createVoiceChannel(data.get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
        var permsData = data.get(PERMISSIONS);

        if(permsData != null && !permsData.equals(""))
            assignRolesPermissions((ArrayList<DiscordConfig.ConfigItem.Permission>) permsData, guild, channel);

        assignCategory((String) data.get(CATEGORY), guild, channel);

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static ForumChannel createForumChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var data = action.data();
        var channel = guild.createForumChannel(data.get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
        var permsData = data.get(PERMISSIONS);

        if(permsData != null && !permsData.equals(""))
            assignRolesPermissions((ArrayList<DiscordConfig.ConfigItem.Permission>) permsData, guild, channel);

        assignCategory((String) data.get(CATEGORY), guild, channel);

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static StageChannel createStageChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var data = action.data();
        var channel = guild.createStageChannel(data.get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
        var permsData = data.get(PERMISSIONS);

        if(permsData != null && !permsData.equals(""))
            assignRolesPermissions((ArrayList<DiscordConfig.ConfigItem.Permission>) permsData, guild, channel);

        assignCategory((String) data.get(CATEGORY), guild, channel);

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static Role createRole(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        var data = action.data();
        var roleColor = (String) data.get(COLOR);
        var allowedPerms = ((ArrayList<DiscordConfig.ConfigItem.Permission>) data.get(PERMISSIONS)).get(0).allow();
        Role role;

        try {
            role = guild
                    .createRole()
                    .setName(data.get(NAME).toString())
                    .setPermissions(
                            allowedPerms == null ? null : Permission.getPermissions(Long.parseLong(allowedPerms))
                    )
                    .setColor(roleColor == null ? null : Color.decode(roleColor))
                    .completeAfter(1, TimeUnit.SECONDS);
        } catch(InsufficientPermissionException ignored) {
            role = guild
                    .createRole()
                    .setName(data.get(NAME).toString())
                    .completeAfter(1, TimeUnit.SECONDS);
        }

        return role;
    }

    private static HashMap<Long, HashMap<String, String>> editCategory(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Category category
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);

        result.put(
                category.getIdLong(),
                new HashMap<>(1) {{
                    put("name", category.getName());
                }}
        );

        // Set new name if not null or empty
        assignName((String) action.data().get(NAME), category.getManager());

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editTextChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull TextChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);

        result.put(
                channel.getIdLong(),
                new HashMap<>(2) {{
                    put("name", channel.getName());
                    put("desc", channel.getTopic());
                }}
        );

        // Set new name if not null or empty
        assignName((String) action.data().get(NAME), channel.getManager());

        // Set new description if not null or empty
        assignDescription((String) action.data().get(DESCRIPTION), channel.getManager());

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editVoiceChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull VoiceChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        assignName((String) action.data().get(NAME), channel.getManager());

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editForumChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull ForumChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        assignName((String) action.data().get(NAME), channel.getManager());

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editStageChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull StageChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        assignName((String) action.data().get(NAME), channel.getManager());

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editRole(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Role role
    ) {
        var nameData = action.data().get(NAME);
        var colorData = action.data().get(COLOR);
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>(1);
        final Color color = role.getColor();

        result.put(
                role.getIdLong(),
                new HashMap<>(2) {{
                    put("name", role.getName());
                    put("color", color == null ? null : String.valueOf(color.getRGB()));
                }}
        );

        // Set new name if not null or empty
        if(nameData != null && !nameData.equals(""))
            role.getManager().setName(nameData.toString()).completeAfter(1, TimeUnit.SECONDS);

        // Set new color if not null or empty
        if(colorData != null && !colorData.equals(""))
            role.getManager().setColor(Color.decode(colorData.toString())).completeAfter(1, TimeUnit.SECONDS);

        return result;
    }

    private static boolean isPublicRole(@NonNull String roleName) {
        return roleName.equalsIgnoreCase("@everyone") || roleName.equalsIgnoreCase("everyone");
    }

    private static void assignRolesPermissions(
            @NonNull ArrayList<DiscordConfig.ConfigItem.Permission> data,
            @NonNull Guild guild,
            @NonNull IPermissionContainer channel
    ) {
        var executableActions = new ArrayList<RestAction<?>>();

        for(var perm: data) {
            List<Role> roles = guild.getRolesByName(perm.role(), true);
            long allowed = perm.allow() == null ? 0 : Long.parseLong(perm.allow());
            long denied = perm.deny() == null ? 0 : Long.parseLong(perm.deny());

            if(isPublicRole(perm.role())) {
                executableActions.add(
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(allowed)
                                .setDenied(denied)
                                .onErrorMap(e -> ignoreError())
                );
            } else if(!roles.isEmpty()) {
                executableActions.add(
                        channel.upsertPermissionOverride(roles.get(0))
                                .setAllowed(allowed)
                                .setDenied(denied)
                                .onErrorMap(e -> ignoreError())
                );
            }
        }

        if(!executableActions.isEmpty())
            RestAction.allOf(executableActions).complete();
    }

    private static void assignCategory(
            @Nullable String catName,
            @NonNull Guild guild,
            @NonNull StandardGuildChannel channel
    ) {
        if(catName != null && !catName.isEmpty()) {
            List<Category> categories = guild.getCategoriesByName(catName, true);

            if(!categories.isEmpty()) {
                Category category = categories.get(0);

                channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
            }
        }
    }

    private static void assignName(@Nullable String name, @NonNull ChannelManager<?, ?> manager) {
        if(name != null && !name.isEmpty())
            manager.setName(name).completeAfter(1, TimeUnit.SECONDS);
    }

    private static void assignDescription(
            @Nullable String description,
            @NonNull StandardGuildMessageChannelManager<?, ?> manager
    ) {
        if(description != null && !description.isEmpty())
            manager.setTopic(description).completeAfter(1, TimeUnit.SECONDS);
    }

    @Nullable
    private static <T> T ignoreError() {
        return null;
    }

    @Nullable
    private static <T> T captureError(@NonNull Throwable e, @NonNull List<String> messages) {
        messages.add(e.getMessage());
        return null;
    }

    @NonNull
    private static <T> T checkDiscordEntity(@Nullable T entity, @NonNull String name, long id) {
        if(entity == null)
            throw new DiscordEntityException("%s not found. ID: %d".formatted(name, id));
        return entity;
    }
}
