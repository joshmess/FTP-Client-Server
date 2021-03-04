import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens for new connections on the normal or terminate connection port and creates a new
 * thread for that FTP connection.
 */
class ConnectionListener implements Runnable {
    public enum ListenerType {
        NORMAL      ("NORMAL"),
        TERMINATE   ("TERMINATE");

        private final String name;

        ListenerType(String name) { this.name = name; }
    }

    private static FileLocks fileLocks;
    private static TaskTable taskTable;

    private int port;
    private ListenerType type;

    ConnectionListener(int nPort, ListenerType type) {
        port = nPort;
        this.type = type;
        System.out.printf("Creating thread: %s Port Listener\n", type.name);
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.printf("Listening on %s port...\n", type.name);

            // Accepts incoming client connections and creates new thread to handle connection.
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (type == ListenerType.NORMAL) {
                        new NormalConnection(clientSocket, new File("."), fileLocks, taskTable);
                    } else {
                        new TerminateConnection(clientSocket);
                    }
                    System.out.printf("Connection established on %s port.\n", type.name);
                } catch (IOException e) {
                    System.err.printf("[ERROR] Unable to accept %s connection. Continuing to accept new connections.\n", type.name);
                }
            }
        } catch (IOException e) {
            System.err.printf("[ERROR] Unable to create %s port socket.\n", type.name);
        } catch (IllegalArgumentException e) {
            System.err.printf("[ERROR] Invalid server %s port number. Value must be 0 - 65535.\n", type.name);
        }
    }
}
