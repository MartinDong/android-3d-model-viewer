/*****************************************************************************
 * STLFileReader.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2001, 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

package org.andresoviedo.android_3d_model_engine.services.stl;

// External Imports

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

// Local imports

/**
 * Class to read STL (Stereolithography) files.<p>
 * Usage: First create a <code>STLFileReader</code> object. To obtain the number
 * of objects, name of objects and number of facets for each object use the
 * appropriate methods. Then use the {@link #getNextFacet} method repetitively
 * to obtain the geometric data for each facet. Call {@link #close} to free the
 * resources.<p>
 * In case that the file uses the binary STL format, no check can be done to
 * assure that the file is in STL format. A wrong format will only be
 * recognized if an invalid amount of data is contained in the file.<p>
 * 用于读取STL(立体光刻)文件的类
 * 用法:首先创建一个<code>STLFileReader</code>对象。要获取对象的数量、对象的名称和每个对象的面数，请使用适当的方法。然后重复使用{@link#getNextFacet}方法来获取每个facet的几何数据。调用{@link #close}来释放资源
 * 如果文件使用二进制STL格式，则不能进行检查以确保文件是STL格式。只有当文件中包含无效的数据量时，才会识别出错误的格式
 *
 * @author Dipl. Ing. Paul Szawlowski -  University of Vienna, Dept. of Medical Computer Sciences
 * @version $Revision: 1.3 $
 */
public class STLFileReader {
    /**
     * STL 解析器
     */
    private STLParser itsParser;

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format.
     * 创建一个<code>STLFileReader</code>对象，从文件中读取STL文件。数据可以是ASCII或二进制格式。
     *
     * @param file <code>File</code> object of STL file to read. <code>File</code要读取的STL文件的>对象
     * @throws IllegalArgumentException The file was structurally incorrect 文件结构不正确
     */
    public STLFileReader(File file)
            throws IllegalArgumentException, IOException {
        this(file.toURL());
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format.
     * 创建一个<code>STLFileReader</code>对象，从文件中读取STL文件。数据可以是ASCII或二进制格式。
     *
     * @param fileName Name of STL file to read. 要读取的STL文件的名称。
     * @throws IllegalArgumentException The file was structurally incorrect 文件结构不正确
     */
    public STLFileReader(String fileName)
            throws IllegalArgumentException, IOException {
        this(new URL(fileName));
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format.
     * 创建一个<code>STLFileReader</code>对象，从文件中读取STL文件。数据可以是ASCII或二进制格式。
     *
     * @param fileName Name of STL file to read.
     * @param strict   Attempt to deal with crappy data or short downloads.
     *                 Will try to return any useable geometry.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(String fileName, boolean strict)
            throws IllegalArgumentException, IOException {
        this(new URL(fileName), strict);
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format.
     *
     * @param url URL of STL file to read.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(URL url)
            throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser();

        if (asciiParser.parse(url)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser();
            binParser.parse(url);
            itsParser = binParser;
        }
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format.
     *
     * @param url    URL of STL file to read.
     * @param strict Attempt to deal with crappy data or short downloads.
     *               Will try to return any useable geometry.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(URL url, boolean strict)
            throws IllegalArgumentException, IOException {

        final STLParser asciiParser = new STLASCIIParser(strict);

        if (asciiParser.parse(url)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser(strict);
            binParser.parse(url);
            itsParser = binParser;
        }
    }


    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param url             URL of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(URL url, Component parentComponent)
            throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser();
        if (asciiParser.parse(url, parentComponent)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser();
            binParser.parse(url, parentComponent);
            itsParser = binParser;
        }
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from an
     * URL. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param url             URL of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @param strict          Attempt to deal with crappy data or short downloads.
     *                        Will try to return any useable geometry.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(URL url, Component parentComponent, boolean strict)
            throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser(strict);
        if (asciiParser.parse(url, parentComponent)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser(strict);
            binParser.parse(url, parentComponent);
            itsParser = binParser;
        }
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param file            <code>File</code> object of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(File file, Component parentComponent)
            throws IllegalArgumentException, IOException {
        this(file.toURL(), parentComponent);
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param file            <code>File</code> object of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @param strict          Attempt to deal with crappy data or short downloads.
     *                        Will try to return any useable geometry.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(File file, Component parentComponent, boolean strict)
            throws IllegalArgumentException, IOException {
        this(file.toURL(), parentComponent, strict);
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param fileName        Name of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(String fileName, Component parentComponent)
            throws IllegalArgumentException, IOException {
        this(new URL(fileName), parentComponent);
    }

    /**
     * Creates a <code>STLFileReader</code> object to read a STL file from a
     * file. The data may be in ASCII or binary format. A progress monitor will
     * show the progress during reading.
     *
     * @param fileName        Name of STL file to read.
     * @param parentComponent Parent <code>Component</code> of progress monitor.
     *                        Use <code>null</code> if there is no parent.
     * @param strict          Attempt to deal with crappy data or short downloads.
     *                        Will try to return any useable geometry.
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public STLFileReader(String fileName, Component parentComponent, boolean strict)
            throws IllegalArgumentException, IOException {
        this(new URL(fileName), parentComponent, strict);
    }

    /**
     * Returns the data for a facet. The orientation of the facets (which way
     * is out and which way is in) is specified redundantly. First, the
     * direction of the normal is outward. Second, the vertices are listed in
     * counterclockwise order when looking at the object from the outside
     * (right-hand rule).<p>
     * 返回facet的数据。面的方向(哪个方向是出方向，哪个方向是进方向)是冗余指定的。
     * 首先，法线的方向是向外的。其次，当从外部看物体时，顶点是按逆时针顺序排列的(右手规则)。
     * Call consecutively until all data is read.
     * 连续调用，直到读取所有数据。
     *
     * @param normal   array of size 3 to store the normal vector. 数组大小为3，用于存储法向量。
     * @param vertices array of size 3x3 to store the vertex data. 大小为3x3的数组来存储顶点数据。
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
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    public boolean getNextFacet(double[] normal, double[][] vertices)
            throws IllegalArgumentException, IOException {
        return itsParser.getNextFacet(normal, vertices);
    }

    /**
     * Get array with object names.
     * 获取带有对象名称的数组。
     *
     * @return Array of strings with names of objects. Size of array = number
     * of objects in file. If name is not contained then the appropriate
     * string is <code>null</code>.
     * 带有对象名称的字符串数组。数组的大小=文件中对象的数量。如果不包含name，则适当的字符串是<code>null</code>。
     */
    public String[] getObjectNames() {
        return itsParser.getObjectNames();
    }

    /**
     * Get number of facets per object.
     * 获取每个对象的面数。
     *
     * @return Array with the number of facets per object. Size of array =
     * number of objects in file.
     * 数组，包含每个对象的面数。数组的大小=文件中对象的数量。
     */
    public int[] getNumOfFacets() {
        return itsParser.getNumOfFacets();
    }

    /**
     * Get detailed messages on what was wrong when parsing.  Only can happen
     * when strictParsing is false.  Means things like getNumOfFacets might
     * be larger then reality.
     * 获取解析时错误的详细消息。只有当strictParsing为false时才会发生。意味着像getNumOfFacets这样的事情可能比现实更大。
     */
    public List<String> getParsingMessages() {
        return itsParser.getParsingMessages();
    }

    /**
     * Get number of objects in file.
     * 获取文件中对象的数量。
     */
    public int getNumOfObjects() {
        return itsParser.getNumOfObjects();
    }

    /**
     * Releases used resources. Must be called after finishing reading.
     * 版本使用资源。必须在阅读完毕后调用。
     */
    public void close() throws IOException {
        if (itsParser != null) {
            itsParser.close();
        }
    }
}