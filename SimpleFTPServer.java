import java.io.*;
import java.net.ServerSocket;

public class SimpleFTPServer {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("[ERR] Include two port number arguments");
			System.exit(0);
		}

		int nPort = Integer.parseInt(args[0]);
		int tPort = Integer.parseInt(args[1]);
		new NormalConnectionListener(nPort);

		// Terminate port listener in main thread
		try {
			ServerSocket tServerSocket = new ServerSocket(tPort);
			System.out.println("Listening on terminate port...");
		} catch (IOException e) {
			// TODO
		}


	}
}
