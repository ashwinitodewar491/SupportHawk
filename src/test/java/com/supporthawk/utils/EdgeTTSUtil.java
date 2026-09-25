package com.supporthawk.utils;

import com.supporthawk.config.ConfigReader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Generates a WAV file for fake microphone input using Microsoft Edge TTS.
 * This follows the fake-audio-capture approach: Chromium reads the generated
 * WAV directly as microphone input.
 */
public final class EdgeTTSUtil {

    /**
     * Language labels from {@code QueryModel.language} → Edge TTS neural voices.
     * Matching is case-insensitive.
     */
    private static final Map<String, String> LANGUAGE_VOICES = Map.ofEntries(
            Map.entry("english", "en-IN-NeerjaNeural"),
            Map.entry("hindi", "hi-IN-SwaraNeural"),
            Map.entry("bengali", "bn-IN-TanishaaNeural"),
            Map.entry("marathi", "mr-IN-AarohiNeural"),
            Map.entry("gujarati", "gu-IN-DhwaniNeural"),
            Map.entry("tamil", "ta-IN-PallaviNeural"),
            Map.entry("kannada", "kn-IN-SapnaNeural"),
            Map.entry("telugu", "te-IN-ShrutiNeural"),
            Map.entry("malayalam", "ml-IN-SobhanaNeural")
    );

    private EdgeTTSUtil() {
    }

    /**
     * Converts input text to speech using {@code tts.voice} from config
     * (legacy callers with no language).
     *
     * @param text text to convert into speech
     * @return path to generated wav file
     */
    public static Path generateWavFile(String text) {
        return generateWavFile(text, null);
    }

    /**
     * Converts input text to speech using a voice resolved from {@code language}.
     * Unknown or blank language falls back to {@code tts.voice} from config.
     *
     * @param text     text to convert into speech
     * @param language query language from {@code QueryModel} (e.g. "Hindi"), or null
     * @return path to generated wav file
     */
    public static Path generateWavFile(String text, String language) {
        try {
            String voice = resolveVoice(language);
            String rate = ConfigReader.get("tts.rate");
            String outputFolder = ConfigReader.get("tts.output.folder");
            int leadSilenceMs = Integer.parseInt(ConfigReader.get("tts.lead.silence.ms"));
            int tailSilenceMs = Integer.parseInt(ConfigReader.get("tts.tail.silence.ms"));

            Path outputDir = Paths.get(outputFolder);
            Files.createDirectories(outputDir);

            Path textFile = outputDir.resolve("voice-query-" + System.currentTimeMillis() + ".txt");
            Path mp3File = outputDir.resolve("voice-query-" + System.currentTimeMillis() + ".mp3");
            Path wavFile = outputDir.resolve("voice-query-" + System.currentTimeMillis() + ".wav");

            Files.writeString(textFile, text, StandardCharsets.UTF_8);
            generateWithEdgeTts(textFile, voice, rate, mp3File);
            convertMp3ToWavWithSilence(mp3File, wavFile, leadSilenceMs, tailSilenceMs);

            Files.deleteIfExists(textFile);
            Files.deleteIfExists(mp3File);
            return wavFile;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate voice WAV using Edge TTS: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves an Edge TTS voice for the given language label.
     * Falls back to {@code tts.voice} when language is null/blank/unknown.
     */
    public static String resolveVoice(String language) {
        if (language != null && !language.isBlank()) {
            String mapped = LANGUAGE_VOICES.get(language.trim().toLowerCase(Locale.ROOT));
            if (mapped != null) {
                return mapped;
            }
        }
        return ConfigReader.get("tts.voice");
    }

    /**
     * Calls edge-tts using configured neural voice and rate.
     */
    private static void generateWithEdgeTts(Path textFile, String voice, String rate, Path mp3File) throws Exception {
        runProcess(
                Arrays.asList(
                        pythonExecutable(), "-m", "edge_tts",
                        "--voice", voice,
                        "--rate", rate,
                        "--file", textFile.toAbsolutePath().toString(),
                        "--write-media", mp3File.toAbsolutePath().toString()
                ),
                "edge-tts synthesis failed"
        );
    }

    /**
     * Windows local setups use the {@code py} launcher; Linux/macOS CI uses {@code python3}
     * (as installed by the GitHub Actions workflow).
     */
    private static String pythonExecutable() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "py";
        }
        return "python3";
    }

    /**
     * Converts MP3 to 16kHz mono PCM WAV and pads silence around speech.
     * The WAV output is required by Chromium fake microphone capture.
     */
    private static void convertMp3ToWavWithSilence(
            Path mp3File,
            Path wavFile,
            int leadSilenceMs,
            int tailSilenceMs
    ) throws Exception {
        String leadSeconds = String.valueOf(leadSilenceMs / 1000.0);
        String tailSeconds = String.valueOf(tailSilenceMs / 1000.0);

        runProcess(
                Arrays.asList(
                        "ffmpeg", "-y",
                        "-f", "lavfi", "-t", leadSeconds, "-i", "anullsrc=r=16000:cl=mono",
                        "-i", mp3File.toAbsolutePath().toString(),
                        "-f", "lavfi", "-t", tailSeconds, "-i", "anullsrc=r=16000:cl=mono",
                        "-filter_complex", "[0:a][1:a][2:a]concat=n=3:v=0:a=1",
                        "-ar", "16000",
                        "-ac", "1",
                        "-acodec", "pcm_s16le",
                        wavFile.toAbsolutePath().toString()
                ),
                "ffmpeg conversion to wav failed"
        );
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

    /**
     * Returns WAV duration in milliseconds.
     */
    public static long getWavDurationMs(Path wavFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wavFile.toFile())) {
            long frames = audioInputStream.getFrameLength();
            float frameRate = audioInputStream.getFormat().getFrameRate();
            if (frameRate <= 0) {
                return 4000;
            }
            return (long) ((frames / frameRate) * 1000);
        } catch (Exception e) {
            return 4000;
        }
    }
}
