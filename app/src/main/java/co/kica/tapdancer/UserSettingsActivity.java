package co.kica.tapdancer;

import java.util.HashSet;

import co.kica.fileutils.Storage;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
 
public class UserSettingsActivity extends PreferenceActivity {
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        addPreferencesFromResource(R.xml.settings);
        
        final ListPreference storage = (ListPreference)this.findPreference("prefStorageInUse");
        
        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        setListPreferenceData(storage);

        storage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setListPreferenceData(storage);
                return false;
            }
        });
 
    }
    
    protected static void setListPreferenceData(ListPreference lp) {
    	
    	HashSet<String> loc = Storage.getStorageSet();
    	
        CharSequence[] entries = new CharSequence[loc.size()];
        CharSequence[] entryValues = new CharSequence[loc.size()];
        
        int idx = 0;
        for (String s: loc) {
        	CharSequence key = s.subSequence(0, s.length());
        	CharSequence value = s.subSequence(0, s.length());
        	entries[idx] = key;
        	entryValues[idx] = value;
        	idx++;
        }
        
        lp.setEntries(entries);
        lp.setDefaultValue(entryValues[0]);
        lp.setEntryValues(entryValues);
    }
}
