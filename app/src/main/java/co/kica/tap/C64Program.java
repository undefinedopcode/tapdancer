package co.kica.tap;

public class C64Program extends C64Tape {

	private int loadModel = 1;
	private int idx = 0;

	public C64Program() {
		super();
	}
	
	@Override
	public void Load( String filename ) {
		PRGFormat prg = new PRGFormat(filename, idx);
		if (this.loadModel == -1) {
			this.Data = prg.generate();
		} else {
			prg.setTurboMode(this.loadModel);
			this.Data = prg.generateWithTurboTape();
		}
		this.setValid(true);
		this.setStatus(tapeStatusOk);
	}

	public void setLoadModel(int o_type) {
		this.loadModel  = o_type;	
	}
	
	@Override
	public void writeAudioStreamData( String path, String base ) {
		super.writeAudioStreamData( path, base );
		
		IntermediateBlockRepresentation w = new IntermediateBlockRepresentation(path, base);
		w.setLoaderType(this.loadModel);
		w.commit();
	}

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

}
