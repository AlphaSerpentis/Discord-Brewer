package dev.alphaserpentis.bots.brewer.handler.commands.audio;

import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static dev.alphaserpentis.bots.brewer.handler.openai.CustomOpenAiService.OPENAI_MAX_FILE_SIZE;

public class AudioHandler {
    private static String ytdlCommand;
    private static String ffmpegCommand;

    public static void init(@NonNull String ytdlCommand, @NonNull String ffmpegCommand) {
        AudioHandler.ytdlCommand = ytdlCommand;
        AudioHandler.ffmpegCommand = ffmpegCommand;
    }

    @NonNull
    public static byte[] readUrlStream(@NonNull String url) throws IOException {
        return readUrlStream(url, null, null);
    }

    @NonNull
    public static byte[] readUrlStream(
            @NonNull String url,
            @Nullable Long startTime,
            @Nullable Long endTime
    ) throws IOException {
        ArrayList<String> command = generateCommands(url, startTime, endTime);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        byte[] bytes;

        try(var in = process.getInputStream()) {
            var buffer = new ByteArrayOutputStream();
            var data = new byte[16384];
            int nRead;

            while((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);

                if(buffer.size() > OPENAI_MAX_FILE_SIZE)
                    throw new GenerationException(GenerationException.Type.FILE_TOO_LARGE_OPENAI_MAX);
            }

            buffer.flush();
            bytes = buffer.toByteArray();

            if(bytes.length == 0)
                throw new GenerationException(GenerationException.Type.FILE_EMPTY);

            return bytes;
        }
    }

    /**
     * Hashes the audio bytes to a SHA3-256 hash
     * @param bytes The audio bytes
     * @return The SHA3-256 hash
     */
    @NonNull
    public static String hashAudioBytes(@NonNull byte[] bytes) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA3-256").digest(bytes);
        var sb = new StringBuilder();

        for(byte b: hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    @NonNull
    private static ArrayList<String> generateCommands(
            @NonNull String url,
            @Nullable Long startTime,
            @Nullable Long endTime
    ) {
        var command = new ArrayList<String>(12);

        command.add(ytdlCommand);
        if(startTime != null) {
            command.add("--download-sections");
            command.add("*%s-%s".formatted(startTime, endTime));
        }
        command.add("--ffmpeg-location");
        command.add(ffmpegCommand);
        command.add("--no-warnings");
        command.add("--extract-audio");
        command.add("--audio-format");
        command.add("mp3");
        command.add("--output");
        command.add("-");
        command.add(url);

        return command;
    }
}
