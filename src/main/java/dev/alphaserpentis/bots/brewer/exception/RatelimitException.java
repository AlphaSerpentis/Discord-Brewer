package dev.alphaserpentis.bots.brewer.exception;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Thrown if a user/guild is ratelimited (controlled by Brewer, not OpenAI or Discord)
 */
public class RatelimitException extends RuntimeException {
    public enum Type {
        USER,
        GUILD
    }

    private final Type type;

    public RatelimitException(@NonNull Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
