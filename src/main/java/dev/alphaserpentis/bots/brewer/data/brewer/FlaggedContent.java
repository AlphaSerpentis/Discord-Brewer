package dev.alphaserpentis.bots.brewer.data.brewer;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Represents flagged content by OpenAI's moderation endpoint
 * @param userId The user who ran the command
 * @param guildId The guild the command was run in, if any
 * @param flaggedContent The content that was flagged
 */
public record FlaggedContent(
        long userId,
        long guildId,
        @NonNull String flaggedContent
) {}
