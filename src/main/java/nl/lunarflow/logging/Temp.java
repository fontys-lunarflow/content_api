package nl.lunarflow.logging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class Logger {
    private static final String LOG_FILE_PATH = "/logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_LOG_FILE_SIZE = 5 * 1024 * 1024;

    private BufferedWriter writer;
    private LocalDate currentDate;
    private int currentFileIndex = 1;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Paths.get(LOG_FILE_PATH));
            openNewLogFile();
            log("Started logger");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    void shutdown() {
        try {
            log("Stopping logger");
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
