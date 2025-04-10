package dev.alphaserpentis.bots.brewer.data.brewer;

/**
 * Type of services Brewer provides with their default runs per hour and paid runs per hour.
 *
 * <p>Transcribe and translate are measured in seconds per hour.
 * <p>Chat is measured in total active sessions allowed in a server
 */
public enum ServiceType {
    CREATE(5, 10),
    RENAME(5, 10),
    SUMMARIZE(10, 50),
    SUMMARIZE_CONTEXT(10, 50),
    SUMMARIZE_ATTACHMENT(10, 50),
    TRANSCRIBE_ATTACHMENT(1000, 9000),
    TRANSCRIBE_VC(1000, 3000),
    TRANSLATE_ATTACHMENT(1000, 9000),
    TRANSLATE_VC(1000, 3000),
    CHAT(1, 10);

    private final int defaultRunsPerHour;
    private final int paidRunsPerHour;

    ServiceType(int defaultRunsPerHour, int paidRunsPerHour) {
        this.defaultRunsPerHour = defaultRunsPerHour;
        this.paidRunsPerHour = paidRunsPerHour;
    }

    public int getDefaultRunsPerHour() {
        return defaultRunsPerHour;
    }

    public int getPaidRunsPerHour() {
        return paidRunsPerHour;
    }
}
