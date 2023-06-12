package dev.alphaserpentis.bots.brewer.handler.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import dev.alphaserpentis.bots.brewer.data.brewer.FlaggedContent;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

import static com.theokanning.openai.service.OpenAiService.*;

public class OpenAIHandler {
    private static Path flaggedContentDirectory;
    private static Path transcriptionCacheFile;
    private static Path translationCacheFile;
    /**
     * Key: Hashed audio file
     * <p>Value: Transcription
     */
    private static HashMap<String, String> transcriptionCache = new HashMap<>();
    /**
     * Key: Hashed audio file
     * <p>Value: Translation
     */
    private static HashMap<String, String> translationCache = new HashMap<>();
    public static CustomOpenAiService service;
    public static final String COMPLETION_MODEL = "gpt-3.5-turbo";

    public static void init(
            @NonNull String apiKey,
            @NonNull Path flaggedContentDirectory,
            @NonNull Path transcriptionCacheFile,
            @NonNull Path translationCacheFile
    ) {
        ObjectMapper mapper = defaultObjectMapper();
        OkHttpClient client = defaultClient(apiKey, Duration.ofSeconds(180));
        Retrofit retrofit = defaultRetrofit(client, mapper);

        service = new CustomOpenAiService(retrofit.create(CustomOpenAiApi.class));
        OpenAIHandler.flaggedContentDirectory = flaggedContentDirectory;
        OpenAIHandler.transcriptionCacheFile = transcriptionCacheFile;
        OpenAIHandler.translationCacheFile = translationCacheFile;
    }

    /**
     * Checks if the content is flagged by OpenAI's moderation API.
     * @param content The content to check.
     * @return {@code true} if the content is flagged, {@code false} otherwise.
     */
    public static boolean isContentFlagged(@NonNull String content, long userId, long guildId) {
        ModerationResult req = service.createModeration(
                new ModerationRequest(
                        content,
                        "text-moderation-stable"
                )
        );
        boolean isFlagged = req.getResults().get(0).isFlagged();

        if(isFlagged)
            writeFlaggedContentToDirectory(new FlaggedContent(userId, guildId, content));

        return isFlagged;
    }

    public static ChatCompletionResult getCompletion(@NonNull ChatMessage system, @NonNull String prompt) {
        ChatMessage message = new ChatMessage("user", prompt);

        return service.createChatCompletion(
                new ChatCompletionRequest(
                        COMPLETION_MODEL,
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

    public static AudioTranscriptionResponse getAudioTranscription(@NonNull String audioUrl) {
        String fileName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);

        return service.createAudioTranscription(
                new AudioTranscriptionRequest(
                        "whisper-1",
                        audioUrl,
                        fileName
                )
        );
    }

    private static void readAndSetCaches() throws IOException {

    }

    private static void writeFlaggedContentToDirectory(@NonNull FlaggedContent content) {
        flaggedContentDirectory.toFile().mkdirs();
    }
}
