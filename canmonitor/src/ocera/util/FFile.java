package ocera.util;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

import java.io.*;
import java.util.*;

public class FFile 
{
	private String ffileName;
	
	public FFile(String fname) {ffileName = fname;}

//-----------------------------------------------------------------------------	
	public String fileName() {return ffileName;}

//-----------------------------------------------------------------------------	
	public LinkedList toStringLList() throws FileNotFoundException, IOException
	{
		BufferedReader reader = null;
		LinkedList al = new LinkedList();
		try {
			reader = new BufferedReader(new FileReader(ffileName));
			for(String nextLine = reader.readLine(); nextLine != null; nextLine = reader.readLine()) {
				al.add(nextLine);
			}
		} 
/*			
			catch(FileNotFoundException e) {
				System.err.println(fileName + "File_not_found");
			} 
			catch (IOException e ) {
				System.err.println(fileName + "IO_error_read");
			} 
	*/		
		finally {	
			if(reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
					System.err.println(fileName() + " - IO_error_close");
				}
			}
		}
		return al;
	}

//-----------------------------------------------------------------------------	
	public String asString() throws FileNotFoundException, IOException
	{
		LinkedList ll = toStringLList();
		String s = "";
		for (ListIterator it = ll.listIterator(); it.hasNext();) {
			if(it.hasPrevious()) s += '\n';
			s += (String) it.next();
		}
		return s;
	}
//-----------------------------------------------------------------------------	
	public String[] toStringArray() throws FileNotFoundException, IOException
	{
		LinkedList ll = toStringLList();
        return (String[]) ll.toArray(new String[ll.size()]);
	}
}

