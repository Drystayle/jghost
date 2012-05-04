package ch.drystayle.jghost.bnet;

import java.io.File;

import org.apache.log4j.Logger;

import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.util.JniUtil;

public class BNCSUtilInterface {
	
	//---- Static
	
	/** The Logger to use in this class. */
	private static final Logger LOG = Logger.getLogger(BNCSUtilInterface.class);
	
	
	//---- State
	
	private long m_NLS;
	private Bytearray m_EXEVersion;			// set in HELP_SID_AUTH_CHECK
	private Bytearray m_EXEVersionHash;		// set in HELP_SID_AUTH_CHECK
	private String m_EXEInfo;				// set in HELP_SID_AUTH_CHECK
	private Bytearray m_KeyInfoROC;			// set in HELP_SID_AUTH_CHECK
	private Bytearray m_KeyInfoTFT;			// set in HELP_SID_AUTH_CHECK
	private Bytearray m_ClientKey;			// set in HELP_SID_AUTH_ACCOUNTLOGON
	private Bytearray m_M1;					// set in HELP_SID_AUTH_ACCOUNTLOGONPROOF
	private Bytearray m_PvPGNPasswordHash;	// set in HELP_PvPGNPasswordHash

	//---- Constructors
	
	public BNCSUtilInterface (String userName, String userPassword) {
		m_NLS = JniUtil.nls_init(userName, userPassword);
	}

	//---- Methods
	
	public Bytearray GetEXEVersion () 			{ return m_EXEVersion; }
	public Bytearray GetEXEVersionHash ()		{ return m_EXEVersionHash; }
	public String GetEXEInfo () 				{ return m_EXEInfo; }
	public Bytearray GetKeyInfoROC( ) 			{ return m_KeyInfoROC; }
	public Bytearray GetKeyInfoTFT( ) 			{ return m_KeyInfoTFT; }
	public Bytearray GetClientKey( ) 			{ return m_ClientKey; }
	public Bytearray GetM1( ) 					{ return m_M1; }
	public Bytearray GetPvPGNPasswordHash( ) 	{ return m_PvPGNPasswordHash; }

	public void SetEXEVersion( Bytearray nEXEVersion )			{ m_EXEVersion = nEXEVersion; }
	public void SetEXEVersionHash( Bytearray nEXEVersionHash )	{ m_EXEVersionHash = nEXEVersionHash; }
	
	public void Reset (String userName, String userPassword) {
		if (m_NLS == 0) {
			m_NLS = JniUtil.nls_init(userName, userPassword);
		} else {
			m_NLS = JniUtil.nls_reset(m_NLS, userName, userPassword);
		}
	}

	public boolean HELP_SID_AUTH_CHECK (String war3Path, String keyROC, String keyTFT, String valueStringFormula, String mpqFileName, Bytearray clientToken, Bytearray serverToken ) {
		// set m_EXEVersion, m_EXEVersionHash, m_EXEInfo, m_InfoROC, m_InfoTFT

		String FileWar3EXE = war3Path + "war3.exe";
		String FileStormDLL = war3Path + "storm.dll";
		String FileGameDLL = war3Path + "game.dll";
		
		boolean ExistsWar3EXE = new File(FileWar3EXE).exists();
		boolean ExistsStormDLL = new File(FileStormDLL).exists();
		boolean ExistsGameDLL = new File(FileGameDLL).exists();

		if( ExistsWar3EXE && ExistsStormDLL && ExistsGameDLL )
		{
			String[] buf = JniUtil.getExeInfo(FileWar3EXE, JniUtil.BNCSUTIL_PLATFORM_X86);
			m_EXEInfo = buf[1];
			m_EXEVersion = new Bytearray(new Integer(buf[0]));
			int EXEVersionHash = JniUtil.checkRevisionFlat(valueStringFormula, FileWar3EXE, FileStormDLL, FileGameDLL, mpqFileName);
			m_EXEVersionHash = new Bytearray(EXEVersionHash);
			m_KeyInfoROC = CreateKeyInfo( keyROC, clientToken.toInt(), serverToken.toInt());
			m_KeyInfoTFT = CreateKeyInfo( keyTFT, clientToken.toInt(), serverToken.toInt());

			if(m_KeyInfoROC.size() == 36 && m_KeyInfoTFT.size() == 36)
				return true;
			else
			{
				if( m_KeyInfoROC.size() != 36 )
					LOG.error("unable to create ROC key info - invalid ROC key");

				if( m_KeyInfoTFT.size() != 36 )
					LOG.error("unable to create TFT key info - invalid TFT key" );
			}
		}
		else
		{
			if( !ExistsWar3EXE )
				LOG.error("unable to open [" + FileWar3EXE + "]" );

			if( !ExistsStormDLL )
				LOG.error("unable to open [" + FileStormDLL + "]" );

			if( !ExistsGameDLL )
				LOG.error("unable to open [" + FileGameDLL + "]" );
		}

		return false;
	}
	
	public boolean HELP_SID_AUTH_ACCOUNTLOGON( ) {
		char[] buf = JniUtil.nls_getPublicKey(m_NLS);
		m_ClientKey = new Bytearray(buf);
		return true;
	}
	
	public boolean HELP_SID_AUTH_ACCOUNTLOGONPROOF( Bytearray salt, Bytearray serverKey ) {
		char[] buf = JniUtil.nls_getClientSessionKey(
			m_NLS, salt.toCharString(), serverKey.toCharString()
		);
		m_M1 = new Bytearray(buf);
		return true;
	}
	
	public boolean HELP_PvPGNPasswordHash(String userPassword ) {
		/*// set m_PvPGNPasswordHash
		//TODO implement hash
		char buf[20];
		hashPassword( userPassword.c_str( ), buf );
		m_PvPGNPasswordHash = UTIL_CreateByteArray( (unsigned char *)buf, 20 );*/
		return true;
	}

	private Bytearray CreateKeyInfo(String key, int clientToken, int serverToken ) {
		Bytearray KeyInfo = new Bytearray();
		long decoderPointer = JniUtil.decoder_init(key);
	
		if (JniUtil.decoder_isKeyValid(decoderPointer))
		{
			KeyInfo.addInt(key.length());
			KeyInfo.addInt(JniUtil.decoder_getProduct(decoderPointer));
			KeyInfo.addInt(JniUtil.decoder_getVal1(decoderPointer));
			for (int i = 0; i < 4; i++) {
				KeyInfo.addChar((char) 0);
			}
			KeyInfo.addCharArray(JniUtil.decoder_getHash(decoderPointer, clientToken, serverToken));
		}

		return KeyInfo;
	}
}
