package co.kica.tapdancer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import co.kica.tapdancer.R;
import co.kica.tapdancer.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class SplashActivity extends Activity {
	
	private int splash_1_duration = 2000;
	private int splash_2_duration = 2000;

	@Override 
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_player);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		showSplashOne();
	}
	
	public void showSplashOne() {
		setContentView(R.layout.splash_2);
		Date d = new Date(System.currentTimeMillis()+splash_2_duration);
		new Timer().schedule(new TimerTask(){
		    public void run() { 
		        startActivity(new Intent(SplashActivity.this, SecondSplashActivity.class));
		    }
		}, d /*amount of time in milliseconds before execution*/ );
	}
	
}
