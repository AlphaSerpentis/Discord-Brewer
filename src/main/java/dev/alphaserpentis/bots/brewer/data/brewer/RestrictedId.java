package dev.alphaserpentis.bots.brewer.data.brewer;

import io.reactivex.rxjava3.annotations.Nullable;

public record RestrictedId(
        long id,
        @Nullable String reason
) {

}
