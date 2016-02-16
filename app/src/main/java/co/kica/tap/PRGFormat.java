package co.kica.tap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import co.kica.fileutils.SmartFile;
import co.kica.fileutils.SmartFileInputStream;
import co.kica.tap.T64Format.DirEntry;
import co.kica.tap.Turbo;

public class PRGFormat {
	public ByteArrayOutputStream tapData = new ByteArrayOutputStream();
	private String filename;
	private byte[] data;
	private int progtype;
	private int start;
	private int end;
	private byte checkByte;
	private int turboMode = 1;
	public static final int shortPulse = 0x30;
	public static final int mediumPulse = 0x42;
	public static final int longPulse = 0x56;
	
	public PRGFormat( String fn, int idx ) {

		if (fn.toUpperCase().endsWith(".T64")) {
			T64Format t64 = new T64Format( fn, true );
			if (t64.validHeader()) {
				ArrayList<DirEntry> dir = t64.getDir();
				DirEntry d = dir.get(idx);
				this.start = d.start;
				this.progtype = 3;
				this.data = d.getProgramData();
				this.end = this.start + data.length;
				this.setFilename(d.getFilename().toUpperCase().replaceAll(".PRG", ""));
			}
		} else {
			if (fn.toUpperCase().endsWith(".P00")) {
				this.loadP00File(fn);
			} else {
				this.loadFile(fn);
			}
		}
	}
	
	private void loadP00File(String filename) {
		byte[] header = new byte[26];
		byte[] laddr = new byte[2];
		
		SmartFile f = new SmartFile(filename);
		data = new byte[(int) (f.length()-28)];
		
		try {
			BufferedInputStream bis = new BufferedInputStream(new SmartFileInputStream(f));
			int laSize = bis.read(header);
			int lbSize = bis.read(laddr);
			int plSize = bis.read(data);
			bis.close();
			
			int addr = (laddr[0]&0xff) + 256*(laddr[1]&0xff);
			this.start = addr;
			this.end = addr + data.length;
			this.progtype = 3;
			this.setFilename(f.getName().toUpperCase().replaceFirst(".P00$", ""));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadFile(String filename) {
		byte[] loadAddr = new byte[2];
		
		SmartFile f = new SmartFile(filename);
		data = new byte[(int) (f.length()-2)];
		
		try {
			BufferedInputStream bis = new BufferedInputStream(new SmartFileInputStream(f));
			int laSize = bis.read(loadAddr);
			int plSize = bis.read(data);
			bis.close();
			
			int addr = (loadAddr[0]&0xff) + ((loadAddr[1]&0xff)*256);
			this.start = addr;
			this.end = addr + data.length;
			this.progtype = 3;
			this.setFilename(f.getName().toUpperCase().replaceFirst(".PRG$", ""));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ByteArrayOutputStream generate() {
		this.writeHeader();
		this.writeHeaderRepeat();
		this.writeSilence();
		this.writeData();
		this.writeDataRepeat();
		return tapData;
	}
	
	public ByteArrayOutputStream generateWithTurboTape() {
		// save real start and end addresses
		int savedStartAddress = this.start;
		int savedEndAddress = this.end;
		byte[] savedProgData = this.data;
		
		// seed the turbo tape start and end addresses
		this.start = Turbo.CBMDataLoadAddress;
		this.end   = Turbo.CBMDataEndAddress;
		this.data  = Turbo.CBMDataBlock_02a7;
		
		// write the header
		this.writeTurboHeader();
		this.writeTurboHeaderRepeat();
		this.writeSilence();
		
		// write the turbo loader code
		this.writeData();
		this.writeDataRepeat();
		
		// reset the data
		this.start = savedStartAddress;
		this.end = savedEndAddress;
		this.data = savedProgData;
		
		// now write ze turbo blocken
		this.writeSilence();
		this.writeTurboData();
		
		
		return tapData;
	}
	
	

	private void writeTurboHeaderData() {
		this.checkByte = 0;
		this.writeDataByte(this.progtype, true);
		// start address lSB, mSB
		int sl = this.start & 0xff;
		int sm = (this.start / 256) & 0xff;
		this.writeDataByte(sl, true);
		this.writeDataByte(sm, true);
		// end address lSB, mSB
		int el = this.end & 0xff;
		int em = (this.end / 256) & 0xff;
		this.writeDataByte(el, true);
		this.writeDataByte(em, true);
		// filename padded to 16 chars, lower case
		String tmp = this.getFilename().toUpperCase();
		while (tmp.length() < 16)
			tmp = tmp + " ";
		tmp = tmp.substring(0, 16);
		byte[] fndata = tmp.getBytes();
		for (byte b: fndata) {
			this.writeDataByte(b, true);
		}
		// header code here
		byte[] header = Turbo.getHeaderBlock(this.turboMode);
		for (byte b: header) {
			this.writeDataByte(b & 0xff, true);
		}
		
		// pad out rest of it with spaces 
		for (int i=0; i<(171-header.length); i++) {
			this.writeDataByte(32, true);
		}
		// checkbyte
		this.writeDataByte(this.checkByte & 0xff, false);
	}

	public void writeTurboHeader() {
		this.writeHeaderPilot();
		this.writeHeaderDataSyncTrain();
		writeTurboHeaderData();
	}
	
	public void writeTurboHeaderRepeat() {
		this.writeHeaderRepeatPilot();
		this.writeRepeatSyncTrain();
		writeTurboHeaderData();
		this.writeRepeatTrailer();
	}

	public void writeTurboData() {
		
		// pre pilot
		for (int i=0; i<Turbo.PrePilotLength; i++) {
			writeTurboDataByte(Turbo.PrePilot);
		}
		
		// first pilot bytes ?
		for (int i=0; i<2048; i++) {
			writeTurboDataByte(Turbo.PilotByte);
		}
		
		// countdown synchro sequence $09, .., $01
		for (byte b: Turbo.SyncTrain) {
			writeTurboDataByte(b & 0xff);
		}
		
		// write type byte
		this.writeTurboDataByte(0x01);
		
		// start address lo, hi
		// start address lSB, mSB
		int sl = this.start & 0xff;
		int sm = (this.start / 256) & 0xff;
		this.writeTurboDataByte(sl);
		this.writeTurboDataByte(sm);
		// end address lSB, mSB
		int el = this.end & 0xff;
		int em = (this.end / 256) & 0xff;
		this.writeTurboDataByte(el);
		this.writeTurboDataByte(em);
		
		// six bytes in zero page
		// countdown synchro sequence $09, .., $01
		byte[] z = Turbo.SixBytes;
		if (this.start != 0x0801) {
			z = Turbo.SixBytesExec;
		}
		
		for (byte b: z) {
			writeTurboDataByte(b & 0xff);
		}
		
		// write data
		for (byte b: this.data) {
			this.writeTurboDataByte(b & 0xff);
		}
		
		// checkbyte
		int cb = this.calculateChecksum(this.data);
		this.writeTurboDataByte(cb);
		this.writeTurboDataByte(cb);
		
		// repeat for happiness
		//this.writeTurboDataByte(cb);
		//this.writeSilence();
		
	}

	public void writeByte( int val ) {
		tapData.write((byte)(val & 0xff));
	}
	
	public void writeBit(int bit) {
		switch (bit & 1) {
		case 0:	writeByte(shortPulse);
				writeByte(mediumPulse);
				break;
		case 1: writeByte(mediumPulse);
			  	writeByte(shortPulse);
			  	break;
		}
	}
	
	public void writeTurboBit(int bit) {
		switch (bit & 1) {
		case 0:	writeByte(Turbo.zeroVal(this.turboMode));
				break;
		case 1: writeByte(Turbo.oneVal(this.turboMode));
			  	break;
		}
	}
	
	public void newDataMarker() {
		writeByte(longPulse);
		writeByte(mediumPulse);
	}
	
	public void endDataMarker() {
		writeByte(longPulse);
		writeByte(shortPulse);
	}
	
	public void writeDataByte( int val, boolean moreData ) {
		int cb = 1;
		byte by = (byte) val;
		for (int i=0; i<8; i++) {
			int bit = val & 1;
			val = val >>> 1;
			cb = cb ^ bit;
			writeBit(bit);
		}
		// write check bit
		writeBit(cb & 1);
		// write data marker
		if (moreData) 
			newDataMarker();
		else
			endDataMarker();
		
		this.checkByte ^= by;
	}
	
	public void writeTurboDataByte( int val ) {
		byte by = (byte) val;
		for (int i=0; i<8; i++) {
			int bit = (val & 128) >>> 7;
			val = val << 1;
			writeTurboBit(bit);
		}
		
		this.checkByte ^= by;
	}
	
	public void writeByteStream( byte[] raw ) {
		int idx = 0;
		for (byte b: raw) {
			if (idx < raw.length-1) 
				writeDataByte((int)(b & 0xff), true);
			else
				writeDataByte((int)(b & 0xff), false);
			idx ++;
		}
	}
	
	public void writeHeaderPilot() {
		for (int i=0; i<0x6A00; i++) {
			writeByte(shortPulse);
		}
	}
	
	public void writeHeaderRepeatPilot() {
		for (int i=0; i<0x4f; i++) {
			writeByte(shortPulse);
		}
	}
	
	public void writeDataPilot() {
		for (int i=0; i<0x1A00; i++) {
			writeByte(shortPulse);
		}
	}
	
	public void writeDataRepeatPilot() {
		for (int i=0; i<0x4f; i++) {
			writeByte(shortPulse);
		}
	}
	
	public void writeHeaderDataSyncTrain() {
		byte[] data = {(byte) 0x89, (byte) 0x88, (byte) 0x87, (byte) 0x86, 
				       (byte) 0x85, (byte) 0x84, (byte) 0x83, (byte) 0x82, (byte) 0x81};
		// new data marker
		this.newDataMarker();
		// bytes
		int i=0;
		for (byte b: data) {
			if (i < 8) 
				writeDataByte((int)(b & 0xff), true);
			else
				writeDataByte((int)(b & 0xff), false);
			i++;
		}
	}
	
	public void writeRepeatSyncTrain() {
		byte[] data = {(byte) 0x09, (byte) 0x08, (byte) 0x07, (byte) 0x06, 
				       (byte) 0x05, (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01};
		// new data marker
		this.newDataMarker();
		// bytes
		int i=0;
		for (byte b: data) {
			if (i < 8) 
				writeDataByte((int)(b & 0xff), true);
			else
				writeDataByte((int)(b & 0xff), false);
			i++;
		}
	}
	
	public void writeHeader() {
		this.writeHeaderPilot();
		this.writeHeaderDataSyncTrain();
		writeHeaderData();
	}
	
	public void writeHeaderRepeat() {
		this.writeHeaderRepeatPilot();
		this.writeRepeatSyncTrain();
		writeHeaderData();
		this.writeRepeatTrailer();
	}
	
	public void writeData() {
		this.writeDataPilot();
		this.writeHeaderDataSyncTrain();
		this.writeDataBlock();
	}

	public void writeDataRepeat() {
		this.writeDataRepeatPilot();
		this.writeRepeatSyncTrain();
		this.writeDataBlock();
		this.writeRepeatTrailer();
	}
	
	public void writeDataBlock() {
		// reset checkbyte
		this.checkByte = 0;
		for (byte b: this.data) {
			this.writeDataByte(b, true);
		}
		// checkbyte
		this.writeDataByte(calculateChecksum(this.data), false);
	}
	
	public void writeRepeatTrailer() {
		for (int i=0; i<0x4e; i++) {
			writeByte(shortPulse);
		}
	}
	
	public void writeSilence() {
		for (int i=0; i<10; i++) {
			writeByte(0);
		}
	}

	private void writeHeaderData() {
		// now the type of data
		// reset checkbyte
		this.checkByte = 0;
		this.writeDataByte(this.progtype, true);
		// start address lSB, mSB
		int sl = this.start & 0xff;
		int sm = (this.start / 256) & 0xff;
		this.writeDataByte(sl, true);
		this.writeDataByte(sm, true);
		// end address lSB, mSB
		int el = this.end & 0xff;
		int em = (this.end / 256) & 0xff;
		this.writeDataByte(el, true);
		this.writeDataByte(em, true);
		// filename padded to 16 chars, lower case
		String tmp = this.getFilename().toUpperCase();
		while (tmp.length() < 16)
			tmp = tmp + " ";
		tmp = tmp.substring(0, 16);
		byte[] fndata = tmp.getBytes();
		for (byte b: fndata) {
			this.writeDataByte(b, true);
		}
		// 171 bytes.. 
		for (int i=0; i<171; i++) {
			this.writeDataByte(32, true);
		}
		// checkbyte
		this.writeDataByte(this.checkByte & 0xff, false);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public int calculateChecksum( byte[] block) {
		byte b = 0;
		for (byte x: block) {
			b ^= x;
		}
		return b&0xff;
	}

	public int getTurboMode() {
		return turboMode;
	}

	public void setTurboMode(int turboMode) {
		this.turboMode = turboMode;
	}
}
