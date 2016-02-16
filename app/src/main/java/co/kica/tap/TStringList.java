package co.kica.tap;

import java.util.ArrayList;

public class TStringList extends ArrayList<String> {

	public String Text() {
		String result = "";
		
		for (int x=0; x<this.size(); x++) {
			if (result != "") {
				result = result + '\r' + '\n';
			} 
			result = result + this.get(x);
		}
		
		return result;
	}

	public void setText(String key) {
		String[] parts = key.split("[\r\n]+");
		this.clear();
		for (int x=0; x<parts.length; x++) 
			this.add(parts[x]);
	}
	
	public int Count() {
		return this.size();
	}

}
