package ch.drystayle.jghost.util;

import java.io.FileNotFoundException;
import java.io.IOException;

public class JniUtil {
	
	//---- Static
	
	public static final int BNCSUTIL_PLATFORM_X86 = 1;
	public static final int BNCSUTIL_PLATFORM_WINDOWS = 1;
	public static final int BNCSUTIL_PLATFORM_WIN = 1;
	public static final int BNCSUTIL_PLATFORM_MAC = 2;
	public static final int BNCSUTIL_PLATFORM_PPC = 2;
	public static final int BNCSUTIL_PLATFORM_OSX = 3;
	
	//---- Native Function Calls
	
	public static native long nls_init (String username, String password);
	
	public static native long nls_reset (long nlsPointer, String username, String password);
	
	public static native void nls_delete (long nlsPointer);
	
	public static native char[] nls_getPublicKey (long nlsPointer);
	
	public static native char[] nls_getClientSessionKey (long nlsPointer, String salt, String serverKey);
	
	public static native String[] getExeInfo (String fileWar3exe, int platform);
	
	public static native int checkRevisionFlat(String valueStringFormula, String war3exeFileName, String stormDllFileName, String gameDllFileName, String mpqFileName);
	
	public static native long decoder_init (String key);
	
	public static native boolean decoder_isKeyValid (long decoderPointer);
	
	public static native int decoder_getProduct (long decoderPointer);
	
	public static native int decoder_getVal1 (long decoderPointer);

	public static native char[] decoder_getHash (long decoderPointer, int clientToken, int serverToken);
	
	public static native void decoder_delete (long decoderPointer);
	
	public static native long archive_open (String archivePath) throws IOException;
	
	public static native byte[] archive_readFile (long archivePointer, String filePath) throws FileNotFoundException ,IOException;
	
	public static native void archive_close (long archivePointer);
	
	public static native int crc32_full (byte[] data);
	
	public static native long sha1_init ();

	public static native void sha1_reset (long sha1Pointer);
	
	public static native void sha1_update (long sha1Pointer, byte[] data);
	
	public static native void sha1_final (long sha1Pointer);
	
	public static native char[] sha1_getHash (long sha1Pointer);
	
	public static native void sha1_delete (long sha1Pointer);
	
	public static native int crc_valXORRotateLeft (int val, byte[] data);
	
	public static native int crc_rotl (int val, int i);
}
