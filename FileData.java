import java.util.HashMap;


public class FileData extends HashMap<Integer, byte[]>{

	int numberOfSegments;

	/**added generated UID*/
	private static final long serialVersionUID = -6228235788589837032L;

	public FileData(int numberOfSegments){
		this.numberOfSegments=numberOfSegments;
	}
	
	public boolean isComplete(){
		boolean missingPiece=false;
		for (int i=0;i<numberOfSegments;i++){
			if (this.containsKey(i)){
				missingPiece=true;
			}
		}
		return !missingPiece;
	}
	
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
