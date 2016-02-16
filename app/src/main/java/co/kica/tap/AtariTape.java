package co.kica.tap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.lang.Math.*;

import co.kica.tap.IntermediateBlockRepresentation.SampleTable;

public class AtariTape extends GenericTape {
	
	public class CASChunk {
		public char[] id = {' ', ' ', ' ', ' '};
		public int length = 0;
		public int aux = 0;
		public byte[] chunkData;
		
		public String toString() {
			String idstr = new String(id);
			return idstr+" (length "+Integer.toString(length)+" byte(s), aux = "+Integer.toString(aux)+")";
		}
		
		public String recordType() {
			return new String(id);
		}
	}
	
	private byte VersionMinor;
	private byte VersionMajor;
	public static final double PULSE_AMPLITUDE = -0.99;
	public static final double PULSE_MIDDLE = 0.99;
	public static final double PULSE_REST = 0.99;
	public static final double PAL_CLK = 985248;
	public static final double MU = 1000000;
	private int dataPos = 0;
	private long bitCount = 0;
	
	private float baseFrequency = 1200; // Hz
	private float baudRate = 600;       // STANDARD ATARI CASSETTE Baud rate per SIO specs.
	private float phase = 180;          // wavePhase
	private float carrierFrequency = baseFrequency*2;
	private CASChunk lastChunk = null;
	
	private double markToneDuration = MU / MARK_TONE;  // MARK 	= 5327Hz @ 600 Baud
	private double spaceToneDuration = MU / SPACE_TONE; // SPACE	= 3995Hz @ 600 Baud
	
	private static final double MARK_TONE = 5327;
	private static final double SPACE_TONE = 3995;
	
	private double fudge = 1;
	
	private SampleTable markTone;
	private SampleTable spaceTone;
	
	public AtariTape() {
		// TODO Auto-generated constructor stub
		super();
		VersionMinor = 0;
		VersionMajor = 0;
	}
	
	public byte[] getMAGIC() {
		byte[] magic = Arrays.copyOfRange(Header.toByteArray(), 0, 4);
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
				byte[] magic = "FUJI".getBytes();
				if (Arrays.equals(getMAGIC(),magic)) {
					System.out.println("*** File is a valid Atari/FUJI File by the looks of it.");
					setValid(true);
					setStatus(tapeStatusOk);
				} else {
					//System.out.println("xxx File has unrecognized magic: "+byteArrayToString(getMAGIC()));
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
				'F','U','J','I', 0, 0, 0, 0 };
		return first;
	}
	
	public int headerSize() {
		return 4;
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
	
	public int getWordFromChunk(byte[] data) {
		int b = (data[dataPos] & 0xff) + (256*(data[dataPos+1] & 0xff));
		dataPos += 2;
		return b;
	}
	
	public float getFloatFromChunk(byte[] data) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.mark();
		b.put( Arrays.copyOfRange(data, 0, 3) );
		b.reset();
		return b.getFloat();
	}
	
	public CASChunk getNextChunk(byte[] data) {
		if (!hasData()) {
			return null;
		}
		
		CASChunk chunk = new CASChunk();
		for (int i=0; i<4; i++) {
			chunk.id[i] = (char)getDataByte(data);
		}
		chunk.length = getWordFromChunk(data);
		chunk.aux = getWordFromChunk(data);
		
		chunk.chunkData = new byte[chunk.length];
		
		for (int i=0; i<chunk.length; i++) {
			chunk.chunkData[i] = getDataByte(data);
		}
		
		return chunk;
	}
	
	public void writeAudioStreamData( String path, String base ) {
		IntermediateBlockRepresentation w = new IntermediateBlockRepresentation(path, base);
		
		// init needed sample tables
		markTone = w.new SampleTable(44100, true, 1, MARK_TONE, 0.99);
		spaceTone = w.new SampleTable(44100, true, 1, SPACE_TONE, 0.99);
		
		//Data.reset();
		byte[] raw = Data.toByteArray();
		
		int bytesread = 0;
		double duration = 0;
		double cnv = 1.0;
		
		while (hasData()) {
			CASChunk chunk = getNextChunk(raw);
			//System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" with size "+chunk.chunkData.length+" bytes.");
			
			if (chunk != null) {
				System.out.println(chunk);
			}
			
			try {
				handleChunk(chunk, w);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// store last block for &101
			lastChunk = chunk;
		}
		
		w.done();
		
		//return w;
	}
	
	private double amplitudeAtTime(IntermediateBlockRepresentation w, double freq, double hi, double rest) {
		double pos = w.getDuration();
		double toneDuration = 1000000 / freq;
		double toneDurationHalf = toneDuration / 2;
		
		double remainder = pos % this.spaceToneDuration;
		
		if (remainder < toneDurationHalf) {
			return hi;
		} else {
			return rest;
		}
	}
	
	private void zeroBit(IntermediateBlockRepresentation w) {
		// SPACE bit 3995 Hz
		double bitDuration = 1000000.0 / this.baudRate;
		
		spaceTone.reset();
		
		w.addSampleTableForDuration(spaceTone, bitDuration);
		
		bitCount++;
	}
	
	private void oneBit(IntermediateBlockRepresentation w) {
		// SPACE bit 3995 Hz
		double bitDuration = 1000000 / this.baudRate;
		
		markTone.reset();
		
		w.addSampleTableForDuration(markTone, bitDuration);
		
		bitCount++;
	}
	
	private int triWordFromChunk( CASChunk chunk, int offset ) {
		int lo = chunk.chunkData[0+offset] & 0xFF;
		int hi = chunk.chunkData[1+offset] & 0xFF;
		int vhi = chunk.chunkData[2+offset] & 0xFF;
		
		//System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
		
		return lo + (256*hi) + (65536*vhi);
	}
	
	private int wordFromChunk( CASChunk chunk, int offset ) {
		int lo = chunk.chunkData[0+offset] & 0xFF;
		int hi = chunk.chunkData[1+offset] & 0xFF;
		
		//System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
		
		return lo + (256*hi);
	}
	
	private int wordFromChunk( CASChunk chunk ) {
		return wordFromChunk(chunk,0);
	}
	
	private String byteArrayToString(byte[] bytes) {
		String r = "";
		
		for (byte b: bytes) {
			char c = (char) (b & 0xff);
			r = r + c;
		}
		
		return r;
	}

	private void handleChunk(CASChunk chunk, IntermediateBlockRepresentation w) throws Exception {
		
		if (chunk.recordType().equals("FUJI")) {
			
		}
		else if (chunk.recordType().equals("baud")) {
			this.baudRate = chunk.aux;
			System.out.println("Setting default baudrate to "+Integer.toString(chunk.aux));
		}
		else if (chunk.recordType().equals("data")) {
			handleDATAChunk(chunk, w);
		} else {
			throw new Exception("Unhandled chunk type "+chunk.id[0]+chunk.id[1]+chunk.id[2]+chunk.id[3]);
		}
		
	}
	
	private void handleDATAChunk(CASChunk chunk, IntermediateBlockRepresentation w) {
		/*
			while bytes remain in UEF chunk
			output a zero bit (the start bit)
			read a byte from the UEF chunk, store it to NewByte
			let InternalBitCount = 8
			while InternalBitCount > 0
			output least significant bit of NewByte
			shift NewByte right one position
			decrement InternalBitCount
			output a one bit (the stop bit)
		 */
		
		// add IRG
		double irg = chunk.aux * 1000.0;

		//w.addSilence(irg, PULSE_MIDDLE);
		w.addSampleTableForDuration(markTone, irg);
		
		for (int i=0; i<chunk.chunkData.length; i++) {
			byte b = chunk.chunkData[i];
			this.zeroBit(w);  // start bit
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
			this.oneBit(w); // stop bit
		}
		
		w.addSilence(MU/this.baudRate, PULSE_MIDDLE);
	}

	public boolean isHeaderData() {
		return true;
	}

	@Override
	public String getTapeType() {
		// TODO Auto-generated method stub
		return "FUJI";
	}

	@Override
	public float getRenderPercent() {
		// TODO Auto-generated method stub
		return (float)dataPos / (float)Data.size();
	}

}

