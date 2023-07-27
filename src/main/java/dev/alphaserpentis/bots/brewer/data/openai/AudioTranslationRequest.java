package dev.alphaserpentis.bots.brewer.data.openai;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

public record AudioTranslationRequest(
        @NonNull String model,
        @NonNull String name,
        @Nullable String file,
        @NonNull byte[] audioBytes,
        @Nullable String prompt,
        @Nullable String responseFormat,
        double temperature
) {
    public AudioTranslationRequest(
            @NonNull String model,
            @NonNull String name,
            @NonNull String file,
            @NonNull byte[] audioBytes
    ) {
        this(model, name, file, audioBytes, null, null, 0.0);
    }

    public AudioTranslationRequest(
            @NonNull String model,
            @NonNull String name,
            @NonNull byte[] audioBytes
    ) {
        this(model, name, null, audioBytes, null, null, 0.0);
    }
}