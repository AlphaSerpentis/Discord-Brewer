package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.data.discord.DiscordConfig;
import dev.alphaserpentis.bots.brewer.data.brewer.UserSession;
import dev.alphaserpentis.bots.brewer.exception.DiscordEntityException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.*;

public class Interpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Interpreter.class);

    public record InterpreterResult(
            boolean completeSuccess,
            @Nullable ArrayList<String> messages,
            @Nullable ArrayList<GuildChannel> channels,
            @Nullable ArrayList<Role> roles,
            @Nullable OriginalState originalState
    ) {
        public InterpreterResult(
                boolean completeSuccess,
                @Nullable ArrayList<String> messages
        ) {
            this(completeSuccess, messages, null, null, null);
        }

        public InterpreterResult(
                boolean completeSuccess,
                @Nullable ArrayList<String> messages,
                @Nullable ArrayList<GuildChannel> channels,
                @Nullable ArrayList<Role> roles
        ) {
            this(completeSuccess, messages, channels, roles, null);
        }

        public InterpreterResult(
                boolean completeSuccess,
                @Nullable ArrayList<String> messages,
                @NonNull OriginalState originalState
        ) {
            this(completeSuccess, messages, null, null, originalState);
        }
    }

    /**
     * Stores the original state of the guild before edits were made
     * @param originalCategoryData The original data of the categories
     * @param originalTextChannelData The original data of the text channels
     * @param originalVoiceChannelData The original data of the voice channels
     * @param originalRoleData The original data of the roles.
     */
    public record OriginalState(
            @NonNull HashMap<Long, HashMap<String, String>> originalCategoryData,
            @NonNull HashMap<Long, HashMap<String, String>> originalTextChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> originalVoiceChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> originalForumChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> originalStageChannelData,
            @NonNull HashMap<Long, HashMap<String, String>> originalRoleData
    ) {
        public OriginalState() {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

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
        boolean completeSuccess = true;

        if(validAction == ParseActions.ValidAction.CREATE) {
            channels = new ArrayList<>();
            roles = new ArrayList<>();
        } else if(validAction == ParseActions.ValidAction.EDIT) {
            originalState = new OriginalState();
        }

        for(ParseActions.ExecutableAction action : actions) {
            try {
                switch(action.targetType()) {
                    case CATEGORY -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var category = createCategory(action, guild);
                                channels.add(category);
                            }
                            case EDIT -> originalState.originalCategoryData.putAll(editCategory(
                                    action,
                                    guild.getCategoriesByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    case TEXT_CHANNEL -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var channel = createTextChannel(action, guild);
                                channels.add(channel);
                            }
                            case EDIT -> originalState.originalTextChannelData.putAll(editTextChannel(
                                    action,
                                    guild.getTextChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    case VOICE_CHANNEL -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var channel = createVoiceChannel(action, guild);
                                channels.add(channel);
                            }
                            case EDIT -> originalState.originalVoiceChannelData.putAll(editVoiceChannel(
                                    action,
                                    guild.getVoiceChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    case FORUM_CHANNEL -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var channel = createForumChannel(action, guild);
                                channels.add(channel);
                            }
                            case EDIT -> originalState.originalForumChannelData.putAll(editForumChannel(
                                    action,
                                    guild.getForumChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    case STAGE_CHANNEL -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var channel = createStageChannel(action, guild);
                                channels.add(channel);
                            }
                            case EDIT -> originalState.originalStageChannelData.putAll(editStageChannel(
                                    action,
                                    guild.getStageChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    case ROLE -> {
                        switch(action.action()) {
                            case CREATE -> {
                                var role = createRole(action, guild);
                                roles.add(role);
                            }
                            case EDIT -> originalState.originalRoleData.putAll(editRole(
                                    action,
                                    guild.getRolesByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            ));
                        }
                    }
                    default -> throw new IllegalArgumentException("Invalid target type");
                }
            } catch(Exception e) {
                completeSuccess = false;
                messages.add(e.getMessage());

                LOGGER.error("Error while executing action", e);
            }
        }

        switch(validAction) {
            case CREATE -> {
                return new InterpreterResult(
                        completeSuccess,
                        messages,
                        channels,
                        roles
                );
            }
            case EDIT -> {
                return new InterpreterResult(
                        completeSuccess,
                        messages,
                        originalState
                );
            }
            default -> throw new IllegalArgumentException("Invalid action type");
        }
    }

    @NonNull
    public static InterpreterResult deleteAllChanges(@NonNull UserSession session) {
        var messages = new ArrayList<String>();
        ParseActions.ValidAction action = session.getAction();

        switch(action) {
            case CREATE -> {
                var channels = session.getInterpreterResult().channels();
                var roles = session.getInterpreterResult().roles();

                for(var channel: channels) {
                    try {
                        channel.delete().completeAfter(1, TimeUnit.SECONDS);
                    } catch (InsufficientPermissionException e) {
                        messages.add(e.getMessage());
                    }
                }
                for(var role: roles) {
                    try {
                        role.delete().completeAfter(1, TimeUnit.SECONDS);
                    } catch (InsufficientPermissionException e) {
                        messages.add(e.getMessage());
                    }
                }

                return new InterpreterResult(
                        messages.isEmpty(),
                        null,
                        null,
                        null,
                        null
                );
            }
            case EDIT -> {
                var guild = session.getJDA().getGuildById(session.getGuildId());
                var originalState = session.getInterpreterResult().originalState();

                if(guild == null) {
                    return new InterpreterResult(
                            false,
                            new ArrayList<>(List.of("Guild not found. Guild ID: " + session.getGuildId()))
                    );
                }

                for(long categoryId: originalState.originalCategoryData().keySet()) {
                    try {
                        var category = guild.getCategoryById(categoryId);

                        if(category == null)
                            throw new DiscordEntityException("Category not found. Category ID: " + categoryId);

                        category.getManager().setName(
                                originalState.originalCategoryData().get(categoryId).get("name")
                        ).completeAfter(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long textChannelId: originalState.originalTextChannelData().keySet()) {
                    try {
                        var channel = guild.getTextChannelById(textChannelId);

                        if(channel == null)
                            throw new DiscordEntityException("Text channel not found. Text channel ID: " + textChannelId);

                        channel.getManager().setName(
                                originalState.originalTextChannelData().get(textChannelId).get("name")
                        ).completeAfter(1, TimeUnit.SECONDS);

                        channel.getManager().setTopic(
                                originalState.originalTextChannelData().get(textChannelId).get("desc")
                        ).completeAfter(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long voiceChannelId: originalState.originalVoiceChannelData().keySet()) {
                    try {
                        var channel = guild.getVoiceChannelById(voiceChannelId);

                        if(channel == null)
                            throw new DiscordEntityException("Voice channel not found. Voice channel ID: " + voiceChannelId);

                        channel.getManager().setName(
                                originalState.originalVoiceChannelData().get(voiceChannelId).get("name")
                        ).completeAfter(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        messages.add(e.getMessage());
                    }
                }
                for(long roleId: originalState.originalRoleData().keySet()) {
                    try {
                        var role = guild.getRoleById(roleId);

                        if(role == null)
                            throw new DiscordEntityException("Role not found. Role ID: " + roleId);

                        role.getManager().setName(
                                originalState.originalRoleData().get(roleId).get("name")
                        ).completeAfter(1, TimeUnit.SECONDS);

                        role.getManager().setColor(
                                Color.decode(originalState.originalRoleData().get(roleId).get("color"))
                        ).completeAfter(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        messages.add(e.getMessage());
                    }
                }

                return new InterpreterResult(
                        messages.isEmpty(),
                        messages
                );
            }
            default -> throw new IllegalArgumentException("Invalid action type");
        }
    }

    @SuppressWarnings("unchecked")
    private static Category createCategory(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var category = guild.createCategory(
                action.data().get(NAME).toString()
        ).completeAfter(1, TimeUnit.SECONDS);

        // Check if there's a data name for permissions
        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("") || action.data().get(PERMISSIONS) == null) {
                return category;
            }

            var permsData = (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(var perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed = perm.allow() == null ? "0" : perm.allow();
                    String denied = perm.deny() == null ? "0" : perm.deny();

                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        category.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(!roles.isEmpty()) {
                        var role = roles.get(0);

                        category.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        return category;
    }

    @SuppressWarnings("unchecked")
    private static TextChannel createTextChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var channel = guild.createTextChannel(
                action.data().get(NAME).toString()
        ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("")
                    || action.data().get(PERMISSIONS) == null
            ) {
                return channel;
            }

            var permsData = (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(var perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed = perm.allow() == null ? "0" : perm.allow();
                    String denied = perm.deny() == null ? "0" : perm.deny();

                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(!roles.isEmpty()) {
                        var role = roles.get(0);

                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(
                action.data().containsKey(CATEGORY)
                        && (!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null)
        ) {
            List<Category> categories = guild.getCategoriesByName(
                    action.data().get(CATEGORY).toString(),
                    true
            );
            Category category;

            if(!categories.isEmpty()) {
                category = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true).get(0);
            } else {
                return channel;
            }

            channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static VoiceChannel createVoiceChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var channel = guild
                .createVoiceChannel(
                        action.data().get(NAME).toString()
                ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("") || action.data().get(PERMISSIONS) == null) {
                return channel;
            }

            var permsData = (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(var perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed = perm.allow() == null ? "0" : perm.allow();
                    String denied = perm.deny() == null ? "0" : perm.deny();

                    if(
                            perm.role().equalsIgnoreCase("@everyone") ||
                                    perm.role().equalsIgnoreCase("everyone")
                    ) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(!roles.isEmpty()) {
                        var role = roles.get(0);

                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(
                action.data().containsKey(CATEGORY)
                        && (!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null)
        ) {
            List<Category> categories = guild.getCategoriesByName(
                    action.data().get(CATEGORY).toString(),
                    true
            );
            Category category;

            if(!categories.isEmpty()) {
                category = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true).get(0);
            } else {
                return channel;
            }

            channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static ForumChannel createForumChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var channel = guild
                .createForumChannel(
                        action.data().get(NAME).toString()
                ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("") || action.data().get(PERMISSIONS) == null) {
                return channel;
            }

            var permsData = (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(var perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed = perm.allow() == null ? "0" : perm.allow();
                    String denied = perm.deny() == null ? "0" : perm.deny();

                    if(
                            perm.role().equalsIgnoreCase("@everyone") ||
                                    perm.role().equalsIgnoreCase("everyone")
                    ) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(!roles.isEmpty()) {
                        var role = roles.get(0);

                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(
                action.data().containsKey(CATEGORY)
                        && (!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null)
        ) {
            List<Category> categories = guild.getCategoriesByName(
                    action.data().get(CATEGORY).toString(),
                    true
            );
            Category category;

            if(!categories.isEmpty()) {
                category = guild.getCategoriesByName(
                        action.data().get(CATEGORY).toString(),
                        true
                ).get(0);
            } else {
                return channel;
            }

            channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static StageChannel createStageChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Guild guild
    ) {
        var channel = guild
                .createStageChannel(
                        action.data().get(NAME).toString()
                ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("") || action.data().get(PERMISSIONS) == null) {
                return channel;
            }

            var permsData = (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(var perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed = perm.allow() == null ? "0" : perm.allow();
                    String denied = perm.deny() == null ? "0" : perm.deny();

                    if(
                            perm.role().equalsIgnoreCase("@everyone") ||
                                    perm.role().equalsIgnoreCase("everyone")
                    ) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(!roles.isEmpty()) {
                        var role = roles.get(0);

                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(
                action.data().containsKey(CATEGORY)
                        && (!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null)
        ) {
            List<Category> categories = guild.getCategoriesByName(
                    action.data().get(CATEGORY).toString(),
                    true
            );
            Category category;

            if(!categories.isEmpty()) {
                category = guild.getCategoriesByName(
                        action.data().get(CATEGORY).toString(),
                        true
                ).get(0);
            } else {
                return channel;
            }

            channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static Role createRole(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        String roleColor = (String) action.data().get(COLOR);
        String allowedPerms =
                ((ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(
                        PERMISSIONS)
                ).get(0).allow();
        Role role;

        try {
            role = guild
                    .createRole()
                    .setName(
                            action.data().get(NAME).toString()
                    )
                    .setPermissions(
                            allowedPerms == null ? null : Permission.getPermissions(Long.parseLong(allowedPerms))
                    )
                    .setColor(
                            roleColor == null ? null : Color.decode(roleColor)
                    )
                    .completeAfter(1, TimeUnit.SECONDS);
        } catch(InsufficientPermissionException ignored) {
            role = guild
                    .createRole()
                    .setName(
                            action.data().get(NAME).toString()
                    )
                    .completeAfter(1, TimeUnit.SECONDS);
        }

        return role;
    }

    private static HashMap<Long, HashMap<String, String>> editCategory(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Category category
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();

        result.put(
                category.getIdLong(),
                new HashMap<>(1) {{
                    put("name", category.getName());
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            category.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editTextChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull TextChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();

        result.put(
                channel.getIdLong(),
                new HashMap<>(2) {{
                    put("name", channel.getName());
                    put("desc", channel.getTopic());
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            channel.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        // Set new description if not null or empty
        if(
                action.data().containsKey(DESCRIPTION)
                        && (!action.data().get(DESCRIPTION).equals("") || action.data().get(DESCRIPTION) != null)
        ) {
            channel.getManager().setTopic(
                    action.data().get(DESCRIPTION).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editVoiceChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull VoiceChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            channel.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editForumChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull ForumChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            channel.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editStageChannel(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull StageChannel channel
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();

        result.put(
                channel.getIdLong(),
                new HashMap<>(1) {{
                    put("name", channel.getName());
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            channel.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }

    private static HashMap<Long, HashMap<String, String>> editRole(
            @NonNull ParseActions.ExecutableAction action,
            @NonNull Role role
    ) {
        final HashMap<Long, HashMap<String, String>> result = new HashMap<>();
        final Color color = role.getColor();

        result.put(
                role.getIdLong(),
                new HashMap<>(2) {{
                    put("name", role.getName());
                    put("color", color == null ? null : String.valueOf(color.getRGB()));
                }}
        );

        // Set new name if not null or empty
        if(
                action.data().containsKey(NAME)
                        && (!action.data().get(NAME).equals("") || action.data().get(NAME) != null)
        ) {
            role.getManager().setName(
                    action.data().get(NAME).toString()
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        // Set new color if not null or empty
        if(
                action.data().containsKey(COLOR)
                        && (!action.data().get(COLOR).equals("") || action.data().get(COLOR) != null)
        ) {
            role.getManager().setColor(
                    Color.decode(action.data().get(COLOR).toString())
            ).completeAfter(1, TimeUnit.SECONDS);
        }

        return result;
    }
}
