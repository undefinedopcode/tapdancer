package co.kica.tapdancer;

import co.kica.tapdancer.R;
import co.kica.tapdancer.R.layout;
import co.kica.tapdancer.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void clickChooseFile( View view ) {
		// start the file picker activity
		Intent intent = new Intent(this, FileChooser.class);
		startActivity( intent );
	}

}
