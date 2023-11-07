package dev.alphaserpentis.bots.brewer.handler.commands.summarize;

import dev.alphaserpentis.bots.brewer.data.openai.ChatCompletionModels;
import dev.alphaserpentis.bots.brewer.data.openai.Prompts;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import io.reactivex.rxjava3.annotations.NonNull;

public class SummarizeHandler {
    public static String generateSummarization(@NonNull String input) {
        return cleanOutput(OpenAIHandler.getCompletion(
                ChatCompletionModels.GPT_3_5_TURBO_1106.getName(),
                Prompts.SETUP_SYSTEM_PROMPT_SUMMARIZE_TEXT,
                input,
                0.25,
                0.1,
                0.1
        ).getChoices().get(0).getMessage().getContent());
    }

    public static String cleanOutput(@NonNull String input) {
        if(input.startsWith("\"")) {
            input = input.substring(1);
        }

        return input;
    }
}
