import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server implements Runnable {
	private final int thePort = 53535;
	ArrayList<RemotePeerInfo> peerList;
	ArrayList<Socket> peerSockets = new ArrayList<Socket>();
	ServerSocket serverSocket;
	
	public Server(ArrayList<RemotePeerInfo> peerList) {
		this.peerList = peerList;
		try {
			serverSocket = new ServerSocket(thePort);
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		while(true) {
			try {
				Socket s = serverSocket.accept();
				peerSockets.add(s);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
