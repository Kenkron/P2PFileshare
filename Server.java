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
		try {
			while(!Thread.interrupted()) {
				Socket s = serverSocket.accept();
				Logger.debug(Logger.DEBUG_STANDARD, "Server: recieved a connection");

				PeerHandler ph = new PeerHandler(s);
				if(peerProcess.addPeerHandlerToList(ph)) {
					ph.start();
					String otherPeerID = null;
					/*
					//Get the peerID from the hostname of the associated socket
					for(RemotePeerInfo rpi : peerList) {
						if(rpi.peerAddress.equals(s.getInetAddress().getCanonicalHostName())) {
							otherPeerID = rpi.peerId;
						}
					}
					*/
					
					otherPeerID = peerProcess.getRPI(ph).peerId;
					Logger.connectedFrom(Integer.valueOf(otherPeerID));
					Logger.debug(Logger.DEBUG_STANDARD,"Server: Connection From "+otherPeerID);
				} else {
					Logger.debug(Logger.DEBUG_STANDARD,"Server: connection rejected");
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
