package com.supporthawk.utils;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plays audio files for voice automation.
 * Playback is synchronous, so this method returns only after playback finishes.
 */
public final class AudioPlayer {

    private AudioPlayer() {
    }

    /**
     * Plays an mp3 file and waits until it completes.
     *
     * @param audioFile path to an mp3 file
     */
    public static void playMp3(Path audioFile) {
        try {
            if (audioFile == null || !Files.exists(audioFile)) {
                throw new RuntimeException("Audio playback failed: file not found: " + audioFile);
            }

            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(audioFile.toFile()))) {
                Player player = new Player(inputStream);
                player.play();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Audio playback failed for file " + audioFile + ": " + e.getMessage(), e);
        }
    }
}
