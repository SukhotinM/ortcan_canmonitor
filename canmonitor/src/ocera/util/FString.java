package ocera.util;

/**
 * @author ocera
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FString 
{
	protected String fstr;

    public static final int NOT_INTEGER = Integer.MIN_VALUE;

	public FString() {fstr = "";}
	public FString(String s) {fstr = s;}
	public FString(FString fs) {fstr = fs.toString();}

	public String toString() {return fstr;}

    /**
     * @see ocera.util.FString#slice(String str, int from)
     * @return slice(str, from, Integer.MAX_VALUE);
     */
	public static String slice(String str, int from)
	{
        return slice(str, from, Integer.MAX_VALUE);
    }

    /**
     * Returns char at position ix from begining of string. If ix is negative counts chars from end of the string.
     * If ix falls out of string bounds, returns 0
     *
     * @param s string to examine
     * @param ix position (can be negative)
     * @return char at position ix
     */
    public static char charAt(String s, int ix)
    {
        int l = s.length();
        if(ix < 0) ix = l + ix;
        if(ix < 0) return 0;
        if(ix >= l) return 0;
        return s.charAt(ix);
    }

    /**
     *
     * @param str string to be sliced
     * @param from beggining position of slice (included), can be negative, slice is counted from the end of string.
     * @param to   ending position of slice (not included), can be negative, slice is counted from the end of string.
     * @return slice of str beginning on position from and ending on position to.<br>
     * returned string is exactly the same as the Python's slice() returns
     */
    public static String slice(String str, int from, int to)
    {
        int len = str.length();
        if(from < 0) from += len;
        if(to < 0) to += len;
        if(from < 0) from = 0;
        if(to > len) to = len;
        if(to <= from) return "";
        return str.substring(from, to);
    }

	public static String rep(char what, int count)
	{
		String s = "";
		for (int i = 0; i < count; i++) s += what;
		return s;
	}

    public static String byte2Hex(int val)
    {
        return Integer.toString( ( val & 0xff ) + 0x100, 16 /* radix */ ) .substring( 1 );
    }

    public static int toInt(String s)
    {
        s = s.trim();
        if(s.length() == 0) return 0;
        try {
            return Integer.decode(s).intValue();
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public static int toInt(String s, int radix)
    {
        if(radix == 16) return toInt("0x" + s.trim());
        return toInt(s);
    }

}

