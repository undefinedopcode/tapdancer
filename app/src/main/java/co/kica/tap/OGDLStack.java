package co.kica.tap;

/**************************************************************************
 OGDLStack
 ---------------------------------------------------------------------------
 This code was translated from ObjectPascal by Scruffy (The Janitor).
 **************************************************************************/
public class OGDLStack {

	/* instance vars */
	private OGDLNodeList S;

	public  OGDLNodeList S()
	{

		/* vars */
		OGDLNodeList result;
		result = S;

		/* enforce non void return */
		return result;

	}
	
	public OGDLNodeList Nodes() {
		return S;
	}

	public  void setS(OGDLNodeList v)
	{

		/* vars */

		S = v;

	}

	public  void Empty()
	{

		/* vars */


		S = new OGDLNodeList();

	}

	public   OGDLStack()
	{

		/* vars */


		this.Empty();

	}

	public  OGDLNode Peek()
	{

		/* vars */
		OGDLNode result;

		result = null;
		if (S.size() > 0)
			result = S.get(S.size());

		/* enforce non void return */
		return result;

	}

	public  OGDLNode Pop()
	{

		/* vars */
		OGDLNode result;

		result = null;
		if (S.size() > 0)
		{
			result = S.get(S.size());
			/* don't need to shrink this: SetLength(S,length(S)-1); */
		}

		/* enforce non void return */
		return result;

	}

	public  void Push(OGDLNode  value)
	{

		/* vars */


		/* don't need to grow this: SetLength(S, length(S)+1); */
		S.add(value);

	}

}
