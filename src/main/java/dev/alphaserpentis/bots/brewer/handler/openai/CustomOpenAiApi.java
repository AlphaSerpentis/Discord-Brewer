package dev.alphaserpentis.bots.brewer.handler.openai;

import com.theokanning.openai.OpenAiApi;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranscriptionResponse;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import io.reactivex.Single;
import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CustomOpenAiApi extends OpenAiApi {
    @POST("/v1/audio/transcriptions")
    Single<AudioTranscriptionResponse> createAudioTranscription(@NonNull @Body MultipartBody request);

    @POST("/v1/audio/translations")
    Single<AudioTranslationResponse> createAudioTranslation(@NonNull @Body MultipartBody request);
}
