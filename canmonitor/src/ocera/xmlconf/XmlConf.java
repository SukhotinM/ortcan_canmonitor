/*
 * @(#)XmlConf.java	1.20 02/06/13
 */
package ocera.xmlconf;

//import ocera.util.FFileFilter;
//import ocera.util.FramedLayoutPanel;
//import ocera.util.StringParser;
//import ocera.util.xmlconfig.XmlConfig;
//import ocera.util.xmlconfig.XmlConfElement;
//import ocera.msg.InfoMsg;
import ocera.xmlconf.XmlConfPanel;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Properties;
//import java.util.Enumeration;
//import java.util.Iterator;
//import java.io.*;

import javax.swing.*;
//import javax.swing.border.Border;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.tree.DefaultMutableTreeNode;
//import javax.swing.tree.DefaultTreeModel;
//import javax.swing.tree.TreePath;
//
//import org.jdom.Element;

/**
 * Sample application using the simple text editor component that
 * supports only one font.
 *
 * @author  Timothy Prinzing
 * @version 1.20 06/13/02
 */
public class XmlConf extends JFrame
{
    XmlConf()
    {
        super();

        // Force SwingSet to come up in the Cross Platform L&F
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // If you want the System L&F instead, comment out the above line and
            // uncomment the following:
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exc) {
            System.err.println("Error loading L&F: " + exc);
        }
    }

    public static void main(String[] args)
    {
        XmlConf app = new XmlConf();

        app.setTitle("Xml Configurator");
        app.setBackground(Color.lightGray);
        //app.getContentPane().setLayout(new GridLayout());
        final XmlConfPanel xconfpan = new XmlConfPanel();
        app.getContentPane().add(xconfpan);

        // application closing
        app.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE );
        app.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.out.println("collecting garbage & quit");
                System.gc();
                System.exit(0);
            }
        });

        if(args.length > 0) {
            xconfpan.openConfigFile(args[0]);
        }

        app.pack();
        app.setSize(600, 400);
        app.show();

/*
        Properties prop = System.getProperties();
        for(Enumeration e = prop.keys(); e.hasMoreElements() ;) {
            String key = e.nextElement().toString();
            System.out.println(key + "=" + prop.getProperty(key));
        }
*/

    }
}
