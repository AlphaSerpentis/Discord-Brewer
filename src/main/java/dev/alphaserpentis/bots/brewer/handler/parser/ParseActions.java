package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.data.DiscordConfig;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ParseActions {

    public record ExecutableAction(
            @NonNull ValidTarget target,
            @NonNull ValidAction action,
            @NonNull Map<String, Object> data
    ) {
        @Override
        public String toString() {
            return String.format(
                    "\nAction: %s %s\nData: %s",
                    action.readable,
                    target.readable,
                    data.toString()
            );
        }
    }

    public enum ValidTarget {
        CATEGORY ("cat", "Category"),
        TEXT_CHANNEL ("chnl-txt", "Text Channel"),
        VOICE_CHANNEL ("chnl-vc", "Voice Channel"),
        ROLE ("role", "Role");

        public final String role;
        public final String readable;

        ValidTarget(String role, String readable) {
            this.role = role;
            this.readable = readable;
        }
    }

    public enum ValidAction {
        CREATE ("create", "Create"),
        EDIT ("edit", "Edit");

        public final String role;
        public final String readable;

        ValidAction(String role, String readable) {
            this.role = role;
            this.readable = readable;
        }
    }

    public enum ValidDataNames {
        NAME ("name", "Name"),
        DESCRIPTION ("desc", "Description"),
        CATEGORY ("cat", "Category"),
        PERMISSIONS ("perms", "Permissions"),
        COLOR ("color", "Color"),
        VALUE ("value", "Value");

        public final String role;
        public final String readable;

        ValidDataNames(String role, String readable) {
            this.role = role;
            this.readable = readable;
        }
    }

    public enum ValidPermissions {
        ROLE ("role"),
        ALLOW ("allow"),
        DENY ("deny");

        public final String role;

        ValidPermissions(String role) {
            this.role = role;
        }
    }

    @NonNull
    public static ArrayList<ExecutableAction> parseActions(@NonNull DiscordConfig config) {
        ArrayList<ExecutableAction> actions = new ArrayList<>();

        for (Map.Entry<String, DiscordConfig.ConfigItem> entry : config.roles().entrySet()) {
            String roleName = config.roles().get(entry.getKey()).name();

            actions.add(new ExecutableAction(
                    ValidTarget.ROLE,
                    ValidAction.CREATE,
                    Map.of(
                            ValidDataNames.NAME.role, Objects.requireNonNullElse(roleName, ""),
                            ValidDataNames.COLOR.role, Objects.requireNonNullElse(entry.getValue().color(), ""),
                            ValidDataNames.PERMISSIONS.role, Objects.requireNonNullElse(entry.getValue().perms(), "")
                    )
            ));
        }
        for (Map.Entry<String, DiscordConfig.ConfigItem> entry : config.categories().entrySet()) {
            actions.add(new ExecutableAction(
                    ValidTarget.CATEGORY,
                    ValidAction.CREATE,
                    Map.of(
                            ValidDataNames.NAME.role, Objects.requireNonNullElse(entry.getValue().name(), ""),
                            ValidDataNames.PERMISSIONS.role, Objects.requireNonNullElse(entry.getValue().perms(), "")
                    )
            ));
        }
        for (Map.Entry<String, DiscordConfig.ConfigItem> entry : config.channels().entrySet()) {
            String catName = config.channels().get(entry.getKey()).cat();

            for (Map.Entry<String, DiscordConfig.ConfigItem> catEntry: config.categories().entrySet()) {
                if (catEntry.getKey().equals(catName) || catEntry.getValue().name().equals(catName)) {
                    catName = catEntry.getValue().name();
                    break;
                }
            }

            actions.add(new ExecutableAction(
                    ValidTarget.TEXT_CHANNEL,
                    ValidAction.CREATE,
                    Map.of(
                            ValidDataNames.NAME.role, Objects.requireNonNullElse(entry.getValue().name(), ""),
                            ValidDataNames.DESCRIPTION.role, Objects.requireNonNullElse(entry.getValue().desc(), ""),
                            ValidDataNames.CATEGORY.role, Objects.requireNonNullElse(catName, ""),
                            ValidDataNames.PERMISSIONS.role, Objects.requireNonNullElse(entry.getValue().perms(), "")
                    )
            ));
        }

        return actions;
    }
}
