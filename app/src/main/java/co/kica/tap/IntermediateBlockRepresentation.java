package co.kica.tap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/*
 * This class represents a simple container for PCM 8 bit data.
 * Format: 8 bit, unsigned, mono, 44100
 */

public class IntermediateBlockRepresentation {
	
	private String baseName = "cowsarecool";
	private String basePath = ".";
	private String baseExt  = "pcm_u8";
	private int sampleRate = 44100;
	private int bitsPerSample = 8;
	private int channels = 1;
	private int blockIndex = 1;
	private int totalBytes = 0;
	private int startOfBlock = 0;
	private int totalBlocks = 0;
	private int totalData = 0;
	private int totalGap = 0;
	private OGDLDocument manifest = new OGDLDocument();
	private String system = "TAP";
	private BufferedOutputStream blockData = null;
	private byte earLevel = 0; 
	
	private int playingBlock = 1;
	private int playingByteInBlock = 0;
	private byte[] playingBuffer = new byte[0];
	private int bytesWritten;
	private int totalPlayed; 
	
	private double accumulatedTimeClock = 0;
	private double accumulatedTimeSamples = 0;
	
	public class SampleTable {
		
		public double[] values;
		public int sampleRate = 44100;
		public double amp = 0.5;
		public double frequency = 5327;
		public int duration;
		public int index =0;
		
		public SampleTable( int sampleRate, boolean sine, int duration, double frequency, double amp ) {
			values = new double[sampleRate*duration];
			// generate waveform at the given frequency
			this.sampleRate = sampleRate;
			this.duration = duration;
			this.frequency = frequency;
			this.amp = amp;
			
			// generate data
			double samplesPerWave = sampleRate / frequency;
			double halfSamplesPerWave = samplesPerWave / 2.0;
			
			for (int i=0; i<values.length; i++) {
				double remainder = i % samplesPerWave;
				double rval = (remainder / samplesPerWave) * (2*Math.PI);
				if (sine) {
					values[i] = amp*Math.sin(rval);
				} else {
					if (remainder <= halfSamplesPerWave) {
						values[i] = amp;
					}
					else {
						values[i] = -amp;
					}
				}
			}
		}
		
		public double getSample() {
			double v = values[index];
			index = (index + 1) % values.length;
			return v;
		}
		
		public void reset() {
			index = 0;
		}
	}
	
	public void addSampleTableForDuration( SampleTable tbl, double duration ) {
		long samples = Math.round((duration / 1000000.0) * this.sampleRate);
		
		for (long i=0; i<samples; i++) {
			addSample( tbl.getSample() );
		}
	}
	
	public void addSample( double amplitude ) {
		add8Bit( asByte((byte) (amplitude * 127 + 128)) );
	}
	
	public void reset() {
		playingBlock = 1;
		playingByteInBlock = 0;
		playingBuffer = null;
		System.gc();
		playingBuffer = blockData(playingBlock);
		this.totalPlayed = 0;
	}
	
	public byte[] getCurrentBuffer(boolean invertWaveform) {
		if (this.validBlock(playingBlock)) {
			
			if (invertWaveform) {
				for (int i=0; i<playingBuffer.length; i++) {
					int v = 0xff - (playingBuffer[i] & 0xff);
					playingBuffer[i] = (byte)v;
				}
			}
			
			return playingBuffer;
		} else {
			return new byte[0];
		}
	}
	
	public boolean hasBuffer() {
		return (this.validBlock(playingBlock));
	}
	
	public int nextBuffer() {
		totalPlayed += playingBuffer.length;
		playingBlock++;
		if (hasBuffer()) {
			playingBuffer = null;
			System.gc();
			playingBuffer = blockData(playingBlock);
			return playingBuffer.length;
		}
		return 0;
	}
	
	// simulates read from disk but its read from buffer
	public int read( byte[] b ) {
		for (int j=0; j<b.length; j++) {
			b[j] = (byte)(128 & 0xff);
		}
		//System.out.println("*** Block = "+this.playingBlock+", Offset = "+this.playingByteInBlock+", Size = "+playingBuffer.length);
		int bytesAvailable = playingBuffer.length - playingByteInBlock;
		if (bytesAvailable >= b.length) {
			// fill buffer and return
			for (int i=0; i<b.length; i++) {
				b[i] = playingBuffer[playingByteInBlock];
				playingByteInBlock++;
			}
			this.totalPlayed += b.length;
			return b.length;
		}
		else if (bytesAvailable > 0) {
			this.totalPlayed += bytesAvailable;
			for (int i=0; i<bytesAvailable; i++) {
				b[i] = playingBuffer[playingByteInBlock];
				playingByteInBlock++;
			}
			for (int j=0; j<(b.length-bytesAvailable); j++) {
				b[bytesAvailable+j] = (byte)(128 & 0xff);
			}
			return b.length;
		}
		else {
			// in this case we load more data and return silence...
			playingBlock++;
			playingByteInBlock = 0;
			if (validBlock(playingBlock)) {
				playingBuffer = blockData(playingBlock);
				return b.length;	
			} 
		}
		
		return 0;
	}
	
	public int getDuration() {
		return blockDuration(this.playingBlock);
	}
	
	public boolean isFirstSilence() {
		return (getType().equals("SILENCE") && this.playingBlock <= 2);
	}
	
	public int getRemaining() {
		return playingBuffer.length - playingByteInBlock;
	}
	
	public String getType() {
		return blockType(this.playingBlock);
	}
	
	public boolean isStopped() {
		return (!validBlock(playingBlock));
	}
	
	public IntermediateBlockRepresentation( String path, String base ) {
		//blockData.reset();
		this.baseName = base;
		this.basePath = path;
		File f = new File(path+"/"+base+".manifest");
		
		//System.out.println(f.getPath());
		
		if (f.exists()) {
			//System.out.println("Exists");
			this.manifest = OGDLDocument.ReadOGDLFile(f.getPath());
			//this.sampleRate = Integer.parseInt(this.manifest.getValue("Info.SampleRate"));
			//this.manifest.Root().Dump();
		} else {
			this.manifest.setValue("Info.BaseName", this.baseName);
			this.manifest.setValue("Info.BasePath", this.basePath);
			this.manifest.setValue("Info.System", this.system );
			this.manifest.setValue("Info.Extension", this.baseExt);
			this.manifest.setValue("Info.Blocks.Total", "0");
			this.manifest.setValue("Info.Blocks.Data", "0");
			this.manifest.setValue("Info.Blocks.Gap", "0");
			this.manifest.setValue("Info.BitsPerSample", Integer.toString(this.bitsPerSample));
			this.manifest.setValue("Info.SampleRate", Integer.toString(this.sampleRate));
			this.manifest.setValue("Info.Channels", Integer.toString(this.channels));
			// create a file
			this.bytesWritten = 0;
			this.blockData = null;

		}
	}
	
	private short asByte(byte b) {
		return (short)(b & 0xff);
	}
	
	private void add8Bit(short value) {
		
		this.bytesWritten++;
		
		if (blockData == null) {
			try {
				blockData = new BufferedOutputStream( new FileOutputStream(this.basePath+"/"+getCurrentFile()), 32768 );
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			blockData.write(value & 0xff);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int blockSize() {
		return this.bytesWritten;
	}
	
	public void addSquareWave( double duration, double amplitude, double rest_amplitude ) {
		
		if (duration<1) {
			return;
		}
		
		long neededSamples =  Math.round(getSampleRate() * (duration / 1000000));
		long a = Math.round(neededSamples  / 2);
		long b = a;
		
		long i;
		
		// high part
		for (i=0;i<b;i++) {
			add8Bit( asByte((byte)(rest_amplitude * 127 + 128)) );
		}
		
		// low part
		for (i=0;i<a;i++) {
			add8Bit( asByte((byte) (amplitude * 127 + 128)) );
		}
		
		totalBytes += neededSamples;
	}
	
	public void addPauseOld( double duration, double amplitude ) {
		
		if (duration < 1000.0) {
			return;
		}
		
		addPulse( 1000.0, amplitude );
		this.earLevel = 0; // low
		
		this.addPulse(duration-1000.0, amplitude);
		
		this.earLevel = 0;
		
		this.accumulatedTimeClock = 0;
		this.accumulatedTimeSamples = 0;
	}
	
	public void addPause( double duration, double amplitude ) {
		
		if (duration < 1000.0) {
			return;
		}
		
		this.earLevel = 0; // low
		addPulseFlat( 1000.0, amplitude );
		
		/*double fillsamples = 3000;
		double val = 1.0/fillsamples;
		double v = -1.0;
		while (Math.abs(v) > 0.0001) {
			add8Bit( asByte((byte)(v * 127 + 128)) );
			v += val;
			val = val*1.0001;
		}*/
		
		this.addSilence(duration-1000.0, amplitude);
		
		this.earLevel = 0;
		
		this.accumulatedTimeClock = 0;
		this.accumulatedTimeSamples = 0;
	}
	
	public void addPulseFlat( double duration, double amplitude ) {
		
		double rest_amplitude = amplitude;
		
		if (this.earLevel == 1) {
			rest_amplitude = amplitude * -1.0;
		}
		
		if (duration<1) {
			return;
		}
		
		long neededSamples =  Math.round(getSampleRate() * (duration / 1000000.0));
		//System.out.println("needed samples = "+neededSamples);
		double skewval = this.accumulatedTimeClock - this.accumulatedTimeSamples;
		double onesample = 1000000.0 / (double)getSampleRate();
		
		// check if we need to either ADD or SUBTRACT samples to keep timing
		if (Math.abs(skewval) > onesample) {
			long fillSamples = Math.round(skewval / onesample);
			//System.out.println("*** Adjusted by LEAP SAMPLES ("+fillSamples+")");
			neededSamples += fillSamples;
		}
		
		// high part
		double amp = 0;
		double gradient = 0;
		for (long i=0;i<neededSamples;i++) {
			amp = rest_amplitude * (1-(gradient*i));
			add8Bit( asByte((byte)(amp * 127 + 128)) );
		}
		
		totalBytes += neededSamples;
		
		/* adjust clocks */
		this.accumulatedTimeSamples += neededSamples * (1000000.0 / (double)getSampleRate());
		this.accumulatedTimeClock += duration;
		
		//System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");
		
		/* invert the pulse at the end */
		this.earLevel = (byte) ((this.earLevel+1) & 1);
	}
	
	public void addPulse( double duration, double amplitude ) {
		
		double rest_amplitude = amplitude;
		
		if (this.earLevel == 1) {
			rest_amplitude = amplitude * -1.0;
		}
		
		if (duration<1) {
			return;
		}
		
		long neededSamples =  Math.round(getSampleRate() * (duration / 1000000.0));
		//System.out.println("needed samples = "+neededSamples);
		double skewval = this.accumulatedTimeClock - this.accumulatedTimeSamples;
		double onesample = 1000000.0 / (double)getSampleRate();
		
		// check if we need to either ADD or SUBTRACT samples to keep timing
		if (Math.abs(skewval) > onesample) {
			long fillSamples = Math.round(skewval / onesample);
			//System.out.println("*** Adjusted by LEAP SAMPLES ("+fillSamples+")");
			neededSamples += fillSamples;
		}
		
		// high part
		double amp = 0;
		//double gradient = 0.08 / (double)neededSamples;
		double gradient = 0;
		for (long i=0;i<neededSamples;i++) {
			amp = rest_amplitude * (1-(gradient*i));
			add8Bit( asByte((byte)(amp * 127 + 128)) );
		}
		
		totalBytes += neededSamples;
		
		/* adjust clocks */
		this.accumulatedTimeSamples += neededSamples * (1000000.0 / (double)getSampleRate());
		this.accumulatedTimeClock += duration;
		
		//System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");
		
		/* invert the pulse at the end */
		this.earLevel = (byte) ((this.earLevel+1) & 1);
	}
	
	
	public void writeSamples( long neededSamples, byte eLevel, double amplitude ) {
		
		double rest_amplitude = amplitude;
		this.earLevel = eLevel;
		
		if (this.earLevel == 1) {
			rest_amplitude = amplitude * -1.0;
		}
		
		// high part
		double amp = 0;
		//double gradient = 0.08 / (double)neededSamples;
		double gradient = 0;
		for (long i=0;i<neededSamples;i++) {
			amp = rest_amplitude * (1-(gradient*i));
			add8Bit( asByte((byte)(amp * 127 + 128)) );
		}
		
		totalBytes += neededSamples;
		
		//System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");
		
		/* invert the pulse at the end */
		this.earLevel = (byte) ((this.earLevel+1) & 1);
	}
	
	public void addSilence( double duration, double amplitude ) {
		
		flushChunkIfNeeded();
		
		if (duration<1) {
			return;
		}
		
		long neededSamples = Math.round((long)getSampleRate() * (duration / 1000000));
		
		long i;
		
		//for (i=0;i<neededSamples;i++) {
		//	add8Bit( asByte((byte)128) );
		//}
		
		totalBytes += neededSamples;
		
		// add entry to manifest
		this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Type", "SILENCE");
		this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Duration", Long.toString(neededSamples) );
		this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Start", Integer.toString(this.startOfBlock));
		
		startOfBlock = totalBytes;
		
		blockIndex++;
		//this.blockData.reset();
		
		totalGap++;
		totalBlocks++;
	}

	private void flushChunkIfNeeded() {
		// if we have any pcm data, write it out to a file
		if (this.blockSize() > 0) {

			try {
				// close current file
				blockData.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// add entry to manifest
			this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Source", getCurrentFile());
			this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Type", "DATA");
			this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Duration", Integer.toString(bytesWritten) );
			this.manifest.setValue("Data."+Integer.toString(this.blockIndex)+".Start", Integer.toString(this.startOfBlock));
			
			// now reset for next block
			blockIndex++;
			
			startOfBlock = totalBytes;
			totalData++;
			totalBlocks++;
			bytesWritten = 0;
			blockData = null;
		}
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public int getChannels() {
		return channels;
	}
	
	public String getCurrentFile() {
		return this.baseName + "_"+Integer.toString(this.blockIndex)+"."+this.baseExt;
	}
	
	public String getManifestName() {
		return this.basePath + "/" + this.baseName + ".manifest";
	}

	protected void finalize() throws Throwable
	{
		flushChunkIfNeeded();
		super.finalize();
	}
	
	public void done() {
		flushChunkIfNeeded();
		writeMeta();
	}
	
	public void commit() {
		OGDLDocument.WriteOGDLFile(this.getManifestName(), this.manifest);
	}

	private void writeMeta() {
		this.manifest.setValue("Info.Blocks.Total", Integer.toString(this.totalBlocks));
		this.manifest.setValue("Info.Blocks.Data", Integer.toString(this.totalData));
		this.manifest.setValue("Info.Blocks.Gap", Integer.toString(this.totalGap));
		this.manifest.setValue("Info.SampleRate", Integer.toString(this.sampleRate));
		this.manifest.setValue("Info.System", this.system );
		
		OGDLDocument.WriteOGDLFile(this.getManifestName(), this.manifest);
	}

	public String getSystem() {
		return this.manifest.getValue("Info.System");
	}

	public void setSystem(String system) {
		this.system = system;
		this.manifest.setValue("Info.System", system);
	}

	public String getBaseName() {
		return this.manifest.getValue("Info.BaseName");
	}

	public void setBaseName(String baseName) {
		this.baseName = baseName;
		this.manifest.setValue("Info.BaseName", baseName);
	}

	public String getBasePath() {
		return this.manifest.getValue("Info.BasePath");
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
		this.manifest.setValue("Info.BasePath", basePath);
	}

	public String getBaseExt() {
		return this.manifest.getValue("Info.Extension");
	}

	public void setBaseExt(String baseExt) {
		this.baseExt = baseExt;
		this.manifest.setValue("Info.Extension", baseExt);
	}

	public BufferedOutputStream getBlockData() {
		return blockData;
	}

	public void setBlockData(BufferedOutputStream blockData) {
		this.blockData = blockData;
	}

	public int getBlockIndex() {
		return blockIndex;
	}

	public void setBlockIndex(int blockIndex) {
		this.blockIndex = blockIndex;
	}

	public int getTotalBytes() {
		return totalBytes;
	}

	public void setTotalBytes(int totalBytes) {
		this.totalBytes = totalBytes;
	}

	public int getStartOfBlock() {
		return startOfBlock;
	}

	public void setStartOfBlock(int startOfBlock) {
		this.startOfBlock = startOfBlock;
	}

	public int getTotalBlocks() {
		return Integer.parseInt(this.manifest.getValue("Info.Blocks.Total"));
	}

	public void setTotalBlocks(int totalBlocks) {
		this.totalBlocks = totalBlocks;
		this.manifest.setValue("Info.Blocks.Total", Integer.toString(totalBlocks));
	}

	public int getTotalData() {
		return Integer.parseInt(this.manifest.getValue("Info.Blocks.Data"));
	}

	public void setTotalData(int totalData) {
		this.totalData = totalData;
		this.manifest.setValue("Info.Blocks.Data", Integer.toString(totalData));
	}

	public int getTotalGap() {
		return Integer.parseInt(this.manifest.getValue("Info.Blocks.Gap"));
	}

	public void setTotalGap(int totalGap) {
		this.totalGap = totalGap;
		this.manifest.setValue("Info.Blocks.Gap", Integer.toString(totalGap));
	}

	public OGDLDocument getManifest() {
		return manifest;
	}
	
	// block specific accessors
	public boolean validBlock(int index) {
		return (index >= 1 && index <= this.getTotalBlocks());
	}
	
	public int blockDuration( int index ) {
		if (validBlock(index)) {
			return Integer.parseInt(this.manifest.getValue("Data."+Integer.toString(index)+".Duration"));
		}
		return 0;
	}

	public int blockStart( int index ) {
		if (validBlock(index)) {
			return Integer.parseInt(this.manifest.getValue("Data."+Integer.toString(index)+".Start"));
		}
		return 0;
	}
	
	public String blockSource( int index ) {
		if (validBlock(index)) {
			return this.getBasePath()+"/"+this.manifest.getValue("Data."+Integer.toString(index)+".Source");
		}
		return "";
	}
	
	public String blockType( int index ) {
		if (validBlock(index)) {
			return this.manifest.getValue("Data."+Integer.toString(index)+".Type");
		}
		return "INVALID";
	}
	
	public byte[] blockData( int index ) {
		if (validBlock(index)) {
			String type = this.blockType(index);
			if (type.equals("DATA")) {
				return this.blockSourceLoad(blockSource(index));
			}
			else
			if (type.equals("SILENCE")) {
				return this.blockSourceGenerate(blockDuration(index));
			}
		}
		return new byte[0];
	}

	private byte[] blockSourceGenerate(int blockDuration) {
		this.playingBuffer = null;
		System.gc();
		this.playingBuffer = new byte[blockDuration];
		for (int i=0; i<this.playingBuffer.length; i++)
			playingBuffer[i] = (byte) (128 & 0xff);
		return this.playingBuffer;
	}

	private byte[] blockSourceLoad(String blockSource) {
		try {
			File f = new File(blockSource);
			FileInputStream fis = new FileInputStream(f);
			this.playingBuffer = null;
			System.gc();
			this.playingBuffer = new byte[(int) f.length()];
			int x = fis.read(this.playingBuffer);
			fis.close();
			return this.playingBuffer;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}
	
	public int getLength() {
		int res = 0;
		for (int i=0; i<=this.getTotalBlocks(); i++) {
			res += this.blockDuration(i);
		}
		return res;
	}
	
	public int getPlayed() {
		return this.totalPlayed;
	}

	public int getPlayingBlock() {
		// TODO Auto-generated method stub
		return this.playingBlock;
	}

	public int getNextSilence() {
		/*
		 * Returns the block index of the next silence AFTER the current block
		 */
		
		for (int i=playingBlock; i<=this.totalBlocks; i++) {
			if (blockType(i).equals("SILENCE")) 
				return i;
		}
		
		return -1;
	}
	
	public void setLoaderType( int model ) {
		this.manifest.setValue("Info.Loader.Model", Integer.toString(model));
	}
	
	public int getLoaderType() {
		String s = this.manifest.getValue("Info.Loader.Model");
		if (s == null || s.equals("")) {
			s = "-1";
		}
		return Integer.parseInt(s);
	}

	public byte getEarLevel() {
		return earLevel;
	}

	public void setEarLevel(byte earLevel) {
		this.earLevel = earLevel;
	}
	
	public void toRawAudio(String filename) {
		this.reset();
		//byte[] buff = new byte[1024];
		
		try {
			FileOutputStream fos = new FileOutputStream( new File(filename) );
			byte[] buff = this.getCurrentBuffer(false);
			while (buff.length > 0) {
				fos.write(buff);
				int i = this.nextBuffer();
				buff = this.getCurrentBuffer(false);
			}
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	public int getRenderedSampleRate() {
		int v = 44100;
		String s = this.manifest.getValue("Info.SampleRate");
		if (s != null && s.length() > 0) {
			v = Integer.parseInt(s);
		}
		return v;
	}
	
}
