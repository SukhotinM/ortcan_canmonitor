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
import ocera.rtcan.eds.*;
import ocera.rtcan.CanMonClient;
import ocera.rtcan.CanMsg;
import ocera.rtcan.CanOpen.ODNode;
import ocera.rtcan.CanOpen.ObjectDictionary;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * document for tblProp
 */
class AttrModel extends AbstractTableModel
 {
    protected EdsTreeNode attrNode = null;
    public static final String[] attrColNames = {"Name", "Value", "Description"};

    public void setAttrNode(EdsTreeNode attrNode) {
        this.attrNode = attrNode;
    }

    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        if(attrNode == null) return 0;
        return attrNode.getAttrCnt();
    }

    public String getColumnName(int column) {
        return attrColNames[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if(attrNode == null) return "<NULL>";   //model has not assigned node
        if(columnIndex == 0) return attrNode.getAttrName(rowIndex);
        else if(columnIndex == 1) return attrNode.getAttrValue(rowIndex);
        else if(columnIndex == 2) return attrNode.getAttrDescription(rowIndex);
        else return "<NULL>";
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if(attrNode == null) return;
        if(columnIndex == 1) {
            attrNode.setAttrValue(rowIndex, value.toString());
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public boolean isCellEditable(int row, int col) {
        if(col > 0) return true;
        return false;
    }
}

public class CanMonitor extends JFrame implements Runnable {

    protected CanMonClient canConn = new CanMonClient();
    protected ODNode selectedObject = null;
    //protected int nodeNo = 1;  // number of the node to communicate with

    protected Container pane;
    protected ActionMap actions = new ActionMap();
    protected ObjectDictionary objectDictionary = new ObjectDictionary();

    protected JPanel panSDOTable;
    protected JTextField edSDO;
    protected JButton btUploadSDO;
    protected JButton btDownloadSDO;
    protected JTable tblProp;
    protected JTextField edCanID;
    protected JTextField edCanData0;
    protected JTextField  edCanData1;
    protected JTextField  edCanData2;
    protected JTextField edCanData3;
    protected JTextField edCanData4;
    protected JTextField edCanData5;
    protected JTextField edCanData6;
    protected JTextField edCanData7;
    protected JTextField[] edCanData = new JTextField[8];
    protected JButton btSend;
    protected JTextField edNodeID;

    protected JTabbedPane tabPane;
    protected JTree treeEds;
    protected DefaultTreeModel treeEdsModel;
    protected DefaultMutableTreeNode treeEdsRootNode = new DefaultMutableTreeNode("root");
    protected JTextArea txtMsg;
    protected AttrModel attrModel = new AttrModel();
    protected JCheckBox cbxShowRoughMessages;

    protected XmlConfig xmlConfig = new XmlConfig();
    public static final String CONFIG_FILE_NAME = "CanMonitor.conf.xml";
    //protected String configFile = "N/A";
    protected boolean configChanged = false;
    private ConfigLookup confLookup = new ConfigLookup(".canmonitor", CONFIG_FILE_NAME, this.getClass());
    //private JTabbedPane panelMain;
    private CanMonStatusBar statusBar = new CanMonStatusBar();
    private short[] valueProcessedByDownload = null;

    public static void main (String [] args)
    {
        FLog.logTreshold = FLog.LOG_DEB;
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
                if(c == 'n') {
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
                else if(c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -e EDS_file_name\n");
                    System.exit(0);
                }
            }
        }
        if(edsFile != null) {
            treeEdsRootNode.setUserObject(edsFile);
//            openEDS(edsFile);
        }
    }

    protected void connect()
    {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getValue("/connection/port", "1001").trim();
        int p = FString.toInt(port);
        if(ip.length() > 0) {
            txtMsg.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtMsg.append(canConn.getErrMsg() + "\n");
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
                            openEDS(fname);
                            tabPane.setSelectedIndex(0);    // select tab EDS
                        }

                    }
                };
        actions.put("OpenEds", act);

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

        act = new AbstractAction("ResetDevice") {
                    public void actionPerformed(ActionEvent e) {
                        edCanID.setText("0");
                        edCanData[0].setText("1");
                        edCanData[1].setText("0");
                        btSend.getActionListeners()[0].actionPerformed(null);
                    }
                };
        actions.put("ResetDevice", act);

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
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem itm = new JMenuItem(actions.get("OpenEds")); menu.add(itm);
        menu.addSeparator();
        itm = new JMenuItem(actions.get("Connect")); menu.add(itm);
        itm = new JMenuItem(actions.get("Disconnect")); menu.add(itm);
        menu.addSeparator();
        itm = new JMenuItem(actions.get("Quit")); menu.add(itm);

        menu = new JMenu("Tools");
        menuBar.add(menu);
        itm = new JMenuItem(actions.get("Config")); menu.add(itm);
        menu.addSeparator();
        itm = new JMenuItem(actions.get("ResetDevice")); menu.add(itm);

        //==========================================================
        // tool bar
        //==========================================================
        JToolBar tb = new JToolBar();
        tb.add(actions.get("OpenEds"));
        //tb.addSeparator();
        //tb.add(new JSeparator(SwingConstants.VERTICAL));
        /*
        tb.add(new Box.Filler(new Dimension(0, 0), new Dimension(5000, 5), new Dimension(10000, 1000)));
        tb.add(new JLabel("node ID: "));
        tb.addSeparator(new Dimension(2, 0));
        edNodeID = new JTextField(xmlConfig.getValue("/canopen/node", "1"));
        edNodeID.setMinimumSize(new Dimension(30, 10));
        //edNodeID.setPreferredSize(new Dimension(10, edNodeID.getPreferredSize().height));
        //edNodeID.setMaximumSize(new Dimension(100, 1000));
        tb.add(edNodeID);
        */
        pane.add(tb, BorderLayout.NORTH);

        treeEdsModel = new DefaultTreeModel(treeEdsRootNode);
        //treeEds = new JTree(treeEdsModel);
        treeEds.setShowsRootHandles(true);
        treeEds.setModel(treeEdsModel);

        tblProp.setModel(attrModel);

        edNodeID.setText(xmlConfig.getValue("/canopen/node", "1"));

        //==========================================================
        //        layout
        //==========================================================
        tabPane.setSelectedIndex(1);    // select tab CAN
        pane.add(tabPane, BorderLayout.CENTER);

        pane.add(statusBar.panel, BorderLayout.SOUTH);
        enableControls();

        //==========================================================
        //        JTree listenners
        //==========================================================
        treeEds.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                selectedObject = null;
                if(e.isAddedPath()) {
//                    System.out.println(path.getLastPathComponent().getClass());
                    EdsTreeNode nd = null;
                    try {
                        nd = (EdsTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
                        System.out.println("Vybrany node " + nd);
                    } catch (java.lang.ClassCastException ex) {
                        // tree root node selected
//                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    if(nd instanceof ODNode) selectedObject = (ODNode)nd;
                    enableControls();
                    refreshForm();
                    attrModel.setAttrNode(nd);
                    attrModel.fireTableDataChanged();
                }
            }
        });

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
                txtMsg.append("SENDING:\t" + msg + "\n");
                canConn.send(msg);
            }
        });

        //==========================================================
        //        btUploadSDO listenner
        //==========================================================
        // {SDOR UPLOAD server_port client_port node index subindex}
        // server_port == 0, client_port == 0 means: use default values for SDO communication
        btUploadSDO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String s = Integer.toString(selectedObject.index, 16);
                s += " " + Integer.toString(selectedObject.subIndex, 16);
                String node = edNodeID.getText();
                String msg = "{SDOR UPLOAD 0 0 " + node + " " + s + "}";
                txtMsg.append("SENDING:\t" + msg + "\n");
                canConn.send(msg);
            }
        });

        //==========================================================
        //        btDownloadSDO listenner
        //==========================================================
        // {SDOR DOWNLOAD server_port client_port node index subindex [dd dd ...]}
        btDownloadSDO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String node = edNodeID.getText();
                String msg = "{SDOR DOWNLOAD 0 0 ";
                msg += node + " ";
                msg += Integer.toString(selectedObject.index, 16) + " ";
                msg += Integer.toString(selectedObject.subIndex, 16) + " ";
                String bytes =  edSDO.getText();
                msg += "[" + bytes + "]}";
                txtMsg.append("SENDING:\t" + msg + "\n");
                canConn.send(msg);
                valueProcessedByDownload = ODNode.string2ValArray2(bytes);
                //FLog.log("CanMonitor", FLog.LOG_TRASH, "valueProcessedByDownload = " + bytes);
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
        if(selectedObject == null) edSDO.setText("");
        else edSDO.setText(selectedObject.valToString());

        if(canConn.getSocket() == null)
            statusBar.lbl1.setText("disconnected");
        else
            statusBar.lbl1.setText(canConn.getSocket().toString());
        statusBar.lbl2.setText("");
        statusBar.lbl3.setText("");
    }

    private void enableControls()
    {
        if(selectedObject == null || selectedObject.subNumber > 0) {
            btUploadSDO.setEnabled(false);
            btDownloadSDO.setEnabled(false);
        }
        else {
            btUploadSDO.setEnabled(selectedObject.accessType < ODNode.ACCES_TYPE_WO);
            btDownloadSDO.setEnabled(selectedObject.accessType > ODNode.ACCES_TYPE_RO);
        }
    }

    public void init()
    {
        if(treeEdsRootNode.getUserObject() != null) {
            String s = (String)treeEdsRootNode.getUserObject();
            // if EDS file is not assigned from command line user object name is 'root'
            if(!s.equalsIgnoreCase("root")) openEDS((String)treeEdsRootNode.getUserObject());
        }
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

    private void openEDS(String fname)
    {
        System.out.println("opening " + fname);
        System.out.println("start at " + new Date());

        treeEdsRootNode.removeAllChildren();
        treeEdsModel.reload();  // removeAllChildren is not enough do delete whole tree, you should call reload() afterwards
        treeEdsRootNode.setUserObject(fname);

        LinkedList odlst = new LinkedList();

        String ss[];
        try {
            FFile file = new FFile(fname);
            ss = file.toStringArray();
        }
        catch(FileNotFoundException e) {
//            new ErrorMsg(this).show("File not found: " + fname);
            new ErrorMsg(this).show(e.toString());
            return;
        }
        catch (IOException e ) {
            new ErrorMsg(this).show(e.toString());
            return;
        }

        // fill tree
        LinkedList attlist = new LinkedList();
        LinkedList subndlist = new LinkedList();
        ODNode odnd = null, odsubnd = null;
        EdsNode edsnd = null;
        String s, s1;
        int index, subindex;
        // scan file lines
        for (int i = 0; ; i++) {
            if(i < ss.length) {
                s = ss[i].trim();
                if(s.length() == 0) continue; // skip empty lines
            }
            else s = null;

            // parse line
            if(i == ss.length || s.charAt(0) == '[') {
                // new node definition or end of file

                // resolve mew index & subindex
                index = subindex = -1;
                if(i < ss.length) {
                    s = FString.slice(s, 1, -1);
                    char c = s.charAt(0);
                    if(c < '0' || c > '9') {
                        //EDS text node
                    }
                    else {
                        // index or subindex
                        String slice[] = StringParser.cutInt("0x" + s);
                        if(slice[1].trim().length() == 0) {
                            //index
                            index = FString.toInt(slice[0]);
                        }
                        else {
                            if(slice[1].length() > 3) {
                                if(slice[1].substring(0, 3).equalsIgnoreCase("sub")) {
                                    // subindex
                                    slice[1] = "0x" + slice[1].substring(3);
                                    index = FString.toInt(slice[0]);
                                    subindex = FString.toInt(slice[1]);
                                }
                            }
                        }
                    }
                }

                //close curent section
                if(edsnd != null) {
                    // close EDS text node
                    edsnd.attributes = (EdsAttribute[]) attlist.toArray(new EdsAttribute[attlist.size()]);
                    //edsList.addNode(edsnd);
                    edsnd = null;
                }
                else if(odnd != null) {
                    if(odsubnd != null) {
                        // close subindex definition
                        subndlist.add(odsubnd);
                        odsubnd = null;
                    }
                    if(odnd.index != index) {
                        // close index definition
                        odnd.subNodes = (ODNode[]) subndlist.toArray(new ODNode[subndlist.size()]);
                        odlst.add(odnd);
                        subndlist = new LinkedList();
                        odnd = null;
                    }
                }

                if(i == ss.length) break;   // end of file

                // start new section
                if(s.length() == 0) break;

                if(index < 0) {
                    //EDS text node
                    attlist.clear();
                    edsnd = new EdsNode(s);
                    treeEdsRootNode.add(new DefaultMutableTreeNode(edsnd));
                }
                else {
                    // index or subindex
                    if(index != -1 && subindex == -1) {
                        //index
                        odnd = new ODNode();
                        //tnd = new DefaultMutableTreeNode(odnd);
                        //treeEdsRootNode.add(tnd);
                        odnd.index = index;
                        odnd.subIndex = 0;
                    }
                    else if(index != -1 && subindex != -1) {
                        //subindex
                        odsubnd = new ODNode();
                        odsubnd.subObject = true;
//                        odsubnd.type = ODNode.STRUCT_ITEM;
                        //odlst.add(odsubnd);
                        //if(tnd != null)
                        //    tnd.add(new DefaultMutableTreeNode(odsubnd));
                        odsubnd.index = index;
                        odsubnd.subIndex = subindex;
                    }
                }
            }
            else {
                // parse attribute
                int ix = s.indexOf('=');
                if(ix < 0) {
                    s1 = "";
                    s = s.trim();
                }
                else {
                    s1 = FString.slice(s, ix + 1).trim();
                    s = FString.slice(s, 0, ix).trim();
                }

                if(edsnd != null) {
                    EdsAttribute att = new EdsAttribute(s, s1);
                    attlist.add(att);
                }
                else if(odsubnd != null) {
                    odsubnd.setAttr(s, s1);
                }
                else if(odnd != null) {
                    odnd.setAttr(s, s1);
                }
            }
        }

        ODNode[] odir = (ODNode[])odlst.toArray(new ODNode[odlst.size()]);
        Arrays.sort(odir);
        objectDictionary.setOd(odir);

        DefaultMutableTreeNode tnd = null;
        // add nodes to the tree
        //int oldix = -1;
        for(int i = 0; i < odir.length; i++) {
            ODNode nd = odir[i];
            //if(nd.index == oldix) continue;

            //oldix = nd.index;
            tnd = new DefaultMutableTreeNode(nd);
            if(nd.subNodes != null) {
//                nd.type = ODNode.STRUCT_NODE;
                nd.subIndex = -1;
                for(int j = 0; j < nd.subNodes.length; j++) {
    //                FLog.log("CanMonitor", FLog.LOG_ERR, "pocet subnodu: " + nd.subNodes.length);
                    ODNode snd = nd.subNodes[j];
                    tnd.add(new DefaultMutableTreeNode(snd));
                }
            }
            treeEdsRootNode.add(tnd);
        }
        treeEds.expandRow(0);   // expand root node
        System.out.println("finish at " + new Date());
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
            s = FString.slice(s, 1, -1);
            String ss[];
            if(s.matches("CANDTG.*")) {
                // found CAN msg start
                if(!cbxShowRoughMessages.isSelected()) continue;

                txtMsg.append("RECEIVE[" + msgCount + "]:\t" + s + "\n");
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
                txtMsg.append("\t" + canmsg + "\n");
            }
            else if(s.matches("SDOC.*")) {
                // found SDO msg start
                // {SDOC UPLOAD server_port client_port node index subindex [...]}
                // {SDOC DOWNLOAD server_port client_port node index subindex}
                int index, subindex;
                short[] value = null;
                boolean upload = false;
                txtMsg.append("RECEIVE[" + msgCount + "]:\t" + s + "\n");
                s = s.substring(5).trim();
                if(s.matches("UPLOAD.*")) {
                    s = s.substring(7); upload = true;
                }
                else if(s.matches("DOWNLOAD.*")) s = s.substring(9);

                ss = StringParser.cutInt(s, 16); s = ss[1]; //server_port
                ss = StringParser.cutInt(s, 16); s = ss[1]; //client_port
                ss = StringParser.cutInt(s, 16); s = ss[1]; //node
                ss = StringParser.cutInt(s, 16); s = ss[1]; //index
                index = FString.toInt(ss[0], 16);
                ss = StringParser.cutInt(s, 16); s = ss[1]; //subindex
                subindex = FString.toInt(ss[0], 16);
                // check abort
                s = s.trim();
                if(s.matches("ABORT.*")) {
                    new ErrorMsg(this).show(s);
                }
                else if(s.matches("ERROR.*")) {
                    new ErrorMsg(this).show(s);
                }
                else {
                    if(upload) {
                        if(s.charAt(0) == '[') {
                            s = FString.slice(s, 1, -1);
                            value = ODNode.string2ValArray2(s);
                        }
                        else {
                            value = new short[0];
                        }
                        objectDictionary.setValue(index, subindex, value);
                        refreshForm();
                    }
                    else {
                        // succesful download, store new value aloso to OD
                        if(valueProcessedByDownload != null) {
                            objectDictionary.setValue(index, subindex, valueProcessedByDownload);
                        }
                    }
                }
            }
            else {
                FLog.logcont(FLog.LOG_DEB, "unknown message type: " + s);
            }
            valueProcessedByDownload = null;
            refreshForm();
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
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.addTab("EDS", _2);
        final JSplitPane _3;
        _3 = new JSplitPane();
        _3.setDividerSize(8);
        _3.setDividerLocation(164);
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.setRightComponent(_4);
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _4.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 6, new Insets(2, 3, 0, 3), 5, -1));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JTextField _7;
        _7 = new JTextField();
        edSDO = _7;
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 1, 6, 6, null, null, null));
        final JButton _8;
        _8 = new JButton();
        btUploadSDO = _8;
        _8.setMargin(new Insets(2, 5, 2, 5));
        _8.setText("Upload");
        _8.setMnemonic(85);
        _8.setDisplayedMnemonicIndex(0);
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _9;
        _9 = new JButton();
        btDownloadSDO = _9;
        _9.setMargin(new Insets(2, 5, 2, 5));
        _9.setText("Download");
        _9.setMnemonic(68);
        _9.setDisplayedMnemonicIndex(0);
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("node");
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _11;
        _11 = new JTextField();
        edNodeID = _11;
        _6.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 8, 1, 0, 0, null, new Dimension(20, -1), null));
        final com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 0, 1, null, new Dimension(30, -1), null));
        final JScrollPane _13;
        _13 = new JScrollPane();
        _5.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTable _14;
        _14 = new JTable();
        tblProp = _14;
        _13.setViewportView(_14);
        final JScrollPane _15;
        _15 = new JScrollPane();
        _3.setLeftComponent(_15);
        final JTree _16;
        _16 = new JTree();
        treeEds = _16;
        _15.setViewportView(_16);
        final JPanel _17;
        _17 = new JPanel();
        _17.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 5, 3, 5), -1, -1));
        _1.addTab("CAN", _17);
        final JPanel _18;
        _18 = new JPanel();
        _18.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _17.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _19;
        _19 = new JCheckBox();
        cbxShowRoughMessages = _19;
        _19.setText("Show rough messages");
        _19.setMnemonic(83);
        _19.setDisplayedMnemonicIndex(0);
        _18.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JScrollPane _20;
        _20 = new JScrollPane();
        _18.add(_20, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTextArea _21;
        _21 = new JTextArea();
        txtMsg = _21;
        _20.setViewportView(_21);
        final JPanel _22;
        _22 = new JPanel();
        _22.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _18.add(_22, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _23;
        _23 = new JPanel();
        _23.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 10, new Insets(0, 0, 0, 0), 5, 0));
        _22.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 0, 0, null, null, null));
        final JTextField _24;
        _24 = new JTextField();
        edCanID = _24;
        _23.add(_24, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 1, 6, 6, new Dimension(50, -1), null, null));
        final JTextField _25;
        _25 = new JTextField();
        edCanData0 = _25;
        _23.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 6, 0, null, null, null));
        final JTextField _26;
        _26 = new JTextField();
        edCanData4 = _26;
        _23.add(_26, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _27;
        _27 = new JTextField();
        edCanData6 = _27;
        _23.add(_27, new com.intellij.uiDesigner.core.GridConstraints(1, 7, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _28;
        _28 = new JLabel();
        _28.setText("ID");
        _28.setIconTextGap(0);
        _23.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _29;
        _29 = new JLabel();
        _29.setText("byte[0]");
        _29.setIconTextGap(0);
        _23.add(_29, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _30;
        _30 = new JLabel();
        _30.setText("byte[4]");
        _30.setIconTextGap(0);
        _23.add(_30, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _31;
        _31 = new JLabel();
        _31.setText("byte[6]");
        _31.setIconTextGap(0);
        _23.add(_31, new com.intellij.uiDesigner.core.GridConstraints(0, 7, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _32;
        _32 = new JLabel();
        _32.setText("byte[2]");
        _32.setIconTextGap(0);
        _23.add(_32, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _33;
        _33 = new JTextField();
        edCanData2 = _33;
        _23.add(_33, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _34;
        _34 = new JLabel();
        _34.setText("byte[1]");
        _34.setIconTextGap(0);
        _23.add(_34, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _35;
        _35 = new JLabel();
        _35.setText("byte[3]");
        _35.setIconTextGap(0);
        _23.add(_35, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _36;
        _36 = new JLabel();
        _36.setText("byte[5]");
        _36.setIconTextGap(0);
        _23.add(_36, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _37;
        _37 = new JLabel();
        _37.setText("byte[7]");
        _37.setIconTextGap(0);
        _23.add(_37, new com.intellij.uiDesigner.core.GridConstraints(0, 8, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _38;
        _38 = new JTextField();
        edCanData1 = _38;
        _23.add(_38, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _39;
        _39 = new JTextField();
        edCanData3 = _39;
        _23.add(_39, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _40;
        _40 = new JTextField();
        edCanData5 = _40;
        _23.add(_40, new com.intellij.uiDesigner.core.GridConstraints(1, 6, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _41;
        _41 = new JTextField();
        edCanData7 = _41;
        _23.add(_41, new com.intellij.uiDesigner.core.GridConstraints(1, 8, 1, 1, 8, 1, 6, 0, null, null, null));
        final JButton _42;
        _42 = new JButton();
        btSend = _42;
        _42.setText("Send");
        _42.setMnemonic(83);
        _42.setDisplayedMnemonicIndex(0);
        _23.add(_42, new com.intellij.uiDesigner.core.GridConstraints(1, 9, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _43;
        _43 = new com.intellij.uiDesigner.core.Spacer();
        _22.add(_43, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
    }

}
