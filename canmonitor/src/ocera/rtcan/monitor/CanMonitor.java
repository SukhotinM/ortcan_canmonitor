/**
 * Created by IntelliJ IDEA.
 * User: Vacek
 * Date: Nov 1, 2002
 * Time: 9:30:53 AM
 * To change this template use Options | File Templates.
 */
package ocera.rtcan.monitor;

import ocera.util.*;
import ocera.util.xmlconfig.XmlConfig;
import ocera.msg.*;
import ocera.rtcan.CanMonClient;
import ocera.rtcan.CanMsg;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import org.jx.xmlgui.XMLMenuBuilder;


public class CanMonitor extends JFrame implements Runnable
{
    protected JCheckBox cbxShowRoughMessages;
    protected JTextField edCanID;
    protected JTextField edCanData0;
    protected JTextField edCanData1;
    protected JTextField edCanData2;
    protected JTextField edCanData3;
    protected JTextField edCanData4;
    protected JTextField edCanData5;
    protected JTextField edCanData6;
    protected JTextField edCanData7;
    protected JTextField[] edCanData = new JTextField[8];

    protected CanMonClient canConn = new CanMonClient();
    //protected int nodeNo = 1;  // number of the node to communicate with

    protected Container pane;
    protected ActionMap actions = new ActionMap();

    protected JPanel panSDOTable;

    protected JTabbedPane tabPane;
    protected JTextArea txtLog;

    protected XmlConfig xmlConfig = new XmlConfig();
    public static final String CONFIG_FILE_NAME = "CanMonitor.conf.xml";
    protected boolean configChanged = false;
    private ConfigLookup confLookup = new ConfigLookup(".canmonitor", CONFIG_FILE_NAME, this.getClass());
    private CanMonStatusBar statusBar = new CanMonStatusBar();
    private JButton btSend;
    //private LinkedList candeviceList = new LinkedList();
    private JButton btClearLog;

    public static void main (String [] args)
    {
        FLog.logTreshold = FLog.LOG_ERR;
        //FLog.logTreshold = FLog.LOG_DEB;
        CanMonitor app = new CanMonitor(args);
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
        app.setSize(800, 600);
        app.setVisible(true);
//        app.pack();
    }

    private void openConfigDialog()
    {
        ConfigDialog xconf = new ConfigDialog(xmlConfig);
        xconf.show();
        configChanged = true;
    }

    private void processCmdLine(String[] args)
    {
        if(args == null) return;
        String edsFile = null;
        for(int i = 0; i < args.length; i++) {
            String s = args[i];
            if(FString.charAt(s, 0) == '-') {
                char c = FString.charAt(s, 1);
                if(c == 'a') {
                    // address
                    i++;
                    if(i < args.length) {
                        xmlConfig.setValue("/connection/host", args[i]);
                        //canProxyIP = args[i];
                    }
                }
                else if(c == 'n') {
                    // node
                    i++;
                    if(i < args.length) {
                        xmlConfig.setValue("/canopen/node", args[i]);
                        //xmlConfig.getRootElement().cd("/canopen/node").setValue(args[i]);
                        //nodeNo = FString.toInt(args[i]);
                    }
                }
                else if(c == 'e') {
                    // EDS file name
                    i++;
                    if(i < args.length) edsFile = args[i];
                }
                else if(c == 'v') {
                    // verbose
                    FLog.logTreshold = FLog.LOG_INF;
                }
                else if(c == 'g') {
                    // debug level
                    i++;
                    if(i < args.length) FLog.logTreshold = Integer.parseInt(args[i]);
                }
                else if(c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -e EDS_file_name -v -g debug_level\n");
                    System.exit(0);
                }
            }
        }
        if(edsFile != null) {
//            treeEdsRootNode.setUserObject(edsFile);
//            openEDS(edsFile);
        }
    }

    protected void connect()
    {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getValue("/connection/port", "1001").trim();
        int p = FString.toInt(port);
        if(ip.length() > 0) {
            txtLog.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtLog.append(canConn.getErrMsg() + "\n");
        }
        refreshForm();
    }

    protected void disConnect()
    {
        canConn.disconnect();
        refreshForm();
    }

    public CanMonitor(String[] cmd_line_args)
    {
        super("RT CAN monitor - Beta 0.1");

        edCanData[0] = edCanData0;
        edCanData[1] = edCanData1;
        edCanData[2] = edCanData2;
        edCanData[3] = edCanData3;
        edCanData[4] = edCanData4;
        edCanData[5] = edCanData5;
        edCanData[6] = edCanData6;
        edCanData[7] = edCanData7;

        loadConfig();
        processCmdLine(cmd_line_args);
        System.out.println("Logging level: " + FLog.logTreshold);
        initActions();
        initGui();
        init();
        connect();
//        setBounds(50, 50, 800, 600);
    }

    /**
     * Opens configuration file, triing next locations:<br>
     */
    protected void loadConfig()
    {
        String conf_file = confLookup.findConfigFile();
        System.out.println("loading config from '" + conf_file + "'");
        if(conf_file != null) xmlConfig.fromURI(conf_file);
        else FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " not found.");
    }

    protected void saveConfig()
    {
        if(!configChanged) return;
        String conf_file = confLookup.createConfigFile();
        if(conf_file == null) {
            FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " cann't be written.");
            return;
        }

        String s = xmlConfig.toString();
        FFile ff = new FFile(conf_file);
        try {
            ff.writeString(s, FFile.MODE_OVERWRITE, FFile.OVERWRITE_FILE);
        } catch (IOException e) {
            new ErrorMsg(this).show("Error writing config file: " + e);
        } catch (FFileException e) {
            new ErrorMsg(this).show("Error writing config file: " + e);
        }
    }

    public void initActions()
    {
        final CanMonitor app = this;
        URL url = this.getClass().getResource("resources/file-open.png");
        ImageIcon ico = new ImageIcon(url);
        Action act;
        act = new AbstractAction("Open EDS", ico) {
                    public void actionPerformed(ActionEvent e) {

                        JFileChooser fc = new JFileChooser();
                        FFileFilter filter = new FFileFilter();
                        filter.addExtension("EDS");
                        filter.addExtension("eds");
                        filter.setDescription("EDS Electronic Data Sheets");
                        fc.setFileFilter(filter);
                        String pwd = System.getProperty("user.dir", ".");
                        fc.setCurrentDirectory(new File(pwd));

                        int ret = fc.showOpenDialog(CanMonitor.this);
                        if(ret == JFileChooser.APPROVE_OPTION) {
                            String fname = fc.getSelectedFile().getAbsolutePath();
                            int ix = tabPane.getTabCount();
                            // find free node number
                            int node = 0;
                            for(int i=1; i<ix; i++) {
                                CANopenDevicePanel p =  (CANopenDevicePanel)tabPane.getComponentAt(i);
                                int nd = p.getNodeID();
                                if(nd > node) node = nd;
                            }
                            CANopenDevicePanel candev = new CANopenDevicePanel(app, ix);
                            tabPane.add(candev);
                            tabPane.setSelectedIndex(ix);    // select tab EDS
                            candev.openEDS(fname);
                            candev.setNodeID(++node);
                        }
                    }
                };
        act.putValue(Action.SHORT_DESCRIPTION, "Open EDS in new tab");
        actions.put("EdsOpen", act);

        ico = new ImageIcon(getClass().getResource("resources/eds-close.png"));
        act = new AbstractAction("Close EDS", ico) {
                    public void actionPerformed(ActionEvent e) {
                        int ix = tabPane.getSelectedIndex();
                        if(ix > 0) {
                            CANopenDevicePanel candev = (CANopenDevicePanel)tabPane.getComponentAt(ix);
                            tabPane.remove(candev);
                            //candeviceList.remove(candev);
                        }
                    }
                };
        act.putValue(Action.SHORT_DESCRIPTION, "Close current EDS tab");
        actions.put("EdsClose", act);

        act = new AbstractAction("Connect") {
                    public void actionPerformed(ActionEvent e) {
                        connect();
                    }
                };
        actions.put("Connect", act);

        act = new AbstractAction("Disconnect") {
                    public void actionPerformed(ActionEvent e) {
                        disConnect();
                    }
                };
        actions.put("Disconnect", act);

        act = new AbstractAction("Config") {
                    public void actionPerformed(ActionEvent e) {
                        openConfigDialog();
                    }
                };
        actions.put("Config", act);

        act = new AbstractAction("Reset device") {
                    public void actionPerformed(ActionEvent e) {
                        edCanID.setText("0");
                        edCanData[0].setText("1");
                        edCanData[1].setText("0");
                        btSend.getActionListeners()[0].actionPerformed(null);
                    }
                };
        actions.put("DeviceReset", act);

        act = new AbstractAction("Quit") {
                    public void actionPerformed(ActionEvent e) {
                        cleanUp();
                        System.gc();
                        System.exit(0);
                    }
                };
        actions.put("Quit", act);
    }

    public void initGui()
    {
        pane = getContentPane();

        //==========================================================
        // menu bar
        //==========================================================
        XMLMenuBuilder builder = new XMLMenuBuilder(this);
        String menu_file_name = "resources/menu.xml";
        FLog.log("CanMonitor", FLog.LOG_DEB, "initGui() - open URL '" + menu_file_name + "'");
        URL url = getClass().getResource(menu_file_name);
        setJMenuBar(builder.buildFrom(url, actions));

        //==========================================================
        // tool bar
        //==========================================================
        JToolBar tb = new JToolBar();
        tb.setRollover(true);
        tb.add(actions.get("EdsOpen"));
        tb.add(actions.get("EdsClose"));
        //tb.addSeparator();
        //tb.add(new JSeparator(SwingConstants.VERTICAL));
        pane.add(tb, BorderLayout.NORTH);

        //==========================================================
        //        layout
        //==========================================================
        tabPane.setSelectedIndex(0);    // select tab CAN
        pane.add(tabPane, BorderLayout.CENTER);

        pane.add(statusBar.panel, BorderLayout.SOUTH);
        enableControls();

        //==========================================================
        //        btSend listenner
        //==========================================================
        btSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                {CANDTG 0 0 0 209 [FF]}
                String msg = "{CANDTG 0 0 0 " + edCanID.getText() + " [";
                String s;
                for(int i=0; i<8; i++) {
                    s = edCanData[i].getText().trim();
                    if(s.length() == 0) break;
                    if(i>0) msg += " ";
                    msg += s;
                }
                msg += "]}";
                txtLog.append("SENDING:\t" + msg + "\n");
                canConn.send(msg);
            }
        });

        btClearLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });

        //==========================================================
        //        JFrame listenners
        //==========================================================
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actions.get("Quit").actionPerformed(null);
            }
        });
    }

    private void refreshForm()
    {
        if(canConn.getSocket() == null)
            statusBar.lbl1.setText("disconnected");
        else
            statusBar.lbl1.setText(canConn.getSocket().toString());
        statusBar.lbl2.setText("");
        statusBar.lbl3.setText("");
    }

    public void setTabLabel(int tabix, String lbl)
    {
        tabPane.setTitleAt(tabix, lbl);
    }

    private void enableControls()
    {
    }

    public void init()
    {
        canConn.setGuiUpdate(this);
//        canConn.connect(serverAddr, 0);

        Thread t = new Thread(this);
        t.start();
    }

    public void cleanUp()
    {
//        if(canReaderThread != null)
//            try {canReaderThread.join();} catch(InterruptedException e) {}
        disConnect();
        saveConfig();
    }

    /**
     * sends msg to the socket if it is opened
     * @param msg
     */
    void sendMessage(String msg)
    {
        if(canConn == null) return;
        if(!canConn.connected()) return;
        canConn.send(msg);
    }

    private int msgCount = 0;

    /**
     * code refreshing GUI controls from variables set by other thread
     * mainly CanMonClient thread
     */
    public void run()
    {
        // read all received messages if any
        while(true) {
            String s = (String)canConn.readQueue.remove(RoundQueue.NO_BLOCKING);
            if(s == null) break;
            FLog.log("CanMonitor", FLog.LOG_DEB, "received CAN message " + s);
            msgCount++;

            // parse datagram
            String msg = s;
            s = FString.slice(s, 1, -1);
            String ss[];
            if(s.matches("CANDTG.*")) {
                // found CAN msg start
                if(!cbxShowRoughMessages.isSelected()) continue;

                txtLog.append("RECEIVE[" + msgCount + "]:\t" + s + "\n");
                s = s.substring(7);
                CanMsg canmsg = new CanMsg();
                ss = StringParser.cutInt(s, 16); s = ss[1]; //flags
                ss = StringParser.cutInt(s, 16); s = ss[1]; //cob
                ss = StringParser.cutInt(s, 16); s = ss[1]; //timestamp
                ss = StringParser.cutInt(s, 16); s = ss[1]; //id
                canmsg.id = FString.toInt(ss[0], 16);   //id
                canmsg.length = 0;
                s = s.trim();
                if(s.charAt(0) == '[') {
                    s = FString.slice(s, 1, -1);
                    while(canmsg.length < CanMsg.DATA_LEN_MAX) {
                        ss = StringParser.cutInt(s, 16); s = ss[1];
                        if(ss[0].trim().length() == 0) break;
                        canmsg.data[canmsg.length++] = (short) FString.toInt(ss[0], 16);
                    }
                }
                txtLog.append("\t" + canmsg + "\n");
                refreshForm();
            }
            else {
                // scan all CANopen devices
                for(int i=1; i<tabPane.getTabCount(); i++) {
                    CANopenDevicePanel p =  (CANopenDevicePanel)tabPane.getComponentAt(i);
                    if(p.tasteMessage(msg)) break;
                }
            }
            FLog.logcont(FLog.LOG_DEB, "unknown message: " + msg);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JTabbedPane _1;
        _1 = new JTabbedPane();
        tabPane = _1;
        _1.setTabLayoutPolicy(0);
        _1.setTabPlacement(1);
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(3, 3, 0, 0), 3, -1));
        _1.addTab("CAN", _2);
        final JScrollPane _3;
        _3 = new JScrollPane();
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTextArea _4;
        _4 = new JTextArea();
        txtLog = _4;
        _3.setViewportView(_4);
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 10, new Insets(0, 0, 0, 0), 5, 0));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 0, 0, null, null, null));
        final JTextField _7;
        _7 = new JTextField();
        edCanID = _7;
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 1, 6, 6, new Dimension(50, -1), null, null));
        final JTextField _8;
        _8 = new JTextField();
        edCanData0 = _8;
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 6, 0, null, null, null));
        final JTextField _9;
        _9 = new JTextField();
        edCanData4 = _9;
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _10;
        _10 = new JTextField();
        edCanData6 = _10;
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 7, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setIconTextGap(0);
        _11.setText("ID");
        _6.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _12;
        _12 = new JLabel();
        _12.setIconTextGap(0);
        _12.setText("byte[0]");
        _6.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _13;
        _13 = new JLabel();
        _13.setIconTextGap(0);
        _13.setText("byte[4]");
        _6.add(_13, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _14;
        _14 = new JLabel();
        _14.setIconTextGap(0);
        _14.setText("byte[6]");
        _6.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 7, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _15;
        _15 = new JLabel();
        _15.setIconTextGap(0);
        _15.setText("byte[2]");
        _6.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _16;
        _16 = new JTextField();
        edCanData2 = _16;
        _6.add(_16, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _17;
        _17 = new JLabel();
        _17.setIconTextGap(0);
        _17.setText("byte[1]");
        _6.add(_17, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _18;
        _18 = new JLabel();
        _18.setIconTextGap(0);
        _18.setText("byte[3]");
        _6.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _19;
        _19 = new JLabel();
        _19.setIconTextGap(0);
        _19.setText("byte[5]");
        _6.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _20;
        _20 = new JLabel();
        _20.setIconTextGap(0);
        _20.setText("byte[7]");
        _6.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 8, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _21;
        _21 = new JTextField();
        edCanData1 = _21;
        _6.add(_21, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _22;
        _22 = new JTextField();
        edCanData3 = _22;
        _6.add(_22, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _23;
        _23 = new JTextField();
        edCanData5 = _23;
        _6.add(_23, new com.intellij.uiDesigner.core.GridConstraints(1, 6, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _24;
        _24 = new JTextField();
        edCanData7 = _24;
        _6.add(_24, new com.intellij.uiDesigner.core.GridConstraints(1, 8, 1, 1, 8, 1, 6, 0, null, null, null));
        final JButton _25;
        _25 = new JButton();
        btSend = _25;
        _25.setText("Send");
        _25.setMnemonic(83);
        _25.setDisplayedMnemonicIndex(0);
        _6.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 9, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _26;
        _26 = new com.intellij.uiDesigner.core.Spacer();
        _5.add(_26, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _27;
        _27 = new JPanel();
        _27.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _28;
        _28 = new JCheckBox();
        cbxShowRoughMessages = _28;
        _28.setText("Show rough messages");
        _28.setMnemonic(83);
        _28.setDisplayedMnemonicIndex(0);
        _27.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JButton _29;
        _29 = new JButton();
        btClearLog = _29;
        _29.setVerticalAlignment(0);
        _29.setText("Clear log");
        _27.add(_29, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 1, 1, 3, 1, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _30;
        _30 = new com.intellij.uiDesigner.core.Spacer();
        _27.add(_30, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
    }

}
