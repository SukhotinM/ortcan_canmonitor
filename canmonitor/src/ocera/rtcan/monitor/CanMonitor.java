/**
 * Created by IntelliJ IDEA.
 * User: Vacek
 * Date: Nov 1, 2002
 * Time: 9:30:53 AM
 * To change this template use Options | File Templates.
 */
package ocera.rtcan.monitor;

import ocera.util.xmlconfig.XmlConfig;
import ocera.util.*;
import ocera.msg.*;
import ocera.rtcan.CanMonClient;
import ocera.rtcan.msg.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import org.jx.xmlgui.XMLMenuBuilder;
import org.jx.xmlgui.XMLActionMapBuilder;
import org.jx.xmlgui.XMLAction;
import org.jx.xmlgui.XMLToolbarBuilder;
import org.flib.FLog;
import org.flib.FString;

class LogTextAreaDocumentFilter extends DocumentFilter
{
    private int maxChars;
    private int charGap;

    LogTextAreaDocumentFilter() {
        this(1000);
    }
    LogTextAreaDocumentFilter(int max_chars) {
        maxChars = max_chars;
        charGap = maxChars / 3;
    }

    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException
    {
        Document doc = fb.getDocument();
        if(doc.getLength() + str.length() > maxChars) {
            doc.remove(0, charGap);
        }
        super.insertString(fb, doc.getLength(), str, a);
    }
}

public class CanMonitor extends JFrame implements Runnable {
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

    protected JCheckBox cbxShowRoughMessages;
    boolean cbxShowRoughMessages_prev_state = false;

    private JTextField edRawMessagesId;
    private JTextField edRawMessagsMask;
    private JTextField edRawMessagesMask;
    private JPanel upperPanel;
    private JCheckBox cbxCanFlagRTR;
    private JCheckBox cbxCanFlagEXT;
    private JCheckBox cbxCanFlagOVR;
    private JCheckBox cbxCanFlagLOCAL;

    public static void main(String[] args) {
        FLog.logTreshold = FLog.LOG_ERR;
        //FLog.logTreshold = FLog.LOG_DEB;
        CanMonitor app = new CanMonitor(args);
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
        app.setSize(800, 600);
        app.setVisible(true);
//        app.pack();
    }

    private void openConfigDialog() {
        ConfigDialog xconf = new ConfigDialog(xmlConfig);
        xconf.show();
        configChanged = true;
    }

    private void processCmdLine(String[] args) {
        if (args == null) return;
        String edsFile = null;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (FString.charAt(s, 0) == '-') {
                char c = FString.charAt(s, 1);
                if (c == 'a') {
                    // address
                    i++;
                    if (i < args.length) {
                        xmlConfig.setValue("/connection/host", args[i]);
                        //canProxyIP = args[i];
                    }
                } else if (c == 'n') {
                    // node
                    i++;
                    if (i < args.length) {
                        xmlConfig.setValue("/canopen/node", args[i]);
                        //xmlConfig.getRootElement().cd("/canopen/node").setValue(args[i]);
                        //nodeNo = FString.toInt(args[i]);
                    }
                } else if (c == 'e') {
                    // EDS file name
                    i++;
                    if (i < args.length) edsFile = args[i];
                } else if (c == 'v') {
                    // verbose
                    FLog.logTreshold = FLog.LOG_INF;
                } else if (c == 'g') {
                    // debug level
                    i++;
                    if (i < args.length) FLog.logTreshold = Integer.parseInt(args[i]);
                    System.err.println("logTreshold: " + FLog.logTreshold);
                } else if (c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -e EDS_file_name -v -g debug_level\n");
                    System.exit(0);
                }
            }
        }
        if (edsFile != null) {
            openEDS(edsFile);
        }
    }

    protected void connect() {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getValue("/connection/port", "1001").trim();
        int p = FString.toInt(port);
        if (ip.length() > 0) {
            txtLog.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtLog.append(canConn.getErrMsg() + "\n");
        }
        refreshForm();
    }

    protected void disConnect() {
        canConn.disconnect();
        refreshForm();
    }

    public CanMonitor(String[] cmd_line_args) {
        super("CANopen monitor - Beta 0.9");
        this.initUI();
        boolean asserts_enabled = false;
        assert asserts_enabled = true;


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
        System.err.println("asserts enabled: " + asserts_enabled);
        System.err.println("Logging level: " + FLog.logTreshold);
        initActions();
        initGui();
        init();
        connect();
//        setBounds(50, 50, 800, 600);
    }

    /**
     * Opens configuration file, triing next locations:<br>
     */
    protected void loadConfig() {
        String conf_file = confLookup.findConfigFile();
        System.out.println("loading config from '" + conf_file + "'");
        if (conf_file != null) xmlConfig.fromURI(conf_file);
        else FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " not found.");
    }

    protected void saveConfig() {
        if (!configChanged) return;
        String conf_file = confLookup.createConfigFile();
        if (conf_file == null) {
            FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " cann't be written.");
            return;
        }

        String s = xmlConfig.toString();
        FFile ff = new FFile(conf_file);
        try {
            ff.writeString(s, FFile.MODE_OVERWRITE, FFile.OVERWRITE_FILE);
        } catch (IOException e) {
            ErrorMsg.show("Error writing config file: " + e);
        } catch (FFileException e) {
            ErrorMsg.show("Error writing config file: " + e);
        }
    }

    public void initActions() {
        //final CanMonitor app = this;

        XMLActionMapBuilder ab = new XMLActionMapBuilder(this);
        actions = ab.buildFrom("resources/menu.xml");

        XMLAction act;
        act = (XMLAction) actions.get("EdsOpen");
        if (act != null) act.actionListener = new ActionListener() {
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
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fname = fc.getSelectedFile().getAbsolutePath();
                    openEDS(fname);
                }
            }
        };

        act = (XMLAction) actions.get("EdsClose");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int ix = tabPane.getSelectedIndex();
                if (ix > 0) {
                    CANopenDevicePanel candev = (CANopenDevicePanel) tabPane.getComponentAt(ix);
                    tabPane.remove(candev);
                    //candeviceList.remove(candev);
                }
            }
        };

        act = (XMLAction) actions.get("Connect");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        };

        act = (XMLAction) actions.get("Disconnect");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disConnect();
            }
        };

        act = (XMLAction) actions.get("Config");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openConfigDialog();
            }
        };

        act = (XMLAction) actions.get("DevicesReset");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edCanID.setText("0");
                edCanData[0].setText("1");
                edCanData[1].setText("0");
                btSend.getActionListeners()[0].actionPerformed(null);
            }
        };

        act = (XMLAction) actions.get("Quit");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cleanUp();
                System.gc();
                System.exit(0);
            }
        };
    }

    private void openEDS(String fname) {
        int ix = tabPane.getTabCount();
        // find free node number
        int node = 0;
        for (int i = 1; i < ix; i++) {
            CANopenDevicePanel p = (CANopenDevicePanel) tabPane.getComponentAt(i);
            int nd = p.getNodeID();
            if (nd > node) node = nd;
        }
        CANopenDevicePanel candev = new CANopenDevicePanel(CanMonitor.this, ix);
        tabPane.add(candev);
        tabPane.setSelectedIndex(ix);    // select tab EDS
        candev.openEDS(fname);
        candev.setNodeID(++node);
    }

    public void initGui() {
        pane = getContentPane();

        //==========================================================
        // menu bar
        //==========================================================
        XMLMenuBuilder builder = new XMLMenuBuilder(this, actions);
        setJMenuBar(builder.buildFrom("resources/menu.xml"));

        //==========================================================
        // tool bar
        //==========================================================
        XMLToolbarBuilder tbb = new XMLToolbarBuilder(this, actions);
        JToolBar tb = tbb.buildFrom("resources/menu.xml");
        pane.add(tb, BorderLayout.NORTH);

        //==========================================================
        //        layout
        //==========================================================
        tabPane.setSelectedIndex(0);    // select tab CAN
        pane.add(tabPane, BorderLayout.CENTER);

        pane.add(statusBar.mainPanel, BorderLayout.SOUTH);
        enableControls();

        //==========================================================
        //        btSend listenner
        //==========================================================
        btSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CANDtgMsg msg = new CANDtgMsg();
                msg.id = FString.toInt(edCanID.getText());
                for (msg.length = 0; msg.length < CANDtgMsg.DATA_LEN_MAX; ) {
                    String s = edCanData[msg.length].getText().trim();
                    if (s.length() == 0) break;
                    msg.data[msg.length++] = (byte) FString.toInt(s, 10);
                }
                if (cbxCanFlagRTR.isSelected()) msg.flags |= CANDtgMsg.MSG_RTR;
                if (cbxCanFlagEXT.isSelected()) msg.flags |= CANDtgMsg.MSG_EXT;
                if (cbxCanFlagOVR.isSelected()) msg.flags |= CANDtgMsg.MSG_OVR;
                if (cbxCanFlagLOCAL.isSelected()) msg.flags |= CANDtgMsg.MSG_LOCAL;
                txtLog.append("SENDING:\t" + msg);
                canConn.send(msg);
            }
        });

        btClearLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });

        //==========================================================
        //        cbxShowRoughMessages listenner
        //==========================================================
        cbxShowRoughMessages.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                ServiceSetRawMsgParamsRequest rq = new ServiceSetRawMsgParamsRequest();
                boolean state = cb.isSelected();
                // one click on checkbox generate 5 listenner calls
                if (state != cbxShowRoughMessages_prev_state) {
                    cbxShowRoughMessages_prev_state = state;
                    if (state == true) {
                        rq.command = ServiceSetRawMsgParamsRequest.ADD;
                        rq.id = FString.toInt(edRawMessagesId.getText(), 16);
                        rq.mask = FString.toInt(edRawMessagesMask.getText(), 16);
                        cb.setBackground(Color.ORANGE);
                    } else {
                        rq.command = ServiceSetRawMsgParamsRequest.REMOVE_ALL;
                    }
                    //txtLog.append("event " + cb.isSelected() + "\n");
                    txtLog.append("SENDING:\t" + rq);
                    canConn.send(rq);
                }
            }
        });

        //==========================================================
        //        JTextArea logs listenners
        //==========================================================
        int logsize = FString.toInt(xmlConfig.getValue("/logging/logscreensize", "1000000"));
        ((AbstractDocument) txtLog.getDocument()).setDocumentFilter(new LogTextAreaDocumentFilter(logsize));

        //==========================================================
        //        JFrame listenners
        //==========================================================
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actions.get("Quit").actionPerformed(null);
            }
        });
    }

    private void refreshForm() {
        boolean connected = (canConn.getSocket() != null);
        XMLAction act;
        act = (XMLAction) actions.get("Connect");
        act.setEnabled(!connected);
        act = (XMLAction) actions.get("Disconnect");
        act.setEnabled(connected);
        if (!connected) {
            statusBar.lbl1.setText("disconnected");
        } else {
            statusBar.lbl1.setText(canConn.getSocket().toString());
        }
        statusBar.lbl2.setText("");
        statusBar.lbl3.setText("");
    }

    public void setTabLabel(int tabix, String lbl) {
        tabPane.setTitleAt(tabix, lbl);
    }

    private void enableControls() {
    }

    public void init() {
        canConn.setGuiUpdate(this);
//        canConn.connect(serverAddr, 0);

        Thread t = new Thread(this);
        t.start();
    }

    public void cleanUp() {
//        if(canReaderThread != null)
//            try {canReaderThread.join();} catch(InterruptedException e) {}
        disConnect();
        saveConfig();
    }

    /**
     * sends msg to the socket if it is opened
     */
    void sendMessage(Object o) {
        if (canConn == null) return;
        if (!canConn.connected()) return;
        canConn.send(o);
    }

    private int msgCount = 0;

    /**
     * code refreshing GUI controls from variables set by other thread
     * mainly CanMonClient thread
     */
    public void run() {
        // read all received messages if any
        while (true) {
            Object o = canConn.readQueue.remove(RoundQueue.NO_BLOCKING);
            if (o == null) break;
            FLog.log("CanMonitor", FLog.LOG_DEB, "received object " + o);

            if (o instanceof CANDtgMsg) {
                msgCount++;
                if (cbxShowRoughMessages.isSelected()) txtLog.append("RECEIVE[" + msgCount + "]:\t" + o);
            } else if (o instanceof ServiceSetRawMsgParamsConfirm) {
                cbxShowRoughMessages.setBackground(upperPanel.getBackground());
                ServiceSetRawMsgParamsConfirm spc = (ServiceSetRawMsgParamsConfirm) o;
                if (spc.code != ServiceSetRawMsgParamsConfirm.OK) {
                    ErrorMsg.show(tabPane, spc.errmsg);
                }
            } else {
                // scan all CANopen devices
                for (int i = 1; i < tabPane.getTabCount(); i++) {
                    CANopenDevicePanel p = (CANopenDevicePanel) tabPane.getComponentAt(i);
                    if (p.tasteObject(o)) break;
                }
            }
        }
        // refresh form
        refreshForm();
        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "run(): GUI updated");
    }

    private void initUI() {
        tabPane = new JTabbedPane();
        tabPane.setTabLayoutPolicy(0);
        tabPane.setTabPlacement(1);
        final JPanel rootPanel = new JPanel();
        GridBagLayout rootGridLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill   = GridBagConstraints.BOTH;
        c.gridheight = 3;
        c.gridwidth  = 1;
        c.gridx = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;
        c.weightx = 1.0;
        c.weighty = 0.02;
        rootPanel.setLayout(rootGridLayout);//new GridLayoutManager(3, 1, new Insets(3, 3, 0, 0), 3, -1));
        tabPane.addTab("CAN", rootPanel);

        upperPanel = new JPanel();
        GridBagLayout upperGridLayout = new GridBagLayout();
        GridBagConstraints cUp = new GridBagConstraints();
        cUp.anchor = GridBagConstraints.WEST;
        cUp.fill   = GridBagConstraints.HORIZONTAL;
        cUp.gridheight = 1;
        cUp.gridwidth  = 4;
        cUp.gridx = GridBagConstraints.RELATIVE;
        cUp.gridy = 1;
        cUp.insets = new Insets(0, 0, 0, 0);
        cUp.ipadx = 0;
        cUp.ipady = 0;
        cUp.weightx = 0.2;
        cUp.weighty = 1.0;

        upperPanel.setLayout(upperGridLayout);
        rootGridLayout.setConstraints(upperPanel, c);
        rootPanel.add(upperPanel);
        cbxShowRoughMessages = new JCheckBox();
        cbxShowRoughMessages.setAutoscrolls(false);
        cbxShowRoughMessages.setBorderPainted(false);
        cbxShowRoughMessages.setBorderPaintedFlat(false);
        cbxShowRoughMessages.setContentAreaFilled(true);
        cbxShowRoughMessages.setEnabled(true);
        cbxShowRoughMessages.setSelected(false);
        cbxShowRoughMessages.setText("Show rough messages");
        cbxShowRoughMessages.setMnemonic('S');
        cbxShowRoughMessages.setDisplayedMnemonicIndex(0);
        upperGridLayout.setConstraints(cbxShowRoughMessages, cUp);
        upperPanel.add(cbxShowRoughMessages);

        final JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JLabel label10 = new JLabel();
        label10.setText("ID");
        idPanel.add(label10);
        edRawMessagesId = new JTextField();
        edRawMessagesId.setMinimumSize(new Dimension(80, 30));
        edRawMessagesId.setPreferredSize(new Dimension(80, 30));
        edRawMessagesId.setText("");
        idPanel.add(edRawMessagesId);
        final JLabel label12 = new JLabel();
        label12.setText("hex");
        idPanel.add(label12);
        cUp.weightx = 0.1;
        upperGridLayout.setConstraints(idPanel, cUp);
        upperPanel.add(idPanel);


        final JPanel maskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JLabel label11 = new JLabel();
        label11.setText("Mask");
        maskPanel.add(label11);
        edRawMessagesMask = new JTextField();
        edRawMessagesMask.setMinimumSize(new Dimension(80, 30));
        edRawMessagesMask.setPreferredSize(new Dimension(80, 30));
        maskPanel.add(edRawMessagesMask);
        final JLabel label13 = new JLabel();
        label13.setText("hex");
        maskPanel.add(label13);
        cUp.weightx = 0.1;
        upperGridLayout.setConstraints(maskPanel, cUp);
        upperPanel.add(maskPanel);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btClearLog = new JButton();
        btClearLog.setText("Clear log");
        btClearLog.setVerticalAlignment(0);
        buttonPanel.add(btClearLog);
        cUp.weightx = 0.5;
        upperGridLayout.setConstraints(buttonPanel, cUp);
        upperPanel.add(buttonPanel);


        final JScrollPane scrollTextArea = new JScrollPane();
        c.weighty = 0.9;
        rootGridLayout.setConstraints(scrollTextArea, c);
        rootPanel.add(scrollTextArea);
        txtLog = new JTextArea();
        scrollTextArea.setViewportView(txtLog);


        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        c.weighty = 0.05;
        rootGridLayout.setConstraints(bottomPanel, c);
        rootPanel.add(bottomPanel);



        final JPanel inputPanel = new JPanel();
        GridBagLayout inputGridLayout = new GridBagLayout();
        GridBagConstraints cIn = new GridBagConstraints();
        inputPanel.setLayout(inputGridLayout);
        bottomPanel.add(inputPanel);

        cIn.anchor = GridBagConstraints.WEST;
        cIn.fill   = GridBagConstraints.HORIZONTAL;
        cIn.gridheight = 1;
        cIn.gridwidth  = 1;
        cIn.gridx = GridBagConstraints.RELATIVE;
        cIn.gridy = 1;
        cIn.insets = new Insets(0, 0, 0, 8);
        cIn.ipadx = 0;
        cIn.ipady = 0;
        cIn.weightx = 1.0;
        cIn.weighty = 1.0;

        final JLabel label1 = new JLabel();
        label1.setIconTextGap(0);
        label1.setText("ID");
        cIn.gridx = 1;
        inputGridLayout.setConstraints(label1, cIn);
        inputPanel.add(label1);
        final JLabel label2 = new JLabel();
        label2.setIconTextGap(0);
        label2.setText("byte[0]");
        cIn.gridx = 2;
        inputGridLayout.setConstraints(label2, cIn);
        inputPanel.add(label2);
        final JLabel label6 = new JLabel();
        label6.setIconTextGap(0);
        label6.setText("byte[1]");
        cIn.gridx = 3;
        inputGridLayout.setConstraints(label6, cIn);
        inputPanel.add(label6);
        final JLabel label5 = new JLabel();
        label5.setIconTextGap(0);
        label5.setText("byte[2]");
        cIn.gridx = 4;
        inputGridLayout.setConstraints(label5, cIn);
        inputPanel.add(label5);
        final JLabel label7 = new JLabel();
        label7.setIconTextGap(0);
        label7.setText("byte[3]");
        cIn.gridx = 5;
        inputGridLayout.setConstraints(label7, cIn);
        inputPanel.add(label7);
        final JLabel label3 = new JLabel();
        label3.setIconTextGap(0);
        label3.setText("byte[4]");
        cIn.gridx = 6;
        inputGridLayout.setConstraints(label3, cIn);
        inputPanel.add(label3);
        final JLabel label8 = new JLabel();
        label8.setIconTextGap(0);
        label8.setText("byte[5]");
        cIn.gridx = 7;
        inputGridLayout.setConstraints(label8, cIn);
        inputPanel.add(label8);
        final JLabel label4 = new JLabel();
        label4.setIconTextGap(0);
        label4.setText("byte[6]");
        cIn.gridx = 8;
        inputGridLayout.setConstraints(label4, cIn);
        inputPanel.add(label4);
        final JLabel label9 = new JLabel();
        label9.setIconTextGap(0);
        label9.setText("byte[7]");
        cIn.gridx = 9;
        inputGridLayout.setConstraints(label9, cIn);
        inputPanel.add(label9);

        cbxCanFlagRTR = new JCheckBox();
        cbxCanFlagRTR.setText("RTR");
        cIn.gridx = 10;
        inputGridLayout.setConstraints(cbxCanFlagRTR, cIn);
        inputPanel.add(cbxCanFlagRTR);
        cbxCanFlagOVR = new JCheckBox();
        cbxCanFlagOVR.setText("OVR");
        cIn.gridx = 11;
        inputGridLayout.setConstraints(cbxCanFlagOVR, cIn);
        inputPanel.add(cbxCanFlagOVR);


        cIn.gridy = 2;

        edCanID = new JTextField(0);
        edCanID.setPreferredSize(new Dimension(50, 30));
        cIn.gridx = 1;
        inputGridLayout.setConstraints(edCanID, cIn);
        inputPanel.add(edCanID);
        edCanData0 = new JTextField(0);
        edCanData0.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 2;
        inputGridLayout.setConstraints(edCanData0, cIn);
        inputPanel.add(edCanData0);
        edCanData1 = new JTextField();
        edCanData1.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 3;
        inputGridLayout.setConstraints(edCanData1, cIn);
        inputPanel.add(edCanData1);
        edCanData2 = new JTextField();
        edCanData2.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 4;
        inputGridLayout.setConstraints(edCanData2, cIn);
        inputPanel.add(edCanData2);
        edCanData3 = new JTextField();
        edCanData3.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 5;
        inputGridLayout.setConstraints(edCanData3, cIn);
        inputPanel.add(edCanData3);
        edCanData4 = new JTextField();
        edCanData4.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 6;
        inputGridLayout.setConstraints(edCanData4, cIn);
        inputPanel.add(edCanData4);
        edCanData5 = new JTextField();
        edCanData5.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 7;
        inputGridLayout.setConstraints(edCanData5, cIn);
        inputPanel.add(edCanData5);
        edCanData6 = new JTextField();
        edCanData6.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 8;
        inputGridLayout.setConstraints(edCanData6, cIn);
        inputPanel.add(edCanData6);
        edCanData7 = new JTextField();
        edCanData7.setPreferredSize(new Dimension(40, 30));
        cIn.gridx = 9;
        inputGridLayout.setConstraints(edCanData7, cIn);
        inputPanel.add(edCanData7);


        cbxCanFlagEXT = new JCheckBox();
        cbxCanFlagEXT.setText("EXT");
        cIn.gridx = 10;
        inputGridLayout.setConstraints(cbxCanFlagEXT, cIn);
        inputPanel.add(cbxCanFlagEXT);
        cbxCanFlagLOCAL = new JCheckBox();
        cbxCanFlagLOCAL.setText("LOCAL");
        cIn.gridx = 11;
        inputGridLayout.setConstraints(cbxCanFlagLOCAL, cIn);
        inputPanel.add(cbxCanFlagLOCAL);

        btSend = new JButton();
        btSend.setText("Send");
        btSend.setMnemonic('S');
        btSend.setDisplayedMnemonicIndex(0);
        inputGridLayout.setConstraints(btSend, cIn);
        bottomPanel.add(btSend);


    }
}
