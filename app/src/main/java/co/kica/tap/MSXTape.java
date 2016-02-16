package co.kica.tap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math.*;

public class MSXTape extends GenericTape {
	
	private static final int CID_UNKNOWN  = 0x000;
	private static final int CID_ASCII  = 0x100;
	private static final int CID_BINARY = 0x200;
	private static final int CID_BASIC = 0x400;
	
	public class MSXChunk {
		public int id = 0;
		public byte[] chunkData;
	}
	
	private byte VersionMinor;
	private byte VersionMajor;
	public static final double PULSE_AMPLITUDE = -1;
	public static final double PULSE_MIDDLE = 1;
	public static final double PULSE_REST = 1;
	public static final double PAL_CLK = 985248;
	public static final double MU = 1000000;
	
	private double longSilence = 2000000;
	private double shortSilence = 1000000;
	private float baseFrequency = 1200; // Hz
	private float baudRate = 1200;      // Baud rate
	private float phase = 180;          // wavePhase
	private float carrierFrequency = baseFrequency*2;
	private float shortPulseDuration = 1000000 / 2400;
	private float longPulseDuration = 1000000 / 1200;
	private int longHeader = 16000;
	private int shortHeader = 4000;
	private MSXChunk lastChunk = null;
	private boolean eof = false;
	private float renderPercent = 0f;
	
	private byte[] ASCII =  new byte[] { (byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA,(byte)0xEA };
	private byte[] BIN = new byte[] { (byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0,(byte)0xD0 };
	private byte[] BASIC = new byte[] { (byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3,(byte)0xD3 };
	private byte[] HEADER = new byte[] { 0x1f, (byte) 0xa6, (byte) 0xde, (byte) 0xba, (byte) 0xcc, 0x13, 0x7d, 0x74 };
	
	private double fudge = 1;
	
	public MSXTape() {
		// TODO Auto-generated constructor stub
		super();
		VersionMinor = 10;
		VersionMajor = 0;
	}
	
	public byte[] getMAGIC() {
		byte[] magic = Arrays.copyOfRange(Header.toByteArray(), 0, 8);
		return magic;
	}
	
	public boolean parseHeader( InputStream f ) {
		byte[] buff = new byte[headerSize()];
		setValid(false);
		
		try {
			//f.reset();
			int len = f.read(buff);
			
			if (len == headerSize()) {
				// reset and store into header
				Header.reset();
				Header.write(buff);
				byte[] magic = { 0x1f, (byte) 0xa6, (byte) 0xde, (byte) 0xba, (byte) 0xcc, 0x13, 0x7d, 0x74 };
				if (Arrays.equals(getMAGIC(),magic)) {
					System.out.println("*** File is a valid MSX Tape by the looks of it.");
					setValid(true);
					//setVersionMinor(buff[10]);
					//setVersionMajor(buff[11]);
					//System.out.println("*** Header says version is "+getVersionMajor()+"."+getVersionMinor());
					setStatus(tapeStatusOk);
				} else {
					//System.out.println("xxx File has unrecognized magic: "+getMAGIC());
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
				'U','E','F',' ','F','i','l','e','!',0,
				getVersionMinor(),
				getVersionMajor()};
		return first;
	}
	
	public int headerSize() {
		return 8;
	}
	
	public int minPadding() {
		return 0;
	}

	public byte getVersionMinor() {
		return VersionMinor;
	}

	public void setVersionMinor(byte version) {
		VersionMinor = version;
	}
	
	public byte getVersionMajor() {
		return VersionMajor;
	}

	public void setVersionMajor(byte version) {
		VersionMajor = version;
	}
	
	public short asByte(byte b) {
		return (short)(b & 0xff);
	}
	
	public boolean hasData() {
		return (dataPos < Data.size());
	}
	
	public byte getDataByte(byte[] data) {
		if (!hasData()) {
			return 0;
		}
		return data[dataPos++];
	}
	
	private byte[] next8Bytes( byte[] data ) {
		// get next 8 bytes
		byte[] b = Arrays.copyOfRange(data, dataPos, dataPos+8);
		
		return b;
	}
	
	private byte[] next10Bytes( byte[] data ) {
		// get next 8 bytes
		byte[] b = Arrays.copyOfRange(data, dataPos, dataPos+10);
		
		return b;
	}
	
	public void writeData( IntermediateBlockRepresentation w, byte[] data ) {
		  byte[] buffer;

		  eof=false;
		  while ((data.length-dataPos) >= 8) {
		    buffer = next8Bytes(data);
		    if (Arrays.equals(buffer, HEADER)) return;

		    writeByte(w,buffer[0]);
		    if ((buffer[0] & 0xff)==0x1a) eof=true;
		    dataPos++;
		  }

		  // write remaining bytes
		  if ((data[dataPos] & 0xff) == 0x1a) eof = true;
		  
		  while (dataPos < data.length) {
			  writeByte( w, data[dataPos] );
			  dataPos++;
		  }

		  return;
		
	}
	
	@Override
	public void writeAudioStreamData( String path, String base ) {
		IntermediateBlockRepresentation w = new IntermediateBlockRepresentation(path, base);
		
		//Data.reset();
		byte[] raw = Data.toByteArray();
		
		int bytesread = 0;
		double duration = 0;
		double cnv = 1.0;
		this.dataPos = 0;
		
		// start cuefile now
		ArrayList<String> cuedata = new ArrayList<String>();
		boolean lastData = false;
		
		while (hasData()) {		
			
			this.renderPercent = dataPos / Data.size();
			
		      /* it probably works fine if a long header is used for every */
		      /* header but since the msx bios makes a distinction between */
		      /* them, we do also (hence a lot of code).                   */
		    if (Arrays.equals(HEADER, next8Bytes(raw))) {
		    	
		      dataPos += 8;
		      if ((raw.length-dataPos) >= 10) {

		        if (Arrays.equals(ASCII, next10Bytes(raw))) {

		          this.addSilence(w, longSilence, PULSE_MIDDLE);
		          writeHeader(w, this.longHeader);
		          writeData(w, raw);

		          do {

		            dataPos+=8; //fseek(input,position,SEEK_SET);
		            this.addSilence(w, shortSilence, PULSE_MIDDLE);
		            writeHeader(w, this.shortHeader);
		            writeData(w, raw);

		          } while (!eof && hasData());

		        }
		        else if (Arrays.equals(BIN, next10Bytes(raw)) || Arrays.equals(BASIC, next10Bytes(raw))) {

		          //fseek(input,position,SEEK_SET);
		          this.addSilence(w, longSilence, PULSE_MIDDLE);
		          writeHeader(w, this.longHeader);
		          writeData(w, raw);
		          this.addSilence(w, shortSilence, PULSE_MIDDLE);
		          writeHeader(w, this.shortHeader);
		          dataPos+=8; // skip next header
		          writeData(w, raw);

		        } else {

		          System.out.println("unknown file type: using long header");
		          this.addSilence(w, longSilence, PULSE_MIDDLE);
		          writeHeader(w, this.longHeader);
		          writeData(w, raw);
		        }

		      }
		      else { 
		          System.out.println("unknown file type: using long header");
		          this.addSilence(w, longSilence, PULSE_MIDDLE);
		          writeHeader(w, this.longHeader);
		          writeData(w, raw);
		      }

		    } else {
		      
		      /* should not occur */
		      System.out.println("skipping unhandled data");
		      dataPos++;
		    }

		}
		
		// lets add some silence to contemplate the finer things in life :-)
		this.addSilence(w, 3000000.0, PULSE_REST);
		
		// write cue
		w.done();
	}
	
	private void writeHeader( IntermediateBlockRepresentation w, int pulseCount ) {
		for (int i=0; i<pulseCount; i++) {
			this.addSquareWave(w, shortPulseDuration, PULSE_AMPLITUDE, PULSE_REST);
		}
	}

	private void zeroBit(IntermediateBlockRepresentation w) {
		this.addSquareWave(w, longPulseDuration, PULSE_AMPLITUDE, PULSE_REST);
	}
	
	private void oneBit(IntermediateBlockRepresentation w) {
		// two short pulses
		this.addSquareWave(w, shortPulseDuration, PULSE_AMPLITUDE, PULSE_REST);
		this.addSquareWave(w, shortPulseDuration, PULSE_AMPLITUDE, PULSE_REST);
	}
	
	private void writeByte( IntermediateBlockRepresentation w, byte b ) {
		// start bit
		this.zeroBit(w);
		int bitcount = 8;
		while (bitcount > 0) {
			if ((b & 1) == 1) {
				this.oneBit(w);
			} else {
				this.zeroBit(w);
			}
			b = (byte) (b >>> 1);
			bitcount--;
		}
		// 2 stop bits
		this.oneBit(w);
		this.oneBit(w);
	}
	
	private void writeChunkData( MSXChunk chunk, IntermediateBlockRepresentation w ) {
		for (int i=0; i<chunk.chunkData.length; i++) {
			writeByte( w, chunk.chunkData[i] );
		}
	}
	
	
	private String byteArrayToString(byte[] bytes) {
		String r = "";
		
		for (byte b: bytes) {
			char c = (char) (b & 0xff);
			r = r + c;
		}
		
		return r;
	}

	public boolean isHeaderData() {
		return true;
	}

	@Override
	public String getTapeType() {
		// TODO Auto-generated method stub
		return "MSX";
	}

	@Override
	public float getRenderPercent() {
		// TODO Auto-generated method stub
		return (float)dataPos / (float)Data.size();
	}

}

