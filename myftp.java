import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.math.*;

public class myftp {

	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);

		try {
			if(args.length != 2){
				System.out.println("[ERR] Please include two arguments, machine name and port number");
			}
			String machine = args[0];
			int port = Integer.parseInt(args[1]);
			Socket client_sock = new Socket(machine,port);
			DataOutputStream dout=new DataOutputStream(client_sock.getOutputStream()); 
			DataInputStream din=new DataInputStream(client_sock.getInputStream());  
			String cmd = "";
			String prompt = "myftp>";
			while(!cmd.equals("quit")) {
				System.out.print(prompt + " ");
				cmd = in.nextLine();
				System.out.println();

				if(cmd.equals("ls") || cmd.equals("pwd") || cmd.indexOf("mkdir") != -1) {
					dout.writeUTF(cmd);  
					dout.flush();  
					System.out.println(din.readUTF());
				}else if(cmd.indexOf("get") != -1) {
					dout.writeUTF(cmd);  
					dout.flush();  
					String response = din.readUTF();
					if(response.equals("FOUND")) {
						//create file
					}
					else {
						System.out.println("[ERR] File not found.");
						
					}
				}
			}
			dout.close();  
			client_sock.close();  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}