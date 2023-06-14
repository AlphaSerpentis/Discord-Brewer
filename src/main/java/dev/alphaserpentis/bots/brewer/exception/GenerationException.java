package dev.alphaserpentis.bots.brewer.exception;

/**
 * Thrown when an error occurs during a generation.
 */
public class GenerationException extends RuntimeException {
    public enum ExceptionType {
        JSON_EXCEPTION ("JSON Exception (Potentially bad JSON by ChatGPT)"),
        TIMEOUT_EXCEPTION ("Timeout Exception (ChatGPT API timed out)"),
        OVERLOADED_EXCEPTION ("Overloaded Exception (ChatGPT API is overloaded)");

        private final String descriptions;

        ExceptionType(String descriptions) {
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

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
