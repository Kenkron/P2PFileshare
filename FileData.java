import java.util.HashMap;

/**and extension of {@link HashMap} that will hold segments of a byte array
 * for concatenation later.*/
public class FileData extends HashMap<Integer, byte[]>{

	/**the number of segments this FileData will have once complete*/
	int numberOfSegments;

	/**added generated UID*/
	private static final long serialVersionUID = -6228235788589837032L;

	/**creates a fileData that will ultimately have the given number of segments*/
	public FileData(int numberOfSegments){
		this.numberOfSegments=numberOfSegments;
	}
	
	/**returns true iff all of the file segments have been added.*/
	public boolean isComplete(){
		boolean missingPiece=false;
		for (int i=0;i<numberOfSegments;i++){
			if (this.containsKey(i)){
				missingPiece=true;
			}
		}
		return !missingPiece;
	}
	
	/**returns the final file.  Not to be called until isComplete returns true*/
	public byte[] getFinalFile(){
		int totalSize=0;
		for (int i=0;i<numberOfSegments;i++){
			totalSize+=this.get(i).length;
		}
		byte[] result=new byte[totalSize];
		
		int cursor=0;
		for (int i=0;i<numberOfSegments;i++){
			int segmentLength=get(i).length;
			System.arraycopy(get(i), 0, result, cursor, segmentLength);
			cursor+=segmentLength;
		}
		
		return result;
	}
}
