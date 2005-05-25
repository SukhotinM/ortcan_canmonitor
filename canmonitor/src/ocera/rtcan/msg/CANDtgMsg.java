/**
 * Created by IntelliJ IDEA.
 *
 * Date: Dec 10, 2002
 * Time: 1:10:51 PM
 * @author Frantisek Vacek, vacek@rtime.felk.cvut.cz
 */

package ocera.rtcan.msg;

import org.flib.FString;

/**
 * CAN datagram message
 */
public class CANDtgMsg
{

    public final static int DATA_LEN_MAX = 8;
    public int id;
    public short length;
    final public static int data_ARRAY_SIZE = DATA_LEN_MAX;
    public byte data[] = new byte[data_ARRAY_SIZE];

    /**
     * Converts CAM messsage to the string
     */
    public String toString() {
        String ret = "CAN message {id=" + Integer.toString(id, 16) + ", length=" + length + ", data=[";
        for(int i = 0; i < length; i++) {
            if(i > 0) ret += " ";
            ret += FString.byte2Hex(data[i]);
        }
        ret += "]}\n";
        return ret;
    }
}

