package dev.alphaserpentis.bots.brewer.data.openai;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

public record AudioTranscriptionRequest(
        @NonNull String model,
        @NonNull String file,
        @NonNull String fileName,
        @Nullable String prompt,
        @Nullable String responseFormat,
        double temperature,
        @Nullable String language
) {
    public AudioTranscriptionRequest(@NonNull String model, @NonNull String audioUrl, @NonNull String fileName) {
        this(model, audioUrl, fileName, null, null, 0.0, null);
    }
}