/**
 * Created by IntelliJ IDEA.
 *
 * Date: Dec 10, 2002
 * Time: 1:10:51 PM
 * @author Frantisek Vacek, vacek@rtime.felk.cvut.cz
 */

package ocera.rtcan;

import ocera.util.*;

public class CanMsg
{

    public final static int DATA_LEN_MAX = 8;
    public int id;
    public int length;
    public short data[] = new short[DATA_LEN_MAX];

    /**
     * Converts CAM messsage to the string
     */
    public String toString() {
        String ret = "{id=" + Integer.toString(id, 16) + ", length=" + length + ", data=[";
        for(int i = 0; i < length; i++) {
            if(i > 0) ret += " ";
            ret += FString.byte2Hex(data[i]);
        }
        ret += "]}";
        return ret;
    }
}

