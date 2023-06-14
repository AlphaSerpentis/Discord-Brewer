package dev.alphaserpentis.bots.brewer.handler.commands.vote;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VoteHandler {
    private static final HashMap<Long, Long> remindedExpiration = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean initialized = false;

    public static void init() {
        initialized = true;
        scheduler.scheduleAtFixedRate(VoteHandler::update, 0, 60, java.util.concurrent.TimeUnit.MINUTES);
    }

    public static void addUserToRemindedMap(long userId) {
        initializeIfNot();
        long currentTime = System.currentTimeMillis() / 1000L;

        remindedExpiration.put(userId, currentTime + 86400);
    }

    public static boolean isUserInRemindedMap(long userId) {
        initializeIfNot();
        return remindedExpiration.containsKey(userId);
    }

    public static void update() {
        long currentTime = System.currentTimeMillis() / 1000L;

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
