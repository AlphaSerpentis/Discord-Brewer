package dev.alphaserpentis.bots.brewer.data.openai;

import io.reactivex.rxjava3.annotations.NonNull;

public record AudioTranslationResponse(
        @NonNull String text,
        boolean isCached
) {
    public AudioTranslationResponse(@NonNull String text) {
        this(text, false);
    }
}