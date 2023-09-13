package dev.alphaserpentis.bots.brewer.data.openai;

import io.reactivex.rxjava3.annotations.NonNull;

public record AudioTranscriptionResponse(
        @NonNull String text,
        boolean isCached
) {
    public AudioTranscriptionResponse(@NonNull String text) {
        this(text, false);
    }
}
