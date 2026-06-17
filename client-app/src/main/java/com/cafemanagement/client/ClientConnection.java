package com.cafemanagement.client;

import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;

public class ClientConnection {
    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 9000;
    private static ClientConnection instance;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    static {
        try (InputStream input = ClientConnection.class.getClassLoader().getResourceAsStream("client-config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Không tìm thấy file client-config.properties, dùng giá trị mặc định");
            } else {
                prop.load(input);
                String host = prop.getProperty("server.host");
                String portStr = prop.getProperty("server.port");
                if (host != null) {
                    SERVER_HOST = host;
                }
                if (portStr != null) {
                    try {
                        SERVER_PORT = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Port không hợp lệ, dùng giá trị mặc định 9000");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi đọc file config, dùng giá trị mặc định");
        }
    }

    private ClientConnection() {
        try {
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ClientConnection getInstance() {
        if (instance == null) {
            synchronized (ClientConnection.class) {
                if (instance == null) {
                    instance = new ClientConnection();
                }
            }
        }
        return instance;
    }

    private void connect() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        // TẠO OUTPUT TRƯỚC, FLUSH, RỒI MỚI TẠO INPUT
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush(); // Bắt buộc
        ois = new ObjectInputStream(socket.getInputStream());
    }

    // THÊM SYNCHRONIZED ĐỂ TRÁNH XUNG ĐỘT KHI NHIỀU LUỒNG UI CÙNG GỌI
    public synchronized Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        if (socket == null || socket.isClosed() || oos == null || ois == null) {
            connect();
        }
        oos.writeObject(request);
        oos.flush();
        oos.reset(); // RẤT QUAN TRỌNG: Tránh lỗi bộ đệm của Java Serialization
        return (Response) ois.readObject();
    }

    public void closeConnection() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null && !socket.isClosed()) socket.close();
            instance = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}