import java.util.Random;
import java.util.TimerTask;
import java.util.ArrayList;

public class OptimisticUnchokeTask extends TimerTask {

	private static Random randomizer=new Random((long)(Math.random()*Integer.MAX_VALUE));
    
     /** This method is called by peerProcess according to a given
     * interval. This method will find 1 optimistic neighbor randomly
     * from a list of all unchoked, but interested neighbors */
    public void run() {
    	
        //The peerList below is initialized to all peers
        ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
        
        //TODO: this was a quick fix.  
        //... It should be peer reviewed to ensure that it is good code.
        for (PeerHandler handle: peerProcess.peerHandlerList){
        	peerList.add(peerProcess.getRPI(handle));
        }
        
        if (peerList.isEmpty()){
        	return;
        }
        
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
        
        if (possibleList.size() < 1)
            return;
            
        int randomIndex = randomizer.nextInt(possibleList.size());
        RemotePeerInfo choice = possibleList.get(randomIndex);
        
        //TODO: unchoke choice
        peerProcess.rpiToPeerHandler.get(choice).sendUnchoke();
    }
}
