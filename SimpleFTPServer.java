import sun.applet.Main;

import java.io.*;
import java.net.ServerSocket;

public class SimpleFTPServer {
	public static ThreadPool threadPool;

	public static void main(String[] args) throws InterruptedException {
		if (args.length != 2) {
			System.err.println("[ERR] Include two port number arguments");
			System.exit(0);
		}

		int nPort = Integer.parseInt(args[0]);
		int tPort = Integer.parseInt(args[1]);
		threadPool = new ThreadPool(null, Thread.currentThread());

		// Wait until ThreadPool is initialized
		Main m = new Main();
		synchronized (m) {
			m.wait();
		}

		new ConnectionListener(nPort, ConnectionListener.ListenerType.NORMAL);
		new ConnectionListener(tPort, ConnectionListener.ListenerType.TERMINATE);
	}
}
