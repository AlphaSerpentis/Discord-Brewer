package dev.alphaserpentis.bots.brewer.handler.parser;

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
                switch (action.target()) {
                    case CATEGORY -> {
                        switch (action.action()) {
                            case CREATE -> {
                                Category category = createCategory(action, guild);
                                channels.put(category.getName(), category);
                            }
                            case EDIT -> editCategory(
                                    action,
                                    (Category) channels.get(action.data().get("name").toString())
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
                                    (TextChannel) channels.get(action.data().get("name").toString())
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
                                    (VoiceChannel) channels.get(action.data().get("name").toString())
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
                                    roles.get(action.data().get("name").toString())
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

    public static InterpreterResult deleteAllChanges(@NonNull ArrayList<GuildChannel> channels, @NonNull ArrayList<Role> roles) {
        ArrayList<String> messages = new ArrayList<>();

        for(GuildChannel channel: channels) {
            try {
                channel.delete().complete();
            } catch (InsufficientPermissionException e) {
                messages.add(e.getMessage());
            }
        }
        for(Role role: roles) {
            try {
                role.delete().complete();
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
    }

    @SuppressWarnings("unchecked")
    private static Category createCategory(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        Category category = guild.createCategory(action.data().get("name").toString()).complete();

        // Check if there's a data name for permissions
        if(action.data().containsKey("perms")) {
            if(action.data().get("perms").equals("") || action.data().get("perms") == null) {
                return category;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get("perms");

            for(DiscordConfig.ConfigItem.Permission perm: permsData) {
                try {
                    List<Role> roles = guild.getRolesByName(perm.role(), true);

                    if(perm.role().equalsIgnoreCase("@everyone") || perm.role().equalsIgnoreCase("everyone")) {
                        category.upsertPermissionOverride(guild.getPublicRole())
                                .setAllowed(Long.parseLong(perm.allow()))
                                .setDenied(Long.parseLong(perm.deny()))
                                .complete();
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        category.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(perm.allow()))
                                .setDenied(Long.parseLong(perm.deny()))
                                .complete();
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
                        action.data().get("name").toString()
                ).complete();

        if(action.data().containsKey("perms")) {
            if(action.data().get("perms").equals("") || action.data().get("perms") == null) {
                return channel;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get("perms");

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
                                .complete();
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .complete();
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(action.data().containsKey("cat")) {
            if(!action.data().get("cat").equals("") || action.data().get("cat") == null) {
                List<Category> categories = guild.getCategoriesByName(action.data().get("cat").toString(), true);
                Category category;

                if(categories.size() > 0) {
                    category = guild.getCategoriesByName(action.data().get("cat").toString(), true).get(0);
                } else {
                    return channel;
                }

                channel.getManager().setParent(category).complete();
            }
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static VoiceChannel createVoiceChannel(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        VoiceChannel channel = guild
                .createVoiceChannel(
                        action.data().get("name").toString()
                ).complete();

        if(action.data().containsKey("perms")) {
            if(action.data().get("perms").equals("") || action.data().get("perms") == null) {
                return channel;
            }

            ArrayList<DiscordConfig.ConfigItem.Permission> permsData =
                    (ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get("perms");

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
                                .complete();
                    } else if(roles.size() > 0) {
                        Role role = roles.get(0);
                        channel.upsertPermissionOverride(role)
                                .setAllowed(Long.parseLong(allowed))
                                .setDenied(Long.parseLong(denied))
                                .complete();
                    }
                } catch(InsufficientPermissionException ignored) {}
            }
        }

        if(action.data().containsKey("cat")) {
            if(!action.data().get("cat").equals("") || action.data().get("cat") == null) {
                List<Category> categories = guild.getCategoriesByName(action.data().get("cat").toString(), true);
                Category category;

                if(categories.size() > 0) {
                    category = guild.getCategoriesByName(action.data().get("cat").toString(), true).get(0);
                } else {
                    return channel;
                }

                channel.getManager().setParent(category).complete();
            }
        }

        return channel;
    }

    @SuppressWarnings("unchecked")
    private static Role createRole(@NonNull ParseActions.ExecutableAction action, @NonNull Guild guild) {
        String roleColor = (String) action.data().get("color");
        String allowedPerms = ((ArrayList<DiscordConfig.ConfigItem.Permission>) action.data().get("perms")).get(0).allow();
        Role role;

        try {
            role = guild
                    .createRole()
                    .setName(
                            action.data().get("name").toString()
                    )
                    .setPermissions(
                            allowedPerms == null ? null : Permission.getPermissions(Long.parseLong(allowedPerms))
                    )
                    .setColor(
                            roleColor == null ? null : Color.decode(roleColor)
                    )
                    .complete();
        } catch(InsufficientPermissionException ignored) {
            role = guild
                    .createRole()
                    .setName(
                            action.data().get("name").toString()
                    )
                    .complete();
        }

        return role;
    }

    private static void editCategory(@NonNull ParseActions.ExecutableAction action, @NonNull Category category) {

    }

    private static void editTextChannel(@NonNull ParseActions.ExecutableAction action, @NonNull TextChannel channel) {

    }

    private static void editVoiceChannel(@NonNull ParseActions.ExecutableAction action, @NonNull VoiceChannel channel) {

    }

    private static void editRole(@NonNull ParseActions.ExecutableAction action, @NonNull Role role) {

    }

}
