package ocera.rtcan.CanOpen;

import ocera.rtcan.monitor.EdsTreeNode;
import ocera.util.FString;

import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: 30.3.2003
 * Time: 14:42:26
 * To change this template use Options | File Templates.
 */
public class ODNode implements EdsTreeNode, Comparable
{
    public final static int ACCES_TYPE_READ = 1;
    public final static int ACCES_TYPE_WRITE = 2;

    public ODNode[] subNodes = null;

    public short[] value = null;

    protected static Field[] fields = null;
    protected static int attrOffset; // skip index & subindex

    public int index;
    public int subIndex = 0;
    public String parameterName = "";
    public int subNumber = 0;
    public int objectType;
    public int dataType;
    public byte accessType; //bitfield: 1 - read, 2 - write
    public boolean pdoMapping;
    public String lowLimit = "";
    public String highLimit = "";
    public String defaultValue = "";

    public ODNode()
    {
        if(fields == null) {
//            System.err.println("ODNode::fields init\n");
            try {
                fields = Class.forName("ocera.rtcan.CanOpen.ODNode").getFields();
                for(int i=0; i<fields.length; i++) {
                    if(fields[i].getName().equals("parameterName")) {
                        attrOffset = i;
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
        }
    }

    public String toString()
	{
		return getName();
	}

    public String getName()
    {
        String s = "";
        if(subIndex >= 0) s = "." + Integer.toString(subIndex, 16);
        return Integer.toString(index, 16) + s + " - " + parameterName;
    }

    public int getAttrCnt()
    {
        if(fields != null) {
            if(subNumber > 0) return 2; // index node
            return fields.length - attrOffset; // subindex node
        }
        return 0;
    }

    public String getAttrName(int ix)
    {
        if(fields == null) return "<NULL>";
        if(ix >= getAttrCnt()) return "index " + ix + " is out of range (" + getAttrCnt() + ")";
        try {
            return fields[ix + attrOffset].getName();
        } catch (Exception e) {}
        return "Exception occured getting attribute " + fields[ix + attrOffset].getName();
    }

    public String getAttrValue(int ix)
    {
        if(fields == null) return "<NULL>";
        if(ix >= getAttrCnt()) return "index " + ix + " is out of range (" + getAttrCnt() + ")";
        try {
            Field f = fields[ix + attrOffset];
            if(f.getName().equalsIgnoreCase("accessType")) {
                String ret = "";
                if((accessType & ACCES_TYPE_READ) != 0) ret += "r";
                if((accessType & ACCES_TYPE_WRITE) != 0) ret += "w";
                return ret;
            }
            return f.get(this).toString();
        } catch (Exception e) {}
        return "Exception occured getting attribute " + fields[ix + attrOffset].getName();
    }

    public void setAttrValue(int ix, String s)
    {
    }

    public void setAttr(String name, String val)
    {
        name = name.trim();
        val = val.trim();
        if(name.equalsIgnoreCase("ParameterName")) {
            parameterName = val;
        }
        else if(name.equalsIgnoreCase("SubNumber")) {
            subNumber = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("objectType")) {
            objectType = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("dataType")) {
            dataType = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("accessType")) {
            accessType = 0;
            val = val.toLowerCase();
            if(val.indexOf("r") >= 0) accessType |= ACCES_TYPE_READ;
            if(val.indexOf("w") >= 0) accessType |= ACCES_TYPE_WRITE;
        }
        else if(name.equalsIgnoreCase("pdoMapping")) {
            pdoMapping = (FString.toInt(val) == 0)? false: true;
        }
        else if(name.equalsIgnoreCase("lowLimit")) {
            lowLimit = val;
        }
        else if(name.equalsIgnoreCase("highLimit")) {
            highLimit = val;
        }
        else if(name.equalsIgnoreCase("defaultValue")) {
            defaultValue = val;
        }
    }

    public String valToString()
    {
        if(value == null) return "value not loaded";
        String s = "";
        for(int i = 0; i < value.length; i++) {
            if(i > 0) s += " ";
            s += FString.byte2Hex(value[i]);
        }
        return s;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     *
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of <i>expression</i>
     * is negative, zero or positive.
     *
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)<p>
     *
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.<p>
     *
     * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.<p>
     *
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(Object o)
    {
        ODNode on = (ODNode) o;
        if(index < on.index) return -1;
        if(index > on.index) return 1;
        if(subIndex < on.subIndex) return -1;
        if(subIndex > on.subIndex) return 1;
        return 0;
    }
}
