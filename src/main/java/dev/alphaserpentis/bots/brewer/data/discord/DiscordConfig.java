package dev.alphaserpentis.bots.brewer.data.discord;

import io.reactivex.rxjava3.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

public record DiscordConfig(
        Map<String, ConfigItem> categories,
        Map<String, ConfigItem> channels,
        Map<String, ConfigItem> roles,
        String prompt
) {
    public record ConfigItem(
            @Nullable String name,
            @Nullable String type,
            @Nullable String cat,
            @Nullable String desc,
            @Nullable ArrayList<Permission> perms,
            @Nullable String color
    ) {

        public record Permission(
                @Nullable String role,
                @Nullable String allow,
                @Nullable String deny
        ) {
            @Override
            public String toString() {
                return String.format(
                        "Role: %s\nAllow: 0x%s\nDeny: 0x%s",
                        role, allow, deny
                );
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "Name: %s\nType: %s\nDescription: %s\nColor: %s",
                    name, type, desc, color
            );
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Categories:\n");
        for (Map.Entry<String, ConfigItem> entry : categories.entrySet()) {
            sb.append(String.format("  %s:\n", entry.getKey()));
            sb.append(entry.getValue().toString());
            sb.append("\n");
        }
        sb.append("Channels:\n");
        for (Map.Entry<String, ConfigItem> entry : channels.entrySet()) {
            sb.append(String.format("  %s:\n", entry.getKey()));
            sb.append(entry.getValue().toString());
            sb.append("\n");
        }
        sb.append("Roles:\n");
        for (Map.Entry<String, ConfigItem> entry : roles.entrySet()) {
            sb.append(String.format("  %s:\n", entry.getKey()));
            sb.append(entry.getValue().toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
