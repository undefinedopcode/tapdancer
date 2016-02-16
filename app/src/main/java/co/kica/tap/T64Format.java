package co.kica.tap;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.File;

import co.kica.fileutils.SmartFile;
import co.kica.fileutils.SmartFileInputStream;

public class T64Format {
	
	private String filename;
	private byte[] data;
	private int progtype;
	private int start;
	private int end;
	
	public class DirEntry {
		byte[] dref = null;
		String filename = "FILE";
		int type = 0;
		int type_1541 = 0;
		int start = 0;
		int end = 0;
		int size = 0;
		int offset = 0;
		
		public DirEntry(byte[] dref) {
			this.dref = dref;
		}
		
		public byte[] getProgramData() {
			return Arrays.copyOfRange(dref, this.offset, this.offset+this.size);
		}
		
		public int getProgramLoadAddress() {
			return this.start;
		}
		
		public int getProgramEndAddress() {
			return this.end;
		}
		
		public String getFilename() {
			return this.filename;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getType_1541() {
			return type_1541;
		}

		public void setType_1541(int type_1541) {
			this.type_1541 = type_1541;
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}
	}
	
	public T64Format( String fn, boolean smart) {
		if (smart)
			this.loadFileSmart(fn);
		else
			this.loadFile(fn);
	}
	
	private void loadFileSmart(String filename) {
		byte[] loadAddr = new byte[2];
		
		SmartFile f = new SmartFile(filename);
		data = new byte[(int)f.length()];
		
		try {
			BufferedInputStream bis = new BufferedInputStream(new SmartFileInputStream(f));
			int plSize = bis.read(data);
			bis.close();
			this.setFilename(f.getName().toUpperCase().replaceFirst(".T64$", ""));
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
		
		File f = new File(filename);
		data = new byte[(int)f.length()];
		
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			int plSize = bis.read(data);
			bis.close();
			this.setFilename(f.getName().toUpperCase().replaceFirst(".T64$", ""));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public byte[] getMAGIC() {
		byte[] magic = Arrays.copyOfRange(this.data, 0, 3);
		return magic;
	}
	
	public int getVersionMinor() {
		return this.data[0x20] & 0xff;
	}
	
	public int getVersionMajor() {
		return this.data[0x21] & 0xff;
	}
	
	public int getMaxDirEntries() {
		return (this.data[0x22] & 0xff) + 256*(this.data[0x23] & 0xff);
	}
	
	public int getUsedDirEntries() {
		return (this.data[0x24] & 0xff) + 256*(this.data[0x25] & 0xff);
	}
	
	private String byteArrayToString(byte[] bytes) {
		String r = "";
		
		for (byte b: bytes) {
			char c = (char) (b & 0xff);
			r = r + c;
		}
		
		return r;
	}
	
	public String getTapeName() {
		byte[] d = Arrays.copyOfRange(this.data, 0x28, 0x40);
		return byteArrayToString(d);
	}
	
	public boolean validHeader() {
		byte [] magic = this.getMAGIC();
		byte [] valid = { 	0x43, 0x36, 0x34/*, 0x53*/ };
		boolean test = (Arrays.equals(magic, valid));
		return test;
	}
	
	public DirEntry getDirEntry( int index ) {
		
		if (index >= this.getMaxDirEntries()) {
			return null;
		}
		
		int offset = 0x0040 + (0x20 * index);
		int end = offset+32;
		
		byte[] rec = Arrays.copyOfRange(this.data, offset, end);
		int ftype = rec[0x00] & 0xff;
		
		if (ftype == 0) {
			return null; // empty
		}
		
		String ext = ".PRG";
		
		DirEntry d = new DirEntry(this.data);
		d.type = ftype;
		d.type_1541 = rec[0x01] & 0xff;
		
		if (d.type_1541 == 0) {
			if (ftype > 1) {
				ext = ".FRZ";
			} else {
				ext = ".FRE";
			}
		}
		
		d.start = (rec[0x02] & 0xff) + 256*(rec[0x03] & 0xff);
		d.end = (rec[0x04] & 0xff) + 256*(rec[0x05] & 0xff);
		d.size = d.end - d.start;
		d.offset = (rec[0x08] & 0xff) + 256*(rec[0x09] & 0xff) + 65536*(rec[0x0a] & 0xff) + 16777216*(rec[0x0b] & 0xff);
		d.filename = byteArrayToString( Arrays.copyOfRange(rec, 0x10, 0x20) ).replaceAll("[ ]+$", ext);
		
		return d;
	}
	
	public ArrayList<DirEntry> getDir() {
		
		ArrayList<DirEntry> dir = new ArrayList<DirEntry>();
		
		for (int i=0; i<this.getMaxDirEntries(); i++) {
			DirEntry d = this.getDirEntry(i);
			if (d != null) {
				dir.add(d);
			}
		}
		
		// fix for bad end addresses
		int end_offset = data.length;
		for (int j=dir.size()-1; j>=0; j--) {
			DirEntry d = dir.get(j);
			if (d.end == 0xc3c6) {
				d.size = end_offset - d.offset;
				d.end = d.start + d.size;
			}
			end_offset = d.offset + d.size;
		}
		
		return dir;
		
	}

}