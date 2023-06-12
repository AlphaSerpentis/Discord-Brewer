package dev.alphaserpentis.bots.brewer.data.brewer;

import io.reactivex.rxjava3.annotations.NonNull;

public record CachedAudio(
        long expirationTime,
        @NonNull String content
) {}
