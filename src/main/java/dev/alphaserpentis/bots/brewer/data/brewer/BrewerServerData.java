package dev.alphaserpentis.bots.brewer.data.brewer;

import dev.alphaserpentis.coffeecore.data.server.ServerData;

public class BrewerServerData extends ServerData {
    private boolean tryRenamingNsfwChannels = false;
    private boolean acknowledgedNewTos = false;
    private boolean acknowledgedNewPrivacyPolicy = false;
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

    public PaidTier getPaidTier() {
        return paidTier;
    }

}
