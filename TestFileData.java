import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class TestFileData {

	public static int segmentSize=100;
	
	public static void main(String[] args) throws IOException {
		if (args.length<1){
			System.out.println("requires at least one file name as an argument");
		}
		for (String filename: args){
			File completeFile=new File(filename);
			if (completeFile.canRead()){
				System.out.println("File: "+filename);
				System.out.println("\t separating file into sections");
				FileData splitter = new FileData(filename,segmentSize);
				System.out.println("\t recombining sections");
				splitter.writeFinalFile(filename+".out");
				System.out.println("\t checking original and final file for equality");
				File outputFile=new File(filename+".out");
				
				FileInputStream original = new FileInputStream(completeFile);
				FileInputStream output = new FileInputStream(outputFile);
				
				if (original.available()!=output.available()){
					System.out.flush();
					System.err.println("\t BUG! output files are not the same size for file "+filename);
					System.err.flush();
				}else{
					byte[] originalData=new byte[original.available()];
					byte[] outputData=new byte[output.available()];
					original.read(originalData);
					output.read(outputData);
					boolean equal=true;
					for (int i = 0; i< originalData.length;i++){
						if (originalData[i]!=outputData[i]){
							if (equal){
								System.out.flush();
								System.err.println("\t BUG! output files differ, starting at byte: "+i);
								System.err.flush();
								equal=false;
								break;
							}
						}
					}
					if (equal){
						System.out.println("\t "+filename+" reconstructed sucessfully");
					}
				}
				original.close();
				output.close();
			}else{
				System.out.println("Cannot read file: "+filename);
			}
		}

		System.out.println("");
		System.out.println("Press enter to clear temporary files and exit.");
		System.out.println("Press any other key (then enter) to exit without clearing temporary files.");
		System.out.flush();
		int in = System.in.read();
		if (in == 10){
			System.out.println("pressed "+in+", clearing cache and exiting.");
			FileData splitter = new FileData(args[0],segmentSize);
			splitter.clearCache();
		}else{
			System.out.println("pressed "+in+", exiting without clearing cache.");
		}
	}
}
