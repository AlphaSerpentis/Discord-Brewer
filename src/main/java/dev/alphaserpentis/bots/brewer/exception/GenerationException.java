package dev.alphaserpentis.bots.brewer.exception;

/**
 * Thrown when an error occurs during a generation.
 */
public class GenerationException extends RuntimeException {
    public enum Type {
        JSON_EXCEPTION ("JSON Exception (Potentially bad JSON by ChatGPT)"),
        TIMEOUT_EXCEPTION ("Timeout Exception (ChatGPT API timed out)"),
        OVERLOADED_EXCEPTION ("Overloaded Exception (ChatGPT API is overloaded)"),
        FILE_TOO_LARGE_OPENAI_MAX("File too large! Please upload something smaller"),
        FILE_TOO_LARGE_NON_PREMIUM("File too large! Please upload something smaller or upgrade to our Mercury tier!");

        private final String descriptions;

        Type(String descriptions) {
            this.descriptions = descriptions;
        }

        public String getDescriptions() {
            return descriptions;
        }

        @Override
        public String toString() {
            return descriptions;
        }
    }

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
