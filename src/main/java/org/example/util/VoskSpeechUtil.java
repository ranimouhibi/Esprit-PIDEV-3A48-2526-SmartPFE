package org.example.util;

/**
 * Stub - Vosk library not available in this environment.
 */
public class VoskSpeechUtil {
    public static boolean isAvailable() { return false; }
    public static boolean isModelDownloaded() { return false; }
    public static boolean isMicrophoneAvailable() { return false; }
    public static String recordAndTranscribe() { return ""; }
    public void startListening(java.util.function.Consumer<String> callback) {
        callback.accept("");
    }
    public void stopListening() {}
}
