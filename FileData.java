import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**and extension of {@link HashMap} that will hold segments of a byte array
 * for concatenation later.*/
public class FileData{

	/**the temporary directory for storing the file parts*/
	public static final String TEMP_DIR="./part/";
	
	/**the file extension for temporary file parts*/
	public static final String TEMP_EXTENSION=".part";
	
	/**name of the complete file*/
	public final String FILE_NAME;
	
	/**a cached array of booleans indicating whether each segment is owned*/
	boolean[] segmentOwned;

	/**creates a fileData that will ultimately have the given number of segments.
	 * Used when the file is not on this machine*/
	public FileData(int numberOfSegments, String filename){
		segmentOwned=new boolean[numberOfSegments];
		FILE_NAME=filename;
	}
	
	/**creates a fileData based on the given filename.
	 * Used when the file is already present
	 * @throws IOException */
	public FileData(String filename, int segmentSize) throws IOException{
		FILE_NAME=filename;
		breakFile(segmentSize);
	}
	
	/**breaks the file given by FILE_NAME into segments in the DEMP_DIR directory
	 * allowing this program to get individual parts.
	 * @throws IOException */
	private void breakFile(int segmentSize) throws IOException{
		File inputFile = new File(FILE_NAME);
		FileInputStream input=new FileInputStream(inputFile);
		int partCounter=0;
		while (input.available()>0){
			byte[] nextData=new byte[Math.min(segmentSize,input.available())];
			input.read(nextData);
			addPart(partCounter, nextData);
			partCounter++;
		}
		segmentOwned=new boolean[partCounter];
		for (int i = 0; i< segmentOwned.length; i++){
			segmentOwned[i]=true;
		}
	}
	
	/**adds a file part to this FileData, 
	 * saving it to the disk immediately as a file under the 
	 * temporary directory.*/
	public void addPart(int part,byte[] data) throws IOException{
		File outputFile = new File(TEMP_DIR+FILE_NAME+part+TEMP_EXTENSION);
		FileOutputStream output = new FileOutputStream(outputFile);
		output.write(data);
		output.close();
		segmentOwned[part]=true;
	}
	
	/**gets a part of a file*/
	public byte[] getPart(int part) throws IOException{
		File inputFile = new File(TEMP_DIR+FILE_NAME+part+TEMP_EXTENSION);
		FileInputStream input = new FileInputStream(inputFile);
		byte[] data=new byte[(int) inputFile.length()];
		input.read(data);
		return data;
	}
	
	/**returns whether the given part has been received.
	 * (returns cached value, does not re-check)*/
	public boolean hasPart(int part){
		return segmentOwned[part];
	}
	
	/**returns true iff all of the file segments have been added.*/
	public boolean isComplete(){
		boolean missingPiece=false;
		for (int i=0;i<segmentOwned.length;i++){
			if (!segmentOwned[i]){
				missingPiece=true;
			}
		}
		return !missingPiece;
	}
	
	/**returns the final file.  Not to be called until isComplete returns true
	 * @throws IOException */
	public void writeFinalFile() throws IOException{
		File outputFile = new File(FILE_NAME);
		FileOutputStream output = new FileOutputStream(outputFile);
		for (int i=0;i<segmentOwned.length;i++){
			byte[] nextSegment=getPart(i);
			output.write(nextSegment);
		}
	}
}
