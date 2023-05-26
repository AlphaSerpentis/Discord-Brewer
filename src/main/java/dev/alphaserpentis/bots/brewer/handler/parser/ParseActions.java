package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.data.DiscordConfig;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ParseActions {

    public record ExecutableAction(
            @NonNull ValidTarget targetType,
            @NonNull String target,
            @NonNull ValidAction action,
            @NonNull Map<ValidDataNames, Object> data
    ) {
        @Override
        public String toString() {
            return String.format(
                    "Target: %s\nAction: %s\nData: %s",
                    target, action.readable, data
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
        CREATE ("create", "Create", "Create **%s**"),
        EDIT ("edit", "Edit", "Edit **%s** to **%s**");

        public final String role;
        public final String readable;
        public final String editableText;

        ValidAction(String role, String readable, String editableText) {
            this.role = role;
            this.readable = readable;
            this.editableText = editableText;
        }
    }

    public enum ValidDataNames {
        NAME ("name", "Name"),
        DESCRIPTION ("desc", "Desc."),
        CATEGORY ("cat", "Category"),
        PERMISSIONS ("perms", "Perms."),
        COLOR ("color", "Color");
        public final String role;
        public final String readable;

        ValidDataNames(String role, String readable) {
            this.role = role;
            this.readable = readable;
        }
    }

    @NonNull
    public static ArrayList<ExecutableAction> parseActions(@NonNull DiscordConfig config, @NonNull ValidAction action) {
        ArrayList<ExecutableAction> actions = new ArrayList<>();

        for (Map.Entry<String, DiscordConfig.ConfigItem> entry : config.roles().entrySet()) {
            String roleName = config.roles().get(entry.getKey()).name();

            actions.add(new ExecutableAction(
                    ValidTarget.ROLE,
                    action == ValidAction.CREATE ? entry.getValue().name() : entry.getKey(),
                    action,
                    Map.of(
                            ValidDataNames.NAME, Objects.requireNonNullElse(roleName, ""),
                            ValidDataNames.COLOR, Objects.requireNonNullElse(entry.getValue().color(), ""),
                            ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(entry.getValue().perms(), "")
                    )
            ));
        }
        for (Map.Entry<String, DiscordConfig.ConfigItem> entry : config.categories().entrySet()) {
            actions.add(new ExecutableAction(
                    ValidTarget.CATEGORY,
                    action == ValidAction.CREATE ? entry.getValue().name() : entry.getKey(),
                    action,
                    Map.of(
                            ValidDataNames.NAME, Objects.requireNonNullElse(entry.getValue().name(), ""),
                            ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(entry.getValue().perms(), "")
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
                    action == ValidAction.CREATE ? entry.getValue().name() : entry.getKey(),
                    action,
                    Map.of(
                            ValidDataNames.NAME, Objects.requireNonNullElse(entry.getValue().name(), ""),
                            ValidDataNames.DESCRIPTION, Objects.requireNonNullElse(entry.getValue().desc(), ""),
                            ValidDataNames.CATEGORY, Objects.requireNonNullElse(catName, ""),
                            ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(entry.getValue().perms(), "")
                    )
            ));
        }

        return actions;
    }
}
