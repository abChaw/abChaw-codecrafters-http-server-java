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
    try(ServerSocket serverSocket = new ServerSocket(4221)){
    //
    //   // Since the tester restarts your program quite often, setting SO_REUSEADDR
    //   // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);

      Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
      BufferedReader in = new BufferedReader(
              new InputStreamReader(clientSocket.getInputStream())
      );
       for (String line; (line = in.readLine()) != null && !line.isEmpty(); ) {}

       String httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
       OutputStream  out = clientSocket.getOutputStream();
       out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.UTF_8));
       clientSocket.close();
       System.out.println("Response sent, connection closed");
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
