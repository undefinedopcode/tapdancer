package co.kica.tap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**************************************************************************
 OGDLDocument
 ---------------------------------------------------------------------------
 This code was translated from ObjectPascal by Scruffy (The Janitor).
 **************************************************************************/
public class OGDLDocument {

    /* instance vars */
    private OGDLNode fRoot;
    private String doc = "";
    private int index;

    public  OGDLNode Root()
    {
	
        /* vars */
        OGDLNode result;
            result = fRoot;

        /* enforce non void return */
        return result;

    }

    public  void setRoot(OGDLNode v)
    {
	
        /* vars */
        
            fRoot = v;

    }

    public   OGDLDocument()
    {
	
        /* vars */
	    this.setRoot( new OGDLNode(null) );
	    this.Root().setKey("{root}");

    }

    public  OGDLNode FindNode(String  nodepath, boolean  mkdir)
    {
	
        /* vars */
        OGDLNode result;
        OGDLNode n;
        OGDLNode  c;
        String[] parts;
        String chunk;
        int i;
            
	    result = null;
	    if (this.Root() == null)
	    {
		    this.setRoot( new OGDLNode( null ));
		    this.Root().setIndent("");
		    this.Root().setKey("{root}");
	    }		
    	
	    n = this.Root();
    	
	    chunk = "";
	    parts = nodepath.split("[.\\\\]+");
    	
	    for (i=0; i < parts.length; i++)
	    {
		    chunk = parts[i];
		    //L.d("Looking for child by name = "+chunk);
		    c = n.ChildByName( chunk );
		    if (c == null) {
		    	//L.d( "NOT FOUND" );
		    }
	    	
		    if (c == null)
		    {
			    if (mkdir)
			    {
				    c = n.AddChild();
				    c.setKey(chunk);
			    }
			    else
				    return result;
		    }
	    	
		    n = c;
	    }
    	
	    result = n;

        /* enforce non void return */
        return result;

    }

    public  String getValue(String  index)
    {
	
        /* vars */
        String result;
        OGDLNode n;
            
	    result = "";
    	
	    n = this.FindNode( index, false );
	    if (n != null)
	    {
		    if (n.getChildCount() == 1)
		    {
			    result = n.Children().get(0).Key();
		    }
	    }

        /* enforce non void return */
        return result;

    }

    public  void setValue(String  index, String  value)
    {
	
        /* vars */
        OGDLNode n;
            
	    n = this.FindNode( index, true );
	    if (n != null)
	    {
		    if (n.getChildCount() == 1)
		    {
			    n.Children().get(0).setKey(value);
		    }
		    else if (n.getChildCount() == 0)
		    {
			    n = n.AddChild();
			    n.setKey(value);
		    }
	    }

    }

    public  String NextToken()
    {
	
        /* vars */
        String result;
        boolean qq;
        boolean  enq;
        char ch;
            
      qq = false;
      result = "";
    
      ch = doc.charAt(index);
    
      /* meta */
      if ((ch == ',') || (ch == '(') || (ch == ')') || (ch == '\\'))
      {
           result = ""+ch;
           index++;
           return result;
      }
    
      /* line comment */
      if ((ch == '#'))
      {
           index++;
           
           while ((index < doc.length()) && ( (doc.charAt(index) != '\r') && (doc.charAt(index) != '\n') ))
                 index++;
                 
           if ((index < doc.length()) && (  (doc.charAt(index+1) == '\r') || (doc.charAt(index+1) == '\n') ))
              index++;
              
           result = "\n";
           //L.d( "--- Ignored comment until EOL" );
           return result;
      }
    
      if ((ch == '\r') || (ch == '\n'))
      {
           result = "\n";
           index++;
           if ((index < doc.length()) && (  (doc.charAt(index+1) == '\r') || (doc.charAt(index+1) == '\n') ))
               index++;
           //L.d( "--- EOL" );
           return result;
      }
    
      /* chunk starting with double quote */
      if ((ch == '"'))
      {
        index++;
        qq = true;
        enq = false;
        while ((index < doc.length()) && (qq == true))
        {
             switch (doc.charAt(index)) {  
             case '"':       
                                  if (enq)
                                  {
                                      result = result + doc.charAt(index);
                                      enq = false;
                                  }
                                  else
                                  {
                                    qq = false;
                                  }
                                  break;
                             
             case '\\':       
                                  enq = true;
                                  break;
                             
             default:
                 
                      if (enq)
                         result = result + '\\';
                      result = result + doc.charAt(index);
                      enq = false;
                      break;
                 
             }
             index++;
        }
        //L.d( "--- Got chunk: ["+result+"]" );
        return result;
      }
    
      /* chunk starting with double quote */
      if ((ch == '\''))
      {
        index++;
        qq = true;
        enq = false;
        while ((index < doc.length()) && (qq == true))
        {
             switch(doc.charAt(index)) { 
             case '\'':       
                                  if (enq)
                                  {
                                      result = result + doc.charAt(index);
                                      enq = false;
                                  }
                                  else
                                  {
                                    qq = false;
                                  }
                                  break;
                             
             case '\\':       
                                  enq = true;
                                  break;
                             
             default:
                 
                      if (enq)
                         result = result + '\\';
                      result = result + doc.charAt(index);
                      enq = false;
                      break;
                 
             }
             index = index + 1;
        }
        //L.d( "--- Got chunk: ["+result+"]" );
        return result;
      }
    
      /* break */
      if ((doc.charAt(index) == ' ') || (doc.charAt(index) == '\t'))
      {
           while ((index < doc.length()) && ( (doc.charAt(index) == ' ') || (doc.charAt(index) == '\t') ))
           {
                 result = result + doc.charAt(index);
                 index++;
           }
           result = UnTab(result);
           //L.d( "--- Got break: ["+result+"]" );
           //result = ' ';
           return result;
      }
    
      /* word */
      while ((index < doc.length()) && ( (doc.charAt(index) != ' ') && (doc.charAt(index) != '\t') && (doc.charAt(index) != '\r') && (doc.charAt(index) != '\n') && (doc.charAt(index) != ',') && (doc.charAt(index) != '(') && (doc.charAt(index)!=')') ) )
      {
           result = result + doc.charAt(index);
           index++;
      }
    
      //L.d( "--- Got chunk: ["+result+"]" );
      index++;

        /* enforce non void return */
        return result;

    }

	private String UnTab(String result) {
		return result.replace("\t", "  ");
	}

	public  void Parse(String  fulldoc)
    {
	
        /* vars */
        String token;
        OGDLNode cnode;
        OGDLNode  enode;
        OGDLNode  snode;
        int nn;
        String indent;
        OGDLStack S;
        OGDLStack  B;
        TStringList sl;
        int lineCount;
        boolean abortLine;
            
         doc = fulldoc;
        
         this.setRoot( new OGDLNode(null));
         this.Root().setKey("{root}");
         this.Root().setIndent("");
    
         cnode = this.Root();
         snode = cnode;
    
         index = 1;
         token = "";
         indent = "";
         cnode = this.Root();
         
         S = new OGDLStack();
         S.Push( this.Root() );
         B = new OGDLStack();
    
         sl = new TStringList();
         sl.setText(fulldoc);
         lineCount = 0;
         
         while (lineCount < sl.size()) 
         {
              doc = TrimRight(sl.get(lineCount));
              lineCount++;
              //L.d( 'Got line: [', doc, ']' );
    
              index = 0;
              indent = "";
              
              if (Trim(doc) != "")
              {
    
	              /* seed the start of line indentation */
	              token = this.NextToken();
	              if (token.charAt(0) == ' ')
	              {
	                 indent = token;
	              }
	              else
	              {
	                  index = 0;
	              }    
	              //L.d( 'Indent == ['+indent+'], start index == ', index );
	              
	              /* next thing is the first node */
	              token = this.NextToken();
	              //L.d( 'First token is '+token );
    	
	              /* find a parent */
	              nn = S.Nodes().size()-1;
	              while ((S.Nodes().get(nn).Indent().length() >= indent.length()) && (nn > 0) )
	                    nn--;
    	
	              /* create that start node */
	              snode = S.Nodes().get(nn).AddChild();
	              snode.setKey( token );
	              snode.setIndent( indent );
	              
	              /* place this node on the stack too */
	              S.Push( snode );
	              B.Push( snode.Parent() );
    	
	              cnode = snode;
	              abortLine = (index >= doc.length());
	              while ((index < doc.length()) && (!abortLine))
	              {
	                   token = this.NextToken();
	                   //L.d('Token: ['+token+']' );
	                   
	                   if (token.charAt(0) == ' ') {
	                        //L.d( 'SKIP BLANK' );
	                   } else if (token.charAt(0) == '(') {
	                        B.Push( cnode );
	                        //L.d( 'ENTER LIST' );
	                   } else if (token.charAt(0) == ')') {
	                        cnode = B.Pop();
	                        //L.d( 'EXIT LIST' );
	                   } else if (token.charAt(0) == ',') {
	                        cnode = B.Peek();
	                   } else if (token.charAt(0) == '\\') {
	                      abortLine = true;
	                      index = doc.length() + 1;
    	
	                      doc = "";
	                      while ((lineCount < sl.Count()) && (Trim(sl.get(lineCount)) != "") )
	                      {
	                          if (doc == "") {
	                  
	                             doc = Trim(sl.get(lineCount));
	                          
	                          } else {
	                              doc = doc + '\r' + '\n' + Trim(sl.get(lineCount));
	                          }
	                          lineCount++;
	                      }
	                      lineCount++;
	                      
	                      enode = cnode.AddChild();
	                      enode.setKey( doc );
	                      enode.setIndent( indent );
	                   }
	                   else
	                   {
	                        /* word */
	                        enode = cnode.AddChild();
	                        enode.setKey( token );
	                        enode.setIndent( indent );
	                        cnode = enode;
	                   }
	                   
	                   //L.d('loop');
	              }
	              
	              B.Empty();
	              
	         }
              
         }
    
         //L.d;
         //this.Root.Dump;

    }

	private String TrimRight(String string) {
		return string.replaceFirst("[ \t\r\n]*$", "");
	}
	
	private String TrimLeft(String string) {
		return string.replaceFirst("^[ \t\r\n]*", "");
	}
	
	private String Trim(String string) {
		return TrimLeft(TrimRight(string));
	}
	
	public static OGDLDocument ReadOGDLFile( String filename ) {
        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;
 
        try {
            reader = new BufferedReader(new FileReader(new File(filename)));
            String text = null;
 
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text)
                        .append(System.getProperty(
                                "line.separator"));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
		String s = contents.toString();
		
		if (s.equals("")) {
			return null;
		}
		
		OGDLDocument doc = new OGDLDocument();
		doc.Parse(s);
		return doc;
	}
	
	public static void WriteOGDLFile( String filename, OGDLDocument doc ) {
		
		try {
			BufferedWriter bos = new BufferedWriter( new FileWriter(filename) );
			doc.Root().DumpFile(bos);
			bos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void ReadXMLAsOGDL( String filename, OGDLDocument doc, boolean tagAttributes ) 
	{
		// this is a cheat method which employs reading an XML document as an OGDL tree
		
	}
}
