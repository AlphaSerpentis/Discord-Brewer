package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.commands.Brew;
import dev.alphaserpentis.bots.brewer.data.DiscordConfig;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.alphaserpentis.bots.brewer.handler.parser.ParseActions.ValidDataNames.*;

public class Interpreter {
    public record InterpreterResult(
            boolean completeSuccess,
            @Nullable ArrayList<String> messages,
            @Nullable ArrayList<GuildChannel> channels,
            @Nullable ArrayList<Role> roles
    ) {}

    @NonNull
    public static InterpreterResult interpretAndExecute(
            @NonNull ArrayList<ParseActions.ExecutableAction> actions,
            @NonNull Guild guild
    ) {
        final HashMap<String, GuildChannel> channels = new HashMap<>();
        final HashMap<String, Role> roles = new HashMap<>();
        final ArrayList<String> messages = new ArrayList<>();
        boolean completeSuccess = true;

        for (ParseActions.ExecutableAction action : actions) {
            try {
                switch (action.targetType()) {
                    case CATEGORY -> {
                        switch (action.action()) {
                            case CREATE -> {
                                Category category = createCategory(action, guild);
                                channels.put(category.getName(), category);
                            }
                            case EDIT -> editCategory(
                                    action,
                                    guild.getCategoriesByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            );
                        }
                    }
                    case TEXT_CHANNEL -> {
                        switch (action.action()) {
                            case CREATE -> {
                                TextChannel channel = createTextChannel(action, guild);
                                channels.put(channel.getName(), channel);
                            }
                            case EDIT -> editTextChannel(
                                    action,
                                    guild.getTextChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            );
                        }
                    }
                    case VOICE_CHANNEL -> {
                        switch (action.action()) {
                            case CREATE -> {
                                VoiceChannel channel = createVoiceChannel(action, guild);
                                channels.put(channel.getName(), channel);
                            }
                            case EDIT -> editVoiceChannel(
                                    action,
                                    guild.getVoiceChannelsByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            );
                        }
                    }
                    case ROLE -> {
                        switch (action.action()) {
                            case CREATE -> {
                                Role role = createRole(action, guild);
                                roles.put(role.getName(), role);
                            }
                            case EDIT -> editRole(
                                    action,
                                    guild.getRolesByName(
                                            action.target(),
                                            true
                                    ).get(0)
                            );
                        }
                    }
                }
            } catch (Exception e) {
                completeSuccess = false;
                messages.add(e.getMessage());
            }
        }

        return new InterpreterResult(
                completeSuccess,
                messages,
                new ArrayList<>(channels.values()),
                new ArrayList<>(roles.values())
        );
    }

    @NonNull
    public static InterpreterResult deleteAllChanges(@NonNull Brew.UserSession session) {
        ArrayList<String> messages = new ArrayList<>();
        ParseActions.ValidAction action = session.getAction();

        if(action == ParseActions.ValidAction.CREATE) {
            ArrayList<GuildChannel> channels = session.getInterpreterResult().channels();
            ArrayList<Role> roles = session.getInterpreterResult().roles();
            for(GuildChannel channel: channels) {
                try {
                    channel.delete().completeAfter(1, TimeUnit.SECONDS);
                } catch (InsufficientPermissionException e) {
                    messages.add(e.getMessage());
                }
            }
            for(Role role: roles) {
                try {
                    role.delete().completeAfter(1, TimeUnit.SECONDS);
                } catch (InsufficientPermissionException e) {
                    messages.add(e.getMessage());
                }
            }

            return new InterpreterResult(
                    messages.size() == 0,
                    null,
                    null,
                    null
            );
        } else if(action == ParseActions.ValidAction.EDIT) {
            Guild guild = session.getJDA().getGuildById(session.getGuildId());

            if(guild == null) {
                return new InterpreterResult(
                        false,
                        new ArrayList<>(List.of("Guild not found. Guild ID: " + session.getGuildId())),
                        null,
                        null
                );
            }

            for(ParseActions.ExecutableAction executedAction: session.getActionsToExecute()) {
                try {
                    switch (executedAction.targetType()) {
                        case CATEGORY -> {
                            Category category = guild.getCategoriesByName(
                                    executedAction.target(),
                                    true
                            ).get(0);
                            category.getManager().setName(executedAction.target()).completeAfter(1, TimeUnit.SECONDS);
                        }
                        case TEXT_CHANNEL -> {
                            TextChannel channel = guild.getTextChannelsByName(
                                    executedAction.target(),
                                    true
                            ).get(0);
                            channel.getManager().setName(executedAction.target()).completeAfter(1, TimeUnit.SECONDS);
                        }
                        case VOICE_CHANNEL -> {
                            VoiceChannel channel = guild.getVoiceChannelsByName(
                                    executedAction.target(),
                                    true
                            ).get(0);
                            channel.getManager().setName(executedAction.target()).completeAfter(1, TimeUnit.SECONDS);
                        }
                        case ROLE -> {
                            Role role = guild.getRolesByName(
                                    executedAction.target(),
                                    true
                            ).get(0);
                            role.getManager().setName(executedAction.target()).completeAfter(1, TimeUnit.SECONDS);
                        }
                    }
                } catch (Exception e) {
                    messages.add(e.getMessage());
                }
            }

            return new InterpreterResult(
                    messages.size() == 0,
                    messages,
                    null,
                    null
            );
        }

        throw new IllegalStateException("Unexpected value: " + action);
    }

    @SuppressWarnings("unchecked")
    private static Category createCategory(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        Category category = guild.createCategory(action.data().get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);

        // Check if there's a data name for permissions
        if(action.data().containsKey(PERMISSIONS)) {
            if(
                    action.data().get(PERMISSIONS).equals("")
                            || action.data().get(PERMISSIONS) == null
            ) {
                return category;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(DiscordConfig.ConfigItem.Permission perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);

                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        category.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(perm.allow()))
                                .setDenied(Long.parseLong(perm.deny()))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        category.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(perm.allow()))
                                .setDenied(Long.parseLong(perm.deny()))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        return category;
    }

    @SuppressWarnings("unchecked")
    private static TextChannel createTextChannel(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        TextChannel channel = guild
                .createTextChannel(
                        action.data().get(NAME).toString()
                ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(action.data().get(PERMISSIONS).equals("")
                    || action.data().get(PERMISSIONS) == null
            ) {
                return channel;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(DiscordConfig.ConfigItem.Permission perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed, denied;

                    allowed = perm.allow();
                    denied = perm.deny();

                    if(allowed == null)
                        allowed = "0";
                    if(denied == null)
                        denied = "0";

                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(action.data().containsKey(CATEGORY)) {
            if(!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null) {
                List<Category> categories = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true);
                Category category;

                if(categories.size() > 0) {
                    category = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true).get(0);
                } else {
                    return channel;
                }

                channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
            }
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static VoiceChannel createVoiceChannel(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        VoiceChannel channel = guild
                .createVoiceChannel(
                        action.data().get(NAME).toString()
                ).completeAfter(1, TimeUnit.SECONDS);

        if(action.data().containsKey(PERMISSIONS)) {
            if(
                    action.data().get(PERMISSIONS).equals("")
                            || action.data().get(PERMISSIONS) == null
            ) {
                return channel;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get(PERMISSIONS);

            for(DiscordConfig.ConfigItem.Permission perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);
                    String allowed, denied;

                    allowed = perm.allow();
                    denied = perm.deny();

                    if(allowed == null)
                        allowed = "0";
                    if(denied == null)
                        denied = "0";


                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        channel.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .completeAfter(1, TimeUnit.SECONDS);
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(action.data().containsKey(CATEGORY)) {
            if(!action.data().get(CATEGORY).equals("") || action.data().get(CATEGORY) == null) {
                List<Category> categories = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true);
                Category category;

                if(categories.size() > 0) {
                    category = guild.getCategoriesByName(action.data().get(CATEGORY).toString(), true).get(0);
                } else {
                    return channel;
                }

                channel.getManager().setParent(category).completeAfter(1, TimeUnit.SECONDS);
            }
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

    private static void editCategory(@NonNull ParseActions.ExecutableAction action, @NonNull Category category) {
        // Set new name if not null or empty
        if(action.data().containsKey(NAME)) {
            if(!action.data().get(NAME).equals("") || action.data().get(NAME) != null) {
                category.getManager().setName(action.data().get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
            }
        }
    }

    private static void editTextChannel(@NonNull ParseActions.ExecutableAction action, @NonNull TextChannel channel) {
        // Set new name if not null or empty
        if(action.data().containsKey(NAME)) {
            if(!action.data().get(NAME).equals("") || action.data().get(NAME) != null) {
                channel.getManager().setName(action.data().get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
            }
        }

        // Set new description if not null or empty
        if(action.data().containsKey(DESCRIPTION)) {
            if(!action.data().get(DESCRIPTION).equals("") || action.data().get(DESCRIPTION) != null) {
                channel.getManager().setTopic(action.data().get(DESCRIPTION).toString()).completeAfter(1, TimeUnit.SECONDS);
            }
        }
    }

    private static void editVoiceChannel(@NonNull ParseActions.ExecutableAction action, @NonNull VoiceChannel channel) {
        // Set new name if not null or empty
        if(action.data().containsKey(NAME)) {
            if(!action.data().get(NAME).equals("") || action.data().get(NAME) != null) {
                channel.getManager().setName(action.data().get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
            }
        }
    }

    private static void editRole(@NonNull ParseActions.ExecutableAction action, @NonNull Role role) {
        // Set new name if not null or empty
        if(action.data().containsKey(NAME)) {
            if(!action.data().get(NAME).equals("") || action.data().get(NAME) != null) {
                role.getManager().setName(action.data().get(NAME).toString()).completeAfter(1, TimeUnit.SECONDS);
            }
        }

        // Set new color if not null or empty
        if(action.data().containsKey(COLOR)) {
            if(!action.data().get(COLOR).equals("") || action.data().get(COLOR) != null) {
                role.getManager().setColor(Color.decode(action.data().get(COLOR).toString())).completeAfter(1, TimeUnit.SECONDS);
            }
        }
    }
}
