package ch.drystayle.jghost.bnls;

public enum BnlsProtocolEnum {
	
	//---- Static
	
	BNLS_NULL					((char) 0x00),
	BNLS_CDKEY					((char) 0x01),
	BNLS_LOGONCHALLENGE			((char) 0x02),
	BNLS_LOGONPROOF				((char) 0x03),
	BNLS_CREATEACCOUNT			((char) 0x04),
	BNLS_CHANGECHALLENGE		((char) 0x05),
	BNLS_CHANGEPROOF			((char) 0x06),
	BNLS_UPGRADECHALLENGE		((char) 0x07),
	BNLS_UPGRADEPROOF			((char) 0x08),
	BNLS_VERSIONCHECK			((char) 0x09),
	BNLS_CONFIRMLOGON			((char) 0x0a),
	BNLS_HASHDATA				((char) 0x0b),
	BNLS_CDKEY_EX				((char) 0x0c),
	BNLS_CHOOSENLSREVISION		((char) 0x0d),
	BNLS_AUTHORIZE				((char) 0x0e),
	BNLS_AUTHORIZEPROOF			((char) 0x0f),
	BNLS_REQUESTVERSIONBYTE		((char) 0x10),
	BNLS_VERIFYSERVER			((char) 0x11),
	BNLS_RESERVESERVERSLOTS		((char) 0x12),
	BNLS_SERVERLOGONCHALLENGE	((char) 0x13),
	BNLS_SERVERLOGONPROOF		((char) 0x14),
	BNLS_RESERVED0				((char) 0x15),
	BNLS_RESERVED1				((char) 0x16),
	BNLS_RESERVED2				((char) 0x17),
	BNLS_VERSIONCHECKEX			((char) 0x18),
	BNLS_RESERVED3				((char) 0x19),
	BNLS_VERSIONCHECKEX2		((char) 0x1a),
	BNLS_WARDEN					((char) 0x7d);
	
	//---- State
	
	private char val;
	
	//---- Constructors
	
	private BnlsProtocolEnum (char val) {
		this.val = val;
	}
	
	//---- Methods
	
	public char toVal () {
		return this.val;
	}
	
}
