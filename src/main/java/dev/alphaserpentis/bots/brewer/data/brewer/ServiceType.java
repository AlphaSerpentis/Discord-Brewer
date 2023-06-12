package dev.alphaserpentis.bots.brewer.data.brewer;

/**
 * Type of services Brewer provides with their default runs per hour and paid runs per hour.
 *
 * <p>Transcribe and translate are measured in seconds per hour.
 */
public enum ServiceType {
    CREATE(3, 10),
    RENAME(3, 10),
    TRANSCRIBE_ATTACHMENT(600, 6000),
    TRANSCRIBE_VC(300, 3000),
    TRANSLATE_ATTACHMENT(600, 6000),
    TRANSLATE_VC(300, 3000);

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