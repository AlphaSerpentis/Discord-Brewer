package dev.alphaserpentis.bots.brewer.data.brewer;

import io.reactivex.rxjava3.annotations.NonNull;

import java.util.Arrays;

/**
 * Represents an error submission provided by the user
 */
public class ErrorSubmission {
    private final long guildId;
    private final long userId;
    private final Exception exceptionThrown;
    private final String prompt;
    private final String[] attachmentUrl;

    public ErrorSubmission(
            long guildId,
            long userId,
            @NonNull Exception exceptionThrown,
            @NonNull String prompt
    ) {
        this.guildId = guildId;
        this.userId = userId;
        this.exceptionThrown = exceptionThrown;
        this.prompt = prompt;
        this.attachmentUrl = null;
    }

    public ErrorSubmission(
            long guildId,
            long userId,
            @NonNull Exception exceptionThrown,
            @NonNull String[] attachmentUrl
    ) {
        this.guildId = guildId;
        this.userId = userId;
        this.exceptionThrown = exceptionThrown;
        this.prompt = null;
        this.attachmentUrl = attachmentUrl;
    }

    public ErrorSubmission(
            long guildId,
            long userId,
            @NonNull Exception exceptionThrown
    ) {
        this.guildId = guildId;
        this.userId = userId;
        this.exceptionThrown = exceptionThrown;
        this.prompt = null;
        this.attachmentUrl = null;
    }

    @Override
    public String toString() {
        return "Error submission from user " + userId + " in guild " + guildId + ":\n" +
                "Prompt: " + prompt + "\n" +
                "Attachment URL(s): " + Arrays.toString(attachmentUrl) + "\n" +
                "Exception: " + exceptionThrown.toString() + "\n" +
                "Stack trace: " + Arrays.toString(exceptionThrown.getStackTrace());
    }
}
