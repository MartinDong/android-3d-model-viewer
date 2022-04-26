/*****************************************************************************
 * STLBinaryParser.java
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

import org.andresoviedo.util.io.ProgressMonitorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

// Local imports

/**
 * Class to parse STL (stereolithography) files in binary format.<p>
 * 类以二进制格式解析STL(立体光刻)文件
 *
 * @author Dipl. Ing. Paul Szawlowski -
 * University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 1.3 $
 * @see STLFileReader
 * @see STLLoaderTask
 */
class STLBinaryParser extends STLParser {
    /**
     * size of binary header
     * 二进制报头大小
     */
    private static int HEADER_SIZE = 84;

    /**
     * size of one facet record in binary format
     * 二进制格式的一个facet记录的大小
     */
    private static int RECORD_SIZE = 50;

    /**
     * size of comments in header
     * 标题中注释的大小
     */
    private static int COMMENT_SIZE = 80;

    /**
     * The stream that is being read from
     * 正在读取的流
     */
    private BufferedInputStream itsStream;

    /**
     * Common buffer for reading
     * 用于读取的通用缓冲区
     */
    private byte[] itsReadBuffer;

    /**
     * Common buffer for reading the converted data from bytes
     * 用于从字节中读取转换数据的通用缓冲区
     */
    private int[] itsDataBuffer;

    public STLBinaryParser() {
        itsReadBuffer = new byte[48];
        itsDataBuffer = new int[12];
    }

    /**
     * Constructor.
     *
     * @param strict Attempt to deal with crappy data or short downloads.
     *               Will try to return any useable geometry.
     *               尝试处理糟糕的数据或短时间的下载。将尝试返回任何可用的几何图形。
     */
    public STLBinaryParser(boolean strict) {
        super(strict);

        itsReadBuffer = new byte[48];
        itsDataBuffer = new int[12];
    }

    @Override
    public void close() throws IOException {
        if (itsStream != null) {
            itsStream.close();
        }
    }

    @Override
    public boolean parse(URL url)
            throws IllegalArgumentException, IOException {
        InputStream stream = null;
        int length = -1;
        try {
            URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }
        itsStream = new BufferedInputStream(stream);
        return parse(length);
    }

    @Override
    public boolean parse(URL url, Component parentComponent)
            throws IllegalArgumentException, IOException {
        InputStream stream = null;
        int length = -1;
        try {
            URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }
        stream = new ProgressMonitorInputStream(
                parentComponent,
                "parsing " + url.toString(),
                stream);

        itsStream = new BufferedInputStream(stream);
        return parse(length);
    }

    /**
     * Internal convenience method that does the stream parsing regardless of
     * the input source the stream came from. Assumes itsStream is already
     * initialised before it called here.
     * 内部方便的方法，它进行流解析，而不管流来自哪个输入源。假设它的流在这里调用之前已经初始化了。
     *
     * @param length The length of data from the incoming stream, if not. Use
     *               -1 not known.
     *               如果不是，则输入流的数据长度。使用-1不知道。
     * @return true if the method does not work out 如果该方法无效，则为
     */
    private boolean parse(int length)
            throws IllegalArgumentException, IOException {
        try {
            // skip header until number of facets info
            // 跳过头，直到面信息的数量
            for (int i = 0; i < COMMENT_SIZE; i++) {
                itsStream.read();
            }

            // binary file contains only on object
            // 二进制文件只包含在对象上
            itsNumOfObjects = 1;
            itsNumOfFacets =
                    new int[]{LittleEndianConverter.read4ByteBlock(itsStream)};
            itsNames = new String[1];
            // if length of file is known, check if it matches with the content  binary file contains only on object
            // 如果文件长度已知，检查它是否与二进制文件只包含在对象上的内容匹配
            if (strictParsing && length != -1 &&
                    length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {
                String msg = "File size does not match the expected size for" +
                        " the given number of facets. Given " +
                        itsNumOfFacets[0] + " facets for a total size of " +
                        (itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) +
                        " but the file size is " + length;
                close();

                throw new IllegalArgumentException(msg);
            } else if (!strictParsing && length != -1 &&
                    length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {

                String msg = "File size does not match the expected size for" +
                        " the given number of facets. Given " +
                        itsNumOfFacets[0] + " facets for a total size of " +
                        (itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) +
                        " but the file size is " + length;

                if (parsingMessages == null) {
                    parsingMessages = new ArrayList<String>();
                }
                parsingMessages.add(msg);
            }
        } catch (IOException e) {
            close();
            throw e;
        }
        return false;
    }

    /**
     * Read the next face from the underlying stream
     * 从下面的流中读取下一个面孔
     *
     * @return true if the read completed successfully 如果读取成功完成，则为True
     */
    @Override
    public boolean getNextFacet(double[] normal, double[][] vertices)
            throws IOException {
        LittleEndianConverter.read(itsReadBuffer,
                itsDataBuffer,
                0,
                12,
                itsStream);

        boolean nan_found = false;

        for (int i = 0; i < 3; i++) {
            normal[i] = Float.intBitsToFloat(itsDataBuffer[i]);
            if (Double.isNaN(normal[i]) || Double.isInfinite(normal[i])) {
                nan_found = true;
            }
        }

        if (nan_found) {
            // STL spec says use 0 0 0 for autocalc
            // STL规格说使用0 0 0自动计算
            normal[0] = 0;
            normal[1] = 0;
            normal[2] = 0;
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                vertices[i][j] =
                        Float.intBitsToFloat(itsDataBuffer[i * 3 + j + 3]);
            }
        }

        // skip last 2 padding bytes
        itsStream.read();
        itsStream.read();
        return true;
    }
}