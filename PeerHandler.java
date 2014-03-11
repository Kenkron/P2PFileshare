import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PeerHandler {
	private static String HELLO = "HELLO";
	
	public Socket socket = null;
	private OutputStream oos = null;
	private InputHandler ih = null;
	private boolean sentHandshake = false;
	public boolean otherPeerIsInterested = false;
	/**The amount of data received from this peer since the last choke cycle*/
	private int dataRcvd = 0;

	public PeerHandler(Socket s) {
		this.socket = s;
		try {
			oos = s.getOutputStream();
			ih = new InputHandler(s);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendHandshake() {
		//TODO: check about the conflicting requirements
		String handshakeMessage = "HELLO";
		byte[] handshakeBytes = new byte[32];
		System.arraycopy(handshakeMessage.getBytes(), 0, handshakeBytes, 0, handshakeMessage.getBytes().length);
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
			byte[] payload;
			try {
				//listen for handshake:
				ois.read(input, 0, 32);
				//test handshake
				if(new String(input, 0, 5).equals(HELLO)) {
					//handshake approved
				}
				if(!sentHandshake) sendHandshake();
				
				int next=0;
				while((next = ois.read(input, 0, 5)) >=0) {
					//messageLength[0-3], messageType[4]
					Logger.debug(4, "PeerHandler: port "+socket.getPort()+" recieved "+next);
					
					int len = Integer.valueOf(new String(input, 0, 4));
					int type = Integer.valueOf(new String(input, 4, 1));
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
