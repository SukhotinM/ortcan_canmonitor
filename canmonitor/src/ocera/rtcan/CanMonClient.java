/**
 * Created by IntelliJ IDEA.
 * Date: Dec 10, 2002
 * Time: 12:19:53 PM
 * @author F. Vacek, vacek@rtime.felk.cvut.cz
 */
package ocera.rtcan;

import ocera.util.*;

import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class CanMonClient implements Runnable
{
    protected Runnable guiUpdate = null;
    // queue of red CAN messages
    public static final int RQUEUE_LEN_MAX = 50;
    public RoundQueue readQueue = new RoundQueue(RQUEUE_LEN_MAX);
    public static final int DEFAULT_PORT = 1001;
    protected Socket canSocket = null;
    protected String errMsg;
    protected BufferedInputStream in = null;
    protected BufferedOutputStream out = null;
    public static final int CAN_DATAGRAM_LEN_MAX = 26;
    private Thread readThread = null;
//    protected boolean stopReadThread = false;

    /**
     * @param guiUpdate thread running GUI (swing)
     */
    public void setGuiUpdate(Runnable guiUpdate) {
        this.guiUpdate = guiUpdate;
    }

    /**
     * @return the description of the last CanMonClient error.
     */
    public String getErrMsg() {
        return errMsg;
    }

    /**
     * Close the socket to canmond server openned by connect()<br><br>
     * @return 0 if succeed.<br>
     * Use getErrMsg() to find out error description.

     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int disconnect() {
        int ret = 0;
        try {
            if(in != null) in.close();
        } catch(IOException e) {
        }
        in = null;
        out = null;
        errMsg = "OK";
        if(canSocket != null) {
            System.out.println("disconnecting from " + canSocket.getInetAddress());
            try {
                canSocket.close();
            } catch(IOException e) {
                ret = -1;
                errMsg = "Failed to disconnect from server.";
            }
            canSocket = null;
            System.out.println(errMsg);
        }
        return ret;
    }

    /**
     * Opens socket to canmond server<br><br>
     * @return 0 if succeed.<br>
     * Use getErrMsg() to find out error description.

     * @param  ip   the URL of server
     * @param  port the port number, if 0 the default value DEFAULT_PORT is taken.<br><br>

     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int connect(String ip, int port) {
        int ret = -1;
        disconnect();
        errMsg = "";
        try {
            InetAddress addr = InetAddress.getByName(ip);
            System.out.println("connecting to " + addr);
            try {
                if(port == 0) port = DEFAULT_PORT;
                canSocket = new Socket(addr, port);
                errMsg = "connected OK";
                in = new BufferedInputStream(canSocket.getInputStream());
                out = new BufferedOutputStream(canSocket.getOutputStream());

                // launch read thread
                readThread = new Thread(this);
                readThread.start();
                ret = 0;
            } catch(IOException e) {
                errMsg = "ERROR: connection failed";
                canSocket = null;
            }
        } catch(UnknownHostException e) {
            errMsg = "ERROR: unknown host.";
        }
        System.out.println(errMsg);
        return ret;
    }

    /**
     * Send message to canmond server<br><br>
     * Use getErrMsg() to find out error description.

     * @return length of msg if succeed, -1 else.<br>

     * @param  msg   CAN message of the form {CANDTG xx xx xx xx xx [bb bb ...]}<br>xx - hexadecimal int<br>bb - hexadecimal byte

     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int send(String msg) {
        errMsg = "";
        int ret = -1;
        if(out == null) return ret;
        try {
            out.write(msg.getBytes(), 0, msg.length());
            out.flush();
            ret = msg.length();
        } catch(IOException e) {
            errMsg = "ERROR: unsuccesfull write to the socket";
            System.out.println(errMsg);
        }
        return ret;
    }

    /**
     * Communication thread body
     */
    public void run() {
        CanMsg workingmsg = new CanMsg();
        String s;
        StringTokenizer st;
        int c;
        while(true) {
            // parse datagram

/*
            // all ints are in the network endian
            #define CAN_MSG_LENGTH 8
            struct canmsg_t {
                short		flags;
                int		cob;
                unsigned long	id;
                unsigned long	timestamp;
                unsigned int	length;
                unsigned char	data[CAN_MSG_LENGTH];
            } PACKED;
*/

            try {
                while(true) {
                    c = in.read();
//                    System.out.print((char)c);
                    if(c == -1) break;  //end of stream
                    if(c == '{') {
                        // datagram start, read datagram to s
                        s = "{";
                        while(true) {
                            c = in.read();
                            s += (char) c;
                            if(c == '}') break;
                        }
                        System.out.println("data from socked received: " + s);

                        if(guiUpdate != null) {
                            // send msg to the monitor msg queue
                            readQueue.append(s);
                            // update GUI
                            SwingUtilities.invokeLater(guiUpdate);
                        }
                    }
                }
            } catch(IOException e) {
                System.err.println("canmond socket read error - " + e);
//                System.out.println("cause: " + e.getCause());
                break;
            }
        }
        System.out.println("");
    }
}

