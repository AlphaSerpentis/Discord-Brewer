package dev.alphaserpentis.bots.brewer.handler.bot;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.EnumMap;
import java.util.HashMap;

public class RatelimitHandler {
    /**
     * Holds the remaining runs for each service type for each server.
     */
    private static final HashMap<Long, EnumMap<ServiceType, Integer>> serverRemainingRunsMap = new HashMap<>();
    /**
     * Holds the remaining runs for each service type for each user.
     */
    private static final HashMap<Long, EnumMap<ServiceType, Integer>> userRemainingRunsMap = new HashMap<>();

    public int getRemainingRunsForServer(long guildId, @NonNull ServiceType serviceType) {
        EnumMap<ServiceType, Integer> serverRemainingRuns = serverRemainingRunsMap.getOrDefault(guildId, new EnumMap<>(ServiceType.class));
        return serverRemainingRuns.getOrDefault(serviceType, serviceType.getDefaultRunsPerHour());
    }

    public int getRemainingRunsForUser(long userId, @NonNull ServiceType serviceType) {
        EnumMap<ServiceType, Integer> userRemainingRuns = userRemainingRunsMap.getOrDefault(userId, new EnumMap<>(ServiceType.class));
        return userRemainingRuns.getOrDefault(serviceType, serviceType.getDefaultRunsPerHour());
    }

    public void deductRunsFromServer(long guildId, @NonNull ServiceType serviceType, int runs) {
        EnumMap<ServiceType, Integer> serverRemainingRuns = serverRemainingRunsMap.getOrDefault(guildId, new EnumMap<>(ServiceType.class));
        int remainingRuns = serverRemainingRuns.getOrDefault(serviceType, serviceType.getDefaultRunsPerHour());
        if(runs > remainingRuns) {
            throw new IllegalArgumentException("Cannot deduct more runs than remaining runs.");
        }

        serverRemainingRunsMap.put(guildId, serverRemainingRuns);
    }

    public void deductRunsFromUser(long userId, @NonNull ServiceType serviceType, int runs) {
        EnumMap<ServiceType, Integer> userRemainingRuns = userRemainingRunsMap.getOrDefault(userId, new EnumMap<>(ServiceType.class));
        int remainingRuns = userRemainingRuns.getOrDefault(serviceType, serviceType.getDefaultRunsPerHour());
        if(runs > remainingRuns) {
            throw new IllegalArgumentException("Cannot deduct more runs than remaining runs.");
        }

        userRemainingRunsMap.put(userId, userRemainingRuns);
    }
}
