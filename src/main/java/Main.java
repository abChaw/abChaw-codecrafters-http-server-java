import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {

    private static Path rootDir= Paths.get(".").toAbsolutePath().normalize();
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        for (int i = 0; i < args.length - 1; i++) {
            if ("--directory".equals(args[i])) {
                rootDir=Paths.get(args[i + 1]).toAbsolutePath().normalize();
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
          
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                pool.submit(() -> handle(clientSocket));
                System.out.println("Response sent, connection closed");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }finally {
            pool.shutdown();                          // stop accepting new tasks on exit
        }
    }

    private static void handle(Socket clientSocket)  {
       try( BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
        );){
        StringBuilder requestData = new StringBuilder(in.readLine());
        String line="";
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            requestData.append(" "+line);
        }
        System.out.println("line   ::  "+requestData);
        List<String> request = Arrays.asList(requestData.toString().split(" "));
        String httpResponse = build404();

        String msgBody = "";
        String url=request.get(1);
        if ("GET".equals(request.getFirst())) {
            if (url.equals("/")||url.isEmpty()) {
                httpResponse = build200Plain("");
            }

            if (url.startsWith("/echo")) {

                msgBody = url.substring(url.indexOf('o') + 2);
                httpResponse=build200Plain(msgBody);
                }
            if (url.startsWith("/user-agent")) {
                msgBody= request.get(request.indexOf("User-Agent:")+1);
                httpResponse=build200Plain(msgBody);

            }
            if (url.startsWith("/files")) {
                httpResponse=getFileResponse(url.substring("/files/".length()));
            }
        }

        OutputStream out = clientSocket.getOutputStream();
        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
        clientSocket.close();
       } catch (IOException e) {
           System.out.println("Client error: " + e.getMessage());
       }

    }

    private static String getFileResponse(String fileName) {

        try {
            /* sanitise ".." etc. */
            Path p = rootDir.resolve(fileName).normalize();
            if (!p.startsWith(rootDir) || !Files.exists(p) || !Files.isRegularFile(p)) {
                return build404();
            }

            byte[] bytes = Files.readAllBytes(p);
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 200 OK\r\n")
                    .append("Content-Type: application/octet-stream\r\n")
                    .append("Content-Length: ").append(bytes.length).append("\r\n")
                    .append("\r\n");
            return sb.toString() + new String(bytes, StandardCharsets.UTF_8); // keep raw bytes

        } catch (IOException e) {
            return build404();   // I/O error: treat as not-found
        }
    }

    private static String build200Plain(String body) {
        return "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n"
                + "\r\n"
                + body;
    }

    private static String build404() {
        return "HTTP/1.1 404 Not Found\r\n\r\n";
    }
}
