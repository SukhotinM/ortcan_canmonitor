package ocera.rtcan.monitor;

//import com.intellij.uiDesigner.core.GridConstraints;
//import com.intellij.uiDesigner.core.GridLayoutManager;
//import com.intellij.uiDesigner.core.Spacer;
import ocera.rtcan.eds.EdsNode;
import ocera.rtcan.eds.EdsAttribute;
import ocera.rtcan.CanOpen.ODNode;
import ocera.rtcan.CanOpen.ObjectDictionary;
import ocera.rtcan.msg.*;
import ocera.msg.ErrorMsg;
import ocera.util.FFile;
import ocera.util.StringParser;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.table.AbstractTableModel;
import java.util.Date;
import java.util.LinkedList;
import java.util.Arrays;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

import org.flib.FString;
import org.flib.FLog;

/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: Apr 23, 2004
 * Time: 1:45:00 PM
 * To change this template use File | Settings | File Templates.
 */
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

public class CANopenDevicePanel extends JPanel {
    private JTextField edSDO;
    private JButton btUploadSDO;
    private JButton btDownloadSDO;
    private JTextField edNodeID;

    protected JTree treeEds;

    protected JTable tblProp;

    private JButton btClearLog;
    private JTextArea txtLog;
    private int msgCount;

    protected DefaultTreeModel treeEdsModel;
    protected DefaultMutableTreeNode treeEdsRootNode = new DefaultMutableTreeNode("root");

    protected AttrModel attrModel = new AttrModel();
    protected ODNode selectedObject = null;
    protected ObjectDictionary objectDictionary = new ObjectDictionary();
    private byte[] valueProcessedByDownload = null;

    private CanMonitor mainApp;
    private int tabIndex; //< index in CanMonitor tabbed pane
    private JTabbedPane pane;
    private JComboBox representationComboBox;

    public CANopenDevicePanel(CanMonitor mainapp, int tab_index) {
        if (mainapp == null) throw new NullPointerException("CANopenDevicePanel(): mainapp is NULL.");
        mainApp = mainapp;
        tabIndex = tab_index;
        createUI();
        init();
        initGui();
    }

    int getNodeID() {
        String s = edNodeID.getText();
        int node = FString.toInt(s);
        return node;
    }

    public void setNodeID(int new_id) {
        edNodeID.setText(Integer.toString(new_id));
        refreshPanel();
    }

    private void refreshPanel() {
        if (selectedObject == null) edSDO.setText("");
        else try {
            edSDO.setText(ModelViewTransformer.getViewFromValue(selectedObject, representationComboBox.getSelectedIndex()));
        } catch (InvalidTypeOfViewException e) {
            //  representationComboBox.setSelectedIndex(0);
            representationComboBox.setSelectedIndex(RepresentationEnum.HEX_RAW); // If is selected undefined type of view. Set HEX_RAW default
            ErrorMsg.show("Error: " + e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
        //  edSDO.setText(ModelViewTransformer.getViewFromValue(selectedObject, representationComboBox.getSelectedIndex()));
        // else edSDO.setText(ModelViewTransformer.valToStringHexRaw(selectedObject));
        String s = "node ";
        int node = getNodeID();
        if (node <= 0) s += "xx";
        else s += Integer.toString(node);
        mainApp.setTabLabel(tabIndex, s);
    }

    public void init() {
/*
        if(treeEdsRootNode.getUserObject() != null) {
            String s = (String)treeEdsRootNode.getUserObject();
            // if EDS file is not assigned from command line user object name is 'root'
            if(!s.equalsIgnoreCase("root")) openEDS((String)treeEdsRootNode.getUserObject());
        }
*/
    }

    public void initGui() {
        setLayout(new GridLayout());
        add(pane);

        treeEdsModel = new DefaultTreeModel(treeEdsRootNode);
        //treeEds = new JTree(treeEdsModel);
        treeEds.setShowsRootHandles(true);
        treeEds.setModel(treeEdsModel);

        tblProp.setModel(attrModel);
        representationComboBox.setModel(new DefaultComboBoxModel(RepresentationEnum.EnumNames));
        //edNodeID.setText(xmlConfig.getValue("/canopen/node", "1"));
        edNodeID.setText("1");

        URL url = this.getClass().getResource("resources/left.png");
        ImageIcon ico = new ImageIcon(url);
        btUploadSDO.setIcon(ico);
        ico = new ImageIcon(getClass().getResource("resources/right.png"));
        btDownloadSDO.setIcon(ico);

        //==========================================================
        //        JTree listenners
        //==========================================================
        treeEds.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                selectedObject = null;
                if (e.isAddedPath()) {
//                    System.out.println(path.getLastPathComponent().getClass());
                    EdsTreeNode nd = null;
                    try {
                        nd = (EdsTreeNode) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                        System.out.println("Vybrany node " + nd);
                    } catch (ClassCastException ex) {
                        // tree root node selected
//                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    if (nd instanceof ODNode) selectedObject = (ODNode) nd;
                    enableControls();
                    refreshPanel();
                    attrModel.setAttrNode(nd);
                    attrModel.fireTableDataChanged();
                }
            }
        });

        //==========================================================
        //        representationComboBox listenner
        //==========================================================
        representationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedObject == null) edSDO.setText("");
                else {
                    try {
                        edSDO.setText(ModelViewTransformer.getViewFromValue(selectedObject, representationComboBox.getSelectedIndex()));
                    } catch (InvalidTypeOfViewException e1) {
                        representationComboBox.setSelectedIndex(RepresentationEnum.HEX_RAW);
                        ErrorMsg.show("Error: " + e1.getMessage()); //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        });

        //==========================================================
        //        btUploadSDO listenner
        //==========================================================
        // server_port == 0, client_port == 0 means: use default values for SDO communication
        btUploadSDO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SDOUploadRequestMsg msg = new SDOUploadRequestMsg();
                msg.index = selectedObject.index;
                int six = selectedObject.subIndex;
                if (six < 0) six = 0;
                msg.subindex = six;
                msg.node = Integer.parseInt(edNodeID.getText());
                txtLog.append("SENDING:\t" + msg + "\n");
                mainApp.sendMessage(msg);
            }
        });

        //==========================================================
        //        btDownloadSDO listenner
        //==========================================================
        btDownloadSDO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SDODownloadRequestMsg msg = new SDODownloadRequestMsg();
                msg.index = selectedObject.index;
                int six = selectedObject.subIndex;
                if (six < 0) six = 0;
                msg.subindex = six;
                msg.node = Integer.parseInt(edNodeID.getText());
                try {
                    msg.data = ModelViewTransformer.string2ValArray2(edSDO.getText(), selectedObject, representationComboBox.getSelectedIndex());
                    valueProcessedByDownload = msg.data;
                    txtLog.append("SENDING:\t" + msg + "\n");
                    mainApp.sendMessage(msg);
                } catch (NumberFormatException e2) {
                    ErrorMsg.show("Error " + e2.getMessage());
                } catch (InvalidTypeOfViewException e3) {
                    ErrorMsg.show("Error: " + e3.getMessage());
                }
            }
        });

        //==========================================================
        //        btClearLog listenner
        //==========================================================
        btClearLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });

        //==========================================================
        //        edNodeID listenner
        //==========================================================
        edNodeID.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            void update() {
                refreshPanel();
            }
        });

        //==========================================================
        //        JTextArea logs listenners
        //==========================================================
        int logsize = FString.toInt(mainApp.xmlConfig.getValue("/logging/logscreensize", "1000000"));
        ((AbstractDocument) txtLog.getDocument()).setDocumentFilter(new LogTextAreaDocumentFilter(logsize));
    }

    private void enableControls() {
        if (selectedObject == null || selectedObject.subNumber > 0) {
            btUploadSDO.setEnabled(false);
            btDownloadSDO.setEnabled(false);
        } else {
            btUploadSDO.setEnabled(selectedObject.accessType < ODNode.ACCES_TYPE_WO);
            btDownloadSDO.setEnabled(selectedObject.accessType > ODNode.ACCES_TYPE_RO);
        }
    }

    public void openEDS(String fname) {
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
        } catch (FileNotFoundException e) {
//            new ErrorMsg(this).show("File not found: " + fname);
            ErrorMsg.show(e.toString());
            return;
        } catch (IOException e) {
            ErrorMsg.show(e.toString());
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
            if (i < ss.length) {
                s = ss[i].trim();
                if (s.length() == 0) continue; // skip empty lines
            } else s = null;

            // parse line
            if (i == ss.length || s.charAt(0) == '[') {
                // new node definition or end of file

                // resolve mew index & subindex
                index = subindex = -1;
                if (i < ss.length) {
                    s = FString.slice(s, 1, -1);
                    char c = s.charAt(0);
                    if (c < '0' || c > '9') {
                        //EDS text node
                    } else {
                        // index or subindex
                        String slice[] = StringParser.cutInt("0x" + s);
                        if (slice[1].trim().length() == 0) {
                            //index
                            index = FString.toInt(slice[0]);
                        } else {
                            if (slice[1].length() > 3) {
                                if (slice[1].substring(0, 3).equalsIgnoreCase("sub")) {
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
                if (edsnd != null) {
                    // close EDS text node
                    edsnd.attributes = (EdsAttribute[]) attlist.toArray(new EdsAttribute[attlist.size()]);
                    //edsList.addNode(edsnd);
                    edsnd = null;
                } else if (odnd != null) {
                    if (odsubnd != null) {
                        // close subindex definition
                        subndlist.add(odsubnd);
                        odsubnd = null;
                    }
                    if (odnd.index != index) {
                        // close index definition
                        odnd.subNodes = (ODNode[]) subndlist.toArray(new ODNode[subndlist.size()]);
                        odlst.add(odnd);
                        subndlist = new LinkedList();
                        odnd = null;
                    }
                }

                if (i == ss.length) break;   // end of file

                // start new section
                if (s.length() == 0) break;

                if (index < 0) {
                    //EDS text node
                    attlist.clear();
                    edsnd = new EdsNode(s);
                    treeEdsRootNode.add(new DefaultMutableTreeNode(edsnd));
                } else {
                    // index or subindex
                    if (index != -1 && subindex == -1) {
                        //index
                        odnd = new ODNode();
                        //tnd = new DefaultMutableTreeNode(odnd);
                        //treeEdsRootNode.add(tnd);
                        odnd.index = index;
                        odnd.subIndex = 0;
                    } else if (index != -1 && subindex != -1) {
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
            } else {
                // parse attribute
                int ix = s.indexOf('=');
                if (ix < 0) {
                    s1 = "";
                    s = s.trim();
                } else {
                    s1 = FString.slice(s, ix + 1).trim();
                    s = FString.slice(s, 0, ix).trim();
                }

                if (edsnd != null) {
                    EdsAttribute att = new EdsAttribute(s, s1);
                    attlist.add(att);
                } else if (odsubnd != null) {
                    odsubnd.setAttr(s, s1);
                } else if (odnd != null) {
                    odnd.setAttr(s, s1);
                }
            }
        }

        ODNode[] odir = (ODNode[]) odlst.toArray(new ODNode[odlst.size()]);
        Arrays.sort(odir);
        objectDictionary.setOd(odir);

        DefaultMutableTreeNode tnd = null;
        // add nodes to the tree
        //int oldix = -1;
        for (int i = 0; i < odir.length; i++) {
            ODNode nd = odir[i];
            //if(nd.index == oldix) continue;

            //oldix = nd.index;
            tnd = new DefaultMutableTreeNode(nd);
            if (nd.subNodes != null) {
//                nd.type = ODNode.STRUCT_NODE;
                nd.subIndex = -1;
                for (int j = 0; j < nd.subNodes.length; j++) {
                    //                FLog.log("CanMonitor", FLog.LOG_ERR, "pocet subnodu: " + nd.subNodes.length);
                    ODNode snd = nd.subNodes[j];
                    tnd.add(new DefaultMutableTreeNode(snd));
                }
            }
            treeEdsRootNode.add(tnd);
        }
        treeEds.expandRow(0);   // expand root node
        System.out.println("finish at " + new Date());
        refreshPanel();
    }

    /**
     * try to process incoming message
     *
     * @return true if the message is procesed
     */
    public boolean tasteObject(Object o) {
        // read all received messages if any
        FLog.log(getClass().getName(), FLog.LOG_TRASH, "node " + getNodeID() + " - tasteObject() - " + o);
        if (!(o instanceof SDOConfirmMsg)) return false;

        SDOConfirmMsg msg = (SDOConfirmMsg) o;
        // check if it is message for me
        if (msg.node != getNodeID()) return false;

        txtLog.append("RECEIVE[" + ++msgCount + "]:\t" + o + "\n");
        // check abort
        if (msg.type == SDOConfirmMsg.MSG_ABORT) {
            ErrorMsg.show(this, "ABORT - " + msg.errmsg);
        } else if (msg.type == SDOConfirmMsg.MSG_ERROR) {
            ErrorMsg.show(this, "ERROR - " + msg.errmsg);
        } else if (o instanceof SDOUploadConfirmMsg) {
            SDOUploadConfirmMsg umsg = (SDOUploadConfirmMsg) o;
            objectDictionary.setValue(umsg.index, umsg.subindex, umsg.data);
        } else if (o instanceof SDODownloadConfirmMsg) {
            SDODownloadConfirmMsg download_msg = (SDODownloadConfirmMsg) o;
            // succesful download, store new value also to OD
            if (valueProcessedByDownload != null) {
                objectDictionary.setValue(download_msg.index, download_msg.subindex, valueProcessedByDownload);
            }
            valueProcessedByDownload = null;
        }
        refreshPanel();
        return true;
    }


    private void createUI() {
        pane = new JTabbedPane();
        pane.setTabLayoutPolicy(0);
        pane.setTabPlacement(3);

        final JPanel odPanel = new JPanel();
        odPanel.setLayout(new GridLayout(1, 1));
        pane.addTab("OD", odPanel);
        final JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(164);
        splitPane.setDividerSize(8);
        odPanel.add(splitPane);

        final JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        splitPane.setRightComponent(rightPanel);

        final JPanel rightToolPanel = new JPanel();
        rightToolPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        rightPanel.add(rightToolPanel, BorderLayout.PAGE_START);

        edSDO = new JTextField();
        edSDO.setPreferredSize(new Dimension(150, 30));
        rightToolPanel.add(edSDO);

        btUploadSDO = new JButton();
        btUploadSDO.setMargin(new Insets(2, 5, 2, 5));
        btUploadSDO.setText("Upload");
        btUploadSDO.setMnemonic('U');
        btUploadSDO.setDisplayedMnemonicIndex(0);
        rightToolPanel.add(btUploadSDO);

        btDownloadSDO = new JButton();
        btDownloadSDO.setMargin(new Insets(2, 5, 2, 5));
        btDownloadSDO.setText("Download");
        btDownloadSDO.setMnemonic('D');
        btDownloadSDO.setDisplayedMnemonicIndex(0);
        rightToolPanel.add(btDownloadSDO);

        representationComboBox = new JComboBox();
        rightToolPanel.add(representationComboBox);

        final JLabel label1 = new JLabel();
        label1.setText("node");
        rightToolPanel.add(label1);

        edNodeID = new JTextField();
        rightToolPanel.add(edNodeID);


        final JScrollPane tableScrollPane = new JScrollPane();
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);
        tblProp = new JTable();
        tableScrollPane.setViewportView(tblProp);


        final JScrollPane treeScrollPane = new JScrollPane();
        splitPane.setLeftComponent(treeScrollPane);
        treeEds = new JTree();
        treeScrollPane.setViewportView(treeEds);

        final JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());
        pane.addTab("Log", logPanel);

        final JScrollPane logScrollPane = new JScrollPane();
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        txtLog = new JTextArea();
        logScrollPane.setViewportView(txtLog);

        final JPanel clearButtonPane = new JPanel();
        clearButtonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        logPanel.add(clearButtonPane, BorderLayout.PAGE_END);
        btClearLog = new JButton();
        btClearLog.setText("Clear");
        btClearLog.setMnemonic('C');
        btClearLog.setDisplayedMnemonicIndex(0);
        clearButtonPane.add(btClearLog);
    }

}
