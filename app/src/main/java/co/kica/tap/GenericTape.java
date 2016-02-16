package co.kica.tap;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import co.kica.fileutils.SmartFile;
import co.kica.fileutils.SmartFileInputStream;

public abstract class GenericTape {
	
	public final static int tapeStatusOk = 0;
	public final static int tapeStatusHeaderInvalid = 2;
	public final static int tapeStatusDataMismatch = 3;
	public final static int tapeStatusWriteError = 4;
	public final static int tapeStatusReadError = 5;
	public final static int tapeStatusBadHeader = 6;
	
	public final static int CHUNK = 4096;
	
	protected String FileName;
	private String HumanName;
	private int Status;
	protected ByteArrayOutputStream Header;
	protected ByteArrayOutputStream Data;
	protected boolean Valid;
	protected int dataPos;
	private boolean inDataStream;
	private int dataStartSamplePos;
	private int dataDurationSamples;
	private int targetSampleRate = 44100;

	public GenericTape() {
		this.Init();
	}
	
	public GenericTape( String fn ) {
		this.Init();
		this.setFileName(fn);
	}
	
	private void Init() {
		this.setFileName("");
		this.setStatus(tapeStatusOk);	
		this.setValid(true);
		Header = new ByteArrayOutputStream();
		Data   = new ByteArrayOutputStream();
		this.inDataStream = false;
		this.dataDurationSamples = 0;
		this.dataStartSamplePos = 0;
	}

	public String getFileName() {
		return FileName;
	}

	public void setFileName(String fileName) {
		FileName = fileName;
	}

	public int getStatus() {
		return Status;
	}

	public void setStatus(int status) {
		Status = status;
	}

	public ByteArrayOutputStream getHeader() {
		return Header;
	}

	public void setHeader(ByteArrayOutputStream header) {
		Header = header;
	}

	public ByteArrayOutputStream getData() {
		return Data;
	}

	public void setData(ByteArrayOutputStream data) {
		Data = data;
	}

	public boolean isValid() {
		return Valid;
	}

	public void setValid(boolean valid) {
		Valid = valid;
	}
	
	public boolean isGZIPed( String fn ) {
		try {
			byte[] buff = new byte[2];
			InputStream is = new SmartFileInputStream( fn );
			int len = is.read(buff);
			//System.out.println(len+": "+buff[0]+","+buff[1]);
			is.close();
			if (buff[0] == 31 && buff[1] == -117) {
				return true;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	public void Load( String fn ) {
		
		this.FileName = fn;
		
		// set human name
		SmartFile f = new SmartFile(fn);
		String tmp = f.getName();
		this.HumanName = tmp.replaceFirst("[.][^.]+$", "");
		this.HumanName = this.HumanName.replaceAll("[()]", "");
		this.HumanName = this.HumanName.replaceAll("[-_]+", " ");
		
		boolean gz = isGZIPed(fn);
		
		//System.out.println("GZIPed == "+gz);
		
		if (gz) {
			LoadGZIP(fn);
			return;
		}
		
		this.setValid(false);
		
		// open the file
		try {
			InputStream is = new SmartFileInputStream( fn );
			
			byte[] buff = new byte[CHUNK];
			
			if (parseHeader(is) == true) {
				
				if (this.isHeaderData()) {
					Data.write( Header.toByteArray() );
				}
				
				int len = is.read(buff);
				while (len > 0) {
					Data.write(buff, 0, len);
					len = is.read(buff);
				}
				is.close();
				System.out.println("*** Read in "+Data.size()+" bytes");
				setValid(true);
			} else {
				setStatus(tapeStatusBadHeader);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void LoadGZIP( String fn) {
		this.setValid(false);
		
		// open the file
		try {
			InputStream is = new GZIPInputStream(new SmartFileInputStream( fn ));
			
			byte[] buff = new byte[CHUNK];
			
			if (parseHeader(is) == true) {
				
				if (this.isHeaderData()) {
					Data.write( Header.toByteArray() );
				}
				
				int len = is.read(buff);
				while (len > 0) {
					Data.write(buff, 0, len);
					len = is.read(buff);
				}
				is.close();
				System.out.println("*** Read in "+Data.size()+" bytes");
				setValid(true);
			} else {
				setStatus(tapeStatusBadHeader);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void Save( String fn ) {
		try {
			FileOutputStream os = new FileOutputStream( fn );
			
			// rebuild header
			while (Data.size() < minPadding()) {
				Data.write(0);
			}
			Header.reset();
			Header.write(buildHeader());
			
			// write header
			Header.writeTo(os);
			
			// write data
			Data.writeTo(os);
			
			os.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void WriteByte( byte b ) {
		Data.write(b);
	}
	
	public abstract boolean parseHeader( InputStream f );
	
	public abstract byte[] buildHeader();
	
	public abstract int headerSize();
	
	public abstract int minPadding();
	
	public abstract byte[] getMAGIC();
	
	public abstract void writeAudioStreamData(String path, String base);
	
	public abstract boolean isHeaderData();
	
	public String digest() {
	        String res = "";
	        try {
	            MessageDigest algorithm = MessageDigest.getInstance("MD5");
	            algorithm.reset();
	            algorithm.update(Data.toByteArray());
	            byte[] md5 = algorithm.digest();
	            String tmp = "";
	            for (int i = 0; i < md5.length; i++) {
	                tmp = (Integer.toHexString(0xFF & md5[i]));
	                if (tmp.length() == 1) {
	                    res += "0" + tmp;
	                } else {
	                    res += tmp;
	                }
	            }
	        } catch (NoSuchAlgorithmException ex) {}
	        return res;
	}
	
	public float getPercent() {
		return 100*(this.dataPos / (float)this.Data.size());
	}
	
	public abstract String getTapeType();
	
	public String getName() {
		return HumanName;
	}

	public abstract float getRenderPercent();
	
	public void addSilence( IntermediateBlockRepresentation w, double duration, double amplitude ) {
		w.setSystem(this.getTapeType());
		w.addSilence(duration, amplitude);
	}
	
	public void addSquareWave( IntermediateBlockRepresentation w, double duration, double ampHi, double ampLo ) {
		w.addSquareWave(duration, ampHi, ampLo);
		w.setSystem(this.getTapeType());
	}

	public int getTargetSampleRate() {
		return targetSampleRate;
	}

	public void setTargetSampleRate(int targetSampleRate) {
		this.targetSampleRate = targetSampleRate;
	}
	
}
