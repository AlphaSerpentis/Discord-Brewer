package dev.alphaserpentis.bots.brewer.handler.parser;

import dev.alphaserpentis.bots.brewer.data.discord.DiscordConfig;
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
        FORUM_CHANNEL ("chnl-forum", "Forum Channel"),
        STAGE_CHANNEL ("chnl-stage", "Stage Channel"),
        ROLE ("role", "Role");

        public final String role;
        public final String readable;

        ValidTarget(String role, String readable) {
            this.role = role;
            this.readable = readable;
        }
    }

    public enum ValidAction {
        CREATE ("create", "Create", "- Create **%s**"),
        EDIT ("edit", "Edit", "- Edit **%s** to **%s**");

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
        PERMISSIONS ("perm", "Perms."),
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

        if(config.roles() != null)
            for(Map.Entry<String, DiscordConfig.ConfigItem> entry : config.roles().entrySet()) {
                DiscordConfig.ConfigItem item = entry.getValue();

                actions.add(new ExecutableAction(
                        ValidTarget.ROLE,
                        action == ValidAction.CREATE ? item.name() : entry.getKey(),
                        action,
                        Map.of(
                                ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                ValidDataNames.COLOR, Objects.requireNonNullElse(item.color(), ""),
                                ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                        )
                ));
            }
        if(config.cats() != null)
            for(Map.Entry<String, DiscordConfig.ConfigItem> entry : config.cats().entrySet()) {
                DiscordConfig.ConfigItem item = entry.getValue();

                actions.add(new ExecutableAction(
                        ValidTarget.CATEGORY,
                        action == ValidAction.CREATE ? item.name() : entry.getKey(),
                        action,
                        Map.of(
                                ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                        )
                ));
            }
        if(config.channels() != null)
            for(Map.Entry<String, DiscordConfig.ConfigItem> entry : config.channels().entrySet()) {
                DiscordConfig.ConfigItem item = entry.getValue();
                String catName = config.channels().get(entry.getKey()).cat();

                if(config.cats() != null)
                    for(Map.Entry<String, DiscordConfig.ConfigItem> catEntry : config.cats().entrySet()) {
                        if(catEntry.getKey().equals(catName) || catEntry.getValue().name().equals(catName)) {
                            catName = catEntry.getValue().name();
                            break;
                        }
                    }


                if(item.type() != null) {
                    switch(item.type().toLowerCase()) {
                        case "vc" -> actions.add(new ExecutableAction(
                                ValidTarget.VOICE_CHANNEL,
                                action == ValidAction.CREATE ? item.name() : entry.getKey(),
                                action,
                                Map.of(
                                        ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                        ValidDataNames.CATEGORY, Objects.requireNonNullElse(catName, ""),
                                        ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                                )
                        ));
                        case "txt" -> actions.add(new ExecutableAction(
                                ValidTarget.TEXT_CHANNEL,
                                action == ValidAction.CREATE ? item.name() : entry.getKey(),
                                action,
                                Map.of(
                                        ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                        ValidDataNames.DESCRIPTION, Objects.requireNonNullElse(item.desc(), ""),
                                        ValidDataNames.CATEGORY, Objects.requireNonNullElse(catName, ""),
                                        ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                                )
                        ));
                        case "forum" -> actions.add(new ExecutableAction(
                                ValidTarget.FORUM_CHANNEL,
                                action == ValidAction.CREATE ? item.name() : entry.getKey(),
                                action,
                                Map.of(
                                        ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                        ValidDataNames.CATEGORY, Objects.requireNonNullElse(catName, ""),
                                        ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                                )
                        ));
                        case "stage" -> actions.add(new ExecutableAction(
                                ValidTarget.STAGE_CHANNEL,
                                action == ValidAction.CREATE ? item.name() : entry.getKey(),
                                action,
                                Map.of(
                                        ValidDataNames.NAME, Objects.requireNonNullElse(item.name(), ""),
                                        ValidDataNames.CATEGORY, Objects.requireNonNullElse(catName, ""),
                                        ValidDataNames.PERMISSIONS, Objects.requireNonNullElse(item.perm(), "")
                                )
                        ));
                    }
                }
            }

        return actions;
    }
}
