import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server implements Runnable {
	private int thePort;
	ArrayList<RemotePeerInfo> peerList;
	ArrayList<PeerHandler> peerHandlerList;
	ServerSocket serverSocket;
	
	public Server(ArrayList<RemotePeerInfo> peerList, ArrayList<PeerHandler> peerHandlerList, int portNum) {
		this.peerList = peerList;
		this.thePort = portNum;
		this.peerHandlerList = peerHandlerList;
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
				PeerHandler ph = new PeerHandler(s);
				if(peerProcess.addPeerHandlerToList(ph)) {
					ph.start();
					//Attempt to get the peerID from the hostname of the associated socket
					//I'm not sure if this will work.
					//Question: do we log the connection when the sockets are set up or after the handshake messages?
					String otherPeerID = null;
					for(RemotePeerInfo rpi : peerList) {
						if(rpi.peerAddress.equals(s.getInetAddress().getCanonicalHostName())) {
							otherPeerID = rpi.peerId;
						}
					}
					Logger.connectedFrom(Integer.valueOf(otherPeerID));
					Logger.debug(2,"Server Connected From "+otherPeerID);
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
