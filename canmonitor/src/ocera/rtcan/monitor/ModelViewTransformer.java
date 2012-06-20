package ocera.rtcan.monitor;

import ocera.util.StringParser;
import org.flib.FString;
import java.util.ArrayList;
/**
 * User: kubaspet
 */

public class ModelViewTransformer {



//---------Transformation View-> Model byte array  ------------------------

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
}
