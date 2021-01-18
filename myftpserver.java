import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
					//create a directory in the present working directory
					String dirname = cmd.substring(cmd.indexOf(" ")+1);
					File f = new File("./"+dirname);
					f.mkdir();
					dos.writeUTF("");
					dos.flush();
				}else if(cmd.indexOf("get")!= -1){
					//copy file from remote server to local directory
					boolean exists = false;
					String filename = cmd.substring(cmd.indexOf(" ")+1);
					File dir = new File(".");
					File[] file_list = dir.listFiles();
					for(File f: file_list) {
						if(filename.equals(f.getName())) {
							exists = true;
							dos.writeUTF("FOUND");
							dos.flush();
							//copy file here
							int bytes = 0;
							File file = new File(filename);
							FileInputStream fis = new FileInputStream(file);
							//send file size
							dos.writeLong(file.length());
							//send file in chunks
							byte[] buffer = new byte[4*1024];
							while ((bytes=fis.read(buffer))!=-1){
								dos.write(buffer,0,bytes);
								dos.flush();
							}
							fis.close();
						}
					}
					if(!exists) {
						dos.writeUTF("UNFOUND");
						dos.flush();
					}
				}else if(cmd.indexOf("cd") != -1){
					//change the present working directory
					boolean exists = false;
					String dirname = cmd.substring(cmd.indexOf(" ")+1);
					
			        File directory = new File(dirname).getAbsoluteFile();
			       
			        exists = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
			        
			        
			        if(!exists) {
			        	dos.writeUTF("No such file or directory");
						dos.flush();
			        }else {
			        	dos.writeUTF("");
						dos.flush();
			        }

				}else if(cmd.indexOf("put")!= -1){
					//put file from local directory into remote server
					String filename = cmd.substring(cmd.indexOf(" ")+1);
					FileOutputStream fos = new FileOutputStream(filename);
					int bytes = 0;
					long size = dis.readLong();   
					byte[] buffer = new byte[4*1024];
					while (size > 0 && (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
						fos.write(buffer,0,bytes);
						size -= bytes;     
					}
					fos.close();
					
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