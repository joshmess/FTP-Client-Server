import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Normal FTP connection thread.
 */
class NormalConnection implements Runnable {
    private Socket clientSocket;
    private ThreadPool threadPool;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private File pwd;

    NormalConnection(Socket clientSocket, ThreadPool threadPool) {
        this.clientSocket = clientSocket;
        this.threadPool = threadPool;
        pwd = new File(".");
        try {
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            // TODO How to handle? How client should react if no input/output stream created?
        }

        System.out.println("Creating thread: Normal Port Connection");
    }

    private boolean processCommand() {

        return true;
    }

    public void run() {
        // Loops until quit command is received
        while (processCommand()) {}

        try {
            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Exception encountered while closing NormalConnection socket.");
        }
    }
}
