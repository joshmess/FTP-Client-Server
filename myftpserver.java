import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.math.*;

public class myftpserver {

	public static void main(String[] args) {

		try {

			if (args.length == 0){
				System.out.println("[ERR] Include port number argument");
				System.exit(0);
			}
			int port = Integer.parseInt(args[0]);
			ServerSocket server_sock = new ServerSocket(port);
			System.out.println("Server Listening...");
			Socket client_sock = server_sock.accept();
			System.out.println("Connection Established.");
			String cmd = "";
			while(!cmd.equals("quit")) {
				DataInputStream dis=new DataInputStream(client_sock.getInputStream());  
				DataOutputStream dos = new DataOutputStream(client_sock.getOutputStream());
				cmd=(String)dis.readUTF();  
				
				
				if(cmd.equals("ls")) {
					//list all files in pwd
					File dir = new File(".");
					File[] file_list = dir.listFiles();
					String files ="";
					Arrays.sort(file_list);
					for(File f: file_list) {
						files += f.getName() + " ";
					}
					dos.writeUTF(files);
					dos.flush();

				}else if(cmd.equals("pwd")){
					//get the name of the current directory where the server resides
					String pwd = new File(".").getCanonicalPath();
					dos.writeUTF(pwd);
					dos.flush();
				}else if(cmd.indexOf("mkdir") != -1){
					String dirname = cmd.substring(cmd.indexOf(" ")+1);
					File f = new File("./"+dirname);
					f.mkdir();
					dos.writeUTF("Directory created.");
					dos.flush();
				}else if(cmd.indexOf("get")!= -1){
					String filename = cmd.substring(cmd.indexOf(" ")+1);
					File dir = new File(".");
					File[] file_list = dir.listFiles();
					for(File f: file_list) {
						if(filename.equals(f.getName())) {
							dos.writeUTF("FOUND");
							//copy file here
						}
					}
					dos.writeUTF("UNFOUND");
					dos.flush();
				}else {
		
					//command not recognized
					dos.writeUTF("");
					dos.flush();
				}
			}
			server_sock.close();  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}