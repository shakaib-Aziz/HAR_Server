import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensorDataServer_Thread {

    private static final String SOURCE_FOLDER_LOCATION = "C:\\HAR_Server_V1\\Instance Data";

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust the pool size as needed

        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Server listening on port 12345...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());

                // Create a new thread for each client connection
                executorService.submit(new ClientHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            executorService.shutdown(); // Shutdown the executor service when done
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                handleClient();
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }

        private void handleClient() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Connected to Client: " + clientSocket.getInetAddress());
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received data from " + clientSocket.getInetAddress() + ": " + line);

                // Extract information from the current line
                String source = extractSource(line);
                String userId = extractUserId(line);
                String sensorType = extractSensorType(line);
                String timestamp = extractTimestamp(line);

                // Build directory structure
                String directoryPath = getFolderPath(source, userId, sensorType, timestamp);

                // Create directories if they don't exist
                boolean success = new java.io.File(directoryPath).mkdirs();
                if (success || new java.io.File(directoryPath).exists()) {
                    // Directories were created successfully or already exist
                    System.out.println("Directories exist or were created successfully. " + directoryPath);
                } else {
                    // Failed to create directories
                    System.out.println("Failed to create directories. " + directoryPath);
                    return;
                }

                // Save data to a file in CSV format
                saveDataToFile(directoryPath, userId, sensorType, timestamp, line);
            }

            // Close the socket after processing data
            clientSocket.close();
            System.out.println("Connection closed for " + clientSocket.getInetAddress());
            BufferedWriter writer = (new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
            writer.write(200);
            writer.close();
        }
    }

    private static void saveDataToFile(String directoryPath, String userId, String sensorType, String timestamp,
            String data) {
        try {
            // Create a file name using user ID and timestamp
            String fileName = userId + "_" + sensorType + ".csv";
            String filePath = directoryPath + "\\" + fileName;
            System.out.println("Opening file at " + filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true)); // Append to the file

            // Write data to the file as-is
            writer.write(data);
            writer.newLine();
            writer.close();

            System.out.println("Data saved to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFolderPath(String source, String userId, String sensorType, String timestamp) {
        // return SOURCE_FOLDER_LOCATION + "\\" + source + "\\" + userId + "_" +
        // sensorType + "_" + timestamp;
        return SOURCE_FOLDER_LOCATION + "\\" + source + "\\" + userId + "_" + sensorType;
    }

    private static String extractSource(String data) {
        Pattern pattern = Pattern.compile("Source: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownSource";
        }
    }

    private static String extractUserId(String data) {
        Pattern pattern = Pattern.compile("User ID: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownUser";
        }
    }

    private static String extractSensorType(String data) {
        Pattern pattern = Pattern.compile("Sensor Type: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownSensor";
        }
    }

    private static String extractTimestamp(String data) {
        Pattern pattern = Pattern.compile("Timestamp: (\\d+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownTimestamp";
        }
    }
}
