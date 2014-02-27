import java.util.TimerTask;

public class OptimisticUnchokeTask extends TimerTask {

    public OptimisticUnchokeTask() {
    }
    
    public void run() {
        System.out.println("Checking for optimistically unchoked...");
    }
}
