package dev.alphaserpentis.bots.brewer.handler.commands.audio;

import dev.alphaserpentis.bots.brewer.exception.GenerationException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

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

    @SuppressWarnings("UnusedParameters")
    @NonNull
    public static byte[] readUrlStream(
            @NonNull String url,
            @Nullable Long startTime, // TODO: Implement
            @Nullable Long endTime // TODO: Implement
    ) throws IOException {
        ProcessBuilder directLinkProcessBuilder = new ProcessBuilder(obtainDirectLink(url));
        Process directLinkProcess = directLinkProcessBuilder.start();
        String[] directLinks;

        try(var in = directLinkProcess.getInputStream()) {
            directLinks = new String(in.readAllBytes()).split("\n");
        }

        // Remove .m3u8 links
        directLinks = Arrays.stream(directLinks).filter(s -> !s.contains(".m3u8")).toArray(String[]::new);

        for(var directLink: directLinks) {
            ProcessBuilder ffmpegProcessBuilder = new ProcessBuilder(generateCommands(directLink));
            Process ffmpegProcess = ffmpegProcessBuilder.start();
            byte[] bytes;

            try(var in = ffmpegProcess.getInputStream()) {
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
                    continue;

                return bytes;
            }
        }

        throw new GenerationException((GenerationException.Type.FILE_EMPTY));
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

    private static ArrayList<String> obtainDirectLink(@NonNull String url) {
        var command = new ArrayList<String>(5);

        command.add(ytdlCommand);
        command.add("--get-url");
        command.add("--no-warnings");
        command.add(url);

        return command;
    }

    @NonNull
    private static ArrayList<String> generateCommands(@NonNull String url) {
        var command = new ArrayList<String>(12);

        command.add(ffmpegCommand);
        command.add("-i");
        command.add(url);
        command.add("-vn");
        command.add("-acodec");
        command.add("libmp3lame");
        command.add("-q:a");
        command.add("5.0");
        command.add("-movflags");
        command.add("+faststart");
        command.add("-f");
        command.add("mp3");
        command.add("-");

        return command;
    }
}
