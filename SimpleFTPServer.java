public class SimpleFTPServer {
	public static ThreadPool threadPool;

	public static void main(String[] args) throws InterruptedException {
		if (args.length != 2) {
			System.err.println("[ERR] Include two port number arguments");
			System.exit(0);
		}

		int nPort = Integer.parseInt(args[0]);
		int tPort = Integer.parseInt(args[1]);

		// Must begin threadpool before connection listeners
		threadPool = new ThreadPool(null);
		new ConnectionListener(nPort, ConnectionListener.ListenerType.NORMAL);
		new ConnectionListener(tPort, ConnectionListener.ListenerType.TERMINATE);
	}
}
