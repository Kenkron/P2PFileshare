import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.ArrayList;

public class PeerHandler {
	private static final String HELLO = "HELLO";
	
	/**The number of bytes used to store an int*/
	public static final int INT_LENGTH=4;
	/**The number of bytes for the message type declaration*/
	public static final int TYPE_LENGTH=1;
	/**The number of bytes in the handshake message*/
	public static final int HANDSHAKE_LENGTH=32;
	/**The offset for the start of a message, accounting for the
	 * length int, and type length*/
	public static final int PAYLOAD_OFFSET=INT_LENGTH+TYPE_LENGTH;
	
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
	
	public boolean weAreChoked=true;
	
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
		byte[] outputBytes = new byte[HANDSHAKE_LENGTH];
		byte[] peerIDBytes = ByteBuffer.allocate(INT_LENGTH).putInt(peerProcess.peerID).array();
		
		System.arraycopy(HELLO.getBytes(), 0, outputBytes, 0, HELLO.getBytes().length);
		//middle bytes already default to 0
		System.arraycopy(peerIDBytes, 0, outputBytes, HANDSHAKE_LENGTH-INT_LENGTH, INT_LENGTH);
		try {
			oos.write(outputBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		Logger.debug(Logger.DEBUG_ONCE, "Handshake Sent");
		sentHandshake = true;
	}
	
	public void sendChoke() {
	    byte[] chokeBytes = new byte[PAYLOAD_OFFSET];
	    chokeBytes[INT_LENGTH-1] = (byte) TYPE_LENGTH;//set message length to 1
        chokeBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.CHOKE.ordinal(); 
		try {
			oos.write(chokeBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		Logger.debug(Logger.DEBUG_STANDARD, "Choking " + otherPeerID);
	    this.otherPeerIsChoked = true;
	}
	
	public void sendUnchoke() {
	    if (otherPeerIsChoked) {
	        byte[] unchokeBytes = new byte[PAYLOAD_OFFSET];
	        unchokeBytes[INT_LENGTH-1] = (byte) TYPE_LENGTH;//set message length to 1
            unchokeBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.UNCHOKE.ordinal(); 
		    try {
			    oos.write(unchokeBytes);
		    }
		    catch(IOException e) {
		    	e.printStackTrace();
		    }
		    Logger.debug(Logger.DEBUG_STANDARD, "Unchoking " + otherPeerID);
		    otherPeerIsChoked = false;
	    }
	}
	
	/**Send a HAVE message
	 * 4byte message length, 1byte HAVE ordinal, 4byte payload (pieceIndex)*/
	public void sendHave(int pieceIndex) {
		byte[] payloadBytes = ByteBuffer.allocate(INT_LENGTH).putInt(pieceIndex).array();
		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH).putInt(payloadBytes.length + TYPE_LENGTH).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH + payloadBytes.length];//4 + 1 + 4
		
		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
		outputBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.HAVE.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, PAYLOAD_OFFSET, payloadBytes.length);
		try {
		    oos.write(outputBytes);
	    }
	    catch(IOException e) {
	    	e.printStackTrace();
	    }
		Logger.debug(Logger.DEBUG_STANDARD, "Sent HAVE " + pieceIndex + " to " + otherPeerID);
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
			byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH).putInt(payloadBytes.length + TYPE_LENGTH).array();
			byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH + payloadBytes.length];//4 + 1 + variable
			
			System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
			outputBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.BITFIELD.ordinal();
			System.arraycopy(myBitfield, 0, outputBytes, PAYLOAD_OFFSET, myBitfield.length);
			try {
			    oos.write(outputBytes);
		    }
		    catch(IOException e) {
		    	e.printStackTrace();
		    }
		}
		
		Logger.debug(Logger.DEBUG_STANDARD, "Sent BITFIELD to " + otherPeerID);
	}
	
	/**Send a REQUEST message (code 6)
	 * 4byte message length, 1byte type, 4 byte payload (pieceIndex)*/
	public void sendRequest() {
		//An oddly named array which contains indices of segments we don't
		//have and they do && it hasn't been requested yet
		ArrayList<Integer> weDontTheyDo = new ArrayList<Integer>();
		Random randomizer=new Random((long)(Math.random()*Integer.MAX_VALUE));
		
		//First we select a random piece we don't have and they do have
		for (int i = 0; i < remoteSegments.length; i++) {
			//Check that we dont have this segment, they do, and it hasn't been requested yet
			if (remoteSegments[i] && 
				!peerProcess.myCopy.segmentOwned[i] &&
				!peerProcess.currentlyRequestedPieces.contains(new Integer(i))) {
					weDontTheyDo.add(i);
			}
		}
		
		//If the list is empty (no segments they have that we don't), just stop
		if (weDontTheyDo.size() == 0)
			return;
			
		//Choose randomly from the segments we own and they do
		int randomIndex = randomizer.nextInt(weDontTheyDo.size());
        int choice = weDontTheyDo.get(randomIndex);
		
		//Now send the actual message
		byte[] payloadBytes = ByteBuffer.allocate(INT_LENGTH).putInt(choice).array();
		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH).putInt(payloadBytes.length + TYPE_LENGTH).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH + payloadBytes.length];//4 + 1 + 4
		
		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
		outputBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.REQUEST.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, PAYLOAD_OFFSET, payloadBytes.length);
		try {
		    oos.write(outputBytes);
	    }
	    catch(IOException e) {
	    	e.printStackTrace();
	    }
		
		Logger.debug(Logger.DEBUG_STANDARD, "Sent REQUEST for " + choice + " to " + otherPeerID);
	}
	
	/**This method has the job of deciding whether or not to send an interested
	 * message after receiving a HAVE or BITFIELD */
	public void decideInterest() {
		//A boolean which will be set if any one or more of the segments we
		//need are in their bitfield (remoteSegments)
		boolean interested = false;
	
		//Run through all of their segments and see if we don't have one
		for (int i = 0; i < remoteSegments.length; i++) {
			//Check that we dont have this segment and they do
			if (remoteSegments[i] && !peerProcess.myCopy.segmentOwned[i])
				interested = true;
		}	
		
		//If the they dont have any pieces that we don't, send uninterested
		if (!interested)
			sendNotInterested();
		else
		    sendInterested();
	}
	
	/**Send a INTERESTED message (code 2)
	 * 4byte message length, 1byte type*/
	public void sendInterested() {
        byte[] interestedBytes = new byte[PAYLOAD_OFFSET];
	    interestedBytes[INT_LENGTH-1] = (byte) TYPE_LENGTH;//set message length to 1
        interestedBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.INTERESTED.ordinal(); 
		try {
			oos.write(interestedBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		Logger.debug(Logger.DEBUG_STANDARD, "Sent INTERESTED to " + otherPeerID);
	}
	

    /**Send a NOTINTERESTED message (code 3)
	 * 4byte message length, 1byte type*/
	public void sendNotInterested() {
	    byte[] notInterestedBytes = new byte[PAYLOAD_OFFSET];
	    notInterestedBytes[INT_LENGTH-1] = (byte) TYPE_LENGTH;//set message length to 1
        notInterestedBytes[PAYLOAD_OFFSET-TYPE_LENGTH] = (byte) Message.MessageType.NOT_INTERESTED.ordinal(); 
		try {
			oos.write(notInterestedBytes);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		Logger.debug(Logger.DEBUG_STANDARD, "Sent NOT_INTERESTED to " + otherPeerID);
	}

	public void sendPiece(int pieceIndex) {
		byte[] sizeData=new byte[INT_LENGTH];
		byte[] typeData=new byte[TYPE_LENGTH];
		byte[] pieceData=null;
		try {
			 pieceData= peerProcess.myCopy.getPart(pieceIndex);
		} catch (IOException e) {
			peerProcess.myCopy.segmentOwned[pieceIndex]=false;
			System.err.println("Tried to send a piece we don't have");
			e.printStackTrace();
			return;
		}
		ByteBuffer.wrap(sizeData).putInt(pieceData.length+typeData.length);

		try {
			oos.write(sizeData);
			oos.write(typeData);
			oos.write(pieceData);
			oos.flush();
		} catch (IOException e) {
			System.err.println("could not send piece "+pieceIndex);
			e.printStackTrace();
		}
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
		
		public boolean rcvHandshake() throws IOException {
			byte[] input = new byte[HANDSHAKE_LENGTH];
			byte[] payload = new byte[INT_LENGTH];
			boolean approved = false;
			//listen for handshake:
			ois.read(input, 0, HANDSHAKE_LENGTH);
			//test handshake
			Logger.debug(Logger.DEBUG_STANDARD, "Received handshake message: " + new String(input, 0, HELLO.length()));
			if(new String(input, 0, HELLO.length()).equals(HELLO)) {
				System.arraycopy(input, input.length-INT_LENGTH, payload, 0, INT_LENGTH);
				int receivedPeerID = ByteBuffer.wrap(payload).getInt();
				int expectedPeerID = Integer.valueOf(peerProcess.getRPI(PeerHandler.this).peerId);
				otherPeerID = expectedPeerID;
				if(receivedPeerID == expectedPeerID) {
					approved = true;
					if(!sentHandshake) {
						sendHandshake();
					}
					sendBitfield();
				}
			}
			return approved;
		}
		
		public void rcvChoke() {
			Logger.debug(Logger.DEBUG_STANDARD, "Peer " + peerProcess.peerID
                    + " is choked by " + 
                    peerProcess.getRPI(PeerHandler.this).peerId);
			Logger.chokedBy(otherPeerID);
			weAreChoked=true;
		}
		
		public void rcvUnchoke() {
			Logger.debug(Logger.DEBUG_STANDARD, "Peer " + peerProcess.peerID
                    + " is unchoked by " + 
                    peerProcess.getRPI(PeerHandler.this).peerId);
			Logger.unchokedBy(otherPeerID);
			weAreChoked=false;
			//Send back a request message
			sendRequest();
		}
		
		public void rcvInterested() {
			Logger.receivedInterested(otherPeerID);
			otherPeerIsInterested = true;
		}
		
		public void rcvNotInterested() {
			Logger.receivedNotInterested(otherPeerID);
			otherPeerIsInterested = false;
		}
		
		public void rcvHave(byte[] payload) {
			int pieceIndex = ByteBuffer.wrap(payload).getInt();
			Logger.receivedHave(otherPeerID, pieceIndex);
			remoteSegments[pieceIndex] = true;
			//TODO: do we need to request this new piece? (I don't think so... - Sachit)
			//We need to send an interested (or uninterested) message
			decideInterest();
		}
		
		public void rcvBitfield(byte[] payload) {
			//Note: this should not go before while loop because a bitfield message doesn't need to be sent
			//get bitfield, convert to segmentsOwned
			boolean[] segmentOwnedLarge = FileData.createSegmentsOwned(payload);
			//cut the segmentOwned to the appropriate size
			System.arraycopy(segmentOwnedLarge, 0, remoteSegments, 0, remoteSegments.length);
			String logString="recieved bitfield";
			for (boolean bool : remoteSegments)
				logString+=", "+Boolean.toString(bool);
			Logger.debug(Logger.DEBUG_STANDARD,logString);			
			//We now send an interested (or uninterested) message
			decideInterest();
		}
		
		public void rcvRequest(byte[] payload) {
			int pieceIndex = ByteBuffer.wrap(payload).getInt();
			if(!otherPeerIsChoked) {
				sendPiece(pieceIndex);
			}
			else {
				//TODO: add the requested piece to a variable to 
			}
		}
		
		public void rcvPiece(byte[] payload) throws IOException {
			//TODO
			//TODO: review correctness
			byte[] pieceID = new byte[INT_LENGTH];
			byte[] piece = new byte[payload.length - INT_LENGTH];
			System.arraycopy(payload, 0, pieceID, 0, INT_LENGTH);
			System.arraycopy(payload, 0, piece, INT_LENGTH, payload.length-INT_LENGTH);
			int pieceIndex = ByteBuffer.wrap(pieceID).getInt();
			Logger.downloadedPiece(otherPeerID, pieceIndex);
			peerProcess.myCopy.addPart(pieceIndex, piece);
			if(peerProcess.myCopy.isComplete()) {
				Logger.downloadComplete();
				peerProcess.myCopy.writeFinalFile();
			}
			//TODO: determine whether to send NOT_INTERESTED to other peers
			peerProcess.currentlyRequestedPieces.remove(new Integer(pieceIndex));
			//TODO: determine whether to REQUEST another piece from this peer
		}

		@Override
		public void run() {
			byte[] payload = new byte[INT_LENGTH];
			try {
				if(rcvHandshake()) {
					Logger.debug(Logger.DEBUG_STANDARD, "Handshake Approved");
				}
				else {
					Logger.debug(Logger.DEBUG_STANDARD, "Handshake NOT Approved");
					//TODO: what do we do if the expected peerID is not the received peerID?
					//exception? loop until expected is received? resend handshake?
				}
				//TODO: should we ignore a non-handshake first message? loop until a good one is found?
				//this should probably go inside the approval section
				
				payload = new byte[INT_LENGTH];
				int next=0;
				while((next = ois.read(payload, 0, payload.length)) >=0) {
					//messageLength[0-3], messageType[4]
					Logger.debug(Logger.DEBUG_STANDARD, "PeerHandler: port "+socket.getPort()+" received "+next);
					
					int len = ByteBuffer.wrap(payload).getInt() - TYPE_LENGTH;//1 byte is used for the type
					int type = ois.read();
					
					Message.MessageType mType = Message.MessageType.values()[type];
					if(mType == Message.MessageType.CHOKE) {
						rcvChoke();
					}
					else if(mType == Message.MessageType.UNCHOKE) {
						rcvUnchoke();
					}
					else if(mType == Message.MessageType.INTERESTED) {
						rcvInterested();
					}
					else if(mType == Message.MessageType.NOT_INTERESTED) {
						rcvNotInterested();
					}
					else {
						payload = new byte[len];
						dataRcvd += len; //Increment the data received count
						ois.read(payload);
						if(mType == Message.MessageType.HAVE) {
							rcvHave(payload);
						}
						else if(mType == Message.MessageType.BITFIELD) {
							rcvBitfield(payload);
						}
						else if(mType == Message.MessageType.REQUEST) {
							rcvRequest(payload);
						}
						else if(mType == Message.MessageType.PIECE) {
							rcvPiece(payload);
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
