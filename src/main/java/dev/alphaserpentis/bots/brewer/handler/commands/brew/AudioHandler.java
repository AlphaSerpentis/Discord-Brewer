package dev.alphaserpentis.bots.brewer.handler.commands.brew;

import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.OpusPacket;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static dev.alphaserpentis.bots.brewer.handler.openai.CustomOpenAiService.OPENAI_MAX_FILE_SIZE;

public class AudioHandler implements AudioReceiveHandler {

    private boolean receiving = true;
    private final Map<Long, ByteArrayOutputStream> audioStreams = new HashMap<>();
    private final ByteArrayOutputStream combinedAudioStream = new ByteArrayOutputStream();

    public AudioHandler(int seconds) {
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

    public Map<Long, byte[]> getAudioData() throws IOException {
        Map<Long, byte[]> audioDataMap = new HashMap<>();

        for (Map.Entry<Long, ByteArrayOutputStream> entry : audioStreams.entrySet()) {
            long userId = entry.getKey();
            byte[] pcmData = entry.getValue().toByteArray();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 AudioInputStream audioInputStream = new AudioInputStream(
                         new ByteArrayInputStream(pcmData), OUTPUT_FORMAT, pcmData.length / 2 / OUTPUT_FORMAT.getChannels());
                 AudioInputStream waveStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, audioInputStream)) {
                AudioSystem.write(waveStream, AudioFileFormat.Type.WAVE, outputStream);
                audioDataMap.put(userId, outputStream.toByteArray());
            }
        }

        return audioDataMap;
    }

    public byte[] getCombinedAudioData() {
        return combinedAudioStream.toByteArray();
    }

    public static byte[] readUrlStream(@NonNull String url) throws IOException {
        InputStream in = new URL(url).openConnection().getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);

            if(buffer.size() > OPENAI_MAX_FILE_SIZE) {
                throw new GenerationException(GenerationException.Type.FILE_TOO_LARGE_OPENAI_MAX.getDescriptions());
            }
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Hashes the audio bytes to a SHA3-256 hash
     * @param bytes The audio bytes
     * @return The SHA3-256 hash
     */
    public static String hashAudioBytes(@NonNull byte[] bytes) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA3-256").digest(bytes);
        StringBuilder sb = new StringBuilder();
        for(byte b: hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
