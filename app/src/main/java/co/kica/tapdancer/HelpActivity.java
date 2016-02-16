package co.kica.tapdancer;

import co.kica.tapdancer.R;
import co.kica.tapdancer.R.id;
import co.kica.tapdancer.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

public class HelpActivity extends Activity {

	private WebView wv;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
       
        this.setContentView(R.layout.activity_help);
        
        wv = (WebView)this.findViewById(R.id.webView1);
        wv.loadUrl("file:///android_asset/html/index.html");
        
        wv.clearHistory();
    }
    
    public void clickCloseHelp( View view ) {
    	finish();
    }
	
}
