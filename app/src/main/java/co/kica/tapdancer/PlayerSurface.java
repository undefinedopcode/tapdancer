package co.kica.tapdancer;

import java.util.Random;
import java.util.Vector;

import co.kica.tapdancer.R;
import co.kica.tapdancer.R.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class PlayerSurface extends SurfaceView implements Runnable{
     
    Thread thread = null;
    SurfaceHolder surfaceHolder;
    volatile boolean running = false;
     
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Random random;
    private Drawable tapedeck;
    private Drawable tapedeck_inserted;
    private Drawable background;
    private Drawable www;
    private Drawable help;
    private Drawable menu;
	private float touched_x;
	private float touched_y;
	private boolean touched;
	private Context mContext; 
	private PlayActivity mActivity; 
	private String scrolly = "               Welcome to TapDancer ... Press EJECT to load a file... ";
	private Typeface typeface;
	private AudioManager audio;
	private int www_h; 
	private final int counter_x = 450;
	private final int counter_y = 245;
	
	public boolean inserted = false;
	private String lastScrolly = "";
 
  public PlayerSurface(Context context, PlayActivity act) {
   super(context);
   this.mContext = context;
   this.mActivity = act;
   // TODO Auto-generated constructor stub
   surfaceHolder = getHolder();
   random = new Random();
   
   Resources res = this.getResources();
   tapedeck = res.getDrawable(R.drawable.td_player);
   tapedeck_inserted = res.getDrawable(R.drawable.td_player_inserted);
   background = res.getDrawable(R.drawable.tapdancer_background);
   www = res.getDrawable(R.drawable.td_www);
   help = res.getDrawable(R.drawable.td_help);
   menu = res.getDrawable(R.drawable.td_menu);
   
   typeface = Typeface.createFromAsset(res.getAssets(), "fonts/atarcc.ttf");
   
   audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
  }
   
  public void onResumeMySurfaceView(){
   running = true;
   thread = new Thread(this);
   thread.start();
  }
   
  public void onPauseMySurfaceView(){
   boolean retry = true;
   running = false;
   while(retry){
    try {
     thread.join();
     retry = false;
    } catch (InterruptedException e) {
     // TODO Auto-generated catch block
     e.printStackTrace();
    }
   }
  }
 
  @Override
  public void run() {
   // TODO Auto-generated method stub
   while(running){
    if(surfaceHolder.getSurface().isValid()){
     Canvas canvas = surfaceHolder.lockCanvas();
     //... actual drawing on canvas
 
     paint.setStyle(Paint.Style.FILL);
     paint.setStrokeWidth(3);
     paint.setARGB(176, 255, 192, 0);
     
     float s_w = canvas.getWidth();
     float s_h = canvas.getHeight();
     
     float x_mod = s_w / 480f;
     float y_mod = s_h / 320f;
        
     // calc size of deck
     int h = canvas.getHeight();
     int w = h;
     int x1 = (canvas.getWidth() - w) / 2;
     int y1 = 0;
     int x2 = x1+w;
     int y2 = h;
     
     // draw background
     background.setBounds(0,0,canvas.getWidth(), canvas.getHeight());
     background.draw(canvas);
     
     www_h = (canvas.getWidth() - canvas.getHeight()) / 3;
     
     www.setBounds(0, canvas.getHeight()-www_h, www_h-1, canvas.getHeight());
     www.draw(canvas);
     
     paint.setTypeface(typeface);
     
     // draw help
     help.setBounds(0, 0, www_h-1, www_h-1);
     help.draw(canvas);
     
     // draw softmenu key
     menu.setBounds(0, Math.round((s_h-www_h)/2), www_h-1, Math.round((s_h+www_h)/2));
     menu.draw(canvas);
     
     // draw tape deck
     if (inserted) {
    	 tapedeck_inserted.setBounds(x1, y1, x2, y2);
    	 tapedeck_inserted.draw(canvas);
     } else {
    	 tapedeck.setBounds(x1, y1, x2, y2);
    	 tapedeck.draw(canvas);
     }
     //System.out.println("Canvas is w = "+canvas.getWidth()+", h = "+canvas.getHeight());
     
     // draw debug indication
     if (touched) {
    	 canvas.drawCircle(touched_x, touched_y, 32, paint);
     }
     
     // draw message
     paint.setTextSize(y_mod * 15);
     
     String tmp = scrolly.substring(0,15);
     String seed = "@@@@@@@@@@@@@@@";
     
     Rect bounds = new Rect();
     paint.getTextBounds(seed, 0, seed.length(), bounds);
     
     canvas.drawText(tmp, (s_w - bounds.right)/2, y_mod*225, paint);
     
     String ending = scrolly.substring(0,1);
     scrolly = scrolly.substring(1) + ending;
     
     // volume level
     int current     = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
     int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
     //audio.set
     
     // draw volume
     tmp = Integer.toString(current);
     if (tmp.length() < 2) {
    	 tmp = '0'+tmp;
     }
     paint.getTextBounds(tmp, 0, tmp.length(), bounds);
     canvas.drawText(tmp, canvas.getWidth() - bounds.right, canvas.getHeight()-bounds.bottom, paint);
     Rect rect = new Rect();
     rect.left = canvas.getWidth() - bounds.right;
     rect.right = canvas.getWidth();
     rect.bottom = canvas.getHeight()-Math.round(paint.getFontSpacing());
     int potential = Math.round(rect.bottom * ((float)(max - current) / (float)max));
     rect.top = potential;
     canvas.drawRect(rect, paint);
     
     paint.setTextSize(y_mod * 12);
     
     // draw counter
     tmp = Integer.toString(mActivity.getCounterPos());
     while (tmp.length() < 3) {
    	 tmp = '0' + tmp;
     }
     
     // ratio 
     float cvt = (canvas.getHeight() / 512f);
     float ccx = (counter_x*cvt) + ((canvas.getWidth() - canvas.getHeight())/2);
     float ccy = (counter_y*cvt);
     
     paint.getTextBounds(tmp, 0, tmp.length(), bounds);
     
     canvas.drawText(tmp, ccx-(bounds.right/2), ccy+(paint.getFontSpacing()/2), paint);
     
     surfaceHolder.unlockCanvasAndPost(canvas);
     
     
     
     try {
		Thread.sleep(200);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    }
   }
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
   // TODO Auto-generated method stub
    
   touched_x = event.getX();
   touched_y = event.getY();
   
   float s_w = surfaceHolder.getSurfaceFrame().width();
   float s_h = surfaceHolder.getSurfaceFrame().height();
   
   // check for www button
   if ( touched_x < www_h && touched_y > s_h-www_h && event.getAction() == MotionEvent.ACTION_DOWN ) {
	   mActivity.clickLaunchWeb(this);
	   return true;
   }
   
   // check for help button
   if ( touched_x < www_h && touched_y < www_h && event.getAction() == MotionEvent.ACTION_DOWN ) {
	   mActivity.clickLaunchHelp(this);
	   return true;
   }
   
   // check for settings button
   if ( touched_x < www_h && touched_y >= ((s_h-www_h)/2) && touched_y <= ((s_h+www_h)/2) && event.getAction() == MotionEvent.ACTION_DOWN ) {
	   mActivity.launchSettings();
	   return true;
   }
   
   float x_mod = 480f / s_w;
   float y_mod = 320f / s_h;;
   
   // convert points 
   float glx = (x_mod * touched_x) - 240f;
   float gly = ((s_h - touched_y) * y_mod) - 160f;
   
   System.out.println("Touch at ("+glx+", "+gly+"), w = "+s_w+", h = "+s_h);
   
   Vector vec = new Vector( glx, gly );
   String button = this.getButtonPress(vec);
   
   int action = event.getAction();
   
   if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
	   touched = true;
   } else {
	   touched = false;
   }
 
   
   if (!button.equals("") && action == MotionEvent.ACTION_DOWN) {
	   
	   Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
	   
	   // Vibrate for 25 milliseconds
	   v.vibrate(25);

	   
	   System.out.println("BUTTON: "+button);
	   
	   if (button.equals("PLAY")) {
		   mActivity.clickPlayFile(this);
	   }
	   
	   if (button.equals("STOP")) {
		   mActivity.clickStopFile(this);
	   }
	   
	   if (button.equals("PAUSE")) {
		   mActivity.clickPauseFile(this);
	   }
	   
	   if (button.equals("EJECT")) {
		   mActivity.clickChooseFile(this);
	   }
   }
   
   return true; //processed
  }
  
  public class Vector {
	public float x;
	public float y;

	public Vector( float x, float y ) {
		  this.x = x;
		  this.y = y;
	  }
  }
  
	public String getButtonPress( Vector pos ) {
		
		if ( pos.x > -111.4 && pos.x < -72 && pos.y > -147.4 && pos.y < -81) {
			return "EJECT";
		}
		
		if ( pos.x > -62.7 && pos.x < -23.3 && pos.y > -147.4 && pos.y < -81) {
			return "PLAY";
		}
		
		if ( pos.x > -15 && pos.x < 25.8 && pos.y > -147.4 && pos.y < -81) {
			return "PAUSE";
		}
		
		if ( pos.x > 33.6 && pos.x < 73.2 && pos.y > -147.4 && pos.y < -81) {
			return "STOP";
		}
		
		return "";
	}

	public String getScrolly() {
		return scrolly;
	}

	public void setScrolly(String scrolly) {
		if (!scrolly.equals(this.lastScrolly)) {
			this.scrolly = scrolly;
			this.lastScrolly = scrolly;
		}
	}
     
}