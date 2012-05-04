package ch.drystayle.jghost.game;

import ch.drystayle.jghost.common.Bytearray;
import ch.drystayle.jghost.common.Constants;

public class GameSlot {
	
	//---- State
	
	private char m_PID; // player id
	private char m_DownloadStatus; // download status (0% to 100%)
	private char m_SlotStatus; // slot status (0 = open, 1 = closed, 2 = occupied)
	private char m_Computer; // computer (0 = no, 1 = yes)
	private char m_Team; // team
	private char m_Colour; // colour
	private char m_Race; // race (1 = human, 2 = orc, 4 = night elf, 8 = undead, 32 = random, 64 = fixed)
	private char m_ComputerType; // computer type (0 = easy, 1 = human or normal comp, 2 = hard comp)
	private char m_Handicap; // handicap
	
	//---- Constructors
	
	public GameSlot (Bytearray n) {
		if( n.size( ) >= 7 )
		{
			m_PID = n.getChar(0);
			m_DownloadStatus = n.getChar(1);
			m_SlotStatus = n.getChar(2);
			m_Computer = n.getChar(3);
			m_Team = n.getChar(4);
			m_Colour = n.getChar(5);
			m_Race = n.getChar(6);

			if( n.size( ) >= 8 )
				m_ComputerType = n.getChar(7);
			else
				m_ComputerType = Constants.SLOTCOMP_NORMAL;

			if( n.size( ) >= 9 )
				m_Handicap = n.getChar(8);
			else
				m_Handicap = 100;
		} else {
			m_PID = 0;
			m_DownloadStatus = 255;
			m_SlotStatus = Constants.SLOTSTATUS_OPEN;
			m_Computer = 0;
			m_Team = 0;
			m_Colour = 1;
			m_Race = Constants.SLOTRACE_RANDOM;
			m_ComputerType = Constants.SLOTCOMP_NORMAL;
			m_Handicap = 100;
		}
	}
	
	public GameSlot (char nPID, char nDownloadStatus, char nSlotStatus, char nComputer, char nTeam, char nColour, char nRace) {
		this(nPID, nDownloadStatus, nSlotStatus, nComputer, nTeam, nColour, nRace, (char) 1);
	}
	
	public GameSlot (char nPID, char nDownloadStatus, char nSlotStatus, char nComputer, char nTeam, char nColour, char nRace, char nComputerType) {
		this(nPID, nDownloadStatus, nSlotStatus, nComputer, nTeam, nColour, nRace, nComputerType, (char )100);
	}
	
	public GameSlot (char nPID, char nDownloadStatus, char nSlotStatus, char nComputer, char nTeam, char nColour, char nRace, char nComputerType, char nHandicap) {
		m_PID = nPID;
		m_DownloadStatus = nDownloadStatus;
		m_SlotStatus = nSlotStatus;
		m_Computer = nComputer;
		m_Team = nTeam;
		m_Colour = nColour;
		m_Race = nRace;
		m_ComputerType = nComputerType;
		m_Handicap = nHandicap;
	}
	
	//---- Methods
	
	public char GetPID() { return m_PID; }
	public char GetDownloadStatus() { return m_DownloadStatus; }
	public char GetSlotStatus() { return m_SlotStatus; }
	public char GetComputer() { return m_Computer; }
	public char GetTeam() { return m_Team; }
	public char GetColour() { return m_Colour; }
	public char GetRace() { return m_Race; }
	public char GetComputerType() { return m_ComputerType; }
	public char GetHandicap() { return m_Handicap; }

	public void SetPID(char nPID) { m_PID = nPID; }
	public void SetDownloadStatus(char nDownloadStatus) { m_DownloadStatus = nDownloadStatus; }
	public void SetSlotStatus(char nSlotStatus) { m_SlotStatus = nSlotStatus; }
	public void SetComputer(char nComputer) { m_Computer = nComputer; }
	public void SetTeam(char nTeam) { m_Team = nTeam; }
	public void SetColour(char nColour) { m_Colour = nColour; }
	public void SetRace(char nRace) { m_Race = nRace; }
	public void SetComputerType(char nComputerType) { m_ComputerType = nComputerType; }
	public void SetHandicap(char nHandicap) { m_Handicap = nHandicap; }
	
	public Bytearray GetByteArray() {
		Bytearray b = new Bytearray();
		b.addChar(m_PID);
		b.addChar(m_DownloadStatus);
		b.addChar(m_SlotStatus);
		b.addChar(m_Computer);
		b.addChar(m_Team);
		b.addChar(m_Colour);
		b.addChar(m_Race);
		b.addChar(m_ComputerType);
		b.addChar(m_Handicap);
		return b;
	}
	
}
