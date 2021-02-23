import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens for new connections on the normal connection port and creates a new
 * thread for that FTP connection.
 */
class NormalConnectionListener implements Runnable {
    int port;

    NormalConnectionListener(int nPort) {
        port = nPort;
        System.out.println("Creating thread: Normal Port Listener");
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Listening on normal port...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connection established on normal port.");
                    new NormalConnection(clientSocket);
                } catch (IOException e) {
                    System.err.println("[ERROR] Unable to accept connection. Continuing to accept new connections.");
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to create normal port socket.");
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] Invalid server normal port number. Value must be 0 - 65535.");
        }
    }
}
