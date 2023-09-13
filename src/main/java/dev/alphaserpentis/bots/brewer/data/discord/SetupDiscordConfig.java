package dev.alphaserpentis.bots.brewer.data.discord;

import java.util.List;
import java.util.Map;

public record SetupDiscordConfig(List<ConfigItem> data) {

    public record ConfigItem(String target, String action, Map<String, Object> data) {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ConfigItem record: data) {
            sb.append(String.format("Action: %s %s\n", record.action(), record.target()));
            Map<String, Object> recordData = record.data();
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                sb.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}