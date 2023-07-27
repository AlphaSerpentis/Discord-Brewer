package dev.alphaserpentis.bots.brewer.handler.commands.audio;

import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import io.reactivex.rxjava3.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static dev.alphaserpentis.bots.brewer.handler.openai.CustomOpenAiService.OPENAI_MAX_FILE_SIZE;

public class AudioHandler {
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
