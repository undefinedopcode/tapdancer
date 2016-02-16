package co.kica.tap;

/**************************************************************************
OGDLNode
---------------------------------------------------------------------------
This code was translated from ObjectPascal by Scruffy (The Janitor).
**************************************************************************/

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;

public class OGDLNode {

	/* instance vars */
	private String fKey;
	private OGDLNodeList fChildren;
	private OGDLNode fParent;
	private String fIndent;

	public String Key() {

		/* vars */
		String result;
		result = fKey;

		/* enforce non void return */
		return result;

	}

	public void setKey(String v) {

		/* vars */

		fKey = v;

	}

	public OGDLNodeList Children() {

		/* vars */
		OGDLNodeList result;
		result = fChildren;

		/* enforce non void return */
		return result;

	}

	public void setChildren(OGDLNodeList v) {

		/* vars */

		fChildren = v;

	}

	public OGDLNode Parent() {

		/* vars */
		OGDLNode result;
		result = fParent;

		/* enforce non void return */
		return result;

	}

	public void setParent(OGDLNode v) {

		/* vars */

		fParent = v;

	}

	public String Indent() {

		/* vars */
		String result;
		result = fIndent;

		/* enforce non void return */
		return result;

	}

	public void setIndent(String v) {

		/* vars */

		fIndent = v;

	}

	public  boolean isBinary()
	{

		/* vars */
		boolean result;

		result = false;
		for ( char ch1 : this.Key().toCharArray() )
		{
			if ((ch1 >= 32 && ch1 <= 127) || (ch1 == '\r') || (ch1 == '\n') || (ch1 == '\t')) 
			{
				// ok
			} else {
				result = true;
				return result;
			}
		}

		/* enforce non void return */
		return result;

	}

	public OGDLNode getFirstChild() {

		/* vars */
		OGDLNode result;

		result = null;
		if (this.getChildCount() > 0)
			result = this.Children().get(0);

		/* enforce non void return */
		return result;

	}

	public OGDLNode ChildByName(String name) {

		/* vars */
		OGDLNode result;
		int c;

		result = null;
		for (c = 0; c < this.Children().size(); c++) {
			//System.out.println("ChildByName() comparing requested ["+name+"] with item ["+this.Children().get(c).Key()+"]");
			if (this.Children().get(c).Key().equals(name)) {
				result = this.Children().get(c);
				return result;
			}
		}

		/* enforce non void return */
		return result;

	}

	public  OGDLNode LastIndent(String  s)
	{

		/* vars */
		OGDLNode result;

		result = this;
		while ((result.Parent() != null) && (result.Indent().length() > s.length())) 
			result = result.Parent();

		/* enforce non void return */
		return result;

	}

	public  String getValue()
	{

		/* vars */
		String result;
		char ch;
		String s;
		TStringList sl;
		TStringList fl;
		int x;

		result = this.Key();

		if ((this.Key().indexOf('\r') > -1) || (this.Key().indexOf('\n') > -1))
		{
			s = "";
			for (x=1; x <= this.Level()-1; x++)
				s = s + "  ";
			sl = new TStringList();
			fl = new TStringList();
			sl.setText( this.Key() );
			fl.add( sl.get(0) );
			for (x=1; x <= sl.size()-1; x++)
				fl.add( s+sl.get(x) );
			//fl.Add('');
			result = fl.Text();

			//sl.Free;
			//fl.Free;
		}
		else
			if ((this.Key().indexOf(' ') > -1) || (this.Key().indexOf('\t') > -1))
			{
				result = "";
				for ( char ch1: this.Key().toCharArray() )
				{
					if (ch1 == '\'') {
						result = result + '\\' + ch1;
					} else {
						result = result + ch1;
					}
					
				}
				result = "'" + result + "'";
			}

		/* enforce non void return */
		return result;

	}

	private int Level() {
		return this.getLevelsToRoot();
	}

	public  int getLevelsToRoot()
	{

		/* vars */
		int result;
		OGDLNode node;

		result = 0;
		node = this;
		while (node.Parent() != null) 
		{
			result++;
			node = node.Parent();
		}

		/* enforce non void return */
				return result;

	}

	public int getChildIndex(OGDLNode child) {

		/* vars */
		int result;
		int x;

		result = -1;
		for (x = 0; x <= fChildren.size(); x++)
			if (fChildren.get(x) == child) {
				result = x;
				break;
			}

		/* enforce non void return */
		return result;

	}

	public OGDLNode getNextSibling() {

		/* vars */
		OGDLNode result;
		int x;

		x = this.Parent().getChildIndex(this);
		result = this.Parent().Children().get(x + 1);

		/* enforce non void return */
		return result;

	}

	public OGDLNode getPrevSibling() {

		/* vars */
		OGDLNode result;
		int x;

		x = this.Parent().getChildIndex(this);
		result = this.Parent().Children().get(x - 1);

		/* enforce non void return */
		return result;

	}

	public   OGDLNode(OGDLNode  Parent)
	{

		/* vars */
		fKey = "nuttin";
		fChildren = new OGDLNodeList();
		fParent = Parent;
		fIndent = "";

		if ((this.Parent() != null) && (!this.Parent().Children().contains(this))) {
			// bind our node into the child list 
			Parent().Children().add(this);
		}
		
	}

	public int getChildCount() {

		/* vars */
		int result;

		result = fChildren.size();

		/* enforce non void return */
		return result;

	}

	public  OGDLNode getChildNode(int  index)
	{

		/* vars */
		OGDLNode result;

		if ((index >= 0) && (index <= fChildren.size()))
		{
			result = fChildren.get(index);
		} else {
				result = null;
		}

		/* enforce non void return */
		return result;

	}

	public  void setChildNode(int  index, OGDLNode  value)
	{

		/* vars */
		int c;

		if ((index < 0))
		{
			/* don't need to grow this: SetLength(fChildren, length(fChildren)+1); */
			index = 0;
			for (c=fChildren.size(); c >= 0+1; c--)
				fChildren.set(c, fChildren.get(c-1));
			//if (fChildren.size() > index)
			//value.getNextSibling() = fChildren.get(0+1);
		}
		else
			if ((index > fChildren.size()))
			{
				/* don't need to grow this: SetLength(fChildren, length(fChildren)+1); */
				index = fChildren.size();
				//if (0 < index)
				//value.PrevSibling = fChildren.get(fChildren.size()-1);
			}

		fChildren.set(index, value);
		value.setParent(this);

	}

	public OGDLNode AddChild() {

		/* vars */
		OGDLNode result;

		result = new OGDLNode(this);
		//this.Children().add(result);

		/* enforce non void return */
		return result;

	}

	public OGDLNode AddSibling() {

		/* vars */
		OGDLNode result;

		result = new OGDLNode(this.Parent());
		//this.Parent().Children().add(result);

		/* enforce non void return */
		return result;

	}

	public  void Dump()
	{

		/* vars */
		int x;
		String s;
		
		//System.out.println("DEBUG: Dump() called on node with KEY = "+this.Key()+", LEVEL = "+this.Level());
		
		s = "";
		for (x=1; x <= this.Level()-1; x++)
			s = s + "  ";

		if (this.Level() > 0)
		{
			//System.out.println("Level = "+this.Level());
			//System.out.println("Key = "+this.Key());
			if (hasBlock())
			{
				System.out.println( s + this.getValue()+" \\" );
			} else {
				System.out.println( s + this.getValue() );
			}

		}

		
		for (x=0; x < fChildren.size(); x++)
			fChildren.get(x).Dump();

	}

	private boolean hasBlock() {
		boolean has_block;
		has_block = false;
		if (this.getChildCount() == 1) 
		{
			if ((this.Children().get(0).Key().indexOf(10)>-1) || (this.Children().get(0).Key().indexOf(13)>-1))
			{
				has_block = true;
			}
		}
		return has_block;
	}

	public  void DumpFile( BufferedWriter dos )
	{

		/* vars */
		int x;
		String s;

		s = "";
		for (x=1; x <= this.Level()-1; x++)
			s = s + "  ";

		if (this.Level() > 0)
		{
			
			if (hasBlock())
			{
				try {
					dos.write( s + this.getValue()+" \\" + '\r' + '\n' );
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					dos.write( s + this.getValue() + '\r' + '\n' );
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		for (x=0; x < fChildren.size(); x++)
			fChildren.get(x).DumpFile(dos);

	}
}
