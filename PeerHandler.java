import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class PeerHandler {
	private static final String HELLO = "HELLO";
	
	public Socket socket = null;
	private OutputStream oos = null;
	private InputHandler ih = null;
	private boolean sentHandshake = false;
	public boolean otherPeerIsInterested = false;
	/**The amount of data received from this peer since the last choke cycle*/
	private int dataRcvd = 0;
	private boolean[] remoteSegments;

	public PeerHandler(Socket s) {
		this.socket = s;
		//@ TODO: remoteSegments=new boolean[FileData.length];
		try {
			oos = s.getOutputStream();
			ih = new InputHandler(s);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendHandshake() {
		byte[] handshakeBytes = new byte[32];
		System.arraycopy(HELLO.getBytes(), 0, handshakeBytes, 0, HELLO.getBytes().length);
		byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerProcess.peerID).array();
		System.arraycopy(peerIDBytes, 0, handshakeBytes, 28, 4);
		try {
			oos.write(handshakeBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		sentHandshake = true;
	}

	/**
	 * Start the InputHandler
	 */
	public void start() {
		ih.start();
	}
	
	class InputHandler extends Thread {
		private InputStream ois = null;

		public InputHandler(Socket s) {
			try {
				ois = s.getInputStream();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			byte[] input = new byte[32];
			byte[] payload = new byte[4];
			try {
				//listen for handshake:
				ois.read(input, 0, 32);
				//test handshake
				Logger.debug(4, "Received handshake message: " + new String(input, 0, 32));
				if(new String(input, 0, 5).equals(HELLO)) {
					System.arraycopy(input, 28, payload, 0, 4);
					int receivedPeerID = ByteBuffer.wrap(payload).getInt();
					int expectedPeerID = Integer.valueOf(peerProcess.getRPI(PeerHandler.this).peerId);
					if(receivedPeerID == expectedPeerID) {
						Logger.debug(4, "Handshake Approved");
					}
					else {
						Logger.debug(4, "Handshake NOT Approved");
						//TODO: what do we do if the expected peerID is not the received peerID?
						//exception? loop until expected is received? resend handshake?
					}
				}
				//TODO: should we ignore a non-handshake first message? loop until a good one is found?
				//this should probably go inside the approval section
				if(!sentHandshake) sendHandshake();
				
				// @ TODO handle bitfield here.
				
				payload = new byte[4];
				int next=0;
				while((next = ois.read(payload, 0, 4)) >=0) {
					//messageLength[0-3], messageType[4]
					Logger.debug(4, "PeerHandler: port "+socket.getPort()+" received "+next);
					
					int len = ByteBuffer.wrap(payload).getInt();
					int type = ois.read();
					
					Message.MessageType mType = Message.MessageType.values()[type];
					//TODO: HANDLE INCOMING MESSAGES; maybe make this its own method
					if(mType == Message.MessageType.CHOKE) {
						//no payload
						//TODO
					}
					else if(mType == Message.MessageType.UNCHOKE) {
						//no payload
						//TODO
					}
					else if(mType == Message.MessageType.INTERESTED) {
						//no payload
						//TODO: set otherPeerIsInterested to true
					}
					else if(mType == Message.MessageType.NOT_INTERESTED) {
						//no payload
						//TODO: set otherPeerIsInterested to false
					}
					else {
						payload = new byte[len];
						dataRcvd += len; //Increment the data received count
						ois.read(payload);
						if(mType == Message.MessageType.HAVE) {
							//TODO
						}
						else if(mType == Message.MessageType.BITFIELD) {
							//TODO
						}
						else if(mType == Message.MessageType.REQUEST) {
							//TODO
						}
						else if(mType == Message.MessageType.PIECE) {
							//TODO
						}
					}
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** Clears the data received count*/
	public void clearDataCounter() {
		dataRcvd = 0;
	}
	
	/** Fetches the data received count*/
	public int getDataRcvd() {
		return dataRcvd;
	}
}
