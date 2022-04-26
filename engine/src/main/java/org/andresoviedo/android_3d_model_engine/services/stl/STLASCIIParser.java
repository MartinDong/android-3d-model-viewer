/*****************************************************************************
 * STLASCIIParser.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

// Internal imports

/**
 * Class to parse STL (stereolithography) files in ASCII format.<p>
 * 类来解析ASCII格式的STL(立体光刻)文件
 *
 * <p>
 * <b>Internationalisation Resource Names</b>
 * <p>
 * <ul>
 * <li>invalidKeywordMsg: Unknown keyword encountered. </li>
 * <li>emptyFileMsg: File contained the header but no content. </li>
 * <li>invalidDataMsg: Some strange data was encountered. </li>
 * <li>unexpectedEofMsg: We hit an EOF before we were expecting to.</li>
 * </ul>
 *
 * @author Dipl. Ing. Paul Szawlowski -
 * University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 2.0 $
 * @see STLFileReader
 * @see STLLoaderTask
 */
class STLASCIIParser extends STLParser {
    /**
     * Error message of a keyword that we don't recognise
     * 我们不认识的关键字的错误消息
     */
    private static final String UNKNOWN_KEYWORD_MSG_PROP =
            "org.j3d.loaders.stl.STLASCIIParser.invalidKeywordMsg";

    /**
     * Error message when the solid header is found, but there is no
     * geometry after it. Basically an empty file.
     * 当固体标题被发现，但没有几何图形后的错误消息。基本上是一个空文件。
     */
    private static final String EMPTY_FILE_MSG_PROP =
            "org.j3d.loaders.stl.STLASCIIParser.emptyFileMsg";

    /**
     * Unexpected data is encountered during parsing
     * 解析过程中遇到意外数据
     */
    private static final String INVALID_NORMAL_DATA_MSG_PROP =
            "org.j3d.loaders.stl.STLASCIIParser.invalidNormalDataMsg";

    /**
     * Unexpected data is encountered during parsing
     * 解析过程中遇到意外数据
     */
    private static final String INVALID_VERTEX_DATA_MSG_PROP =
            "org.j3d.loaders.stl.STLASCIIParser.invalidVertexDataMsg";

    /**
     * Unexpected EOF is encountered during parsing
     * 解析过程中遇到意外数据
     */
    private static final String EOF_WTF_MSG_PROP =
            "org.j3d.loaders.stl.STLASCIIParser.unexpectedEofMsg";

    /**
     * Reader for the main stream
     * 读者为主流
     */
    private BufferedReader itsReader;

    /**
     * The line number that we're at in the file
     * 我们在文件中的行号
     */
    private int lineCount;

    /**
     * Create a new default parser instance.
     * 创建一个新的默认解析器实例。
     */
    public STLASCIIParser() {
    }


    /**
     * Create a new default parser instance.
     * 创建一个新的默认解析器实例。
     */
    public STLASCIIParser(boolean strict) {
        super(strict);

    }

    /**
     * Finish the parsing off now.
     * 现在完成解析。
     */
    @Override
    public void close() throws IOException {
        if (itsReader != null) {
            itsReader.close();
        }
    }

    /**
     * Fetch a single face from the stream
     * 从流中获取单个人脸
     *
     * @param normal   Array length 3 to copy the normals in to 将法线复制到数组长度3中
     * @param vertices A [3][3] array for each vertex 每个顶点都是[3][3]数组
     * @throws IllegalArgumentException The file was structurally incorrect 文件结构不正确
     * @throws IOException              Something happened during the reading 在阅读过程中发生了一些事情
     */
    @Override
    public boolean getNextFacet(double[] normal, double[][] vertices)
            throws IOException {
        // format of a triangle is:
        //
        // facet normal number number number
        //   outer loop
        //     vertex number number number
        //     vertex number number number
        //     vertex number number number
        //   end loop
        // endfacet

        // First line with normals 第一行是法线
        String input_line = readLine();

        if (input_line == null) {
            return false;
        }

        StringTokenizer strtok = new StringTokenizer(input_line);
        String token = strtok.nextToken();

        // are we the first line of the file? If so, skip it 我们是文件的第一行吗?如果有，跳过它
        if (token.equals("solid")) {
            input_line = readLine();
            strtok = new StringTokenizer(input_line);
            token = strtok.nextToken();
            lineCount = 1;
        }

        // Have we reached the end of file? 文件结束了吗?
        // We've encountered a lot of broken files where they use two words "end solid" rather than the spec-required "endsolid".
        // 我们遇到过很多坏掉的文件，它们使用了两个词“端固”，而不是规范要求的“端固”。
        if (token.equals("endsolid") || input_line.contains("end solid")) {
            // Skip line and read next 跳过这一行，接着读
            try {
                return getNextFacet(normal, vertices);
            } catch (IOException ioe) {
                // gone past end of file 超过文件末尾
                return false;
            }
        }

        if (!token.equals("facet")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount + " word: " + token;
            throw new IllegalArgumentException(msg);
        }

        token = strtok.nextToken();
        if (!token.equals("normal")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount;
            throw new IllegalArgumentException(msg);
        }

        readNormal(strtok, normal);

        // Skip the outer loop line 跳过外部循环行
        input_line = readLine();

        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();
        lineCount++;

        if (!token.equals("outer")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount;
            throw new IllegalArgumentException(msg);
        }

        token = strtok.nextToken();
        if (!token.equals("loop")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount;
            throw new IllegalArgumentException(msg);
        }

        // Next 3x vertex reads 下一个3x顶点读取
        for (int i = 0; i < 3; i++) {
            input_line = readLine();
            strtok = new StringTokenizer(input_line);
            lineCount++;

            token = strtok.nextToken();

            if (!token.equals("vertex")) {
                close();

                I18nManager intl_mgr = I18nManager.getManager();

                String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                        + lineCount;
                throw new IllegalArgumentException(msg);
            }

            readCoordinate(strtok, vertices[i]);
        }

        // Read and skip the endloop && endfacet lines 读取和跳过端回线和端刻线
        input_line = readLine();
        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();
        lineCount++;

        if (!token.equals("endloop")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount;
            throw new IllegalArgumentException(msg);
        }

        input_line = readLine();
        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();
        lineCount++;

        if (!token.equals("endfacet")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": "
                    + lineCount;
            throw new IllegalArgumentException(msg);
        }

        return true;
    }

    /**
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    @Override
    public boolean parse(URL url, Component parentComponent)
            throws InterruptedIOException, IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }

            throw e;
        }

        stream = new ProgressMonitorInputStream(
                parentComponent, "analyzing " + url.toString(), stream);

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));

        boolean isAscii = false;

        try {
            isAscii = parse(reader);
        } finally {
            reader.close();
        }

        if (!isAscii) {
            return false;
        }

        try {
            stream = url.openStream();
        } catch (IOException e) {
            stream.close();
            throw e;
        }

        stream = new ProgressMonitorInputStream(
                parentComponent,
                "parsing " + url.toString(),
                stream);

        reader = new BufferedReader(new InputStreamReader(stream));
        itsReader = reader;

        return true;
    }

    /**
     * @throws IllegalArgumentException The file was structurally incorrect
     */
    @Override
    public boolean parse(URL url)
            throws IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }

            throw e;
        }

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));
        boolean isAscii = false;

        try {
            isAscii = parse(reader);
        } catch (InterruptedIOException e) {
            // should never happen
            e.printStackTrace();
        } finally {
            reader.close();
        }

        if (!isAscii) {
            return false;
        }

        try {
            stream = url.openStream();
        } catch (IOException e) {
            stream.close();
            throw e;
        }

        reader = new BufferedReader(new InputStreamReader(stream));
        itsReader = reader;

        return true;
    }

    /**
     * Parse the stream now from the given reader.
     * 现在从给定的读取器解析流。
     *
     * @param reader The reader to source the file from 文件来源的阅读器
     * @return true if this is a ASCII format file, false if not 如果是ASCII格式文件，则为true，否则为false
     * @throws IllegalArgumentException The file was structurally incorrect 文件结构不正确
     * @throws IOException              Something happened during the reading 在阅读过程中发生了一些事情
     */
    private boolean parse(BufferedReader reader)
            throws IOException, IllegalArgumentException {
        int numOfObjects = 0;
        int numOfFacets = 0;
        ArrayList<Integer> facetsPerObject = new ArrayList<Integer>(10);
        ArrayList<String> names = new ArrayList<String>(10);
        boolean isAscii = true;


        itsReader = reader;
        String line = readLine();
        int line_count = 1;

        line = line.trim();  // "Spec" says whitespace maybe anywhere except within numbers or words.  Great design!
        //“Spec”表示除了数字或单词之外的任何地方都可以有空格。伟大的设计!

        // check if ASCII format 检查是否为ASCII格式
        if (!line.startsWith("solid")) {
            return false;
        } else {
            if (line.length() > 6) {
                names.add(line.substring(6));
            } else {
                names.add(null);
            }
        }

        line = readLine();

        if (line == null) {
            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(EMPTY_FILE_MSG_PROP);
            throw new IllegalArgumentException(msg);
        }

        while (line != null) {
            line_count++;

            if (line.indexOf("facet") >= 0) {
                numOfFacets++;
                // skip next 6 lines: 跳过下面6行:
                // outer loop, 3 * vertex, endloop, endfacet 外环，3 *顶点，环端，端面
                for (int i = 0; i < 6; i++) {
                    readLine();
                }

                line_count += 6;
            }

            // watch order of if: solid contained also in endsolid 手表的if顺序:固载也在端固
            // JC: We have found a lot of badly formatted STL files generated  from some program that incorrectly end a solid object with a space between end and solid. Deal with that here.
            // JC:我们已经发现了许多格式不好的STL文件生成的一些程序，不正确地结束一个固体对象与结束和固体之间的空间。在这里处理吧。
            else if ((line.indexOf("endsolid") >= 0) ||
                    (line.indexOf("end solid") >= 0)) {
                facetsPerObject.add(new Integer(numOfFacets));
                numOfFacets = 0;
                numOfObjects++;
            } else if (line.indexOf("solid") >= 0) {
                line = line.trim();

                if (line.length() > 6) {
                    names.add(line.substring(6));
                }
            } else {
                line = line.trim();
                if (line.length() != 0) {
                    I18nManager intl_mgr = I18nManager.getManager();

                    String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) +
                            ": " + lineCount;

                    throw new IllegalArgumentException(msg);
                }
            }

            line = readLine();
        }

        if (numOfFacets > 0 && numOfObjects == 0) {
            numOfObjects = 1;
            facetsPerObject.add(new Integer(numOfFacets));
        }

        itsNumOfObjects = numOfObjects;
        itsNumOfFacets = new int[numOfObjects];
        itsNames = new String[numOfObjects];

        for (int i = 0; i < numOfObjects; i++) {
            Integer num = (Integer) facetsPerObject.get(i);
            itsNumOfFacets[i] = num.intValue();

            itsNames[i] = (String) names.get(i);
        }

        return true;
    }

    /**
     * Read three numbers from the tokeniser and place them in the double value returned.
     * 从标记器中读取三个数字，并将它们放入返回的double值中。
     */
    private void readNormal(StringTokenizer strtok, double[] vector)
            throws IOException {
        boolean error_found = false;

        for (int i = 0; i < 3; i++) {
            String num_str = strtok.nextToken();

            try {
                vector[i] = Double.parseDouble(num_str);
            } catch (NumberFormatException e) {
                if (!strictParsing) {
                    error_found = true;
                    continue;
                }

                I18nManager intl_mgr = I18nManager.getManager();

                String msg = intl_mgr.getString(INVALID_NORMAL_DATA_MSG_PROP) +
                        num_str;
                throw new IllegalArgumentException(msg);
            }

        }

        if (error_found) {
            // STL spec says use 0 0 0 for autocalc
            // STL规格说使用0 0 0自动计算
            vector[0] = 0;
            vector[1] = 0;
            vector[2] = 0;
        }
    }

    /**
     * Read three numbers from the tokeniser and place them in the double value returned.
     * 从标记器中读取三个数字，并将它们放入返回的double值中。
     */
    private void readCoordinate(StringTokenizer strtok, double[] vector)
            throws IOException {
        for (int i = 0; i < 3; i++) {
            String num_str = strtok.nextToken();

            boolean error_found = false;

            try {
                vector[i] = Double.parseDouble(num_str);
            } catch (NumberFormatException e) {
                if (strictParsing) {
                    I18nManager intl_mgr = I18nManager.getManager();

                    String msg = intl_mgr.getString(INVALID_VERTEX_DATA_MSG_PROP) +
                            ": Cannot parse vertex: " + num_str;
                    throw new IllegalArgumentException(msg);
                } else {
                    // Common error is to use commas instead of . in Europe
                    // 常见的错误是使用逗号代替。在欧洲
                    String new_str = num_str.replace(",", ".");

                    try {
                        vector[i] = Double.parseDouble(new_str);
                    } catch (NumberFormatException e2) {

                        I18nManager intl_mgr = I18nManager.getManager();

                        String msg = intl_mgr.getString(INVALID_VERTEX_DATA_MSG_PROP) +
                                ": Cannot parse vertex: " + num_str;
                        throw new IllegalArgumentException(msg);
                    }
                }
            }
        }
    }

    /**
     * Read a line from the input.  Ignore whitespace.
     * 从输入中读取一行。忽略空白。
     */
    private String readLine() throws IOException {
        String input_line = "";

        while (input_line.length() == 0) {
            input_line = itsReader.readLine();

            if (input_line == null) {
                break;
            }

            if (input_line.length() > 0 && Character.isWhitespace(input_line.charAt(0))) {
                input_line = input_line.trim();
            }
        }

        return input_line;
    }

}