package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileManagerLogger {

    private static final String LOG_FILE_PATH = "file_manager.log"; // Log file path
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logError(String message) {
        log("ERROR", message);
    }

    public static void logInfo(String message) {
        log("INFO", message);
    }

    private static void log(String level, String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(FORMATTER);
            pw.printf("%s [%s]: %s%n", timestamp, level, message);

        } catch (IOException e) {
            e.printStackTrace(); // Fallback logging if the logger itself fails
        }
    }
}



