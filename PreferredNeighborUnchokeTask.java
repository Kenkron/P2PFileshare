import java.util.TimerTask;

public class PreferredNeighborUnchokeTask extends TimerTask {

    public PreferredNeighborUnchokeTask() {
    }

    //@ TODO:
    //most likely, the following method will need to create a sorted list
    //of RPI based on getDataRcvd, and take the tow highest of that list.
    
	/** This method is called by peerProcess according to a given 
	 * interval. This method will find the k preferred neighbors. */
    public void run() {
		// Find the k fastest, interested neighbors
		int[] dataRates = new int[peerProcess.NumberOfPreferredNeighbors];
		RemotePeerInfo[] preferredNeighbors = new RemotePeerInfo[peerProcess.NumberOfPreferredNeighbors];
        for (RemotePeerInfo rpi : peerProcess.peerList) {
			//Get the corresponding peerHandler for this RPI
			PeerHandler peer = peerProcess.rpiToPeerHandler.get(rpi); 
			// @ TODO: does not account for null piers
			if (peer.otherPeerIsInterested) {
				//Check if this peer has better (or equal?) rates than 
				//current preferred neighbors
				for (int i = 0; i < peerProcess.NumberOfPreferredNeighbors; i++) {
					if (peer.getDataRcvd() >= dataRates[i]) {
						dataRates[i] = peer.getDataRcvd();
						// @ TODO: the following assignment is incorrect
						// the highest peer will be put in all the slots
						// and will tend to override the fastest first
						preferredNeighbors[i] = rpi;
					}
				}
			}
			peer.clearDataCounter();
		}
		
		//TODO: Send choke/unchoke messages (make sure to log these? or does the peerHandler do that?)
    }
}
