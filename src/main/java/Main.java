import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class Main {

    private static Path rootDir = Paths.get(".").toAbsolutePath().normalize();

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        for (int i = 0; i < args.length - 1; i++) {
            if ("--directory".equals(args[i])) {
                rootDir = Paths.get(args[i + 1]).toAbsolutePath().normalize();
            }
        }

        processConnectionRequests();
    }

    private static void processConnectionRequests() {
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                pool.submit(() -> processHttpRequest(clientSocket));

            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            pool.shutdown();                          // stop accepting new tasks on exit
        }
    }

    private static void processHttpRequest(Socket clientSocket) {

            try{InputStream in = clientSocket.getInputStream(); OutputStream out = clientSocket.getOutputStream();
                while(true) {
                    StringBuilder requestData = new StringBuilder(readBytesToAscii(in));
                    Map<String, String> hdr = getHttpHeaders(in);
                    boolean wantsClose =
                            "close".equalsIgnoreCase(hdr.getOrDefault("connection", ""));
                    System.out.println("line   ::  " + requestData);
                    List<String> request = Arrays.asList(requestData.toString().split(" "));
                    String httpResponse = buildHttp404();
                    String method = request.getFirst();
                    String url = request.get(1);

                    if (method.equals("GET")) {
                        String response = processHttpGet(out, url, hdr);
                        if (response.isEmpty()) {
                            response = buildHttp404();
                            out.write(response.getBytes(StandardCharsets.UTF_8));

                        }
                        // clientSocket.close();
                        //httpResponse = response.isEmpty()?buildHttp404():response;
                        //return;
                    }
                    if (method.equals("POST")) {
                        byte[] body = readBytesFromHttpRequestBody(clientSocket.getInputStream(), hdr);
                        String response = processHttpPost(url, hdr, body);
                        httpResponse = response.isEmpty() ? buildHttp404() : response;
                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                    }


                    if (wantsClose) {
                        clientSocket.close();
                        in.close();
                        break;
                    }
                    System.out.println("Response sent, connection closed");
                }
            } catch (IOException e) {

                System.out.println("Client error: " + e.getMessage());
            }

    }

    private static String processHttpPost(String url, Map<String, String> hdr, byte[] body) {
        String response = "";
        if (url.startsWith("/files/")) {
            response = createAndSaveBytesToFile(url.substring("/files/".length()), body);
        }
        return response;

    }

    private static String processHttpGet(OutputStream out, String url, Map<String, String> hdr) {
         String httpResponse="",msgBody="";
        if (url.equals("/") || url.isEmpty()) {
            httpResponse = buildHttp200(out, "", hdr);
        }

        if (url.startsWith("/echo")) {

            msgBody = url.substring(url.indexOf('o') + 2);
            httpResponse = buildHttp200(out,msgBody,hdr);
        }
        if (url.startsWith("/user-agent")) {
            msgBody = hdr.getOrDefault("user-agent", "");
            httpResponse = buildHttp200(out, msgBody, hdr);
        }
        if (url.startsWith("/files")) {
            httpResponse = getFileResponse(out,url.substring("/files/".length()));
        }
        return httpResponse;
    }

    private static Map<String, String> getHttpHeaders(InputStream in) {
        Map<String, String> hdr = new HashMap<>();
        String line;
        while (!(line = readBytesToAscii(in).trim()).isEmpty()) {
            int c = line.indexOf(':');
            if (c > 0) hdr.put(line.substring(0,c).toLowerCase(),
                    line.substring(c+1).trim());
        }
        return hdr;
    }

    private static String getFileResponse(OutputStream out, String fileName) {

        try {
            Path p = rootDir.resolve(fileName).normalize();
            if (!p.startsWith(rootDir) || !Files.exists(p) || !Files.isRegularFile(p)) {
                //return buildHttp404();
                try {
                    out.write(buildHttp404().getBytes(StandardCharsets.UTF_8));
                    //  out.write(payload);
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            byte[] bytes = Files.readAllBytes(p);
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 200 OK\r\n")
                    .append("Content-Type: application/octet-stream\r\n")
                    .append("Content-Length: ").append(bytes.length).append("\r\n")
                    .append("\r\n");
             String s=sb.toString() + new String(bytes, StandardCharsets.UTF_8); // keep raw bytes

            try {
                out.write(s.getBytes(StandardCharsets.UTF_8));
              //  out.write(payload);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            return buildHttp404();   // I/O error: treat as not-found
        }
        return "ddd";
    }

    private static String buildHttp200(OutputStream out, String body, Map<String, String> hdr) {
         List<String> encodingTypes = Arrays.asList(hdr.getOrDefault("accept-encoding", "").split(","));
        encodingTypes.replaceAll(String::trim);
        String encodingHeaderValue = encodingTypes.contains("gzip")?"Content-Encoding: "+"gzip\r\n" : "";
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);

        if(!encodingHeaderValue.isEmpty()) {payload = compressPayload(body,hdr);}
        String response= "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: " + payload.length + "\r\n"
                + encodingHeaderValue
                + "\r\n";
        try {
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.write(payload);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
      return "dd";
    }

    private static byte []  compressPayload(String body, Map<String, String> hdr) {

       // List<String> encodingTypes = Arrays.asList(hdr.getOrDefault("accept-encoding", "").split(","));
       // encodingTypes.replaceAll(String::trim);
       // String encodingHeaderValue = encodingTypes.contains("gzip")?"Content-Encoding: "+"gzip\r\n" : "";
//        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
//        if(!encodingHeaderValue.isEmpty()) {
//            //payload = compressPayload(body);
//        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = null;
        try {
            gzipOutputStream = new GZIPOutputStream(bos);
            gzipOutputStream.write(body.getBytes(StandardCharsets.UTF_8));
            gzipOutputStream.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();

    }

    private static String buildHttp404() {
        return "HTTP/1.1 404 Not Found\r\n\r\n";
    }

    private static byte[] readBytesFromHttpRequestBody(InputStream in, Map<String, String> hdr) throws IOException {
        int contentLen = Integer.parseInt(hdr.getOrDefault("content-length", "0"));
        byte[] body = new byte[contentLen];
        if (contentLen > 0) {
            int off = 0;
            while (off < body.length) {
                int r = in.read(body, off, body.length - off);
                if (r == -1) throw new EOFException("Stream ended early");
                off += r;
            }
        }
        return body;
    }

    private static String createAndSaveBytesToFile(String fileName, byte[] data) {
        try {
            Path p = rootDir.resolve(fileName).normalize();
            if (!p.startsWith(rootDir)) return buildHttp404();       // path-traversal guard
            Files.createDirectories(p.getParent());
            Files.write(p, data);
            return buildHttp201();//"HTTP/1.1 201 Created\r\n\r\n";
        } catch (IOException e) {
            return buildHttp500();
        }
    }

    private static String buildHttp500() {
        return "HTTP/1.1 500 Internal Server Error\r\n\r\n";
    }

    private static String buildHttp201() {
        return "HTTP/1.1 201 Created\r\n\r\n";
    }

    /** Read a CRLF-terminated line from a byte stream (no charset decoding). */
    private static String readBytesToAscii(InputStream in) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        int prev = -1, cur;
        try{
            while ((cur = in.read()) != -1) {
            if (prev == '\r' && cur == '\n') {
                buf.write('\r'); // include CRLF in buffer if you need it
                buf.write('\n');
                break;
            }
            buf.write(cur);
            prev = cur;
        }
           // HTTP header = ASCII
    }catch (IOException e) {}
        return buf.toString(StandardCharsets.US_ASCII);
    }

}
