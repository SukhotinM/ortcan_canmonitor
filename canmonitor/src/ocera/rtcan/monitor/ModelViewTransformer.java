package ocera.rtcan.monitor;

import ocera.util.StringParser;
import org.flib.FString;
import java.util.ArrayList;
import ocera.rtcan.CanOpen.ODNode;
/**
 * User: kubaspet
 */

public class ModelViewTransformer {


//---------Transformation byte array of values ->View------------------------

    /**
     * Returns string representation of  ODNode value.
     *
     * @param node  selected ODNode..
     * @param selectedIndex selected type of representation.
     * @return  Number in String represented by  choice.
     */
    public static String getViewFromValue(ODNode node, int selectedIndex){
        String retString;
        switch (selectedIndex) {

            case RepresentationEnum.HEX_RAW:
                retString = valToStringHexRaw(node);
                break;
            case RepresentationEnum.BIN_RAW:
                retString = valToStringBinRaw(node);
                break;
            default:
                retString= valToStringHexRaw(node);
                break;
        }
        return retString;
    }

    /**
     * Returns a string representation of the first argument in hex format.
     *
     * @param node ODNode will be converted to a string.
     * @return  a string representation of the first argument in hex format.
     */
    public static String valToStringHexRaw(ODNode node)
    {
        byte[] val = node.getValue();
        String s = "";
        for(int i = 0; i < val.length; i++) {
            if(i > 0) s += " ";
            s += FString.byte2Hex(val[i]);
        }
        return s;
    }

    /**
     * Returns a string representation of the first argument in binary format.
     *
     * @param node ODNode will be converted to a string.
     * @return  a string representation of the first argument In binary format.
     */
    public static String valToStringBinRaw(ODNode node)
    {
        byte[] val = node.getValue();
        StringBuilder sb= new StringBuilder();
        for(int i = 0; i < val.length; i++) {
            if(i > 0) sb.append(" ");
            sb.append(FString.byte2Bin(val[i]));

        }
        return sb.toString();
    }

//---------Transformation View-> Model byte array  ------------------------
    /**
     * Returns a byte array containing the value of first argument. Format for string is selected by choice..
     *
     * @param s  string will be converted.
     * @param node  selected ODNode.
     * @param selectedIndex choice of representation.
     * @return   a byte array containing the value of first argument.
     */
    public static byte[] string2ValArray2(String s,ODNode node,int selectedIndex) throws NumberFormatException {
        byte[] ret;
        switch (selectedIndex) {
            case RepresentationEnum.HEX_RAW:
                ret = rawHexString2ValArray2(s);
                break;
            case RepresentationEnum.BIN_RAW:
                ret = rawBinString2ValArray2(s);
                break;
            default:
                ret = rawHexString2ValArray2(s);
                break;
        }
        return ret;
    }


    /**
     *
     * convert 'bbbbbbbb bbbbbbbb bbbbbbbb ' to short[], bbbbbbbb are binary numbers
     */

    public static byte[] rawHexString2ValArray2(String s)
    {
        ArrayList lst = new ArrayList(8);
        String ss[];
        while(true) {
            ss = StringParser.cutInt(s, 16); s = ss[1];
            if(ss[0].trim().length() == 0) break;
            lst.add(new Short((short) FString.toInt(ss[0], 16)));
        }
        byte[] ret = new byte[lst.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ((Short)lst.get(i)).byteValue();
        }
        return ret;
    }

    /**
     * Returns a byte array containing the value of first argument.
     *
     * @param s  string will be converted. In string have a form "0000...1 1111..1 ....."
     * @return  a byte array containing the value of first argument.
     */
    public static byte[] rawBinString2ValArray2(String s) throws NumberFormatException
    {
        String parsed[] = StringParser.parseBin(s);
        byte[] ret = new byte[parsed.length];
        Short tmp;
        for (int i = 0; i < parsed.length; i++) {
         //  tmp = Short.parseShort(parsed[i], 2);
            tmp = new Short(Short.parseShort(parsed[i], 2));
            ret[i] = tmp.byteValue();
        }
        return ret;
    }

}
