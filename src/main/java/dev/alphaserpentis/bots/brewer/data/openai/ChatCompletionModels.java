package dev.alphaserpentis.bots.brewer.data.openai;

import dev.alphaserpentis.bots.brewer.data.brewer.PaidTier;

/**
 * Up to date as of July 25, 2023
 */
public enum ChatCompletionModels {
    GPT_3_5_TURBO("gpt-3.5-turbo", PaidTier.NONE),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", PaidTier.NONE),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k", PaidTier.MERCURY),
//    GPT_3_5_TURBO_16K_1106("gpt-3.5-turbo-16k-1106", PaidTier.MERCURY),
    GPT_4("gpt-4", PaidTier.VENUS),
    GPT_4_0613("gpt-4-0613", PaidTier.VENUS),
    GPT_4_32K("gpt-4-32k", PaidTier.EARTH),
    GPT_4_32K_0613("gpt-4-32k-0613", PaidTier.EARTH);

    private final String name;
    private final PaidTier minimumTier;

    ChatCompletionModels(String s, PaidTier tier) {
        name = s;
        minimumTier = tier;
    }

    public String getName() {
        return name;
    }

    public PaidTier getMinimumTier() {
        return minimumTier;
    }
}
