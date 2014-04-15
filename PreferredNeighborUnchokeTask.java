import java.util.TimerTask;
import java.util.ArrayList;

public class PreferredNeighborUnchokeTask extends TimerTask {

    public PreferredNeighborUnchokeTask() {
    }
    
    /** Version 2: This method is called by peerProcess according to a given
     * interval. This method will find the k preferred neighbors. For each of 
     * k preferred neighbors, it will run through the list of peers and select
     * the highest data rate peer, and remove it from the list. This method
     * assumes that k < total number of peers. */
     public void run() {
        //The peerList below is initialized to all peers, but after this method
        //runs, it will contain "un-preferred" neighbors
        ArrayList<PeerHandler> peerList = new ArrayList<PeerHandler>(peerProcess.peerHandlerList);
        
//        for (PeerHandler handle: peerProcess.peerHandlerList){
//        	//peerList.add(peerProcess.getRPI(handle));
//        	peerls
//        }
        
        if (peerList.isEmpty()){
        	return;
        }
        
        //The empty preferred list below will contain the preferred neighbors 
        //after this method is executed. 
        ArrayList<PeerHandler> preferredList = new ArrayList<PeerHandler>();
        
        for (int i = 0; i < peerProcess.NumberOfPreferredNeighbors; i++) {
            PeerHandler best = null;
            int bestRate = 0;
            //go through each peer to see how it compares to current best
            for (PeerHandler peer : peerProcess.peerHandlerList) {
                if (peer != null && peer.otherPeerIsInterested) {
                    //if it is better...
                    if (peer.getDataRcvd() >= bestRate) {
                        best = peer;
                        bestRate = peer.getDataRcvd();
                    }
                }
            }
            //Now place the best neighbor in the preferred list and remove 
            //it from list of peers which are being considered
            if(best != null) {
            	preferredList.add(best);
            	peerList.remove(best);
            }
        }
        
        //now clear data counters for all neighbors
        for (int x = 0; x < peerProcess.peerHandlerList.size(); x++)
            peerProcess.peerHandlerList.get(x).clearDataCounter();
        
        //unchoke preferred neighbors
        for (int i = 0; i < preferredList.size(); i++)
            peerProcess.rpiToPeerHandler.get(preferredList.get(i)).sendUnchoke();
        
        //choke un-preferred neighbors, except optimistically-unchoked neighbor
        for (int y = 0; y < peerList.size(); y++) {
        	PeerHandler ph = peerProcess.rpiToPeerHandler.get(peerList.get(y));
        	if(ph != peerProcess.currentOptimisticallyUnchokedNeighbor) {
        		ph.sendChoke();
        	}
        }
            
        //Log this
        if(preferredList.size() > 0) {
        	String preferredListString = peerProcess.getRPI(preferredList.get(0)).peerId;
        	for (int z = 1; z < preferredList.size(); z++)
        		preferredListString = preferredListString + ", " + 
        				peerProcess.getRPI(preferredList.get(z)).peerId;
        	Logger.debug(Logger.DEBUG_STANDARD, "Peer " + peerProcess.peerID + " has the preferred " +
        			"neighbors " + preferredListString);
        }
     }
}
