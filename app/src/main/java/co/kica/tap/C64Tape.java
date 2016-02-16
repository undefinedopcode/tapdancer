package co.kica.tap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class C64Tape extends GenericTape {
	
	private byte Version;
	public static final double PULSE_AMPLITUDE = 1;
	public static final double PULSE_MIDDLE = -1;
	public static final double PULSE_REST = -1;
	public static final double PAL_CLK = 985248;
	public static final double MU = 1000000;
	
	private float renderPercent; 
	
	public C64Tape() {
		// TODO Auto-generated constructor stub
		super();
		Version = 0;
	}
	
	public byte[] getMAGIC() {
		byte[] magic = Arrays.copyOfRange(Header.toByteArray(), 0, 12);
		return magic;
	}
	
	public String getMAGICString() {
		byte[] b = this.getMAGIC();
		return new String(b );
	}
	
	public boolean parseHeader( InputStream f ) {
		byte[] buff = new byte[headerSize()];
		
		try {
			//f.reset();
			int len = f.read(buff);
			
			if (len == headerSize()) {
				// reset and store into header
				Header.reset();
				Header.write(buff);
				byte[] magic = new byte[] {'C','6','4','-','T','A','P','E','-','R','A','W'};
				if (Arrays.equals(getMAGIC(),magic)) {
					System.out.println("*** File is a valid TAP by the looks of it.");
					setValid(true);
					System.out.println("*** Header says data size is "+getSizeFromHeader());
					setVersion(buff[12]);
					System.out.println("*** Header says version is "+getVersion());
					setStatus(tapeStatusOk);
					return true;
				} else {
					System.out.println("TAP File has unrecognized magic: "+getMAGICString());
					setStatus(tapeStatusHeaderInvalid);
					return false;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public byte[] getSizeAsBytes() {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(Data.size());
		return b.array();
	}
	
	public int getSizeFromHeader() {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.mark();
		b.put( Arrays.copyOfRange(Header.toByteArray(), 16, 19) );
		b.reset();
		return b.getInt();
	}
	
	public byte[] buildHeader() {
		byte[] first = new byte[] {
				'C','6','4','-','T','A','P','E','-','R','A','W',
				getVersion(),
				0,0,0};
		byte[] second = getSizeAsBytes();
		byte[] all = new byte[first.length + second.length];
		System.arraycopy( first, 0, all, 0, first.length);
		System.arraycopy( second, 0, all, first.length, second.length);
		return all;
	}
	
	public int headerSize() {
		return 20;
	}
	
	public int minPadding() {
		return 4;
	}

	public byte getVersion() {
		return Version;
	}

	public void setVersion(byte version) {
		Version = version;
	}
	
	public short asByte(byte b) {
		return (short)(b & 0xff);
	}
	
	public void writeAudioStreamData( String path, String base ) {
		IntermediateBlockRepresentation w = new IntermediateBlockRepresentation(path, base);
		
		//Data.reset();
		byte[] raw = Data.toByteArray();
		
		int bytesread = 0;
		double duration = 0;
		double cnv = 1.0;
		
		renderPercent = 0f;
		
		while (bytesread < Data.size()) {
			
			renderPercent = (float)bytesread / (float)raw.length;
			
			short p = asByte(raw[bytesread++]);
			
			if (p > 0) {
				double cycles = p * 8;
				duration = (cnv * MU * (cycles / PAL_CLK));
				this.addSquareWave(w, duration, PULSE_AMPLITUDE, PULSE_REST);
			} else {
				short a, b, c;
				double cycles;
				switch(getVersion()) {
					case 1:		a = asByte(raw[bytesread++]);
								b = asByte(raw[bytesread++]);
								c = asByte(raw[bytesread++]);
								cycles = a + (256*b) + (65536*c);
								System.out.println("*** a = "+a+", b = "+b+", c = "+c);
								duration = (cnv * MU * (cycles / PAL_CLK));
								break;
					case 0:		cycles = 2048;
								a = raw[bytesread++];
								while (a == 0) {
									cycles = cycles + 2048;
									a = asByte(raw[bytesread++]);
								}
								bytesread--;
								duration = (cnv * MU * (cycles / PAL_CLK));
								break;
					
				}
				System.out.println("Silence duration = "+duration);
				this.addSilence(w, duration, PULSE_AMPLITUDE);
			}
			
		}
		
		// do cue
		w.done();
		
		//return w;
	}
	
	@Override
	public float getRenderPercent() {
		return renderPercent;
	}

	@Override
	public boolean isHeaderData() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getTapeType() {
		// TODO Auto-generated method stub
		return "TAP";
	}

}
