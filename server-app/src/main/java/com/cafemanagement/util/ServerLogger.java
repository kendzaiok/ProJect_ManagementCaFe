package com.cafemanagement.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static ServerLogger instance;
    private static final String LOG_FILE = "server_log.txt";
    private DateTimeFormatter formatter;

    private ServerLogger() {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public static synchronized ServerLogger getInstance() {
        if (instance == null) {
            instance = new ServerLogger();
        }
        return instance;
    }

    public void log(String user, String action, String status) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logLine = String.format("[%s] | [%s] | [%s] | [%s]", timestamp, user, action, status);
        
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logLine);
        } catch (IOException e) {
            System.err.println("Lỗi ghi log: " + e.getMessage());
        }
        
        System.out.println(logLine);
    }
}
