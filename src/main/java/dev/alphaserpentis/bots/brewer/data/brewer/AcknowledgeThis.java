package dev.alphaserpentis.bots.brewer.data.brewer;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

/**
 * Record storing to display to the user to acknowledge for
 */
public record AcknowledgeThis(
        Type type,
        String title,
        String description,
        String footer,
        String color
) {
    public enum Type {
        TOS,
        PRIVACY_POLICY,
        UPDATES
    }

    public AcknowledgeThis(
            Type type,
            String title,
            String description,
            String hexColor
    ) {
        this(type, title, description, null, hexColor);
    }

    public AcknowledgeThis(
            Type type,
            String title,
            String description
    ) {
        this(type, title, description, null, null);
    }

    public EmbedBuilder convertToEmbedBuilder() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setDescription(description);
        eb.setFooter(footer);
        eb.setColor(Color.decode(color));

        return eb;
    }
}
