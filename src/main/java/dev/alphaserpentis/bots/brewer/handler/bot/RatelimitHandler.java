package dev.alphaserpentis.bots.brewer.handler.bot;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;

import java.util.EnumMap;
import java.util.HashMap;

public class RatelimitHandler {
    /**
     * Get the ratelimit of a server by service type.
     */
    private static final HashMap<Long, EnumMap<ServiceType, Integer>> serverRatelimits = new HashMap<>();
    /**
     * Get the ratelimit of a user by service type.
     */
    private static final HashMap<Long, EnumMap<ServiceType, Integer>> userRatelimits = new HashMap<>();
}
