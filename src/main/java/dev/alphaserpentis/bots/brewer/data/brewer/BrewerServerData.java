package dev.alphaserpentis.bots.brewer.data.brewer;

import dev.alphaserpentis.coffeecore.data.server.ServerData;

import java.util.HashMap;
import java.util.Map;

public class BrewerServerData extends ServerData {
    private boolean tryRenamingNsfwChannels = false;
    private boolean acknowledgedNewTos = false;
    private boolean acknowledgedNewPrivacyPolicy = false;
    private boolean acknowledgedNewUpdates = false;
    private boolean serverWideOptOutOfAnalytics = false;
    private Map<Long, Boolean> userDisallowVCTranscriptions = new HashMap<>();
    private PaidTier paidTier = PaidTier.NONE;

    public BrewerServerData() {
        super();
    }
    public BrewerServerData(boolean onlyEphemeral) {
        super(onlyEphemeral);
    }
    public BrewerServerData(boolean onlyEphemeral, boolean tryRenamingNsfwChannels) {
        super(onlyEphemeral);
        this.tryRenamingNsfwChannels = tryRenamingNsfwChannels;
    }

    public void setTryRenamingNsfwChannels(boolean tryRenamingNsfwChannels) {
        this.tryRenamingNsfwChannels = tryRenamingNsfwChannels;
    }

    public void setAcknowledgedNewTos(boolean acknowledgedNewTos) {
        this.acknowledgedNewTos = acknowledgedNewTos;
    }

    public void setAcknowledgedNewPrivacyPolicy(boolean acknowledgedNewPrivacyPolicy) {
        this.acknowledgedNewPrivacyPolicy = acknowledgedNewPrivacyPolicy;
    }

    public void setAcknowledgedNewUpdates(boolean acknowledgedNewUpdates) {
        this.acknowledgedNewUpdates = acknowledgedNewUpdates;
    }

    public void setServerWideOptOutOfAnalytics(boolean serverWideOptOutOfAnalytics) {
        this.serverWideOptOutOfAnalytics = serverWideOptOutOfAnalytics;
    }

    public void setPaidTier(PaidTier paidTier) {
        this.paidTier = paidTier;
    }

    public boolean getTryRenamingNsfwChannels() {
        return tryRenamingNsfwChannels;
    }

    public boolean getAcknowledgedNewTos() {
        return acknowledgedNewTos;
    }

    public boolean getAcknowledgedNewPrivacyPolicy() {
        return acknowledgedNewPrivacyPolicy;
    }

    public boolean getAcknowledgedNewUpdates() {
        return acknowledgedNewUpdates;
    }

    public boolean getServerWideOptOutOfAnalytics() {
        return serverWideOptOutOfAnalytics;
    }

    public PaidTier getPaidTier() {
        return paidTier;
    }

    public boolean isUserOptedOutOfVCTranscription(long userId) {
        return userDisallowVCTranscriptions.getOrDefault(userId, false);
    }

    public void addUserIntoVCTranscriptionOptOut(long userId) {
        userDisallowVCTranscriptions.put(userId, true);
    }

    public void removeUserFromVCTranscriptionOptOut(long userId) {
        userDisallowVCTranscriptions.remove(userId);
    }

}
