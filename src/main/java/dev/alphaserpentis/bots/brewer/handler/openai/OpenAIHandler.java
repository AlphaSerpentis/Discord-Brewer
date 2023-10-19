package dev.alphaserpentis.bots.brewer.handler.openai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import dev.alphaserpentis.bots.brewer.data.brewer.CachedAudio;
import dev.alphaserpentis.bots.brewer.data.brewer.FlaggedContent;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import dev.alphaserpentis.bots.brewer.data.openai.ChatCompletionModels;
import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.audio.AudioHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static com.theokanning.openai.service.OpenAiService.defaultRetrofit;

public class OpenAIHandler {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIHandler.class);
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
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
    public static CustomOpenAiService service;
    public static final String DEFAULT_COMPLETION_MODEL = ChatCompletionModels.GPT_3_5_TURBO.getName();

    public static void init(
            @NonNull String apiKey,
            @NonNull Path transcriptionCacheFile,
            @NonNull Path translationCacheFile
    ) {
        var mapper = defaultObjectMapper();
        var client = defaultClient(apiKey, Duration.ofSeconds(180));
        var retrofit = defaultRetrofit(client, mapper);

        service = new CustomOpenAiService(retrofit.create(CustomOpenAiApi.class));
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
                        checkCachesForExpired();
                        writeCachesToFile();
                    } catch(IOException e) {
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
        var req = service.createModeration(
                new ModerationRequest(
                        content,
                        "text-moderation-stable"
                )
        );
        var isFlagged = req.getResults().get(0).isFlagged();

        if(isFlagged && logIfFlagged)
            ModerationHandler.writeFlaggedContentToDirectory(new FlaggedContent(userId, guildId, content));

        return isFlagged;
    }

    public static ChatCompletionResult getCompletion(@NonNull ChatMessage system, @NonNull String prompt) {
        return getCompletion(DEFAULT_COMPLETION_MODEL, system, prompt);
    }

    public static ChatCompletionResult getCompletion(
            @NonNull String model,
            @NonNull ChatMessage system,
            @NonNull String prompt
    ) {
        return getCompletion(model, system, prompt, 0.8, 0., 0.);
    }

    public static ChatCompletionResult getCompletion(
            @NonNull String model,
            @NonNull ChatMessage system,
            @NonNull String prompt,
            double temperature,
            double presencePenalty,
            double frequencyPenalty
    ) {
        var message = new ChatMessage("user", prompt);
        var builder = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(system, message))
                .temperature(temperature)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty);

        return service.createChatCompletion(
                builder.build()
        );
    }

    public static AudioTranscriptionResponse getAudioTranscription(@NonNull String audioUrl) {
        var fileName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);
        byte[] audioBytes;
        String hash;

        try {
            audioBytes = AudioHandler.readUrlStream(audioUrl);
            hash = AudioHandler.hashAudioBytes(audioBytes);
        } catch(IOException | NoSuchAlgorithmException e) {
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
        var fileName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);
        byte[] audioBytes;
        String hash;

        try {
            audioBytes = AudioHandler.readUrlStream(audioUrl);
            hash = AudioHandler.hashAudioBytes(audioBytes);
        } catch(IOException | NoSuchAlgorithmException e) {
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
                new AudioTranslationRequest("whisper-1", name + ".wav", audioBytes)
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
}
