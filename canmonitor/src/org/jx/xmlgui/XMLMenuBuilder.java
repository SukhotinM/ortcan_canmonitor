/* -*- Mode: java; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is the Grendel mail/news client.
 *
 * The Initial Developer of the Original Code is Giao Nguyen
 * <grail@cafebabe.org>.  Portions created by Giao Nguyen are
 * Copyright (C) 1999 Giao Nguyen. All
 * Rights Reserved.
 *
 * Contributor(s): Morgan Schweers <morgan@vixen.com>
 */

package org.jx.xmlgui;

import ocera.util.FLog;

import java.awt.*;
import java.awt.event.ActionEvent;
//import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;

//import org.jx.ui.*;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;

import javax.swing.*;

/**
 * Build a menu bar from an XML data source. This builder supports:
 * <UL>
 * <LI>Text label cross referencing to a properties file.
 * <LI>Action lookups.
 * </UL>
 */
public class XMLMenuBuilder extends XMLWidgetBuilder
{
  static final String id_attr = "id";
  static final String menu_tag = "menu";
  static final String label_attr = "label";
  static final String separator_attr = "separator";
  static final String menuitem_tag = "menuitem";
  static final String checkbox_attr = "checkbox";
  static final String radio_attr = "radio";
  static final String group_attr = "group";
  static final String accel_attr = "accel";
  static final String type_attr = "type";

  /**
   * The button group indexed by its name.
   * Groups of radio buttons
   */
  protected Hashtable button_group = new Hashtable(2);

  ActionMap actions;  // actions used in current build process
  //MenuBarCtrl component;

  /**
   * Build a menu builder which operates on XML formatted data.
   * 
   * @param ref the reference point for properties location
   */
  public XMLMenuBuilder(Class ref)
  {
    //button_group = new Hashtable();
    this.ref = ref;
  }


  /**
   * Build a menu builder which operates on XML formatted data.
   * 
   * @param frame reference point for properties location
   */
  public XMLMenuBuilder(JFrame frame)
  {
    this(frame.getClass());
  }

  /**
   * Read the input stream and build a menubar from it
   * 
   * @param url location of menu file
   * @param actions array of UIAction objects to map to
   */
  public JMenuBar buildFrom(URL url, ActionMap actions)
  {
      if(url == null)
        FLog.log("XMLMenuBuilder", FLog.LOG_ERR, "buildFrom() - URL is NULL");
      FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildFrom() - build from URL: " + url.toString());
      JMenuBar menubar = null;
      SAXBuilder builder = new SAXBuilder();
      try {
          org.jdom.Document doc = null;
          doc = builder.build(url);
          org.jdom.Element root = doc.getRootElement(), el;

          // get the <head><link> tag for this file
          el = root.getChild("head").getChild("link");
          // set the configuration contained in this node
          setConfiguration(el);

          // skip to the body
          menubar = buildFrom(root.getChild("body").getChild("menubar"), actions);
      }
      catch (JDOMException e) {
        System.err.println(url + " is not well-formed.");
        System.err.println(e.getMessage());
      }
      catch (IOException e) {
        System.err.println("Could not check " + url);
        System.err.println(" because " + e.getMessage());
      }
      return menubar;
  }

public JMenu buildMenu(org.jdom.Element element)
{
    JMenu menu = new JMenu();
    //String my_id = element.getAttribute(id_attr).getValue();
    menu.setText(getReferencedLabel(element, label_attr).trim());
    menu.setActionCommand(element.getAttributeValue(id_attr, "NOT_FOUND"));

    java.util.List lst = element.getContent();
    Iterator it = lst.iterator();
    while (it.hasNext()) {
        Object o = it.next();
        if(o instanceof Element) {
            JComponent comp = buildComponent((Element)o);
//            FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildMenu() - adding menu item: " + comp.getClass().toString());
            menu.add(comp);
        }
    }

    return menu;
}

  public JMenuBar buildFrom(Element element, ActionMap actions)
  {
      if(!element.getName().equals("menubar")) {
//          throw new Exception("XMLMenuBuilder.buildFrom() - element name should be <menubar>");
          FLog.log("XMLMenuBuilder", FLog.LOG_ERR, "buildFrom() - element name should be <menubar>");
      }
      JMenuBar menubar = new JMenuBar();
      this.actions = actions;
      java.util.List lst = element.getContent();
      Iterator it = lst.iterator();
      while (it.hasNext()) {
          Object o = it.next();
          if(o instanceof Element) {
              JMenu menu = buildMenu((Element)o);
              menubar.add(menu);
              //component.addItemByName(menu.getActionCommand(), menu);
          }
      }

      return menubar;
  }

    /**
    * Build the component at the current XML element and add to the parent.
    * @param current the current element
    * @return built component
    */
    protected JComponent buildComponent(Element current)
    {
        String tag = current.getName();
//        FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildMenuItem() - element: " + current.toString()
               // + " label: " + getReferencedLabel(current, "label"));
        JComponent comp = null;

        // menu tag
        if(tag.equals(menu_tag)) {
            comp = buildMenu(current);
        }
        else if(tag.equals("separator")) {
            comp = buildSeparator(current);
        }
        else if(tag.equals(menuitem_tag)) { // menuitem tag
            String type = current.getAttributeValue(type_attr, "");
            // which type of menuitem?
            if (type.equals("")) {
                // no type ? it's a regular menuitem
                comp = buildMenuItem(current);
            }
            else if (type.equals(checkbox_attr)) { // checkboxes
                comp = buildCheckBoxMenuItem(current);
            }
            else if (type.equals(radio_attr)) { // radio
                comp = buildRadioMenuItem(current);
            }
        }

        comp.setFont(new Font("Helvetica", Font.PLAIN, 12));
        return comp;
    }
  
  /**
   * Build a JRadioMenuItem
   * @param current the element that describes the JRadioMenuItem
   * @return the built component
   */
  protected JRadioButtonMenuItem buildRadioMenuItem(Element current)
  {
    String group = current.getAttributeValue(group_attr, "");
    ButtonGroup bg;
    JRadioButtonMenuItem comp = new JRadioButtonMenuItem();
    finishMenuItem(comp, current);
    
    // do we add to a button group?
    if (button_group.containsKey(group)) {
      bg = (ButtonGroup)button_group.get(group);
    } else {
      bg = new ButtonGroup();
      button_group.put(group, bg);
    }
    bg.add((JRadioButtonMenuItem)comp);
    
    comp.setFont(new Font("Helvetica", Font.PLAIN, 12));
    return comp;
  }

  /**
   * Build a JCheckBoxMenuItem.
   * @param current the element that describes the JCheckBoxMenuItem
   * @return the built component
   */
  protected JCheckBoxMenuItem buildCheckBoxMenuItem(Element current) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem();
    finishMenuItem(item, current);
    item.setFont(new Font("Helvetica", Font.PLAIN, 12));
    return item;
  }

  /**
   * Build a JSeparator.
   * @param current the element that describes the JSeparator
   * @return the built component
   */
  protected JSeparator buildSeparator(Element current) {
    return new JSeparator();
  }

    /**
    * Build a JMenuItem.
    * @param current the element that describes the JMenuItem
    * @return the built component
    */
    protected JMenuItem buildMenuItem(Element current)
    {
        JMenuItem item = null;
        Action action = null;
        String s = current.getAttributeValue("action");
        if(s != null) {
            if ((action = (Action)actions.get(s)) != null) {
                item = new JMenuItem(action);
            }
            else {
                item = new JMenuItem(s + " ACTION_NOT_FOUND");
                item.setEnabled(false);
            }
        }
        if(item == null) {
            item = new JMenuItem();
            finishMenuItem(item, current);
            item.setFont(new Font("Helvetica", Font.PLAIN, 12));
        }
        return item;
    }

    private void finishMenuItem(JMenuItem item, Element current)
    {
        String label = getReferencedLabel(current, label_attr);

        if (label.length() > 0) {
            item.setText(label);
            item.setName(label);
        }

        label = getReferencedLabel(current, accel_attr);
        if (label.length() > 0) item.setMnemonic(label.charAt(0));
    }

    public static void main(String[] args) throws Exception
    {
        FLog.logTreshold = FLog.LOG_ERR;
        javax.swing.JFrame frame = new javax.swing.JFrame("Foo bar");
        XMLMenuBuilder builder = new XMLMenuBuilder(frame);
        ActionMap actions = new ActionMap();
        Action act = new AbstractAction("Quit") {
                    public void actionPerformed(ActionEvent e) {
                        System.gc();
                        System.exit(0);
                    }
                };
        actions.put("appExit", act);
        URL url = builder.getClass().getResource("menu1.xml");
        frame.setJMenuBar(builder.buildFrom(url, actions));
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
