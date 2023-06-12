package dev.alphaserpentis.bots.brewer.handler.openai;

import com.theokanning.openai.service.OpenAiService;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationRequest;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class CustomOpenAiService extends OpenAiService {
    private final CustomOpenAiApi api;
    public static final long OPENAI_MAX_FILE_SIZE = 26214400;

    public CustomOpenAiService(@NonNull CustomOpenAiApi api) {
        super(api);

        this.api = api;
    }

    @NonNull
    public AudioTranscriptionResponse createAudioTranscription(@NonNull AudioTranscriptionRequest request) {
        RequestBody audio;
        try {
            audio = RequestBody.create(MediaType.parse("audio/mpeg"), getAudioBytes(request.file()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("model", request.model())
                .addFormDataPart("file", request.fileName(), audio);

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
        RequestBody audio;
        try {
            audio = RequestBody.create(MediaType.parse("audio/mpeg"), getAudioBytes(request.file()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("model", request.model())
                .addFormDataPart("file", request.fileName(), audio);

        if(request.prompt() != null) {
            builder.addFormDataPart("prompt", request.prompt());
        }
        if(request.responseFormat() != null) {
            builder.addFormDataPart("response_format", request.responseFormat());
        }
        builder.addFormDataPart("temperature", String.valueOf(request.temperature()));

        return execute(api.createAudioTranslation(builder.build()));
    }

    private byte[] getAudioBytes(@NonNull String url) throws IOException {
        InputStream in = new URL(url).openConnection().getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);

            if(buffer.size() > OPENAI_MAX_FILE_SIZE) {
                throw new GenerationException(GenerationException.ExceptionType.FILE_TOO_LARGE.getDescriptions());
            }
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}
