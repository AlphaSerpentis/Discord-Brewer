package dev.alphaserpentis.bots.brewer.handler.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import dev.alphaserpentis.bots.brewer.data.brewer.CachedAudio;
import dev.alphaserpentis.bots.brewer.data.brewer.FlaggedContent;
import dev.alphaserpentis.bots.brewer.data.openai.*;
import dev.alphaserpentis.bots.brewer.handler.commands.audio.AudioHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.theokanning.openai.service.OpenAiService.*;

public class OpenAIHandler {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIHandler.class);
    private static Path flaggedContentDirectory;
    private static Path transcriptionCacheFile;
    private static Path translationCacheFile;
    /**
     * Key: Hashed audio file
     * <p>Value: Transcription
     */
    private static Map<String, CachedAudio> transcriptionCache = new HashMap<>();
    /**
     * Key: Hashed audio file
     * <p>Value: Translation
     */
    private static Map<String, CachedAudio> translationCache = new HashMap<>();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static CustomOpenAiService service;
    public static final String COMPLETION_MODEL = ChatCompletionModels.GPT_3_5_TURBO.getName();

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

        try {
            readAndSetCaches();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        executorService.scheduleAtFixedRate(
                () -> {
                    try {
                        writeCachesToFile();
                        checkCachesForExpired();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                },
                300,
                3600,
                TimeUnit.SECONDS
        );
    }

    /**
     * Checks if the content is flagged by OpenAI's moderation API.
     * @param content The content to check.
     * @return {@code true} if the content is flagged, {@code false} otherwise.
     */
    public static boolean isContentFlagged(@NonNull String content, long userId, long guildId, boolean logIfFlagged) {
        ModerationResult req = service.createModeration(
                new ModerationRequest(
                        content,
                        "text-moderation-stable"
                )
        );
        boolean isFlagged = req.getResults().get(0).isFlagged();

        if(isFlagged && logIfFlagged)
            writeFlaggedContentToDirectory(new FlaggedContent(userId, guildId, content));

        return isFlagged;
    }

    public static ChatCompletionResult getCompletion(@NonNull ChatMessage system, @NonNull String prompt) {
        ChatMessage message = new ChatMessage("user", prompt);
        ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                .model(COMPLETION_MODEL)
                .messages(List.of(system, message))
                .temperature(0.8)
                .presencePenalty(0.)
                .frequencyPenalty(0.);

        return service.createChatCompletion(
                builder.build()
        );
    }

    public static AudioTranscriptionResponse getAudioTranscription(@NonNull String audioUrl) {
        String fileName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);
        byte[] audioBytes;
        String hash;

        try {
            audioBytes = AudioHandler.readUrlStream(audioUrl);
            hash = AudioHandler.hashAudioBytes(audioBytes);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);

            throw new RuntimeException(e);
        }

        if(transcriptionCache.containsKey(hash))
            return new AudioTranscriptionResponse(transcriptionCache.get(hash).content(), true);
        else {
            AudioTranscriptionResponse response = service.createAudioTranscription(
                    new AudioTranscriptionRequest(
                            "whisper-1",
                            fileName,
                            audioUrl,
                            audioBytes
                    )
            );

            transcriptionCache.put(
                    hash,
                    new CachedAudio(Instant.now().getEpochSecond() + 172800, response.text())
            );

            return response;
        }
    }

    public static AudioTranscriptionResponse getVoiceTranscription(@NonNull byte[] audioBytes, @NonNull String name) {
        return service.createAudioTranscription(
                new AudioTranscriptionRequest(
                        "whisper-1",
                        name + ".wav",
                        audioBytes
                )
        );
    }

    public static AudioTranslationResponse getAudioTranslation(@NonNull String audioUrl) {
        String fileName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);
        byte[] audioBytes;
        String hash;

        try {
            audioBytes = AudioHandler.readUrlStream(audioUrl);
            hash = AudioHandler.hashAudioBytes(audioBytes);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);

            throw new RuntimeException(e);
        }

        if(translationCache.containsKey(hash))
            return new AudioTranslationResponse(translationCache.get(hash).content(), true);
        else {
            AudioTranslationResponse response = service.createAudioTranslation(
                    new AudioTranslationRequest(
                            "whisper-1",
                            fileName,
                            audioUrl,
                            audioBytes
                    )
            );

            translationCache.put(
                    hash,
                    new CachedAudio(Instant.now().getEpochSecond() + 172800, response.text())
            );

            return response;
        }
    }

    public static AudioTranslationResponse getAudioTranslation(@NonNull byte[] audioBytes, @NonNull String name) {
        return service.createAudioTranslation(
                new AudioTranslationRequest(
                        "whisper-1",
                        name + ".wav",
                        audioBytes
                )
        );
    }

    private static void readAndSetCaches() throws IOException {
        transcriptionCache = new Gson().fromJson(
                Files.newBufferedReader(transcriptionCacheFile), new TypeToken<Map<String, CachedAudio>>(){}.getType()
        );
        translationCache = new Gson().fromJson(
                Files.newBufferedReader(translationCacheFile), new TypeToken<Map<String, CachedAudio>>(){}.getType()
        );

        if(transcriptionCache == null)
            transcriptionCache = new HashMap<>();
        if(translationCache == null)
            translationCache = new HashMap<>();
    }

    private static void writeCachesToFile() throws IOException {
        Files.write(
                transcriptionCacheFile,
                new GsonBuilder().setPrettyPrinting().create().toJson(transcriptionCache).getBytes()
        );
        Files.write(
                translationCacheFile,
                new GsonBuilder().setPrettyPrinting().create().toJson(translationCache).getBytes()
        );
    }

    private static void checkCachesForExpired() {
        long now = Instant.now().getEpochSecond();

        transcriptionCache.entrySet().removeIf(entry -> entry.getValue().expirationTime() < now);
        translationCache.entrySet().removeIf(entry -> entry.getValue().expirationTime() < now);
    }

    private static void writeFlaggedContentToDirectory(@NonNull FlaggedContent content) {
        flaggedContentDirectory.toFile().mkdirs();
    }
}
