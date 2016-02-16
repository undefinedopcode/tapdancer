package co.kica.tapdancer;

import java.util.List;

import co.kica.tapdancer.R;
import co.kica.tapdancer.R.id;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class FileArrayAdapter extends ArrayAdapter<Option>{

    private Context c;
    private int id;
    private List<Option>items;
    private Typeface typeface;
    
    public FileArrayAdapter(Context context, int textViewResourceId,
            List<Option> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/atarcc.ttf");
    }
    public Option getItem(int i)
     {
         return items.get(i);
     }
     @Override
       public View getView(int position, View convertView, ViewGroup parent) {
               View v = convertView;
               if (v == null) {
                   LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                   v = vi.inflate(id, null);
               }
               final Option o = items.get(position);
               if (o != null) {
                       TextView t1 = (TextView) v.findViewById(R.id.TextView01);
                       t1.setTextColor(Color.WHITE);
                       t1.setTypeface(typeface);
                       TextView t2 = (TextView) v.findViewById(R.id.TextView02);
                       t2.setTextColor(Color.LTGRAY);
                       t2.setTypeface(typeface);
                       
                       if(t1!=null)
                           t1.setText(o.getName());
                       if(t2!=null)
                           t2.setText(o.getData());
                       
               }
               v.setBackgroundColor(0xff0042ff);
               return v;
       }

}

