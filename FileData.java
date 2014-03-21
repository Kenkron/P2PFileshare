import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**and extension of {@link HashMap} that will hold segments of a byte array
 * for concatenation later.*/
public class FileData{

	/**the temporary directory for storing the file parts*/
	public final String TEMP_DIR;
	
	/**the file extension for temporary file parts*/
	public static final String TEMP_EXTENSION=".part";
	
	/**name of the complete file*/
	public final String FILE_NAME;
	
	/**a cached array of booleans indicating whether each segment is owned*/
	boolean[] segmentOwned;
	/**get the number of segments owned*/
	int getSegmentsOwnedCount() {
		int count = 0;
		for(boolean x : segmentOwned) {
			if(x) count++;
		}
		return count;
	}
	/**The client's bitfield, should be updated along with segmentOwner*/
	//TODO: create a static utility methods that converts boolean[] to byte[] and vice-versa
	//Remove the updateBitfield() stuff. Reimplement in PeerHandler 
	volatile byte[] bitfield;
	
	/**creates a fileData that will ultimately have the given number of segments.
	 * Used when the file is not on this machine*/
	public FileData(String tempDir, int numberOfSegments, String filename){
		TEMP_DIR=tempDir;
		FILE_NAME=filename;
		segmentOwned=new boolean[numberOfSegments];
		int bitfieldSize = (int)(Math.ceil(((float)numberOfSegments)/8.0));
		bitfield = new byte[bitfieldSize];
		findExistingPartialFiles();//TODO: do we really need this?
	}
	
	/**creates a fileData based on the given filename.
	 * Used when the file is already present
	 * @throws IOException */
	public FileData(String tempDir, String filename, int segmentSize) throws IOException{
		FILE_NAME=filename;
		TEMP_DIR=tempDir;
		breakFile(segmentSize);
	}
	
	/**breaks the file given by FILE_NAME into segments in the DEMP_DIR directory
	 * allowing this program to get individual parts.
	 * @throws IOException */
	private void breakFile(int segmentSize) throws IOException{
		File inputFile = new File(FILE_NAME);
		FileInputStream input=new FileInputStream(inputFile);
		int numOfParts=(int) Math.ceil(inputFile.length()/(float)segmentSize);
		segmentOwned=new boolean[numOfParts];
		int bitfieldSize = (int)(Math.ceil(((float)numOfParts)/8.0));
		bitfield = new byte[bitfieldSize];
		int partCounter=0;
		while (input.available()>0){
			byte[] nextData=new byte[Math.min(segmentSize,input.available())];
			input.read(nextData);
			addPart(partCounter, nextData);
			partCounter++;
		}
		for (int i = 0; i< segmentOwned.length; i++){
			segmentOwned[i]=true;
		}
		updateBitfield();
	}
	
	/**Set segmentOwned and automatically calculates the new bitfield*/
	private void updateSegment(int i, boolean val) {
		segmentOwned[i] = val;
		int bitfieldIndex = i/8;
		boolean[] tempBool = new boolean[8];
		System.arraycopy(segmentOwned, (i/8)*8, tempBool, 0, Math.min(segmentOwned.length-(i/8)*8, 8));
		bitfield[bitfieldIndex] = boolToByte(tempBool);
	}
	/**Updates bitfield to match segmentOwned. Call after finished updating segments*/
	private void updateBitfield() {
		int index = 0;
		for(int i = 0;i<segmentOwned.length;i+=8) {
			boolean[] tempBool = new boolean[8];
			System.arraycopy(segmentOwned, i, tempBool, 0, Math.min(segmentOwned.length-(i/8)*8, 8));
			bitfield[index] = boolToByte(tempBool);
			index++;
		}
	}
	/**Converts a boolean array (expected length [0,8]) to a byte)*/
	private byte boolToByte(boolean[] arr) {
		byte val = 0;
		for(boolean x : arr) {
			val = (byte) (val << 1);
			val = (byte) (val | (x ? 1:0));
		}
		return val;
	}
	
	/**Updates the segmentOwned and bitfields according to any pre-existing partial files*/
	public void findExistingPartialFiles() {
		for(int part = 0;part<segmentOwned.length;part++) {
			File partialFile = new File(TEMP_DIR+FILE_NAME+ part +TEMP_EXTENSION);
			if(partialFile.exists()) {
				segmentOwned[part] = true;
			}
			updateBitfield();
		}
	}
	
	/**adds a file part to this FileData, 
	 * saving it to the disk immediately as a file under the 
	 * temporary directory.*/
	public void addPart(int part,byte[] data) throws IOException{
		File outputDirectory = new File(TEMP_DIR);
		if (!outputDirectory.exists())
			outputDirectory.mkdir();
		File outputFile = new File(TEMP_DIR+FILE_NAME+part+TEMP_EXTENSION);
		FileOutputStream output = new FileOutputStream(outputFile);
		output.write(data);
		output.close();
		//segmentOwned[part]=true;
		updateSegment(part, true);
	}
	
	/**gets a part of a file*/
	public byte[] getPart(int part) throws IOException{
		File inputFile = new File(TEMP_DIR+FILE_NAME+part+TEMP_EXTENSION);
		FileInputStream input = new FileInputStream(inputFile);
		byte[] data=new byte[(int) inputFile.length()];
		input.read(data);
		input.close();
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
		writeFinalFile(FILE_NAME);
	}
	
	/**returns the final file.  Not to be called until isComplete returns true
	 * @param the destination file path
	 * @throws IOException */
	public void writeFinalFile(String filename) throws IOException{
		File outputFile = new File(filename);
		FileOutputStream output = new FileOutputStream(outputFile);
		for (int i=0;i<segmentOwned.length;i++){
			byte[] nextSegment=getPart(i);
			output.write(nextSegment);
		}
		output.close();
	}
	
	/**remove all temporary files*/
	public void clearCache(){
		File tempDir=new File(TEMP_DIR);
	    File[] files = tempDir.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            f.delete();
	        }
	    }
		tempDir.delete();
	}
}
