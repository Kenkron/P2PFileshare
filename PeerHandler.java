import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.ArrayList;
import java.util.Timer;

public class PeerHandler {
	private static final String HELLO = "HELLO";

	/** The number of bytes used to store an int */
	public static final int INT_LENGTH = 4;
	/** The number of bytes for the message type declaration */
	public static final int TYPE_LENGTH = 1;
	/** The number of bytes in the handshake message */
	public static final int HANDSHAKE_LENGTH = 32;
	/**
	 * The offset for the start of a message, accounting for the length int, and
	 * type length
	 */
	public static final int PAYLOAD_OFFSET = INT_LENGTH + TYPE_LENGTH;

	public Socket socket = null;
	private OutputStream oos = null;
	private InputHandler ih = null;

	public int otherPeerID;
	private boolean sentHandshake = false;
	public boolean otherPeerIsInterested = false;
	public Boolean otherPeerIsChoked = true;
	private boolean waitingForRequestFromOtherPeer = false;
	/** The amount of data received from this peer since the last choke cycle */
	private int dataRcvd = 0;
	
	private UnresponsiveUnchokeTask waitTimeoutTask;
	private Timer waitingForRequestTimer = new Timer(true);

	public boolean weAreChoked = true;

	private boolean[] remoteSegments;

	private byte[] getBitfield() {
		return FileData.createBitfield(remoteSegments);
	}

	private String getBitfieldString() {
		String retVal = "";
		for (byte b : getBitfield()) {
			//Convert to binary int, then get last byte (correllating to binary form of b)
			String temp = Integer.toBinaryString(b);//.substring(Integer.SIZE - Byte.SIZE) + " ";

			while(temp.length() <Integer.SIZE)
			    temp = "0" + temp;
			retVal += temp.substring(Integer.SIZE - Byte.SIZE);
		}
		return retVal;
	}

	public boolean isRemoteSegmentsComplete() {
		boolean missingPiece = false;
		for (int i = 0; i < remoteSegments.length; i++) {
			if (!remoteSegments[i]) {
				missingPiece = true;
			}
		}
		return !missingPiece;
	}

	private volatile int requestedPiece = -1;

	public PeerHandler(Socket s) {
		this.socket = s;
		remoteSegments = new boolean[peerProcess.myCopy.getSegmentOwnedLength()];
		try {
			oos = s.getOutputStream();
			ih = new InputHandler(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		waitTimeoutTask = new UnresponsiveUnchokeTask();
	}

	public void sendHandshake() {
		byte[] outputBytes = new byte[HANDSHAKE_LENGTH];
		byte[] peerIDBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(peerProcess.peerID).array();

		System.arraycopy(HELLO.getBytes(), 0, outputBytes, 0,
				HELLO.getBytes().length);
		// middle bytes already default to 0
		System.arraycopy(peerIDBytes, 0, outputBytes, HANDSHAKE_LENGTH - INT_LENGTH, INT_LENGTH);
		try {
			oos.write(outputBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendHandshake(). Socket closed.");
		}
		Logger.debug(Logger.DEBUG_ONCE, "Sent HANDSHAKE");
		sentHandshake = true;
	}

	public void sendChoke() {
		byte[] chokeBytes = new byte[PAYLOAD_OFFSET];
		chokeBytes[INT_LENGTH - 1] = (byte) TYPE_LENGTH;// set message length to 1
		chokeBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.CHOKE.ordinal();
		try {
			oos.write(chokeBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendChoke(). Socket closed.");
		}
		Logger.debug(Logger.DEBUG_STANDARD, "CHOKING " + otherPeerID);
		this.otherPeerIsChoked = true;
	}

	public void sendUnchoke() {
		boolean doUnchoke = false;
		synchronized (otherPeerIsChoked) {
			if (otherPeerIsChoked)
				doUnchoke = true;
		}
		if (doUnchoke) {
			waitingForRequestTimer.schedule(waitTimeoutTask, (long) peerProcess.UnchokingInterval/2);
			otherPeerIsChoked = false;
			byte[] unchokeBytes = new byte[PAYLOAD_OFFSET];
			unchokeBytes[INT_LENGTH - 1] = (byte) TYPE_LENGTH;// set message length to 1
			unchokeBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.UNCHOKE.ordinal();
			try {
				oos.write(unchokeBytes);
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("Can't sendUnchoke(). Socket closed.");
			}
			Logger.debug(Logger.DEBUG_STANDARD, "UNCHOKING " + otherPeerID);
		}
	}

	private boolean isInterested() {
		// A boolean which will be set if any one or more of the segments we
		// need are in their bitfield (remoteSegments)
		boolean interested = false;

		// Run through all of their segments and see if we don't have one
		for (int i = 0; i < remoteSegments.length; i++) {
			//Check that we dont have this segment and they do
			if (remoteSegments[i] && !peerProcess.myCopy.getSegment(i))
				interested = true;
		}
		return interested;
	}

	/**
	 * This method has the job of deciding whether or not to send an interested
	 * message after receiving a HAVE or BITFIELD
	 */
	public void decideInterest() {
		// A boolean which will be set if any one or more of the segments we
		// need are in their bitfield (remoteSegments)
		boolean interested = false;

		// Run through all of their segments and see if we don't have one
		for (int i = 0; i < remoteSegments.length; i++) {
			//Check that we dont have this segment and they do
			if (remoteSegments[i] && !peerProcess.myCopy.getSegment(i))
				interested = true;
		}

		// If the they dont have any pieces that we don't, send uninterested
		if (!interested)
			sendNotInterested();
		else
			sendInterested();
	}

	/**
	 * Send a INTERESTED message (code 2) 4byte message length, 1byte type
	 */
	public void sendInterested() {
		byte[] interestedBytes = new byte[PAYLOAD_OFFSET];
		interestedBytes[INT_LENGTH - 1] = (byte) TYPE_LENGTH;// set message length to 1
		interestedBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.INTERESTED.ordinal();
		try {
			oos.write(interestedBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendInterested(). Socket closed.");
		}

		Logger.debug(Logger.DEBUG_STANDARD, "Sent INTERESTED to " + otherPeerID);
	}

	/**
	 * Send a NOTINTERESTED message (code 3) 4byte message length, 1byte type
	 */
	public void sendNotInterested() {
		byte[] notInterestedBytes = new byte[PAYLOAD_OFFSET];
		notInterestedBytes[INT_LENGTH - 1] = (byte) TYPE_LENGTH;// set message
																// length to 1
		notInterestedBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.NOT_INTERESTED
				.ordinal();
		try {
			oos.write(notInterestedBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendNotInterested(). Socket closed.");
		}
		Logger.debug(Logger.DEBUG_STANDARD, "Sent NOT_INTERESTED to "
				+ otherPeerID);
	}

	/**
	 * Send a HAVE message 4byte message length, 1byte HAVE ordinal, 4byte
	 * payload (pieceIndex)
	 */
	public void sendHave(int pieceIndex) {
		byte[] payloadBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(pieceIndex).array();
		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(payloadBytes.length + TYPE_LENGTH).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH
				+ payloadBytes.length];// 4 + 1 + 4

		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
		outputBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.HAVE
				.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, PAYLOAD_OFFSET,
				payloadBytes.length);
		try {
			oos.write(outputBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendHave(). Socket closed.");
		}
		Logger.debug(Logger.DEBUG_STANDARD, "Sent HAVE " + pieceIndex + " to "
				+ otherPeerID);
	}

	/**
	 * Send a BITFIELD message 4byte message length, 1byte BITFIELD ordinal,
	 * variable sized payload (myBitfield)
	 */
	public void sendBitfield() {
		boolean hasPartialFile = false;
		byte[] myBitfield = peerProcess.myCopy.getBitfield();
		for (byte b : myBitfield) {
			if (b != 0) {
				hasPartialFile = true;
				break;
			}
		}
		if (hasPartialFile) {
			byte[] payloadBytes = myBitfield;
			byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH)
					.putInt(payloadBytes.length + TYPE_LENGTH).array();
			byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH
					+ payloadBytes.length];// 4 + 1 + variable

			System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
			outputBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.BITFIELD
					.ordinal();
			System.arraycopy(myBitfield, 0, outputBytes, PAYLOAD_OFFSET,
					myBitfield.length);
			try {
				oos.write(outputBytes);
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("Can't sendBitfield(). Socket closed.");
			}

			Logger.debug(Logger.DEBUG_STANDARD, "Sent BITFIELD to "
					+ otherPeerID);
		} else {
			Logger.debug(Logger.DEBUG_STANDARD,
					"Unnecessary to send BITFIELD to " + otherPeerID);
		}
	}

	/**
	 * Send a REQUEST message (code 6) 4byte message length, 1byte type, 4 byte
	 * payload (pieceIndex)
	 */
	public void sendRequest() {
		//protocol robustness: a secondary preventative situation for not receiving a piece that we've requested
		if(requestedPiece > -1) {
			synchronized(peerProcess.currentlyRequestedPieces) {
				peerProcess.currentlyRequestedPieces.remove(new Integer(requestedPiece));
			}
		}
		
		// An oddly named array which contains indices of segments we don't
		// have and they do && it hasn't been requested yet
		ArrayList<Integer> weDontTheyDo = new ArrayList<Integer>();
		Random randomizer = new Random(System.currentTimeMillis());

		// synchronize to prevent data race
		int choice;
		synchronized (peerProcess.peerHandlerList) {

			synchronized(peerProcess.currentlyRequestedPieces) {
				//First we select a random piece we don't have and they do have
				for (int i = 0; i < remoteSegments.length; i++) {
					//Check that we dont have this segment, they do, and it hasn't been requested yet
					if (remoteSegments[i] && 
							!peerProcess.myCopy.getSegment(i) &&
							!peerProcess.currentlyRequestedPieces.contains(i)) {
						weDontTheyDo.add(i);
					}
				}

				// If the list is empty (no segments they have that we don't),
				// just stop
				if (weDontTheyDo.size() == 0){
					Logger.debug(Logger.DEBUG_LOGFILE, "No Request Sent: they have no files we want.");
					return;					
				}

				// Choose randomly from the segments we own and they do
				int randomIndex = randomizer.nextInt(weDontTheyDo.size());
				choice = weDontTheyDo.get(randomIndex);
				peerProcess.currentlyRequestedPieces.add(new Integer(choice));
				requestedPiece = choice;
			}
		}

		// Now send the actual message
		byte[] payloadBytes = ByteBuffer.allocate(INT_LENGTH).putInt(choice)
				.array();
		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(payloadBytes.length + TYPE_LENGTH).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH
				+ payloadBytes.length];// 4 + 1 + 4

		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);
		outputBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.REQUEST.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, PAYLOAD_OFFSET,
				payloadBytes.length);
		try {
			oos.write(outputBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendRequest(). Socket closed.");
		}

		Logger.debug(Logger.DEBUG_STANDARD, "Sent REQUEST for " + choice
				+ " to " + otherPeerID);
	}

	public void sendPiece(int pieceIndex) {
		byte[] pieceBytes = null;
		try {
			pieceBytes = peerProcess.myCopy.getPart(pieceIndex);
		} catch (IOException e) {
			peerProcess.myCopy.setSegmentOwned(pieceIndex, false);
			System.err.println("Tried to send a piece we don't have");
			e.printStackTrace();
			return;
		}
		byte[] pieceIndexBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(pieceIndex).array();
		byte[] payloadBytes = new byte[INT_LENGTH + pieceBytes.length];
		System.arraycopy(pieceIndexBytes, 0, payloadBytes, 0,
				pieceIndexBytes.length);
		System.arraycopy(pieceBytes, 0, payloadBytes, pieceIndexBytes.length,
				pieceBytes.length);

		byte[] msgLengthBytes = ByteBuffer.allocate(INT_LENGTH)
				.putInt(payloadBytes.length + TYPE_LENGTH).array();
		byte[] outputBytes = new byte[msgLengthBytes.length + TYPE_LENGTH
				+ payloadBytes.length];// 4 + 1 + (4+variable)

		System.arraycopy(msgLengthBytes, 0, outputBytes, 0, INT_LENGTH);

		outputBytes[PAYLOAD_OFFSET - TYPE_LENGTH] = (byte) Message.MessageType.PIECE
				.ordinal();
		System.arraycopy(payloadBytes, 0, outputBytes, PAYLOAD_OFFSET,
				payloadBytes.length);

		try {
			oos.write(outputBytes);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Can't sendPiece(). Socket closed.");
		}

		Logger.debug(Logger.DEBUG_STANDARD, "Sent PIECE " + pieceIndex + " to "
				+ otherPeerID);
	}

	/** Start the InputHandler */
	public void start() {
		ih.start();
	}

	class InputHandler extends Thread {
		private InputStream ois = null;

		public InputHandler(Socket s) {
			setDaemon(true);
			try {
				ois = s.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public boolean rcvHandshake() throws IOException {
			byte[] input = new byte[HANDSHAKE_LENGTH];
			byte[] payload = new byte[INT_LENGTH];
			boolean approved = false;
			// listen for handshake:
			int lengthRcvd = ois.read(input, 0, HANDSHAKE_LENGTH);
			//if(lengthRcvd < HANDSHAKE_LENGTH) throw new RuntimeException("Received less than we should of");
			while(lengthRcvd < HANDSHAKE_LENGTH) {
				lengthRcvd += ois.read(input, lengthRcvd, HANDSHAKE_LENGTH-lengthRcvd);
			}
			// test handshake
			Logger.debug(Logger.DEBUG_STANDARD, "Received HANDSHAKE: "
					+ new String(input, 0, HELLO.length()));
			if (new String(input, 0, HELLO.length()).equals(HELLO)) {
				System.arraycopy(input, input.length - INT_LENGTH, payload, 0,
						INT_LENGTH);
				int receivedPeerID = ByteBuffer.wrap(payload).getInt();
				
				otherPeerID = Integer.valueOf(peerProcess.getRPI(PeerHandler.this).peerId);
				System.out.println("Expected: " + otherPeerID + "\tReceived: " + receivedPeerID);
				if (receivedPeerID == otherPeerID) {
					approved = true;
					if (!sentHandshake) {
						sendHandshake();
					}
					sendBitfield();
				}
			}
			return approved;
		}

		public void rcvChoke() {
			Logger.chokedBy(otherPeerID);
			weAreChoked = true;
			synchronized(peerProcess.currentlyRequestedPieces) {
				//If we're choked, we should clear outstanding requests
				peerProcess.currentlyRequestedPieces.remove(new Integer(requestedPiece));
				requestedPiece = -1;
			}
		}

		public void rcvUnchoke() {
			Logger.unchokedBy(otherPeerID);
			weAreChoked = false;
			// Send back a request message
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

			// We need to send an interested (or uninterested) message
			decideInterest();
		}

		public void rcvBitfield(byte[] payload) {
			boolean[] segmentOwnedLarge = FileData.createSegmentsOwned(payload);
			// cut received boolean array to size of remoteSegments
			System.arraycopy(segmentOwnedLarge, 0, remoteSegments, 0,
					remoteSegments.length);

			Logger.debug(Logger.DEBUG_STANDARD, "Received BITFIELD: "
					+ getBitfieldString() + "\n");
			// We now send an interested (or uninterested) message
			decideInterest();
		}

		public void rcvRequest(byte[] payload) {
			int pieceIndex = ByteBuffer.wrap(payload).getInt();
			Logger.debug(Logger.DEBUG_STANDARD, "Received REQUEST: "
					+ pieceIndex);
			if (!otherPeerIsChoked) {
				sendPiece(pieceIndex);
			}
		}

		public void rcvPiece(byte[] payload) throws IOException {
			waitingForRequestTimer.cancel();
			waitingForRequestTimer = new Timer(true);
			byte[] pieceID = new byte[INT_LENGTH];
			byte[] piece = new byte[payload.length - pieceID.length];
			System.arraycopy(payload, 0, pieceID, 0, pieceID.length);
			System.arraycopy(payload, pieceID.length, piece, 0, piece.length);
			int pieceIndex = ByteBuffer.wrap(pieceID).getInt();
			Logger.debug(Logger.DEBUG_STANDARD, "Received PIECE " + pieceIndex);

			peerProcess.myCopy.addPart(pieceIndex, piece);
			Logger.downloadedPiece(otherPeerID, pieceIndex);
			if (peerProcess.myCopy.isComplete()) {
				Logger.downloadComplete();
				peerProcess.myCopy.writeFinalFile();
			}

			synchronized (peerProcess.currentlyRequestedPieces) {
				peerProcess.currentlyRequestedPieces.remove(new Integer(
						pieceIndex));
			}

			synchronized (peerProcess.peerHandlerList) {
				// send HAVE to other peers, set other PeerHandler's
				// INTERESTED/NOT_INTERESTED
				for (PeerHandler ph : peerProcess.peerHandlerList) {
					ph.sendHave(pieceIndex);
					if(!ph.isInterested()) {
						//ph.decideInterest();
						ph.sendNotInterested();
					}
				}
			}

			sendRequest();// this handles whether or not to send a request
		}

		@Override
		public void run() {
			byte[] payload = new byte[INT_LENGTH];
			byte[] rcvMessageLengthField = new byte[INT_LENGTH];
			try {
				if (rcvHandshake()) {
					Logger.debug(Logger.DEBUG_STANDARD, "Handshake Approved");
				} else {
					Logger.debug(Logger.DEBUG_STANDARD,
							"Handshake NOT Approved");
				    //System.exit(1);
					throw new RuntimeException("Not an handshake not approved.");
				}

				int next = 0;
				while ((next = ois.read(rcvMessageLengthField, 0,
						rcvMessageLengthField.length)) >= 0
						&& (!isRemoteSegmentsComplete() || !peerProcess.myCopy
								.isComplete())) {
					// messageLength[0-3], messageType[4]
					int len = ByteBuffer.wrap(rcvMessageLengthField).getInt()
							- TYPE_LENGTH;// 1 byte is used for the type
					int type = ois.read();

					Message.MessageType mType = Message.MessageType.values()[type];
					if (mType == Message.MessageType.CHOKE) {
						rcvChoke();
					} else if (mType == Message.MessageType.UNCHOKE) {
						rcvUnchoke();
					} else if (mType == Message.MessageType.INTERESTED) {
						rcvInterested();
					} else if (mType == Message.MessageType.NOT_INTERESTED) {
						rcvNotInterested();
					} else {
						payload = new byte[len];
						dataRcvd += len; // Increment the data received count
						ois.read(payload);
						if (mType == Message.MessageType.HAVE) {
							rcvHave(payload);
						} else if (mType == Message.MessageType.BITFIELD) {
							rcvBitfield(payload);
						} else if (mType == Message.MessageType.REQUEST) {
							rcvRequest(payload);
						} else if (mType == Message.MessageType.PIECE) {
							rcvPiece(payload);
						}
					}
				}
			} catch (IOException e) {
				/*ignored*/
			} finally {
				System.out.println("Connection to peer " + otherPeerID + " closed");
				PeerHandler.this.close();
			}
		}

		private void close() {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException e) {/* ignored */
			}

			synchronized (peerProcess.currentlyRequestedPieces) {
				peerProcess.currentlyRequestedPieces.remove(new Integer(
						requestedPiece));
			}
		}
	}

	/** Clears the data received count */
	public synchronized void clearDataCounter() {
		dataRcvd = 0;
	}

	/** Fetches the data received count */
	public synchronized int getDataRcvd() {
		return dataRcvd;
	}

	public void close() {
		try {
			if (oos != null) {
				oos.close();
			}
			if (socket != null) {
				socket.close();
			}
			if (ih != null) {
				ih.close();
			}
			
			peerProcess.getRPI(this).setHasFile(true);
			peerProcess.removePeerHandlerFromList(this);
			peerProcess.server.checkCompletion(peerProcess.serverThread);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
