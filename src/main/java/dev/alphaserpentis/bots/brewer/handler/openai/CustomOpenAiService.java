package dev.alphaserpentis.bots.brewer.handler.openai;

import com.theokanning.openai.service.OpenAiService;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.util.List;

public class CustomOpenAiService extends OpenAiService {
    private final CustomOpenAiApi api;
    public static final long OPENAI_MAX_FILE_SIZE = 26214400;
    public static final List<String> SUPPORTED_EXTENSIONS = List.of(
            "mp3",
            "mp4",
            "mpeg",
            "mpga",
            "m4a",
            "wav",
            "webm"
    );

    public CustomOpenAiService(@NonNull CustomOpenAiApi api) {
        super(api);

        this.api = api;
    }

    @NonNull
    public AudioTranscriptionResponse createAudioTranscription(@NonNull AudioTranscriptionRequest request) {
        var audio = RequestBody.create(MediaType.parse("audio/mp3"), request.audioBytes());
        var builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("model", request.model())
                .addFormDataPart("file", request.name(), audio);

        if(request.prompt() != null) {
            builder.addFormDataPart("prompt", request.prompt());
        }
        if(request.responseFormat() != null) {
            builder.addFormDataPart("response_format", request.responseFormat());
        }
        if(request.language() != null) {
            builder.addFormDataPart("language", request.language());
        }

        builder.addFormDataPart("temperature", String.valueOf(request.temperature()));

        return execute(api.createAudioTranscription(builder.build()));
    }

    @NonNull
    public AudioTranslationResponse createAudioTranslation(@NonNull AudioTranslationRequest request) {
        var name = fixName(request.name());
        var extension = name.substring(name.lastIndexOf('.') + 1);
        var audio = RequestBody.create(MediaType.parse("audio/" + extension), request.audioBytes());
        var builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("model", request.model())
                .addFormDataPart("file", name, audio);

        if(request.prompt() != null) {
            builder.addFormDataPart("prompt", request.prompt());
        }
        if(request.responseFormat() != null) {
            builder.addFormDataPart("response_format", request.responseFormat());
        }

        builder.addFormDataPart("temperature", String.valueOf(request.temperature()));

        return execute(api.createAudioTranslation(builder.build()));
    }

    public String fixName(@NonNull String name) {
        String nameToReturn = name;
        int questionMarkIndex = name.indexOf('?');

        // Check if a question mark is present
        if(questionMarkIndex != -1) {
            nameToReturn = name.substring(0, questionMarkIndex);
        }

        return nameToReturn;
    }
}
