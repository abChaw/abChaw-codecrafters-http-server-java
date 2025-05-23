import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage
        //
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            //
            //   // Since the tester restarts your program quite often, setting SO_REUSEADDR
            //   // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.

                String httpResponse = getString(clientSocket);
                OutputStream out = clientSocket.getOutputStream();
                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                clientSocket.close();
                System.out.println("Response sent, connection closed");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String getString(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
        );
        String line = in.readLine();
        String[] lines = line.split(" ");
        for (String charStream; (charStream = in.readLine()) != null && !charStream.isEmpty(); ) {

        }
        String httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        if ("GET".equals(lines[0])) {
            if (lines[1].equals("/")) {
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            }
                String msgBody = "";
            if (lines[1].startsWith("/echo")) {
                    msgBody = lines[1].substring(lines[1].indexOf('o') + 2);
                    String hContentType = "Content-Type: text/plain\r\n";
                    String hContentLength = "Content-Length:" + msgBody.length() + "\r\n\r\n";

                    httpResponse += hContentType + hContentLength + msgBody;
                }


        }
        return httpResponse;
    }
}
