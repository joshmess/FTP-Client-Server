import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TerminateConnection implements Runnable{
    private Socket clientSocket;
    private ThreadPool threadPool;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    TerminateConnection(Socket clientSocket, ThreadPool threadPool) {
        this.clientSocket = clientSocket;
        this.threadPool = threadPool;
        try {
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            // TODO How to handle? How client should react if no input/output stream created?
        }

        System.out.println("Creating thread: Terminate Port Connection");
    }

    private void terminateCommand() {
        try {
            long commandID = dataInputStream.readLong();

            // Terminate task and send success to client.
            dataOutputStream.writeBoolean(threadPool.terminateTask(commandID));
            dataOutputStream.flush();
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to read stream from terminate port connection. Aborting connection...");
            return;
        }
    }

    @Override
    public void run() {
        terminateCommand();

        try {
            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Exception encountered while closing TerminateConnection socket.");
        }
    }
}
