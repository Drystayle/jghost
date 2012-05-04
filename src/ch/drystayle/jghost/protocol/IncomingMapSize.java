package ch.drystayle.jghost.protocol;

public class IncomingMapSize {
	
	//---- State
	
	private char m_SizeFlag;
	private int m_MapSize;

	//---- Constructors
		
	public IncomingMapSize (char nSizeFlag, int nMapSize) {
		m_SizeFlag = nSizeFlag;
		m_MapSize = nMapSize;
	}
	
	//---- Methods
	
	public char GetSizeFlag ()	{ return m_SizeFlag; }
	public int GetMapSize ()	{ return m_MapSize; }
	
}
