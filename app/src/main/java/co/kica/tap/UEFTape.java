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

public class UEFTape extends GenericTape {
	
	public class UEFChunk {
		public int id = 0;
		public byte[] chunkData;
	}
	
	private byte VersionMinor;
	private byte VersionMajor;
	public static final double PULSE_AMPLITUDE = -0.99;
	public static final double PULSE_MIDDLE = 0.99;
	public static final double PULSE_REST = 0.99;
	public static final double PAL_CLK = 985248;
	public static final double MU = 1000000;
	private int dataPos = 0;
	
	private float baseFrequency = 1200; // Hz
	private float baudRate = 1200;      // Baud rate
	private float phase = 180;          // wavePhase
	private float carrierFrequency = baseFrequency*2;
	private UEFChunk lastChunk = null;
	
	private double fudge = 1;
	
	public UEFTape() {
		// TODO Auto-generated constructor stub
		super();
		VersionMinor = 10;
		VersionMajor = 0;
	}
	
	public byte[] getMAGIC() {
		byte[] magic = Arrays.copyOfRange(Header.toByteArray(), 0, 10);
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
				byte[] magic = "UEF File!\0".getBytes();
				if (Arrays.equals(getMAGIC(),magic)) {
					System.out.println("*** File is a valid UEF by the looks of it.");
					setValid(true);
					setVersionMinor(buff[10]);
					setVersionMajor(buff[11]);
					System.out.println("*** Header says version is "+getVersionMajor()+"."+getVersionMinor());
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
				'U','E','F',' ','F','i','l','e','!',0,
				getVersionMinor(),
				getVersionMajor()};
		return first;
	}
	
	public int headerSize() {
		return 12;
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
	
	public int getSizeFromChunk(byte[] data) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.mark();
		b.put( Arrays.copyOfRange(data, dataPos, dataPos+3) );
		b.reset();
		dataPos += 4;
		return b.getInt();
	}
	
	public float getFloatFromChunk(byte[] data) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.mark();
		b.put( Arrays.copyOfRange(data, 0, 3) );
		b.reset();
		return b.getFloat();
	}
	
	public UEFChunk getNextChunk(byte[] data) {
		if (!hasData()) {
			return null;
		}
		
		UEFChunk chunk = new UEFChunk();
		chunk.id = getDataByte(data) + (256*getDataByte(data));
		int size = getSizeFromChunk(data);
		
		chunk.chunkData = new byte[size];
		
		for (int i=0; i<size; i++) {
			chunk.chunkData[i] = getDataByte(data);
		}
		
		return chunk;
	}
	
	public void writeAudioStreamData( String path, String base ) {
		IntermediateBlockRepresentation w = new IntermediateBlockRepresentation(path, base);
		
		//Data.reset();
		byte[] raw = Data.toByteArray();
		
		int bytesread = 0;
		double duration = 0;
		double cnv = 1.0;
		
		while (hasData()) {
			UEFChunk chunk = getNextChunk(raw);
			//System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" with size "+chunk.chunkData.length+" bytes.");
			
			try {
				handleChunk(chunk, w);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// store last block for &101
			lastChunk = chunk;
		}
		
		// do cue
		w.done();
		
		//return w;
	}
	
	private void zeroBit(IntermediateBlockRepresentation w) {
		if (baudRate == 1200) {
			// 1 cycle high low at base frequency
			double cycleduration = 1000000.0 / baseFrequency;
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			//System.out.print("0");
		}
		else if (baudRate == 300) {
			// 4 cycles high low at base frequency
			double cycleduration = 1000000.0 / baseFrequency;
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			//System.out.print("0");
		}
	}
	
	private void oneBit(IntermediateBlockRepresentation w) {
		if (baudRate == 1200) {
			// 2 cycle high low at base frequency
			double cycleduration = 1000000.0 / (2*baseFrequency);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			//System.out.print("1");
		}
		else if (baudRate == 300) {
			// 8 cycles high low at base frequency
			double cycleduration = 1000000.0 / (2*baseFrequency);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
			//System.out.print("1");
		}
	}
	
	private void handleChunk0100(UEFChunk chunk, IntermediateBlockRepresentation w) {
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
		
		for (int i=0; i<chunk.chunkData.length; i++) {
			byte b = chunk.chunkData[i];
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
			this.oneBit(w);
		}
	}
	
	private int triWordFromChunk( UEFChunk chunk, int offset ) {
		int lo = chunk.chunkData[0+offset] & 0xFF;
		int hi = chunk.chunkData[1+offset] & 0xFF;
		int vhi = chunk.chunkData[2+offset] & 0xFF;
		
		//System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
		
		return lo + (256*hi) + (65536*vhi);
	}
	
	private int wordFromChunk( UEFChunk chunk, int offset ) {
		int lo = chunk.chunkData[0+offset] & 0xFF;
		int hi = chunk.chunkData[1+offset] & 0xFF;
		
		//System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
		
		return lo + (256*hi);
	}
	
	private int wordFromChunk( UEFChunk chunk ) {
		return wordFromChunk(chunk,0);
	}
	
	private void handleChunk0110(UEFChunk chunk, IntermediateBlockRepresentation w) {
		int cycles = wordFromChunk(chunk);
		System.out.println("CARRIER TONE "+cycles+" cycles...");
		
		double cycleduration = 1000000.0 / carrierFrequency;
		for (int i=0; i<cycles; i++) {
			w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST);
		}
	}
	
	private void handleChunk0111(UEFChunk chunk, IntermediateBlockRepresentation w) {
		int cycles_before = wordFromChunk(chunk,0);
		int cycles_after  = wordFromChunk(chunk,2);
		int dummy = 0xaa;
		
		System.out.println("CARRIER TONE WITH DUMMY BYTE...");
		
		// lead carrier
		double cycleduration_before = 1000000.0 / carrierFrequency;
		for (int i=0; i<cycles_before; i++) {
			w.addSquareWave(cycleduration_before, PULSE_AMPLITUDE, PULSE_REST);
		}
		
		// dummy byte
		// start bit
		zeroBit(w);
		
		// byte 0xaa = 0b10101010
		oneBit(w);
		zeroBit(w);
		oneBit(w);
		zeroBit(w);
		oneBit(w);
		zeroBit(w);
		oneBit(w);
		zeroBit(w);		
		
		// stop bit
		oneBit(w);
		
		// trailing carrier
		double cycleduration_after = 1000000.0 / carrierFrequency;
		for (int i=0; i<cycles_after; i++) {
			w.addSquareWave(cycleduration_after, PULSE_AMPLITUDE, PULSE_REST);
		}
	}
	
	private void handleChunk0112(UEFChunk chunk, IntermediateBlockRepresentation w) {
		int cycles = wordFromChunk(chunk);
		
		double cycleduration = (1000000.0 * cycles) / baseFrequency;
		w.addSilence(cycleduration, PULSE_REST);
		
		System.out.println("SILENCE "+cycles+" cycles...");
	}
	
	private void handleChunk0117(UEFChunk chunk, IntermediateBlockRepresentation w) {
		int cycles = wordFromChunk(chunk);
		
		this.baudRate = cycles;
	}
	
	public static double ldexp(double v, int w) {
		return v * Math.pow(2.0, w);
	}
	
	private float floatFromChunk( UEFChunk chunk ) {
		/* assume a four byte array named Float exists, where Float[0]
		was the first byte read from the UEF, Float[1] the second, etc */

		int[] Float = new int[4];
		Float[0] = chunk.chunkData[0] & 0xFF;
		Float[1] = chunk.chunkData[1] & 0xFF;
		Float[2] = chunk.chunkData[2] & 0xFF;
		Float[3] = chunk.chunkData[3] & 0xFF;
		
		/* decode mantissa */
		int Mantissa;
		Mantissa = Float[0] | (Float[1] << 8) | ((Float[2]&0x7f)|0x80) << 16;

		float Result = (float)Mantissa;
		Result = (float)ldexp(Result, -23);

		/* decode exponent */
		int Exponent;
		Exponent = ((Float[2]&0x80) >> 7) | (Float[3]&0x7f) << 1;
		Exponent -= 127;
		Result = (float)ldexp(Result, Exponent);

		/* flip sign if necessary */
		if((Float[3]&0x80) > 0)
			Result = -Result;

		/* floating point number is now in 'Result' */
		return Result;
	}
	
	private String byteArrayToString(byte[] bytes) {
		String r = "";
		
		for (byte b: bytes) {
			char c = (char) (b & 0xff);
			r = r + c;
		}
		
		return r;
	}

	private void handleChunk(UEFChunk chunk, IntermediateBlockRepresentation w) throws Exception {
		// TODO Auto-generated method stub
		switch (chunk.id) {
		case 0x0000:	System.out.println("SOURCE: ["+byteArrayToString(chunk.chunkData)+"]"); break;
		case 0x0100: 	handleChunk0100(chunk,w); break;
		case 0x0101:	handleChunk0101(chunk,w); break;
		case 0x0102:	handleChunk0102(chunk,w); break;
		case 0x0104:	handleChunk0104(chunk,w); break;
		case 0x0110: 	handleChunk0110(chunk,w); break;
		case 0x0111:	handleChunk0111(chunk,w); break;
		case 0x0112: 	handleChunk0112(chunk,w); break;
		case 0x0113: 	handleChunk0113(chunk,w); break;
		case 0x0114:	handleChunk0114(chunk,w); break;
		case 0x0115:	handleChunk0115(chunk,w); break;
		case 0x0116:	handleChunk0116(chunk,w); break;
		case 0x0117:	handleChunk0117(chunk,w); break;
		default: throw new Exception("Unhandled chunk type "+Integer.toHexString(chunk.id));
		}
	}

	private void handleChunk0115(UEFChunk chunk, IntermediateBlockRepresentation w) {
		// TODO Auto-generated method stub
		
	}

	private void handleChunk0116(UEFChunk chunk, IntermediateBlockRepresentation w) {
		float f = floatFromChunk(chunk);
		double cycleduration = 1000000.0 * f;
		w.addSilence(cycleduration, PULSE_REST);
	}

	private void handleChunk0113(UEFChunk chunk, IntermediateBlockRepresentation w) {	
		float f = floatFromChunk(chunk);
		System.out.println("Changing frequency to "+f+"Hz");		
		baseFrequency = f;	
	}
	
	private void handleChunk0101(UEFChunk chunk, IntermediateBlockRepresentation w) {	
		if (lastChunk == null || (lastChunk.id != 0x0100 && lastChunk.id != 0x0102) ) {
			return;
		}
		
		if (lastChunk.id == 0x0100) 
			handleChunk0100(lastChunk, w);
		if (lastChunk.id == 0x0102)
			handleChunk0102(lastChunk, w);
	}
	
	private void handleChunk0102(UEFChunk chunk, IntermediateBlockRepresentation w) {
		int bitCount = (chunk.chunkData.length * 8) - (chunk.chunkData[0] & 0xFF);
		int byteIndex = 0;
		byte byteValue = 0;
		for (int currentBit = 0; currentBit < bitCount; currentBit++) {
			if (currentBit % 8 == 0) {
				// new byte
				byteValue = chunk.chunkData[byteIndex];
				byteIndex++;
			}
			
			if ((byteValue & 1) == 1) {
				oneBit(w);
			} else {
				zeroBit(w);
			}
			
			byteValue = (byte) (byteValue >> 1);
		}
	}
	
	private void handleChunk0114(UEFChunk chunk, IntermediateBlockRepresentation w) {
		/* TODO: implement security cycles */
		int numCycles = triWordFromChunk( chunk, 0 );
	}

	private void handleChunk0104(UEFChunk chunk, IntermediateBlockRepresentation w) {
		 int bitsPerPacket 	= chunk.chunkData[0] & 0xFF;
		 char parity 		= (char)(chunk.chunkData[1] & 0xFF); 
		 int stopBitCount	= chunk.chunkData[0];
		 boolean needsExtra = (stopBitCount < 0);
		 int bytesRemaining = chunk.chunkData.length-3;
		 for (int bytePos=0; bytePos<bytesRemaining; bytePos++) {
			 // start bit
			 zeroBit(w);
			 // get new byte
			 byte newByte = chunk.chunkData[3+bytePos];
			 int internalBitCount = bitsPerPacket;
			 int oneCount = 0;
			 while (internalBitCount > 0) {
				 if ((newByte & 1) == 1) {
					 oneBit(w);
					 oneCount++;
				 } else {
					 zeroBit(w);
				 }
				 newByte = (byte)(newByte >> 1);
				 internalBitCount--;
			 }
			 // output parity bit if needed
			 if (parity == 'O') {
				 if ((oneCount % 1) == 0) {
					 oneBit(w);
				 } else {
					 zeroBit(w);
				 }
			 }
			 if (parity == 'E') {
				 if ((oneCount % 1) == 0) {
					 zeroBit(w);
				 } else {
					 oneBit(w);
				 }
			 }
			 // now stop bits
			 int internalStopCount = stopBitCount;
			 while (internalStopCount > 0) {
				 oneBit(w);
				 internalStopCount--;
			 }
			 // 
			 if (needsExtra) {
				 double cycleduration = 1000000.0 / carrierFrequency;
				 w.addSquareWave(cycleduration, PULSE_REST, PULSE_AMPLITUDE);
			 }
		 }
	}
	
	public boolean isHeaderData() {
		return false;
	}

	@Override
	public String getTapeType() {
		// TODO Auto-generated method stub
		return "UEF";
	}

	@Override
	public float getRenderPercent() {
		// TODO Auto-generated method stub
		return (float)dataPos / (float)Data.size();
	}

}

