import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server implements Runnable {
	private final int thePort = 53535;
	ArrayList<RemotePeerInfo> peerList;
	ArrayList<Socket> peerSocketList;
	ServerSocket serverSocket;
	
	public Server(ArrayList<RemotePeerInfo> peerList, ArrayList<Socket> peerSocketList) {
		this.peerList = peerList;
		this.peerSocketList = peerSocketList;
		try {
			serverSocket = new ServerSocket(thePort);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(true) {
			try {
				Socket s = serverSocket.accept();
				if(peerProcess.addSocketToList(s)) {
					new PeerHandler(s).start();
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
