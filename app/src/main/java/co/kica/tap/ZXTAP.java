package co.kica.tap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/* This format is a catch-all for spectrum Tape format */

public class ZXTAP extends TZXTape {
	
	public final int PAUSE = 1000;
	public final byte TZXBLOCK = 0x10;
	
	public ZXTAP(int sampleRate) {
		super(sampleRate);
	}

	@Override
	public void Load( String fn ) {
		// we cheat here this reads in the file, and maps it into TZXChunks (type $10)
		this.setValid(false);
		
		File f = new File(fn);
		long totalSize = f.length();
		
		try {
			FileInputStream fis = new FileInputStream( f );
			Data.reset();
			byte[] dataSize = new byte[2]; // header size...
			long bytesRead = 0;
			boolean err = false;
			int r;
			byte[] pause = new byte[2];
			pause[0] = (byte)(PAUSE % 256);
			pause[1] = (byte)(PAUSE / 256);
			
			while (bytesRead < totalSize && !err) {
				// read size of chunk
				r = fis.read(dataSize);
				if (r == 2) {
					// got header
					bytesRead += r;
					int count = (dataSize[0] & 0xff) + (dataSize[1] & 0xff)*256;
					System.out.println("Block header says block is "+count+" bytes...");
					byte[] chunk = new byte[count];
					r = fis.read(chunk);
					if (r != count) {
						err = true;
						System.out.println("!!! Block is only "+r+" bytes...");
						break;
					} else {
						System.out.println("*** Block is correctly "+r+" bytes...");
					}
					bytesRead +=  r;
					// here if block data ok
					Data.write(TZXBLOCK);
					Data.write(pause);
					Data.write(dataSize);
					Data.write(chunk);
					
					System.out.println("Converted TAP block to TZX(10h) block...");
				} else {
					err = true;
				}
			}
			
			fis.close();
			
			this.setValid(!err);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
