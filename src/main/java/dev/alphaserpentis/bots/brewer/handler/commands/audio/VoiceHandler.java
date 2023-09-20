package dev.alphaserpentis.bots.brewer.handler.commands.audio;

import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.OpusPacket;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VoiceHandler implements AudioReceiveHandler {

    private boolean receiving = true;
    private final Map<Long, ByteArrayOutputStream> audioStreams = new HashMap<>();
    private final ByteArrayOutputStream combinedAudioStream = new ByteArrayOutputStream();

    public VoiceHandler(int seconds) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(() -> {
            receiving = false;
        }, seconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean canReceiveEncoded() {
        return receiving;
    }

    @Override
    public void handleEncodedAudio(OpusPacket packet) {
        ByteArrayOutputStream stream = audioStreams.computeIfAbsent(
                packet.getUserId(), id -> new ByteArrayOutputStream());
        byte[] audioData = packet.getAudioData(1.0);

        try {
            stream.write(audioData);
            combinedAudioStream.write(audioData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public Map<Long, byte[]> getAudioData() throws IOException {
        Map<Long, byte[]> audioDataMap = new HashMap<>(audioStreams.size());

        for (Map.Entry<Long, ByteArrayOutputStream> entry : audioStreams.entrySet()) {
            long userId = entry.getKey();
            byte[] pcmData = entry.getValue().toByteArray();

            audioDataMap.put(userId, transcodeToWav(pcmData));
        }

        return audioDataMap;
    }

    @NonNull
    public byte[] getCombinedAudioData() throws IOException {
        return transcodeToWav(combinedAudioStream.toByteArray());
    }

    @NonNull
    private byte[] transcodeToWav(@NonNull byte[] pcmData) throws IOException {
        try(
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(pcmData),
                    OUTPUT_FORMAT,
                    pcmData.length / 2 / OUTPUT_FORMAT.getChannels()
            );
            AudioInputStream waveStream = AudioSystem.getAudioInputStream(
                    AudioFormat.Encoding.PCM_SIGNED,
                    audioInputStream
            )
        ) {
            AudioSystem.write(waveStream, AudioFileFormat.Type.WAVE, outputStream);
            return outputStream.toByteArray();
        }
    }
}
