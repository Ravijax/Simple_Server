import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 2728;

        try {
            // Create a server socket and start listening on the specified port.
            System.out.println("Waiting for User");
            ServerSocket ss = new ServerSocket(port);
            System.out.println("Server Listening on port " + port);

            while (true) {
                // Accept incoming client connections.
                Socket socket = ss.accept();
                // Start a new thread to handle the client request.
                new RequestHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RequestHandler extends Thread {
        private Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Set up input and output streams for communication with the client.
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream outputStream = socket.getOutputStream();

                // Read the HTTP request from the client.
                String request = bufferedReader.readLine();
                System.out.println(request);

                if (request != null) {
                    // Parse the HTTP request to extract the HTTP method and file path.
                    String[] requestParts = request.split(" ");
                    String method = requestParts[0];
                    String filePath = requestParts[1];
                    String[] correctFilePath = filePath.split("\\?");
                    filePath = "htdocs" + correctFilePath[0];
                    System.out.println(filePath);

                    // If the file path is just "/", set it to "htdocs/index.php".
                    if (filePath.equals("htdocs/")) {
                        filePath = "htdocs/index.php";
                    }

                    if (method.equals("GET")) {
                        if (filePath.endsWith(".php")) {
                            // If it's a GET request for a PHP file, execute the PHP script.
                            String params = (correctFilePath.length > 1) ? correctFilePath[1] : "";
                            executePhpScript(filePath, params, outputStream);
                        } else {
                            // If it's a GET request for a regular file, serve the file.
                            serveFile(filePath, outputStream);
                        }
                    } else {
                        // If it's not a GET request (e.g., POST), handle it.
                        handlePostRequest(bufferedReader, filePath, outputStream);
                    }
                }

                // Close the streams and the socket.
                bufferedReader.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void serveFile(String filePath, OutputStream outputStream) throws IOException {
        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            // If the requested file exists, read it and send it as an HTTP response.
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            StringBuilder fileContent = new StringBuilder();
            String line;

            while ((line = fileReader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }

            // Send HTTP response headers and file content to the client.
            String responseHeaders = "HTTP/1.1 200 success\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Connection: close\r\n\r\n";

            outputStream.write(responseHeaders.getBytes());
            outputStream.write(fileContent.toString().getBytes());
        } else {
            // If the requested file doesn't exist, send a 404 error response.
            sendResponse(outputStream, "HTTP/1.1 404 Not Found", "Error: File Not Found");
        }
    }

    private static void sendResponse(OutputStream outputStream, String statusLine, String message) throws IOException {
        // Send a custom HTTP response with a status line and message.
        String response = statusLine + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + message.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                message;

        outputStream.write(response.getBytes());
    }

    private static void executePhpScript(String filePath, String params, OutputStream outputStream) throws IOException {
        try {
            // Execute a PHP script as a separate process and capture its output.
            ProcessBuilder pb = new ProcessBuilder("php", filePath, params);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            InputStream scriptOutput = process.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Send HTTP response headers to the client.
            String responseHeaders = "HTTP/1.1 200 success\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection: close\r\n\r\n";

            outputStream.write(responseHeaders.getBytes());

            // Send the PHP script output to the client.
            while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // If the PHP script execution fails, send a 500 error response.
                sendResponse(outputStream, "HTTP/1.1 500 Internal Server Error", "Error: PHP Script Execution Failed");
            }

            outputStream.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void handlePostRequest(BufferedReader bufferedReader, String filePath, OutputStream outputStream) throws IOException {
        int contentLength = 0;
        String line;

        // Parse the content length from the request headers.
        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        char[] buffer = new char[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = bufferedReader.read(buffer, bytesRead, contentLength - bytesRead);
            if (read == -1) {
                break;
            }
            bytesRead += read;
        }

        String postBody = new String(buffer);
        // Execute the PHP script with the POST request body as input.
        executePhpScript(filePath, postBody, outputStream);
    }
}
