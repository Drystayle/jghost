package ch.drystayle.jghost.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtil {

	//---- Static
	
	public static byte[] readFile (String fileName) throws FileNotFoundException, IOException {
		FileInputStream inputStream = new FileInputStream(fileName);
		int fileLength = inputStream.available();
		byte[] fileData = new byte[fileLength];
		inputStream.read(fileData);
		return fileData;
	}
	
}
