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
    Socket clientSocket;
    File pwd;

    NormalConnection(Socket clientSocket) {
        this.clientSocket = clientSocket;
        pwd = new File(".");
        System.out.println("Creating thread: Normal Port Connection");
    }

    public boolean processCommand(DataInputStream dis, DataOutputStream dos) {
        // Receive and process command
        String cmd;
        try {
            cmd = (String) dis.readUTF();

            if (cmd.equals("quit")) {
                return false;
            } else if (cmd.equals("ls")) {
                // List all files in pwd
                File[] fileList = pwd.listFiles();
                Arrays.sort(fileList);

                String files = "";
                for (File f: fileList) {
                    files += f.getName() + " ";
                }
                dos.writeUTF(files);
                dos.flush();
            } else if (cmd.equals("pwd")) {
                // Get the name of the current directory where the server resides
                dos.writeUTF(pwd.getName());
                dos.flush();
            } else if (cmd.startsWith("mkdir")) {
                // Create a directory in the present working directory
                String dirName = cmd.substring(cmd.indexOf(" ") + 1);
                File f = new File(pwd.getCanonicalPath() + "/" + dirName);
                f.mkdir();
                dos.writeUTF("");
                dos.flush();
            } else if (cmd.startsWith("get")) {
                // Send specified file back to client via DataOutputStream
                String filename = cmd.substring(cmd.indexOf(" ") + 1);
            }
        } catch (IOException e) {
            // TODO
        }


    }

    public void run() {
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            // Loops until quit command is received
            while (processCommand(dis, dos)) {}

            clientSocket.close();
        } catch (IOException e) {
            // TODO
        }
    }
}
