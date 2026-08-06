package com.supporthawk.utils;

import com.supporthawk.config.ConfigReader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates speech audio for a query using Microsoft Edge TTS.
 * Voice, rate, silence padding, and output folder come from config.properties.
 */
public final class EdgeTTSUtil {

    private EdgeTTSUtil() {
    }

    /**
     * Converts input text to speech and writes it to an mp3 file.
     * Speech rate and voice are read from configuration so they can be tuned
     * without changing Java code.
     *
     * @param text text to convert into speech
     * @return path to generated mp3 file (with optional silence padding)
     */
    public static Path generateSpeechFile(String text) {
        try {
            String voice = ConfigReader.get("tts.voice");
            String rate = ConfigReader.get("tts.rate");
            String outputFolder = ConfigReader.get("tts.output.folder");
            int silenceMs = Integer.parseInt(ConfigReader.get("tts.silence.ms"));

            Path outputDir = Paths.get(outputFolder);
            Files.createDirectories(outputDir);

            Path rawSpeechFile = outputDir.resolve("voice-query-raw-" + System.currentTimeMillis() + ".mp3");
            Path outputFile = outputDir.resolve("voice-query-" + System.currentTimeMillis() + ".mp3");

            generateWithEdgeTts(text, voice, rate, rawSpeechFile);

            // Add silence before and after speech so the recognizer does not clip words.
            Path paddedFile = addSilencePadding(rawSpeechFile, outputFile, silenceMs);

            Files.deleteIfExists(rawSpeechFile);
            return paddedFile;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate voice file using Edge TTS: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the edge-tts CLI with the configured neural voice and rate.
     */
    private static void generateWithEdgeTts(String text, String voice, String rate, Path outputFile) {
        List<List<String>> commandsToTry = new ArrayList<>();
        commandsToTry.add(Arrays.asList(
                "python", "-m", "edge_tts",
                "--voice", voice,
                "--rate", rate,
                "--text", text,
                "--write-media", outputFile.toAbsolutePath().toString()
        ));
        commandsToTry.add(Arrays.asList(
                "py", "-m", "edge_tts",
                "--voice", voice,
                "--rate", rate,
                "--text", text,
                "--write-media", outputFile.toAbsolutePath().toString()
        ));

        StringBuilder failures = new StringBuilder();

        for (int i = 0; i < commandsToTry.size(); i++) {
            List<String> command = commandsToTry.get(i);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                String output = readProcessOutput(process.getInputStream());
                int exitCode = process.waitFor();

                if (exitCode == 0 && Files.exists(outputFile) && Files.size(outputFile) > 0) {
                    return;
                }

                failures.append("Command failed: ").append(String.join(" ", command))
                        .append(System.lineSeparator())
                        .append("Output: ").append(output)
                        .append(System.lineSeparator());
            } catch (Exception e) {
                failures.append("Command failed: ").append(String.join(" ", command))
                        .append(System.lineSeparator())
                        .append("Error: ").append(e.getMessage())
                        .append(System.lineSeparator());
            }
        }

        throw new RuntimeException(
                "Edge TTS generation failed. Ensure Python and edge-tts are installed. Details: "
                        + failures
        );
    }

    /**
     * Adds silence before and after the spoken audio using ffmpeg when available.
     * <p>
     * How silence is implemented:
     * 1. Create a short silent clip of {@code silenceMs} (default 500 ms).
     * 2. Concatenate: silence + spoken mp3 + silence.
     * 3. Export the result as the final mp3 used for playback.
     * <p>
     * If ffmpeg is not installed, the raw Edge TTS file is copied as-is so
     * slower speech still works without blocking the whole voice flow.
     */
    private static Path addSilencePadding(Path speechFile, Path outputFile, int silenceMs) {
        if (silenceMs <= 0) {
            try {
                Files.copy(speechFile, outputFile);
                return outputFile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to prepare speech file: " + e.getMessage(), e);
            }
        }

        double silenceSeconds = silenceMs / 1000.0;
        Path silenceFile = speechFile.getParent().resolve("silence-" + System.currentTimeMillis() + ".mp3");

        try {
            // Step 1: generate a silent mp3 of the requested length
            List<String> createSilence = Arrays.asList(
                    "ffmpeg", "-y",
                    "-f", "lavfi",
                    "-i", "anullsrc=r=24000:cl=mono",
                    "-t", String.valueOf(silenceSeconds),
                    "-q:a", "9",
                    silenceFile.toAbsolutePath().toString()
            );

            runProcess(createSilence, "Failed to generate silence clip with ffmpeg");

            // Step 2: concatenate silence + speech + silence
            // filter_complex joins three audio streams into one file
            List<String> concat = Arrays.asList(
                    "ffmpeg", "-y",
                    "-i", silenceFile.toAbsolutePath().toString(),
                    "-i", speechFile.toAbsolutePath().toString(),
                    "-i", silenceFile.toAbsolutePath().toString(),
                    "-filter_complex", "[0:a][1:a][2:a]concat=n=3:v=0:a=1",
                    "-q:a", "9",
                    outputFile.toAbsolutePath().toString()
            );

            runProcess(concat, "Failed to pad speech with silence using ffmpeg");

            if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
                throw new RuntimeException("Silence-padded speech file was not created: " + outputFile);
            }

            return outputFile;
        } catch (Exception e) {
            // ffmpeg may not be installed — fall back to the slower raw speech file
            System.out.println("[EdgeTTSUtil] Silence padding skipped (" + e.getMessage()
                    + "). Using slower speech without silence padding.");
            try {
                Files.copy(speechFile, outputFile);
                return outputFile;
            } catch (Exception copyError) {
                throw new RuntimeException(
                        "Failed to prepare speech file after silence padding fallback: "
                                + copyError.getMessage(),
                        copyError
                );
            }
        } finally {
            try {
                Files.deleteIfExists(silenceFile);
            } catch (Exception ignored) {
            }
        }
    }

    private static void runProcess(List<String> command, String errorMessage) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = readProcessOutput(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(errorMessage + ". Output: " + output);
        }
    }

    private static String readProcessOutput(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
