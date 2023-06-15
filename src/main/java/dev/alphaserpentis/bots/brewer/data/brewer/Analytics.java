package dev.alphaserpentis.bots.brewer.data.brewer;

import java.util.EnumMap;

public class Analytics {
    private final EnumMap<ServiceType, Integer> usagePerServiceType = new EnumMap<>(ServiceType.class);
    private final int snapshotUserCount;
    private final int snapshotAgeOfServer;
    private final PaidTier paidTier;

    public Analytics(int snapshotUserCount, int snapshotAgeOfServer, PaidTier paidTier) {
        this.snapshotUserCount = snapshotUserCount;
        this.snapshotAgeOfServer = snapshotAgeOfServer;
        this.paidTier = paidTier;
    }

    public EnumMap<ServiceType, Integer> getUsagePerServiceType() {
        return usagePerServiceType;
    }

    public int getSnapshotUserCount() {
        return snapshotUserCount;
    }

    public int getSnapshotAgeOfServer() {
        return snapshotAgeOfServer;
    }

    public PaidTier getPaidTier() {
        return paidTier;
    }
}
