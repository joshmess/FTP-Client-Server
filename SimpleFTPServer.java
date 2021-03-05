public class SimpleFTPServer {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("[ERROR] Include two port number arguments");
			System.exit(0);
		}

		int nPort = Integer.parseInt(args[0]);
		int tPort = Integer.parseInt(args[1]);

		new Thread(new ConnectionListener(nPort, ConnectionListener.ListenerType.NORMAL)).start();
		new Thread(new ConnectionListener(tPort, ConnectionListener.ListenerType.TERMINATE)).start();
	}
}
