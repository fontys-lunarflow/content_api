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

    public synchronized void log(String content) {
        try {
            rotateIfNeeded();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String classMethod = getClassMethod();

            String entry = String.format("[%s] %s: %s", timestamp, classMethod, content);
             // [12:23:55] nl.lunarflow.logging.log:59: Something went wrong

            writer.write(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getClassMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = null;

        for (int i = 2; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().equals(this.getClass().getName()) &&
                    !stackTrace[i].getClassName().startsWith("java.lang.Thread")) {
                caller = stackTrace[i];
                break;
            }
        }

        String classMethod = (caller != null)
                ? caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber()
                : "UnknownCaller";
        return classMethod;
    }

    private void rotateIfNeeded() throws IOException {
        LocalDate today = LocalDate.now();

        if (!today.equals(currentDate)) {
            currentDate = today;
            currentFileIndex = 1;
            openNewLogFile();
        }
        else if (Files.size(Paths.get(LOG_FILE_PATH)) > MAX_LOG_FILE_SIZE) {
            currentFileIndex ++ ;
            openNewLogFile();
        }
    }

    private void openNewLogFile() throws IOException {
        if (writer != null) {
            writer.close();
        }

        currentDate = LocalDate.now();
        String filename;

        do {
            filename = String.format("log_%s_%d.log", currentDate.format(DATE_FORMATTER), currentFileIndex);
            currentFileIndex++;
        } while (Files.exists(Paths.get(LOG_FILE_PATH, filename)));

        currentFileIndex -= 1;

        File logFile = new File(LOG_FILE_PATH, filename);
        writer = new BufferedWriter(new FileWriter(logFile, true));
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
