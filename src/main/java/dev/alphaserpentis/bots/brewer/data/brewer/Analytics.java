package dev.alphaserpentis.bots.brewer.data.brewer;

import java.util.EnumMap;

public class Analytics {
    private final EnumMap<ServiceType, Integer> usagePerServiceType = new EnumMap<>(ServiceType.class);
    private final int snapshotUserCount;
    private final long snapshotAgeOfServer;
    private final long snapshotBotAgeInServer;
    private final PaidTier paidTier;

    public Analytics(int snapshotUserCount, long snapshotAgeOfServer, long snapshotBotAgeInServer, PaidTier paidTier) {
        this.snapshotUserCount = snapshotUserCount;
        this.snapshotAgeOfServer = snapshotAgeOfServer;
        this.snapshotBotAgeInServer = snapshotBotAgeInServer;
        this.paidTier = paidTier;
    }

    public EnumMap<ServiceType, Integer> getUsagePerServiceType() {
        return usagePerServiceType;
    }

    public int getSnapshotUserCount() {
        return snapshotUserCount;
    }

    public long getSnapshotAgeOfServer() {
        return snapshotAgeOfServer;
    }

    public long getSnapshotBotAgeInServer() {
        return snapshotBotAgeInServer;
    }

    public PaidTier getPaidTier() {
        return paidTier;
    }
}
