package dev.alphaserpentis.bots.brewer.handler.openai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.rxjava3.annotations.NonNull;

import java.time.Duration;
import java.util.ArrayList;

public class OpenAIHandler {
    public static OpenAiService service;
    public static final String MODEL = "gpt-3.5-turbo";

    public static void init(@NonNull String apiKey) {
        service = new OpenAiService(apiKey, Duration.ofSeconds(180));
    }

    public static boolean isPromptSafeToUse(@NonNull String prompt) {
        ModerationResult req = service.createModeration(
                new ModerationRequest(
                        prompt,
                        "text-moderation-stable"
                )
        );

        return !req.getResults().get(0).isFlagged();
    }

    public static ChatCompletionResult getCompletion(@NonNull ChatMessage system, @NonNull String prompt) {
        ChatMessage message = new ChatMessage("user", prompt);

        System.out.println("Prompt: " + prompt);

        return service.createChatCompletion(
                new ChatCompletionRequest(
                        MODEL,
                        new ArrayList<>() {{
                            add(system);
                            add(message);
                        }},
                        0.8,
                        null,
                        1,
                        false,
                        null,
                        null,
                        0.,
                        0.,
                        null,
                        "BREWER"
                )
        );
    }
}
