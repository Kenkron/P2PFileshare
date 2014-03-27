import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Collections;

public class OptimisticUnchokeTask extends TimerTask {

    public OptimisticUnchokeTask() {
    }
    
     /** This method is called by peerProcess according to a given
     * interval. This method will find 1 optimistic neighbor randomly
     * from a list of all unchoked, but interested neighbors */
    public void run() {
        //The peerList below is initialized to all peers
        ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>(peerProcess.peerList);
        //The empty preferred list below will contain the possible neighbors
        //to choose from later
        ArrayList<RemotePeerInfo> possibleList = new ArrayList<RemotePeerInfo>();
        
        //first check if we only have one peer
        if (peerList.size() <= 1) {
            //if so, just make everyone a preferred neighbor
        	possibleList = new ArrayList<RemotePeerInfo>(peerList);
            //now clear the list of "un-preferred" neighbors
            peerList.clear();
        }
        
        //otherwise, choose randomly for one
        else {
              //go through each peer to see how if it can be added to possible
              for (RemotePeerInfo rpi : peerProcess.peerList) {
                  PeerHandler peer = peerProcess.rpiToPeerHandler.get(rpi);
                  if (peer != null && peer.otherPeerIsInterested
                      && peer.otherPeerIsChoked) {
                      possibleList.add(rpi);
                  }
              }
        }
        
        int randomIndex = (int) Math.random() * ( possibleList.size() - 0 );
        RemotePeerInfo choice = possibleList.get(randomIndex);
        
        //TODO: unchoke choice
        //TODO: rpiToPeerHandler currently can return a null, because the keys are prepopulated
        //what we really care about are the peers that are currently connected.
        //ask Kyle about this when needed, to see if it gets changed.
        peerProcess.rpiToPeerHandler.get(choice).sendUnchoke();
    }
}
