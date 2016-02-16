package co.kica.tapdancer;

import co.kica.fileutils.SmartFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.kica.tap.T64Format;
import co.kica.tap.T64Format.DirEntry;
import co.kica.tapdancer.R;
import co.kica.tapdancer.R.drawable;
import co.kica.tapdancer.R.layout;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

public class FileChooser extends ListActivity {
	
	public final static String PICKED_MESSAGE = "co.kica.tapdancer.PICKED_MESSAGE";
	public final static String PICKED_SUBITEM = "co.kica.tapdancer.PICKED_SUBITEM";
    
    private SmartFile currentDir;
    private FileArrayAdapter adapter;
    private String prefExtDir = "";

	private Option t64option;
    
	private void initFromPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		prefExtDir = prefs.getString("prefStorageInUse", Environment.getExternalStorageDirectory().getAbsolutePath());
		
		String lastPath = prefs.getString("lastdir", prefExtDir);
		
		/* if the user has changed their preferred path we want to note that 
		 * if they havent then prefExtDir will be contained in lastPath */
		
		lastPath = lastPath.replaceFirst("[/]$", "") + "/";
		String testExtDir = prefExtDir.replaceFirst("[/]$", "") + "/";
		
		if (!lastPath.startsWith(testExtDir)) {
			lastPath = prefExtDir;
		}
		
		
		SmartFile lp = new SmartFile(lastPath);
		if (lp.exists()) {
			currentDir = lp;
		} else {
			currentDir = new SmartFile(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
	}
	
	private void saveToPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor edit = prefs.edit();
		edit.putString("lastdir", currentDir.getAbsolutePath());
		edit.commit();
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFromPreferences();
		//requestWindowFeature(Window.FEATURE_NO_TITLE); 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        fill(currentDir);
        
        // after this...
        this.getWindow().setBackgroundDrawableResource(R.drawable.tapdancer_background);
          //.setBackgroundResource(R.drawable.tapdancer_background);
    }
    
    @Override
    protected void onNewIntent( Intent intent ) {
		//requestWindowFeature(Window.FEATURE_NO_TITLE); 
    	initFromPreferences();
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        fill(currentDir);    	
    }
    
    private long kb(SmartFile f) {
    	return f.length() / 1024;
    }
    
    private void fill(SmartFile f)
    {
        SmartFile[]dirs = f.listFiles();
         this.setTitle("Current Dir: "+f.getName());
         List<Option>dir = new ArrayList<Option>();
         List<Option>fls = new ArrayList<Option>();
         try{
             for(SmartFile ff: dirs)
             {
                if(ff.isDirectory()) {
                	if (ff.getName().charAt(0) != '.')
                    dir.add(new Option(ff.getName(),"Folder",ff.getAbsolutePath()));
                } else
                {
                	if (ff.getName().endsWith(".TAP") || ff.getName().endsWith(".tap") || 
                			ff.getName().endsWith(".CAS") || ff.getName().endsWith(".cas") || 
                			ff.getName().endsWith(".UEF") || ff.getName().endsWith(".uef") || 
                			ff.getName().endsWith(".PRG") || ff.getName().endsWith(".prg") || 
                			ff.getName().endsWith(".P00") || ff.getName().endsWith(".p00") || 
                			ff.getName().endsWith(".T64") || ff.getName().endsWith(".t64") || 
                			ff.getName().endsWith(".TZX") || ff.getName().endsWith(".tzx") || 
                			ff.getName().endsWith(".CDT") || ff.getName().endsWith(".cdt")	) {
                		
                		if (ff.getName().charAt(0) != '.')
                			fls.add(new Option(ff.getName(),"File Size: "+kb(ff)+"Kb",ff.getAbsolutePath()));
                	}
                }
             }
         }catch(Exception e)
         {
             
         }
         Collections.sort(dir);
         Collections.sort(fls);
         dir.addAll(fls);
         
         if (!f.getAbsolutePath().equals( this.prefExtDir ))
        	 dir.add(0,new Option("..","Parent Directory",f.getParent()));
         
         adapter = new FileArrayAdapter(FileChooser.this,R.layout.file_view,dir);
         this.setListAdapter(adapter);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        Option o = adapter.getItem(position);
        if(o.getData().equalsIgnoreCase("folder")||o.getData().equalsIgnoreCase("parent directory")){
                currentDir = new SmartFile(o.getPath());
                fill(currentDir);
        }
        else
        {
            onFileClick(o);
        }
    }
    private void onFileClick(Option o)
    {
        //Toast.makeText(this, "File Clicked: "+o.getPath(), Toast.LENGTH_SHORT).show();
        // create an intent so that we can process the render... 
        
        // create an intent so that we can display the render...
    	
    	if (o.getPath().toUpperCase().endsWith(".T64")) {
    		T64Format t64 = new T64Format(o.getPath(), true);
    		if (!t64.validHeader()) {
    			Intent intent = new Intent(this, RenderActivity.class);
    			intent.putExtra(PICKED_MESSAGE, o.getPath());
    			startActivity( intent );
    			return;
    		}
    		// valid file
    		ArrayList<DirEntry> dir = t64.getDir();
    		if (dir.size() == 1) {
    			Intent intent = new Intent(this, RenderActivity.class);
    			intent.putExtra(PICKED_MESSAGE, o.getPath());
    			startActivity( intent );
    			return;
    		}
    		// present chooser..
    		this.chooseT64Item(o, dir);
    	} else {
			Intent intent = new Intent(this, RenderActivity.class);
			intent.putExtra(PICKED_MESSAGE, o.getPath());
			startActivity( intent );
    	}
    }
    
    private void chooseT64Item(Option o, ArrayList<DirEntry> dir) {
		// TODO Auto-generated method stub
    	AlertDialog.Builder alertDialog = new AlertDialog.Builder(FileChooser.this);
    	this.t64option = o;
   	 
        // Setting Dialog Title
        alertDialog.setTitle("Select T64 Subfile");

        // Setting Icon to Dialog
        alertDialog.setIcon(R.drawable.ic_launcher);
        
        String[] storageList = new String[dir.size()];
        int i=0;
        for (DirEntry d: dir) {
        	storageList[i++] = d.getFilename();
        }
        
        alertDialog.setItems(storageList, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(FileChooser.this, RenderActivity.class);
				intent.putExtra(PICKED_MESSAGE, FileChooser.this.t64option.getPath());
				intent.putExtra(PICKED_SUBITEM, Integer.toString(which));
				startActivity( intent );
			}
			
		});
        
        // Showing Alert Message
        alertDialog.show();

	}

	@Override 
    public void onPause() {
    	super.onPause();
    	saveToPreferences();
    }
    
    @Override 
    public void onStop() {
    	super.onStop();
    	saveToPreferences();
    }
}
