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
    public static final String[] attrColNames = {"Name", "Value"};

    public void setAttrNode(EdsTreeNode attrNode) {
        this.attrNode = attrNode;
    }

    public int getColumnCount() {
        return 2;
    }

    public int getRowCount() {
        if(attrNode == null) return 0;
        return attrNode.getAttrCnt();
    }

    public String getColumnName(int column) {
        return attrColNames[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if(attrNode == null) return "<NULL>";   //modele has not assigned node
        if(columnIndex == 0) return attrNode.getAttrName(rowIndex);
        else if(columnIndex == 1) return attrNode.getAttrValue(rowIndex);
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
    protected ODNode[] objectDictionary = null;

    protected JPanel panSDOTable;
    protected JTextField edSDO;
    protected JButton btUploadSDO;
    protected JButton btDownloadSDO;
    protected JTable tblProp;
    protected JTextField edCanID;
    protected JTextField[] edCanData = new JTextField[8];

    protected JTabbedPane tabPane;
    protected JTree treeEds;
    protected DefaultTreeModel treeEdsModel;
    protected DefaultMutableTreeNode treeEdsRootNode;
    protected JTextArea txtMsg;
    protected AttrModel attrModel = new AttrModel();

    protected XmlConfig xmlConfig = new XmlConfig();
    public static final String CONFIG_FILE_NAME = "CanMonitor.conf.xml";
    protected String configFile = "N/A";
    protected boolean configChanged = false;

    public static void main (String [] args)
    {
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
                        xmlConfig.getRootElement().cd("/connection/host").setValue(args[i]);
                        //canProxyIP = args[i];
                    }
                }
                if(c == 'n') {
                    // node
                    i++;
                    if(i < args.length) {
                        xmlConfig.getRootElement().cd("/canopen/node").setValue(args[i]);
                        //nodeNo = FString.toInt(args[i]);
                    }
                }
                else if(c == 'f') {
                    // EDS file name
                    i++;
                    if(i < args.length) edsFile = args[i];
                }
                else if(c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -f EDS_file_name\n");
                }
            }
        }
        if(edsFile != null) {
            openEDS(edsFile);
        }
    }

    protected void connect()
    {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getRootElement().cd("/connection/port").getValue("1001").trim();
        int p = FString.toInt(port);
        if(ip.length() > 0) {
            txtMsg.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtMsg.append(canConn.getErrMsg() + "\n");
        }
    }

    protected void disConnect()
    {
        canConn.disconnect();
    }

    public CanMonitor(String[] cmd_line_args)
    {
        super("RT CAN monitor - Alfa 0.1");
        initGui();
        init();
        loadConfig();
        processCmdLine(cmd_line_args);
        connect();
//        setBounds(50, 50, 800, 600);
    }

    /**
     * Opens configuration file, triing next locations:<br>
     * 1. user home directory<br>
     * 2. current directory<br>
     * 3. default config file
     */
    protected void loadConfig()
    {
        // try to find configuration
        String home = System.getProperty("user.home", "N/A");
        String pwd = System.getProperty("user.dir", ".");
        String configfile;

        // lok if home exists
        File ff;
        configfile = home + "/.canmonitor";
        configFile = "";
        ff = new File(configfile);
        if(!ff.exists()) {
            // try to make directory at home
            if(ff.mkdir()) {
                configFile = home + "/.canmonitor/" + CONFIG_FILE_NAME;
            }
        }
        else {
            configFile = home + "/.canmonitor/" + CONFIG_FILE_NAME;
        }

        configfile = home + "/.canmonitor/" + CONFIG_FILE_NAME;;
        ff = new File(configfile);
        if(!ff.canRead()) {
            configfile = pwd + "/" + CONFIG_FILE_NAME;
            ff = new File(configfile);
            if(!ff.canRead()) {
                // find default config in resources
                URL url = this.getClass().getResource("resources/" + CONFIG_FILE_NAME);
                configfile = url.getFile();
            }
            else {
                configFile = configfile;
            }
        }
        xmlConfig.fromURI(configfile);
    }

    protected void saveConfig()
    {
        if(!configChanged) return;
        // is there reasonable place to write config
        if(configFile.length() == 0) return;

        String s = xmlConfig.toString();
        FFile ff = new FFile(configFile);
        try {
            ff.writeString(s, FFile.MODE_OVERWRITE, FFile.OVERWRITE_FILE);
        } catch (IOException e) {
            new ErrorMsg(this).show("Error writing config file: " + e);
        } catch (FFileException e) {
            new ErrorMsg(this).show("Error writing config file: " + e);
        }
    }

    public void initGui()
    {
        //==========================================================
        //        menu toolbar
        //==========================================================
        pane = getContentPane();
        ImageIcon ico = new ImageIcon("img/file_new.gif");
        Action act;
        act = new AbstractAction("Open EDS", ico) {
                    public void actionPerformed(ActionEvent e) {

                        JFileChooser fc = new JFileChooser();
                        FFileFilter filter = new FFileFilter();
                        filter.addExtension("EDS");
                        filter.addExtension("eds");
                        filter.setDescription("EDS Electronic Data Sheets");
                        fc.setFileFilter(filter);
                        fc.setCurrentDirectory(new File("d:/java/canmonitor"));

                        int ret = fc.showOpenDialog(CanMonitor.this);
                        if(ret == JFileChooser.APPROVE_OPTION) {
                            String fname = fc.getSelectedFile().getAbsolutePath();
                            openEDS(fname);
                        }
                    }
                };
        actions.put("OpenEds", act);

        act = new AbstractAction("Config") {
                    public void actionPerformed(ActionEvent e) {
                        openConfigDialog();
                    }
                };
        actions.put("Config", act);

        act = new AbstractAction("Quit") {
                    public void actionPerformed(ActionEvent e) {
                        cleanUp();
                        System.gc();
                        System.exit(0);
                    }
                };
        actions.put("Quit", act);

        //==========================================================
        // menu bar
        //==========================================================
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem itm = new JMenuItem(actions.get("OpenEds"));
        menu.add(itm);
        itm = new JMenuItem(actions.get("Quit"));
        menu.add(itm);

        menu = new JMenu("Tools");
        menuBar.add(menu);
        itm = new JMenuItem(actions.get("Config"));
        menu.add(itm);

        //==========================================================
        // tool bar
        //==========================================================
        JToolBar tb = new JToolBar();
        tb.add(actions.get("OpenEds"));
        pane.add(tb, BorderLayout.NORTH);

        //==========================================================
        //        layout
        //==========================================================

        //---------------- EDS tab -----------------------------
        treeEdsRootNode = new DefaultMutableTreeNode("root");
        treeEdsModel = new DefaultTreeModel(treeEdsRootNode);
        treeEds = new JTree(treeEdsModel);
        treeEds.setShowsRootHandles(true);
        JScrollPane sc01 = new JScrollPane(treeEds);

        JPanel p1 = new JPanel(new BorderLayout(0, 5));
        JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        edSDO = new JTextField(); p2.add(edSDO);
        btUploadSDO = new JButton("Upload");
        btUploadSDO.setMnemonic(KeyEvent.VK_U);
        p2.add(btUploadSDO);
        btDownloadSDO = new JButton("Download");
        btDownloadSDO.setMnemonic(KeyEvent.VK_D);
        p2.add(btDownloadSDO);
        p1.add(p2, BorderLayout.NORTH);

        tblProp = new JTable(attrModel);//, new TableColumnModel() {"col1", "col2", "col3"});
//        tblProp.setCellEditor(new DefaultCellEditor(new JTextField()));  // tohle neni potreba
        JScrollPane sc02 = new JScrollPane(tblProp);
        p1.add(sc02, BorderLayout.CENTER);

        JSplitPane panEDS = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sc01, p1);
        panEDS.setDividerLocation(300);
        panEDS.setDividerSize(8);

        //---------------- CAN tab -----------------------------
        txtMsg = new JTextArea("--------- Can monitor log window -----------");
        JScrollPane scrlpantxt = new JScrollPane(txtMsg);
        JPanel jpan = new JPanel(new GridLayout(0, 10));
//        jpan.setAlignmentX(0);
//        jpan.setComponentOrientation(new ComponentOrientation());
        jpan.add(new JLabel("ID"));
        for(int i = 0; i < 8; i++) jpan.add(new JLabel("byte[" + i + "]"));
        jpan.add(new JLabel(""));

        edCanID = new JTextField(); //edID.setMinimumSize(new Dimension(70, 10));
        jpan.add(edCanID);
        for(int i = 0; i < 8; i++) {
            edCanData[i] = new JTextField();//            ed.setMinimumSize(50, 0);
            jpan.add(edCanData[i]);
        }
        JButton btSend = new JButton("Send");
        btSend.setMnemonic(KeyEvent.VK_S);
        jpan.add(btSend);

        JPanel panCAN = new JPanel(new BorderLayout());
        panCAN.add(scrlpantxt);
        panCAN.add(jpan, BorderLayout.SOUTH);

        tabPane = new JTabbedPane();
        tabPane.addTab("EDS", null, panEDS, "Show and edit EDS");
        tabPane.addTab("CAN", null, panCAN, "Monitor CAN messages");

        pane.add(tabPane);

        tabPane.setSelectedIndex(1);    // select tab CAN
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
                txtMsg.append("\nSENDING: " + msg);
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
                String node = xmlConfig.getValue("/canopen/node", "1");
                String msg = "{SDOR UPLOAD 0 0 " + node + " " + s + "}";
                txtMsg.append("\nSENDING: " + msg);
                canConn.send(msg);
            }
        });

        //==========================================================
        //        btDownloadSDO listenner
        //==========================================================
        // {SDOR DOWNLOAD server_port client_port node index subindex [dd dd ...]}
        btDownloadSDO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String node = xmlConfig.getValue("/canopen/node", "1");
                String msg = "{SDOR DOWNLOAD 0 0 ";
                msg += node + " ";
                msg += Integer.toString(selectedObject.index, 16) + " ";
                msg += Integer.toString(selectedObject.subIndex, 16) + " ";
                msg += "[" + edSDO.getText() + "]}";
                txtMsg.append("\nSENDING: " + msg);
                canConn.send(msg);
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
    }

    private void enableControls()
    {
        if(selectedObject == null || selectedObject.subNumber > 0) {
            btUploadSDO.setEnabled(false);
            btDownloadSDO.setEnabled(false);
        }
        else {
            btUploadSDO.setEnabled((selectedObject.accessType & ODNode.ACCES_TYPE_READ) != 0);
            btDownloadSDO.setEnabled((selectedObject.accessType & ODNode.ACCES_TYPE_WRITE) != 0);
        }
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

    private void openEDS(String fname) {
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
                        odlst.add(odnd);
                        //tnd = new DefaultMutableTreeNode(odnd);
                        //treeEdsRootNode.add(tnd);
                        odnd.index = index;
                        odnd.subIndex = 0;
                    }
                    else if(index != -1 && subindex != -1) {
                        //subindex
                        odsubnd = new ODNode();
                        odsubnd.isSubObject = true;
//                        odsubnd.type = ODNode.STRUCT_ITEM;
                        odlst.add(odsubnd);
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

        objectDictionary = (ODNode[]) odlst.toArray(new ODNode[odlst.size()]);
        Arrays.sort(objectDictionary);
        DefaultMutableTreeNode tnd = null;
        // add nodes to the tree
        int oldix = -1;
        for(int i = 0; i < objectDictionary.length; i++) {
            ODNode nd = objectDictionary[i];
            if(nd.index == oldix) continue;

            oldix = nd.index;
            tnd = new DefaultMutableTreeNode(nd);
            if(nd.subNodes != null) {
//                nd.type = ODNode.STRUCT_NODE;
                nd.subIndex = -1;
                for(int j = 0; j < nd.subNodes.length; j++) {
    //                System.err.println("pocet subnodu: " + nd.subNodes.length);
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
            System.out.println("CAN_MONITOR: received CAN message " + s);
            msgCount++;
            txtMsg.append("\n[" + msgCount + "] CAN message received: " + s);

            // parse datagram
            s = FString.slice(s, 1, -1);
            String ss[];
            if(s.matches("CANDTG.*")) {
                // found CAN msg start
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
                txtMsg.append("\n\t" + canmsg);
            }
            else if(s.matches("SDOC.*")) {
                // found SDO msg start
                // {SDOC UPLOAD server_port client_port node index subindex [...]}
                // {SDOC DOWNLOAD server_port client_port node index subindex}
                int index, subindex;
                short[] value = null;
                boolean upload = false;
                s = s.substring(5).trim();
                if(s.matches("UPLOAD.*")) {s = s.substring(7); upload = true;}
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
                            LinkedList lst = new LinkedList();
                            while(true) {
                                ss = StringParser.cutInt(s, 16); s = ss[1];
                                if(ss[0].trim().length() == 0) break;
                                lst.add(new Short((short) FString.toInt(ss[0], 16)));
                            }
                            value = new short[lst.size()];
                            int ix = 0;
                            for(Iterator it = lst.iterator(); it.hasNext();) {
                                Short sh = (Short) it.next();
                                value[ix++] = sh.shortValue();
                            }
                        }

                        // find object
                        ODNode odkey = new ODNode();
                        odkey.index = index;
                        odkey.subIndex = subindex;
                        int ix = Arrays.binarySearch(objectDictionary, odkey);
                        if(ix >= 0) {
                            objectDictionary[ix].value = value;
                            refreshForm();
                            //edSDO.setText(objectDictionary[ix].valToString());
                        }
                    }
                }
            }
            else {
                System.err.println("unknown message type: " + s);
            }
        }
    }

}
