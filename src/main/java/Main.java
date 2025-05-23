import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        StringBuilder requestData = new StringBuilder(in.readLine());
      //  HttpRequest request = HttpRequest.parseFromSocket(in);
        String line="";
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            requestData.append(" "+line);
        }
        System.out.println("line   ::  "+requestData);
        List<String> request = Arrays.asList(requestData.toString().split(" "));
        String httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        String hContentType = "Content-Type: text/plain\r\n";
        String hContentLength = "Content-Length:" + 0 + "\r\n\r\n";
        String msgBody = "";
        String url=request.get(1);
        if ("GET".equals(request.getFirst())) {
            if (url.equals("/")||url.isEmpty()) {
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            }

            if (url.startsWith("/echo")) {
                    httpResponse = "HTTP/1.1 200 OK\r\n";
                    msgBody = url.substring(url.indexOf('o') + 2);
                    hContentType = "Content-Type: text/plain\r\n";
                    hContentLength = "Content-Length:" + msgBody.length() + "\r\n\r\n";
                }
            if (url.startsWith("/user-agent")) {
                msgBody= request.get(request.indexOf("User-Agent:")+1);
                httpResponse = "HTTP/1.1 200 OK\r\n";
                hContentType = "Content-Type: text/plain\r\n";
                hContentLength = "Content-Length:" + msgBody.length() + "\r\n\r\n";


            }
        }
        System.out.println(httpResponse + hContentType + hContentLength + msgBody);
       return  httpResponse + hContentType + hContentLength + msgBody;
        //return httpResponse;
    }
}
