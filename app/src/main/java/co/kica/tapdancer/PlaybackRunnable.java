package co.kica.tapdancer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import co.kica.tap.IntermediateBlockRepresentation;

public class PlaybackRunnable implements Runnable {
	
	protected int result = Activity.RESULT_CANCELED;
	protected AudioTrack audio;
	public static final int STATE_PLAYING = 1;
	public static final int STATE_INITIALIZED = 2;
	public static final int STATE_PAUSED = 3;
	public static final int STATE_STOPPED = 4;
	
	public static int BUFFER_SIZE = 16;
	public static final int DISK_SIZE = 8192;
	
	protected int minPauseDuration = 8000;
	private byte lastByte = 0x00;
	private int sameCount = 0;
	
	private int state = STATE_INITIALIZED;
	protected int length;
	protected int position;
	
	//protected Intent mIntent;
	protected PlayActivity mActivity;
	private boolean pauseOnLongSilence = true;
	protected int longPauseDuration;
	protected boolean pauseOnSilence;
	protected boolean pauseFirstSilence;
	protected boolean longSilencesOnly;
	private boolean enhancedPauseBehaviour;
	protected IntermediateBlockRepresentation cue;
	private int breakPoint = -1;
	
	private AudioTrack.OnPlaybackPositionUpdateListener posUpdateListener;
	private int savedPlaybackHeadPosition;
	private boolean resumeCurrentBlock;
	private String path;
	private String name;
	private boolean invertWaveform;
	private int renderSampleRate;
	
	public PlaybackRunnable( PlayActivity act, String path, String name ) {
		this.mActivity = act;
		this.path = path;
		this.name = name;
	}

	@Override
	public void run() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.mActivity);
        
        System.err.println(sharedPrefs.getString("prefShortSilenceDuration", "ndef"));
        
        this.minPauseDuration  = Integer.parseInt(sharedPrefs.getString("prefShortSilenceDuration", "8000"));
        this.longPauseDuration = Integer.parseInt(sharedPrefs.getString("prefLongSilenceDuration", "44100"));
        
        this.pauseOnSilence    = sharedPrefs.getBoolean("prefPauseDuringSilence", false);
        this.pauseFirstSilence    = sharedPrefs.getBoolean("prefPauseFirstSilence", false);
        this.longSilencesOnly    = sharedPrefs.getBoolean("prefLongSilencesOnly", false);
        this.resumeCurrentBlock = false;
        
        this.invertWaveform    = sharedPrefs.getBoolean("prefInvertWaveform", false);
        
        this.setEnhancedPauseBehaviour(sharedPrefs.getBoolean("prefPauseNextSilence", false));
		
		
		String path = this.path;
		String name = this.name;
		
		this.cue = new IntermediateBlockRepresentation(path, name); 
		
		Log.i(getClass().getName(), "Playing file "+path+"/"+name);
		
		try {

			// At this point we already have the IBR data available and loaded
			// so we start at block index 1
			
			cue.reset();
			this.length = cue.getLength();
			
			//audio.play();
			setState(STATE_PLAYING);
			
			this.renderSampleRate = cue.getRenderedSampleRate();
			
			// now we fill and write the buffer to the audio track as fast as possible
			this.position = 0;
			
			//byte[] buffer = cue.getCurrentBuffer();
			int chunk = cue.getCurrentBuffer(false).length;
			
			while (chunk > 0 && this.getState() != STATE_STOPPED) {
				
				if (this.isEnhancedPauseBehaviour() && 
						   this.breakPoint != -1 
						   && cue.getPlayingBlock() == this.breakPoint && 
						   getState() == STATE_PLAYING) {
					this.pause();
					this.breakPoint = -1;
				}
				else
				if (this.pauseOnSilence) {
					
					// if playing and we have silence
					if (getState() == STATE_PLAYING && cue.getType().equals("SILENCE") && cue.getPlayingBlock() != 1) {
						
						int minToPause = this.minPauseDuration;
						if (this.longSilencesOnly) 
							minToPause = this.longPauseDuration;
						
						System.out.println("block duration = "+cue.getDuration()+", firstsilence = "+cue.isFirstSilence()+", mintopause = "+minToPause);
						
						if ((cue.getCurrentBuffer(false).length >= minToPause) || (cue.isFirstSilence() && this.pauseFirstSilence)) {
							this.pause();
						}
					} 
					
				}
				
				// if we are paused, pause at this point... 
				if (this.getState() == STATE_PAUSED) {
					this.sendScrollMessage("                   Paused :) ...");
					while (this.getState() == STATE_PAUSED) {
						Thread.sleep(500);
					}
					if (this.getState() == STATE_STOPPED) {
						this.sendScrollMessage("                   Stopped ...");
					}
				}
				
				if (audio != null) {
					audio.stop();
					audio.release();
					audio = null;
				}
				
				audio = new AudioTrack(
						AudioManager.STREAM_MUSIC,
						renderSampleRate,
						AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_8BIT,
						cue.getCurrentBuffer(false).length,
						AudioTrack.MODE_STATIC
				);
				
				this.sendScrollMessage("                   Playing...");
				
				audio.write(cue.getCurrentBuffer(this.invertWaveform), 0, chunk);
				//buffer = null;
				System.gc();
				
				
				if (this.resumeCurrentBlock) {
					// restore saved position
					System.out.println("RESUMING FROM "+this.savedPlaybackHeadPosition);
					audio.setPlaybackHeadPosition(this.savedPlaybackHeadPosition);
					this.resumeCurrentBlock = false;
				}
				audio.play();
				
				// after starting playback wait 500ms
				Thread.sleep(500);
				
				while (audio != null && audio.getPlaybackHeadPosition() < chunk && this.getState() != STATE_STOPPED) {
					this.savedPlaybackHeadPosition = audio.getPlaybackHeadPosition();
					if (this.getState() == STATE_PAUSED) {
						// special ops pause 
						if (audio != null) {
							this.savedPlaybackHeadPosition = audio.getPlaybackHeadPosition();
							this.resumeCurrentBlock = true;
							audio.pause();
							audio.flush();
							audio.release();
							//audio = null;
						}
						break;
					}
					System.out.println("WAITING FOR SAMPLE TO COMPLETE");
					Thread.sleep(500);
					this.updateCounter();
				}
				
				if (this.getState() == STATE_STOPPED) {
					break;
				}
				
				// update offset base point
				if (!resumeCurrentBlock) {
					this.position += chunk;
				
					// send position
					chunk = cue.nextBuffer();
					//cue.getCurrentBuffer();
				}
				
			}
			
			this.sendScrollMessage("                   Stopped...");
			
			//dis.close();
			if (audio != null) {
				audio.stop();
				audio.release();
			}
			setState(STATE_STOPPED);
			
			result = Activity.RESULT_OK;
		} catch (Exception e) {
			Log.w(getClass().getName(), "Exceptions playing wave", e);
		}
		
		this.sendStopMessage();
		cue = null;
		System.gc();
		
		Messenger messenger = mActivity.getMessenger();
		Message msg = Message.obtain();
		msg.arg1 = result;
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
			
	}
	
	public synchronized void stop() {
		setState(STATE_STOPPED);
		if (audio != null && (getState() == STATE_PLAYING || getState() == STATE_PAUSED)) {
			audio.stop();
			audio.release();
			audio = null;
		}
		cue = null;
		System.gc();
	}
	
	public synchronized void pause() {
		//if (audio != null) {
			if (getState() == STATE_PAUSED) {
				//audio.play();
				setState(STATE_PLAYING);
			} else if (getState() == STATE_PLAYING) {
				//audio.pause();
				setState(STATE_PAUSED);
				this.resumeCurrentBlock = true;
				if (audio != null) {
					this.savedPlaybackHeadPosition = audio.getPlaybackHeadPosition();
					audio.pause();
					audio.flush();
					audio.release();
					audio = null;
				}
			}
		//}
	}
	
	public synchronized void play() {
		if (getState() == STATE_PAUSED) {
			setState(STATE_PLAYING);
		} 
	}
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	private int getTrackPosition() {
		return (this.position + this.savedPlaybackHeadPosition) / this.renderSampleRate;
	}	
	
	private int getTrackLength() {
		return this.length / this.renderSampleRate;
	}

	public boolean isEnhancedPauseBehaviour() {
		return enhancedPauseBehaviour;
	}

	public void setEnhancedPauseBehaviour(boolean enhancedPauseBehaviour) {
		this.enhancedPauseBehaviour = enhancedPauseBehaviour;
	}

	public void setPauseBreakpoint() {
		if (cue.blockType(cue.getPlayingBlock()).equals("SILENCE")) {
			breakPoint = cue.getPlayingBlock()+2;
		} else {
			breakPoint = cue.getPlayingBlock()+1;
		}
		System.out.println("SET BREAKY ACHEY POINT INDEX TO "+breakPoint);
	}
	
	protected void sendSilenceMessage() {
		Messenger messenger = mActivity.getMessenger();
		Message msg = Message.obtain();
		msg.arg1 = 99999;
		msg.arg2 = sameCount;
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
	}

	protected void sendStopMessage() {
		Messenger messenger = mActivity.getMessenger();
		Message msg = Message.obtain();
		msg.arg1 = 9999;
		msg.arg2 = 0;
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
	}

	public void updateCounter() {
		Messenger messenger = mActivity.getMessenger();
		Message msg = Message.obtain();
		msg.arg1 = 999;
		msg.arg2 = getTrackLength() - getTrackPosition();
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
	}
	
	public void sendScrollMessage(String scrolly) {
		Messenger messenger = mActivity.getMessenger();
		Message msg = Message.obtain();
		msg.arg1 = 11111;
		msg.arg2 = getTrackLength() - getTrackPosition();
		msg.obj = scrolly;
		try {
			messenger.send(msg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
	}

}
