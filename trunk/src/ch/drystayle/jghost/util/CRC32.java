package ch.drystayle.jghost.util;

public class CRC32 {

	//---- Static
	
	//---- State
	
	//---- Constructors
	
	//---- Methods
	
	public int FullCRC (byte[] sData) {
		return JniUtil.crc32_full(sData);
	}
}
