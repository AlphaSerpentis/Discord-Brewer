package dev.alphaserpentis.bots.brewer.handler.commands.vote;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VoteHandler {
    private static final HashMap<Long, Long> remindedExpiration = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean initialized = false;

    public static void init() {
        initialized = true;
        scheduler.scheduleAtFixedRate(VoteHandler::update, 60, 60, TimeUnit.MINUTES);
    }

    public static void addUserToRemindedMap(long userId) {
        initializeIfNot();
        long currentTime = Instant.now().getEpochSecond();

        remindedExpiration.put(userId, currentTime + 86400);
    }

    public static boolean isUserInRemindedMap(long userId) {
        initializeIfNot();
        return remindedExpiration.containsKey(userId);
    }

    public static void update() {
        long currentTime = Instant.now().getEpochSecond();

        remindedExpiration.forEach((userId, expiration) -> {
            if (expiration < currentTime) {
                remindedExpiration.remove(userId);
            }
        });
    }

    public static void initializeIfNot() {
        if(!initialized) init();
    }
}
