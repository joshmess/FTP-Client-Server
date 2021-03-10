import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens for new connections on the normal or terminate connection port and creates a new
 * thread for that FTP connection.
 */
class ConnectionListener implements Runnable {
    public enum ListenerType {
        NORMAL      ("Normal"),
        TERMINATE   ("Terminate");

        private final String name;

        ListenerType(String name) { this.name = name; }
    }

    private static FileLocks fileLocks = new FileLocks();
    private static TaskTable taskTable = new TaskTable();

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
                        new Thread(new NormalConnection(clientSocket, fileLocks, taskTable)).start();
                    } else {
                        new Thread(new TerminateConnection(clientSocket, taskTable)).start();
                    }
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
