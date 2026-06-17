package com.cafemanagement.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CafeServer {
    private static int PORT = 9000;
    private static final int THREAD_POOL_SIZE = 10;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean running;

    static {
        try (InputStream input = CafeServer.class.getClassLoader().getResourceAsStream("server-config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Không tìm thấy file server-config.properties, dùng port mặc định 9000");
            } else {
                prop.load(input);
                String portStr = prop.getProperty("server.port");
                if (portStr != null) {
                    try {
                        PORT = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Port không hợp lệ, dùng port mặc định 9000");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi đọc file config, dùng port mặc định 9000");
        }
    }

    public CafeServer() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Cafe Server is running on port " + PORT + "...");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                executorService.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            // Đã bỏ if (running) để luôn in ra lỗi nếu không tạo được ServerSocket
            System.err.println("Không thể khởi động Server: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CafeServer server = new CafeServer();
        server.start();
    }
}
