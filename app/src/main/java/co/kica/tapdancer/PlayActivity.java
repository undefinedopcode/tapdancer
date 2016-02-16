package co.kica.tapdancer;

import java.io.File;

import co.kica.tap.IntermediateBlockRepresentation;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class PlayActivity extends Activity {
	
	private static final int RESULT_SETTINGS = 0;
	private AudioTrack audio; 
	private String currentfile = "";
	private String currentName = "";
	private String currentPath = "";
	private Intent pintent;
	private PlaybackRunnable mBoundService;
	private Thread task;
	//private boolean mBound = false; 
	private int state = 0;
	
	private void showAiplaneSuggestion() {
		
		// do the check in here
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean dontShowMessage = (boolean) prefs.getBoolean("dontShowAirplane", false);
		final Editor edit = prefs.edit();
		
		if (dontShowMessage) {
			return;
		}
		
		// custom dialog
		Context context = this;
		final Dialog dialog = new Dialog(context);
		dialog.setContentView(R.layout.airplanmode);
		dialog.setTitle("Reducing Noise");
		
		final CheckBox cb = (CheckBox)dialog.findViewById(R.id.dontShowWarningAgain);

		Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOk);
		// if button is clicked, close the custom dialog
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (cb == null) {
					System.out.println("CheckBox cb is NULL!!!!");
				}
				
				// save value
				edit.putBoolean("dontShowAirplane", cb.isChecked());
				edit.commit();
				
				dialog.dismiss();
			}
		});

		dialog.show();
	}
	
	private void initFromPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		int volume_level = (int) prefs.getInt("tap.volume", -1);
		if (volume_level != -1) {
			// restore volume level
			manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume_level, 0);
		}
	}
	
	private void saveToPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		int volume_level = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
		Editor edit = prefs.edit();
		edit.putInt("tap.volume", volume_level);
		edit.commit();
	}
	
	public boolean mBound() {
		return (task != null && task.isAlive());
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			if (message.arg1 == Activity.RESULT_OK) {
				Object outputpath = message.obj;
			}
			else if (message.arg1 == 999) {
				// do counter update
				PlayActivity.this.updatedCounter = message.arg2;
			}
			else if (message.arg1 == 9999) {
				// do stop
				PlayActivity.this.clickStopFile(surface);
			}
			else if (message.arg1 == 99999) {
				// do stop
				//PlayActivity.this.clickPauseFile(surface);
				if (PlayActivity.this.mBound()) {
					mBoundService.pause();
					refreshState();
					if (mBoundService.getState() == PlaybackRunnable.STATE_PAUSED) {
						surface.setScrolly("               Paused...       ");
					} else {
						
						surface.setScrolly("               Playing...       ");
					}
				}
			}
			else if (message.arg1 == 11111) {
				// do stop
				//PlayActivity.this.clickPauseFile(surface);
				surface.setScrolly((String)message.obj);
			}
		};
	};
	
	private PlayerSurface surface;
	private int counterLength;
	private int updatedCounter = 0;
	private Messenger messenger;
	
	public void refreshState() {
		
		if (currentfile.equals("")) {
			surface.inserted = false;
		} else {
			surface.inserted = true;
		}
        
	}
	
	@Override 
	protected void onNewIntent(Intent intent) {
		currentfile = intent.getStringExtra("wavfile");		
		if (currentfile == null) {
			currentfile = "";
			surface.setScrolly("               No tape loaded. Press EJECT to load a tape... ");
		} else {
			// we have a file... 
			
			String[] parts = currentfile.split("[:]");
			this.currentPath = parts[0];
			this.currentName = parts[1];
			
			//File z = new File(currentfile);
			//this.counterLength = (int)((z.length() - 44) / 44100);
			this.updateCountPos();
			
			surface.setScrolly("               Ready.  PRESS PLAY ON TAPE ...");
		}
		refreshState();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_player);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		surface = new PlayerSurface(this, this);
		setContentView(surface);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		this.initFromPreferences();
		
		Intent intent = getIntent();
		currentfile = intent.getStringExtra("wavfile");		
		if (currentfile == null) {
			currentfile = "";
		} else {
			// we have a file... 
			String[] parts = currentfile.split("[:]");
			this.currentPath = parts[0];
			this.currentName = parts[1];
			this.updateCountPos();
			surface.setScrolly("               Ready.  PRESS PLAY ON TAPE ...");
		}
		refreshState();
		
		showAiplaneSuggestion();
		
		Runtime rt = Runtime.getRuntime();
		long maxMemory = rt.maxMemory();
		Log.v("onCreate", "maxMemory:" + Long.toString(maxMemory));
	}
	
	private void updateCountPos() {
		if (this.currentfile.equals("")) {
			this.counterLength = 0;
			return;
		}
		
		IntermediateBlockRepresentation temp = new IntermediateBlockRepresentation(this.currentPath,this.currentName);
		this.counterLength = temp.getLength() / temp.getRenderedSampleRate();
	}

	@Override
	protected void onStop() {
	    if (this.mBound()) {
	    	mBoundService.stop();
	    	//unbindService(mConnection);
	    	freeService();
	    }
		saveToPreferences();
		super.onStop();
		System.gc();
	}
	
	public void clickChooseFile( View view ) {
		// start the file picker activity
		if (task != null && task.isAlive()) {
			clickStopFile( view );
		}		
		currentfile = "";
		surface.inserted = false;
		surface.setScrolly("                       No tape loaded. Press EJECT to load a tape...                     ");
		Intent intent = new Intent(this, FileChooser.class);
		startActivity( intent );
	}
	
	public void clickHandleTape( View view ) {
		// moo
	}
	
	public void clickPlayFile(View view) {
		
		if (currentfile.equals("")) 
			return;
		
		if (this.mBound() && mBoundService.getState() == PlaybackRunnable.STATE_PAUSED) {
			//System.out.println("Shit we must be confused...");
			clickPauseFile(view);
			return;
		}
		
		if (this.mBound() && mBoundService.getState() == PlaybackRunnable.STATE_PLAYING) {
			//System.out.println("Shit we must be confused...");
			clickPauseFile(view);
			return;
		}
		
        this.messenger = new Messenger(handler);
        
        // cleanup if needed 
		if (this.mBound()) {
			mBoundService.pause();
			mBoundService.stop();
			this.task.interrupt();
			freeService();
		}
        
		this.mBoundService = new PlaybackRunnable( this, currentPath, currentName );
		this.task = new Thread( this.mBoundService );
		this.task.start();
        
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        refreshState();
        //surface.scrolly = "               Playing...       ";
	}
	
	public void clickPauseFile(View view) {
		if (currentfile.equals("")) 
			return;
		
		//bindService();
		if (this.mBound()) {
			if (mBoundService.isEnhancedPauseBehaviour() && mBoundService.getState() == PlaybackRunnable.STATE_PLAYING) {
				System.out.println("SET BREAKPOINT");
				mBoundService.setPauseBreakpoint();
			} else {
				System.out.println("ACTUAL PAUSE");
				mBoundService.pause();
			}
			refreshState();
		}
	}
	
	private void freeService() {
		task = null;
		mBoundService = null;
	}
	
	public void clickStopFile( View view ) {
		if (currentfile.equals("")) 
			return;
		
		if (this.mBound()) {
			mBoundService.pause();
			mBoundService.stop();
			freeService();
			refreshState();
			surface.setScrolly("               Stopped...       ");
		}
		
		surface.setScrolly("               Stopped...       ");
	}
	
	/* service handling */
	   @Override
	   protected void onResume() {
	    // TODO Auto-generated method stub
	    super.onResume();
	    surface.onResumeMySurfaceView();
	    initFromPreferences();
	   }
	   
	   @Override
	   protected void onPause() {
	    clickStopFile(surface);
	    super.onPause();
	    surface.onPauseMySurfaceView();
	    saveToPreferences();
	   }
	   
	   public int getCounterPos() {
		   	if (this.mBound()) {
		   		return this.updatedCounter ;
		   	} else if (!currentfile.equals("")) {
		   		return this.counterLength;
		   	} else {
		   		return 0;
		   	}
	   }

	public void clickLaunchWeb(PlayerSurface playerSurface) {
		Intent webIntent = new Intent( Intent.ACTION_VIEW );
        webIntent.setData( Uri.parse("http://tapdancer.info") );
        this.startActivity( webIntent );
	}
	
	public void clickLaunchHelp(PlayerSurface playerSurface) {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity( intent );
	}
	
	public int getState() {
		return this.state;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.quit:
	            // quit program 
	        	this.finish();
	            return true;
	        case R.id.help:
	            this.clickLaunchHelp(surface);
	            return true;
	        case R.id.web:
	            this.clickLaunchWeb(surface);
	            return true;
	        case R.id.menu_settings:
	        	launchSettings();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public void launchSettings() {
		Intent intent = new Intent(this, UserSettingsActivity.class);
		startActivityForResult(intent, RESULT_SETTINGS);
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case RESULT_SETTINGS:
            break;
 
        }
 
    }

	public Messenger getMessenger() {
		return this.messenger;
	}	
	
	@Override
	public void onBackPressed() {
		clickStopFile(null);
//		Intent intent = new Intent(this, JYTSpotActivity.class);
//		startActivity(intent);
		finish();
	}

}