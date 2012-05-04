package ch.drystayle.jghost.common;

import java.util.ArrayList;
import java.util.List;

public class Bytearray {

	//---- Static
	
	public static int END = -1;
	
	public static Bytearray fromStringNumbers (String numbers) {
		Bytearray newBa = new Bytearray();
		
		if (numbers != null && numbers.length() != 0) {
			String[] strings = numbers.split(" ");
			
			for (String string : strings) {
				newBa.addChar((char) (int) new Integer(string));
			}
		}
	
		return newBa;
	}
	
	//---- State
	
	private List<Character> bytearray;
	
	//---- Constructors
	
	public Bytearray () {
		this.bytearray = new ArrayList<Character>();
	}
	
	public Bytearray (String s) {
		this.bytearray = new ArrayList<Character>();
		addCharArray(s.toCharArray());
	}
	
	public Bytearray (char[] ca) {
		this.bytearray = new ArrayList<Character>();
		for (char c : ca) {
			this.bytearray.add(c);
		}
	}
	
	public Bytearray (short i) {
		this.bytearray = new ArrayList<Character>();
		addShort(i);
	}
	
	public Bytearray (int i) {
		this.bytearray = new ArrayList<Character>();
		addInt(i);
	}
	
	public Bytearray (Bytearray ba, int begin, int end) {
		this.bytearray = new ArrayList<Character>();
		for (int i = begin; i < end; i++) {
			this.bytearray.add(ba.getChar(i));
		}
	}
	
	public Bytearray(byte[] mapDataPart) {
		this.bytearray = new ArrayList<Character>();
		for (byte b : mapDataPart) {
			this.bytearray.add((char) b);
		}
	}
	
	//---- Methods

	public Bytearray encode () {
		char Mask = 1;
		Bytearray Result = new Bytearray();

		for (int i = 0; i < this.bytearray.size(); i++) {
			if( ( this.bytearray.get(i) % 2 ) == 0 ) {
				Result.addChar((char) (this.bytearray.get(i) + 1));
			} else {
				Result.addChar( this.bytearray.get(i) );
				Mask |= 1 << ( ( i % 7 ) + 1 );
			}

			if (i % 7 == 6 || i == this.bytearray.size( ) - 1) {
				Result.insertChar(Result.size() - 1 - ( i % 7 ), Mask);
				Mask = 1;
			}
		}

		return Result;
	}
	
	public void insertChar (int pos, char c) {
		this.bytearray.add(pos, c);
	}
	
	//FIX char = byte??
	public byte[] asArray () {
		byte[] arr = new byte[this.bytearray.size()];
		for (int i = 0; i < this.bytearray.size(); i++) {
			arr[i] = (byte)(char) this.bytearray.get(i);
		}
		
		return arr;
	}
	
	public boolean isEmpty () {
		return (this.size() == 0);
	}
	
	public void addChar (char b) {
		this.bytearray.add(b);
	}
	
	public void addString (String s) {
		addString(s, true);
	}
	


	public void addString (String s, boolean terminator) {
		addCharArray(s.toCharArray());
		if (terminator) {
			 //add string terminator
			addChar((char) 0);
		}
	}
	
	public void addCharArray (char[] ca) {
		for (char c : ca) {
			this.bytearray.add(c);
		}
	}
	
	public void addShort (short i) {
		this.bytearray.add((char) ((char) (i) & 0x00FF));
		this.bytearray.add((char) ((char) (i >> 8) & 0x00FF));
	}
	
	public void addInt (int i) {
		this.bytearray.add((char) ((char) (i) & 0x00FF));
		this.bytearray.add((char) ((char) (i >> 8) & 0x00FF));
		this.bytearray.add((char) ((char) (i >> 16) & 0x00FF));
		this.bytearray.add((char) ((char) (i >> 24) & 0x00FF));
	}
	
	public void addBytearray (Bytearray b) {
		for (Character c : b.bytearray) {
			this.bytearray.add(c);
		}
	}
	
	public void set (int index, char c) {
		this.bytearray.set(index, c);
	}
	
	public Character getChar (int index) {
		return this.bytearray.get(index);
	}
	
	public int size () {
		return this.bytearray.size();
	}
	
	public short toShort () {
		if (this.bytearray.size() < 2) {
			//TODO
			throw new RuntimeException();
		}
		return (short) (this.bytearray.get(1) << 8 | this.bytearray.get(0));
	}
	
	public int toInt () {
		if (this.bytearray.size() < 4) {
			//TODO
			throw new RuntimeException();
		}
		return this.bytearray.get(3) << 24 | this.bytearray.get(2) << 16 | this.bytearray.get(1) << 8 | this.bytearray.get(0);
	}
	
	public int toInt(int start) {
		if (this.bytearray.size() < start + 4) {
			//TODO
			throw new RuntimeException();
		}
		return this.bytearray.get(start + 3) << 24 | this.bytearray.get(start + 2) << 16 | this.bytearray.get(start + 1) << 8 | this.bytearray.get(start + 0);
	}
	
	public String toFormattedString () {
		if (this.bytearray.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (Character c : this.bytearray) {
			sb.append(((int) c) + " ");
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
	
	public Bytearray extractCString (int start) {
		Bytearray b = new Bytearray();
		
		for (int i = start; i < this.bytearray.size(); i++) {
			if (this.bytearray.get(i) == '\0') {
				return b;
			}
			b.addChar(this.bytearray.get(i));
		}
		
		return b;
	}
	

	public Bytearray extract(final int start, final int length) {
		if (this.bytearray.size() < start + length) {
			//TODO
			throw new RuntimeException();
		}
		
		Bytearray b = new Bytearray();
		if (length == END) {
			for (int i = start; i < this.bytearray.size(); i++) {
				b.addChar(this.bytearray.get(i));
			}
		} else {
			for (int i = start; i < start + length; i++) {
				b.addChar(this.bytearray.get(i));
			}
		}
		
		return b;
	}
	
	public char extractHex (int start, boolean reverse) {
		
		// consider the byte array to contain a 2 character ASCII encoded hex value at b[start] and b[start + 1] e.g. "FF"
		// extract it as a single decoded byte

		if (start + 1 < size()) {
			String temp = extract(start, 2).toCharString();

			if (reverse) {
				temp = "" + temp.charAt(1) + temp.charAt(0);
			}
			return (char) Integer.parseInt(temp, 16);
		}
		
		return 0;
	}
	
	public void resize (int newSize) {
		if (newSize < 0) {
			//TODO
			throw new RuntimeException();
		}
		
		if (newSize < this.bytearray.size()) {
			while (newSize != this.bytearray.size()) {
				this.bytearray.remove(this.bytearray.size() - 1);
			}
		} else if (newSize > this.bytearray.size()) {
			while (newSize != this.bytearray.size()) {
				this.bytearray.add((char) 0);
			}
		}
	}
	
	//---- Object
	
	public String toCharString() {
		StringBuilder sb = new StringBuilder();
		
		for (Character c : this.bytearray) {
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder();
		
		for (Character c : this.bytearray) {
			sb.append(Integer.toHexString((int) c));
			sb.append(' ');
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}
	
}
