package co.kica.tapdancer;

import java.io.File;

import co.kica.tap.C64Tape;

import co.kica.tap.*;
import co.kica.tapdancer.R;
import co.kica.tapdancer.R.drawable;
import co.kica.tapdancer.R.id;
import co.kica.tapdancer.R.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RenderActivity extends Activity {
	private C64Tape tap;
	private String filename;
	private String converted;
	private ProgressBar pb;
	private Typeface typeface; 
	private Messenger messenger;
	
	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			Object outputpath = message.obj;
			if (message.arg1 == 364) {
				// handle status update...
				pb.setProgress(message.arg2);
			}
			else if (message.arg1 == RESULT_OK && outputpath != null) {
				//Toast.makeText(RenderActivity.this, "Audio Ready", Toast.LENGTH_LONG).show();
				// move to another activity
				// start playback service
				Intent intent = new Intent(RenderActivity.this, PlayActivity.class);
				intent.putExtra("wavfile", outputpath.toString());
				startActivity( intent );
			} else {
				
				Context context = RenderActivity.this;
				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						context);
		 
				// set title
				alertDialogBuilder.setTitle("Render Failed");
				// set dialog message
				alertDialogBuilder
					.setMessage("It is possible that the file was corrupt, or you have found a bug. Would you like to try a different file?")
					.setCancelable(false)
					.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, close
							// current activity
							Intent intent = new Intent(RenderActivity.this, FileChooser.class);
							startActivity( intent );
						}
					  })
					.setNegativeButton("No",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, just close
							// the dialog box and do nothing
							Intent intent = new Intent(RenderActivity.this, PlayActivity.class);
							startActivity( intent );
						}
					});
	 
					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();
	 
					// show it
					alertDialog.show();
				
				
			}
		};
	};
	private Thread task;
	private RenderRunnable renderer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);
        
		Intent intent = getIntent();
		filename = intent.getStringExtra(FileChooser.PICKED_MESSAGE);
		String index = intent.getStringExtra(FileChooser.PICKED_SUBITEM);
		int idx = 0;
		if (index != null && index.length() > 0) {
			idx = Integer.parseInt(index);
		}
        
		//requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		this.getWindow().setBackgroundDrawableResource(R.drawable.tapdancer_background);
		
		typeface = Typeface.createFromAsset(this.getAssets(), "fonts/atarcc.ttf");
                
        TextView fn = (TextView) findViewById(R.id.filename);
        fn.setText(filename.replaceFirst(".+[/]", ""));
        fn.setTypeface(typeface);
        
        // get progress bar reference
        this.pb = (ProgressBar) this.findViewById(R.id.progressBar1);
        
        // do the render
        this.messenger = new Messenger(handler);
        /*Intent rintent = new Intent(this, RenderService.class);
        rintent.putExtra("MESSENGER", messenger);
        rintent.putExtra("tapfile", filename);
        startService(rintent);*/
        
        /* we now use a runnable here to do our stuff, benefit being that it will
         * be killed the instance the back button is pressed.
         * 
         */
        this.renderer = new RenderRunnable(this, filename, idx);
        this.task = new Thread( renderer );
        this.task.start();
    }

	public Messenger getMessenger() {
		return this.messenger;
	}
	
	@Override
	protected void onStop() {
		// if we have a task kill it
		if (task != null && task.isAlive()) {
			if (renderer != null) {
				renderer.cancel();
				renderer = null;
			}
			task.interrupt();
			task = null;
		}
		super.onStop();
		System.gc();
	}
	
	@Override
	   protected void onPause() {
		if (task != null && task.isAlive()) {
			if (renderer != null) {
				renderer.cancel();
				renderer = null;
			}
			task.interrupt();
			task = null;
		}
		super.onPause();
		System.gc();
	   }
}
