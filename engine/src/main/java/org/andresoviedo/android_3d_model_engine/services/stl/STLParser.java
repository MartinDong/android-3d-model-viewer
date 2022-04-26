/*****************************************************************************
 * STLParser.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

package org.andresoviedo.android_3d_model_engine.services.stl;

// External imports

import java.io.IOException;
import java.net.URL;
import java.util.List;

// Local imports

/**
 * Abstract base class for parsing STL (stereolithography) files. Subclasses
 * of this class implement parsing the two formats of STL files: binary and
 * ASCII.<p>
 * 用于解析STL(立体光刻)文件的抽象基类。这个类的子类实现了对STL文件的两种格式的解析:二进制和ASCII。
 *
 * @author Dipl. Ing. Paul Szawlowski -
 * University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 1.3 $
 * Copyright (c) Dipl. Ing. Paul Szawlowski<p>
 */
abstract class STLParser {
    protected int itsNumOfObjects = 0;
    protected int[] itsNumOfFacets = null;
    protected String[] itsNames = null;

    /**
     * Do we strictly parse or try harder
     * 我们是严格分析还是更努力
     */
    protected boolean strictParsing;

    /**
     * Detailed parsing messages or null if none
     * 详细的解析消息，如果没有则为空
     */
    protected List<String> parsingMessages;

    public STLParser() {
        this(false);
    }

    /**
     * Constructor.构造函数
     *
     * @param strict Attempt to deal with crappy data or short downloads.
     *               Will try to return any useable geometry.
     *               尝试处理糟糕的数据或短时间的下载。将尝试返回任何可用的几何图形。
     */
    public STLParser(boolean strict) {
        strictParsing = strict;
    }

    /**
     * Get array with object names. {@link #parse} must be called once before
     * calling this method.
     * 获取带有对象名称的数组。{@link #parse}必须在调用此方法之前调用一次。
     *
     * @return Array of strings with names of objects. Size of array = number
     * of objects in file. If name is not contained then the appropriate
     * string is <code>null</code>.
     * 带有对象名称的字符串数组。数组的大小=文件中对象的数量。如果不包含name，则适当的字符串是<code>null</code>。
     */
    String[] getObjectNames() {
        return itsNames;
    }

    /**
     * Get number of facets per object. {@link #parse} must be called once
     * before calling this method.
     * 获取每个对象的面数。{@link #parse}必须在调用此方法之前调用一次。
     *
     * @return Array with the number of facets per object. Size of array =
     * number of objects in file.
     * 数组，包含每个对象的面数。数组的大小=文件中对象的数量。
     */
    int[] getNumOfFacets() {
        return itsNumOfFacets;
    }

    /**
     * Get number of objects in file. {@link #parse} must be called once
     * before calling this method.
     * 获取文件中对象的数量。{@link #parse}必须在调用此方法之前调用一次。
     */
    int getNumOfObjects() {
        return itsNumOfObjects;
    }

    /**
     * Get detailed messages on what was wrong when parsing.  Only can happen
     * when strictParsing is false.  Means things like getNumOfFacets might
     * be larger then reality.
     * 获取解析时错误的详细消息。只有当strictParsing为false时才会发生。意味着像getNumOfFacets这样的事情可能比现实更大。
     */
    public List<String> getParsingMessages() {
        return parsingMessages;
    }

    /**
     * Releases used resources. Must be called after finishing reading.
     * 版本使用资源。必须在阅读完毕后调用。
     */
    abstract void close() throws IOException;

    /**
     * Parses the file to obtain the number of objects, object names and number
     * of facets per object.
     * 解析文件以获取对象数量、对象名称和每个对象的facet数量。
     *
     * @param url URL to read from.
     * @return <code>true</code> if file is in ASCII format, <code>false</code>
     * otherwise. Use the appropriate subclass for reading.
     */
    abstract boolean parse(URL url)
            throws IOException;

    /**
     * Parses the file to obtain the number of objects, object names and number
     * of facets per object. A progress monitor will show the progress during
     * parsing.
     * 解析文件以获取对象数量、对象名称和每个对象的facet数量。进度监视器将显示解析期间的进度。
     *
     * @param url             URL to read from.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @return <code>true</code> if file is in ASCII format, <code>false</code>
     * otherwise. Use the appropriate subclass for reading.
     */
    abstract boolean parse(URL url, Component parentComponent)
            throws IllegalArgumentException, IOException;

    /**
     * Returns the data for a facet. The orientation of the facets (which way
     * is out and which way is in) is specified redundantly. First, the
     * direction of the normal is outward. Second, the vertices are listed in
     * counterclockwise order when looking at the object from the outside
     * (right-hand rule).<p>
     * Call consecutively until all data is read. Call {@link #close} after
     * finishing reading or if an exception occurs.
     * 返回facet的数据。方面的方向(哪个方向是出方向，哪个方向是进方向)是冗余指定的。
     * 首先，法线的方向是向外的。其次，当从外部看物体时，顶点是按逆时针顺序排列的(右手规则)。
     * <p>连续调用，直到读取完所有数据。读取完成后或发生异常时调用{@link #close}。
     *
     * @param normal   array of size 3 to store the normal vector.
     * @param vertices array of size 3x3 to store the vertex data.
     *                 <UL type=disk>
     *                 <LI>first index: vertex
     *                 <LI>second index:
     *                 <UL>
     *                 <LI>0: x coordinate
     *                 <LI>1: y coordinate
     *                 <LI>2: z coordinate
     *                 </UL>
     *                 </UL>
     * @return <code>True</code> if facet data is contained in
     * <code>normal</code> and <code>vertices</code>. <code>False</code>
     * if end of file is reached. Further calls of this method after
     * the end of file is reached will lead to an IOException.
     */
    abstract boolean getNextFacet(double[] normal, double[][] vertices)
            throws IllegalArgumentException, IOException;
}