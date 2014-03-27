import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class PeerHandler {
	private static final String HELLO = "HELLO";
	
	/**The number of bytes used to store an int*/
	public static final int INT_LENGTH=4;
	/**The number of bytes for the message type declaration*/
	public static final int TYPE_LENGTH=1;
	/**The number of bytes in the handshake message*/
	public static final int HANDSHAKE_LENGTH=32;
	
	public Socket socket = null;
	private OutputStream oos = null;
	private InputHandler ih = null;
	
	private int otherPeerID;
	private boolean sentHandshake = false;
	public boolean otherPeerIsInterested = false;
	public boolean otherPeerIsChoked = false;
	private boolean waitingForRequestFromOtherPeer = false;
	/**The amount of data received from this peer since the last choke cycle*/
	private int dataRcvd = 0;
	
	private boolean[] remoteSegments;
	//this might be unnecessary
	private byte[] getBitfield() {
		return FileData.createBitfield(remoteSegments);
	}

	public PeerHandler(Socket s) {
		this.socket = s;
		remoteSegments = new boolean[peerProcess.myCopy.segmentOwned.length];
		try {
			oos = s.getOutputStream();
			ih = new InputHandler(s);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendHandshake() {
		byte[] outputBytes = new byte[32];
		byte[] peerIDBytes = ByteBuffer.allocate(INT_LENGTH).putInt(peerProcess.peerID).array();
		
		System.arraycopy(HELLO.getBytes(), 0, outputBytes, 0, HELLO.getBytes().length);
		//middle bytes already default to 0
		System.arraycopy(peerIDBytes, 0, outputBytes, 28, INT_LENGTH);
		try {
			oos.write(outputBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		sentHandshake = true;
	}
	
	public void sendChoke() {
	    byte[] chokeBytes = new byte[5];
	    chokeBytes[3] = (byte) 1;//set message length to 1
        chokeBytes[4] = (byte) Message.MessageType.CHOKE.ordinal(); 
		try {
			oos.write(chokeBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	    this.otherPeerIsChoked = true;
	    
	}
	
	public void sendUnchoke() {
	    if (otherPeerIsChoked) {
	        byte[] unchokeBytes = new byte[5];
	        unchokeBytes[3] = (byte) 1;//set message length to 1
            unchokeBytes[4] = (byte) Message.MessageType.UNCHOKE.ordinal(); 
		    try {
			    oos.write(unchokeBytes);
		    }
		    catch(IOException e) {
		    	e.printStackTrace();
		    }
		    otherPeerIsChoked = false;
	    }
	}
	
	/**Send a HAVE message
	 * 4byte message length, 1byte HAVE ordinal, 4byte payload (pieceIndex)*/
	public void sendHave(int pieceIndex) {
		byte[] payloadBytes = ByteBuffer.allocate(INT_LENGTH).putInt(pieceIndex).array();
		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH).putInt(payloadBytes.length + 1).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + 1 + payloadBytes.length];//4 + 1 + 4
		
		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
		outputBytes[4] = (byte) Message.MessageType.HAVE.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, 5, payloadBytes.length);
		try {
		    oos.write(outputBytes);
	    }
	    catch(IOException e) {
	    	e.printStackTrace();
	    }
	}

	/**Send a BITFIELD message
	 * 4byte message length, 1byte BITFIELD ordinal, variable sized payload (myBitfield)*/
	public void sendBitfield() {
		boolean hasPartialFile = false;
		byte[] myBitfield = peerProcess.myCopy.getBitfield();
		for(byte b : myBitfield) {
			if(b != 0) {
				hasPartialFile = true;
				break;
			}
		}
		if(hasPartialFile) {
			byte[] payloadBytes = myBitfield;
			byte[] msgLengthBytes = ByteBuffer.allocate(4).putInt(payloadBytes.length + 1).array();
			byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH + payloadBytes.length];//4 + 1 + variable
			
			System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
			outputBytes[4] = (byte) Message.MessageType.BITFIELD.ordinal();
			System.arraycopy(myBitfield, 0, outputBytes, INT_LENGTH + TYPE_LENGTH, myBitfield.length);
			try {
			    oos.write(outputBytes);
		    }
		    catch(IOException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	public void sendRequest() {
	    //TODO: determine random piece you need from neighbor and send request
	}


	/**Start the InputHandler*/
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
			byte[] input = new byte[HANDSHAKE_LENGTH];
			byte[] payload = new byte[INT_LENGTH];
			try {
				//listen for handshake:
				ois.read(input, 0, 32);
				//test handshake
				Logger.debug(4, "Received handshake message: " + new String(input, 0, HELLO.length()));
				if(new String(input, 0, HELLO.length()).equals(HELLO)) {
					System.arraycopy(input, input.length-HELLO.length(), payload, 0, 4);
					int receivedPeerID = ByteBuffer.wrap(payload).getInt();
					int expectedPeerID = Integer.valueOf(peerProcess.getRPI(PeerHandler.this).peerId);
					otherPeerID = expectedPeerID;
					if(receivedPeerID == expectedPeerID) {
						Logger.debug(4, "Handshake Approved");
						if(!sentHandshake) {
							sendHandshake();
						}
						sendBitfield();
					}
					else {
						Logger.debug(4, "Handshake NOT Approved");
						//TODO: what do we do if the expected peerID is not the received peerID?
						//exception? loop until expected is received? resend handshake?
					}
				}
				//TODO: should we ignore a non-handshake first message? loop until a good one is found?
				//this should probably go inside the approval section
				
				
				payload = new byte[4];
				int next=0;
				while((next = ois.read(payload, 0, 4)) >=0) {
					//messageLength[0-3], messageType[4]
					Logger.debug(4, "PeerHandler: port "+socket.getPort()+" received "+next);
					
					int len = ByteBuffer.wrap(payload).getInt() - 1;//1 byte is used for the type
					int type = ois.read();
					
					Message.MessageType mType = Message.MessageType.values()[type];
					//TODO: HANDLE INCOMING MESSAGES; maybe make this its own method
					if(mType == Message.MessageType.CHOKE) {
						Logger.debug(4, "Peer " + peerProcess.peerID
	                                 + " is choked by " + 
	                                 peerProcess.getRPI(PeerHandler.this).peerId);
						Logger.chokedBy(otherPeerID);
						//TODO
					}
					else if(mType == Message.MessageType.UNCHOKE) {
						Logger.debug(4, "Peer " + peerProcess.peerID
	                                             + " is unchoked by " + 
	                                             peerProcess.getRPI(PeerHandler.this).peerId);
						Logger.chokedBy(otherPeerID);
						//TODO: send back a request message
					}
					else if(mType == Message.MessageType.INTERESTED) {
						Logger.receivedInterested(otherPeerID);
						otherPeerIsInterested = true;
					}
					else if(mType == Message.MessageType.NOT_INTERESTED) {
						Logger.receivedNotInterested(otherPeerID);
						otherPeerIsInterested = false;
					}
					else {
						payload = new byte[len];
						dataRcvd += len; //Increment the data received count
						ois.read(payload);
						if(mType == Message.MessageType.HAVE) {
							int pieceIndex = ByteBuffer.wrap(payload).getInt();
							remoteSegments[pieceIndex] = true;
							//TODO: do we need to request this new piece?
						}
						else if(mType == Message.MessageType.BITFIELD) {
							//Note: this should not go before while loop because a bitfield message doesn't need to be sent
							//get bitfield, convert to segmentsOwned
							boolean[] segmentOwnedLarge = FileData.createSegmentsOwned(payload);
							//cut the segmentOwned to the appropriate size
							System.arraycopy(segmentOwnedLarge, 0, remoteSegments, 0, remoteSegments.length);
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
