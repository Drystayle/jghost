package ch.drystayle.jghost.util;

public class SHA1 {
	
	//---- State
	
	private Long sha1Pointer;
	
	//---- Constructors
	
	public SHA1 () {
		this.sha1Pointer = JniUtil.sha1_init();
	}

	//---- Destructors
	
	protected void finalize () throws Throwable {
		super.finalize();
		JniUtil.sha1_delete(sha1Pointer);
	}

	//---- Methods
	
	public void reset () {
		JniUtil.sha1_reset(sha1Pointer);
	}
	
	public void update (byte[] data) {
		JniUtil.sha1_update(sha1Pointer, data);
	}
	
	public void finalizeHash () {
		JniUtil.sha1_final(sha1Pointer);
	}
	
	public char[] getHash () {
		return JniUtil.sha1_getHash(sha1Pointer);
	}
	
}
