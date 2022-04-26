package org.andresoviedo.android_3d_model_engine.services.collada.loader;

import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.services.collada.entities.JointData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.MeshData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.SkeletonData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.SkinningData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.Vertex;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.VertexSkinData;
import org.andresoviedo.util.xml.XmlNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the mesh data for a model from a collada XML file.
 * 从collada XML文件加载模型的网格数据。
 *
 * @author Karl
 */
public class GeometryLoader {

    private final XmlNode geometryNode;//几何体节点
    private final XmlNode materialsData;//材料数据
    private final XmlNode effectsData;//效果数据
    private final XmlNode imagesNode;//图像节点
    private Map<String, SkinningData> skinningDataMap;//蒙皮数据映射
    private SkeletonData skeletonData;//骨架数据

    private float[] verticesArray;//顶点数组
    private float[] normalsArray;//法线数组
    private float[] texturesArray;//纹理数组
    private int[] indicesArray;//索引数组
    private int[] jointIdsArray;//联合ID阵列
    private float[] weightsArray;//权重数组
    private FloatBuffer colorsBuffer;//颜色缓冲区

    List<Vertex> vertices = new ArrayList<>();//顶点

    List<float[]> vertex = new ArrayList<>();//角顶
    List<float[]> textures = new ArrayList<>();//纹理
    List<float[]> normals = new ArrayList<>();//常态
    List<Integer> indices = new ArrayList<>();//指数
    List<float[]> colors = new ArrayList<>();//颜色

    public GeometryLoader(XmlNode geometryNode, XmlNode materialsNode,
                          XmlNode effectsNode, XmlNode imagesNode,
                          Map<String, SkinningData> skinningData,
                          SkeletonData skeletonData) {
        this.skinningDataMap = skinningData;
        this.geometryNode = geometryNode;
        this.materialsData = materialsNode;
        this.imagesNode = imagesNode;
        this.effectsData = effectsNode;
        this.skeletonData = skeletonData;
    }

    /**
     * 提取模型数据
     *
     * @return 网格数据对象集合
     */
    public List<MeshData> extractModelData() {
        List<MeshData> ret = new ArrayList<MeshData>();
        for (XmlNode geometry : geometryNode.getChildren("geometry")) {

            vertices.clear();
            vertex.clear();
            normals.clear();
            textures.clear();
            indices.clear();
            colors.clear();

            String geometryId = geometry.getAttribute("id");
            Log.i("GeometryLoader", "Loading geometry '" + geometryId + "'");

            // process mesh... 处理网格。。。
            XmlNode meshData = geometry.getChild("mesh");

            // read vertices and normals 读取顶点和法线
            loadVertices(meshData, geometryId);

            // link skin weights to vertices 将蒙皮权重链接到顶点
            loadSkinningData(geometryId);

            // read texture and normals 读取纹理和法线
            XmlNode primitive = loadPrimitiveData(meshData);

            // get all primitives 获取所有原语
            List<XmlNode> polys = meshData.getChildren("polylist");

            // default is no color, no texture 默认设置为没有颜色，没有纹理
            Object[] colorAndTexture = new Object[2];

            // TODO: process vcount
            // this only works if poly is vcount=3 (trooper uses it)
            for (XmlNode poly : polys) {
                String material = poly.getAttribute("material");
                colorAndTexture = getMaterialColorAndTexture(material);
                assembleVertices(poly);
            }

            // triangle mesh
            List<XmlNode> triangless = meshData.getChildren("triangles");
            for (XmlNode triangles : triangless) {
                String material = triangles.getAttribute("material");
                colorAndTexture = getMaterialColorAndTexture(material);
                assembleVertices(triangles);
            }

            initArrays(geometryId);
            convertDataToArrays();
            convertIndicesListToArray();

            float[] color = (float[]) colorAndTexture[0];
            String texture = (String) colorAndTexture[1];
            ret.add(new MeshData(geometryId, verticesArray, texturesArray, normalsArray, color, colorsBuffer,
                    texture, indicesArray, jointIdsArray, weightsArray));
        }
        return ret;
    }

    private XmlNode loadPrimitiveData(XmlNode meshData) {

        // get actual primitive
        XmlNode primitiveNode = null;
        if (meshData.getChild("polylist") != null) {
            primitiveNode = meshData.getChild("polylist");
        } else if (meshData.getChild("triangles") != null) {
            primitiveNode = meshData.getChild("triangles");
        }

        // load primitive data
        if (primitiveNode != null) {
            XmlNode inputNormal = primitiveNode.getChildWithAttribute("input", "semantic", "NORMAL");
            loadData(normals, meshData, inputNormal, 3);
            XmlNode inputCoord = primitiveNode.getChildWithAttribute("input", "semantic", "TEXCOORD");
            loadData(textures, meshData, inputCoord, 2);
            XmlNode inputColor = primitiveNode.getChildWithAttribute("input", "semantic", "COLOR");
            loadData(colors, meshData, inputColor, 4);
        }

        return primitiveNode;
    }

    private JointData getJointData(JointData jointData, String geometryId) {
        if (geometryId.equals(jointData.meshId)) {
            return jointData;
        }
        for (JointData childJointData : jointData.children) {
            JointData candidate = getJointData(childJointData, geometryId);
            if (candidate != null) return candidate;
        }
        return null;
    }

    // <vertices> - may contain "VERTEX" and "NORMAL" semantics
    //  读取顶点和法线
    private void loadVertices(XmlNode meshData, String geometryId) {

        // get position & normal source ids 获取位置和正常源ID
        XmlNode verticesNode = meshData.getChild("vertices");
        for (XmlNode node : verticesNode.getChildren("input")) {
            String semanticId = node.getAttribute("semantic");
            if ("POSITION".equals(semanticId)) {
                loadData(vertex, meshData, node, 3);
            } else if ("NORMAL".equals(semanticId)) {
                loadData(normals, meshData, node, 3);
            }
        }

        // load vertices 加载顶点
        for (int i = 0; vertex != null && i < vertex.size(); i++) {
            vertices.add(new Vertex(vertex.get(i)));
        }

        // load normals 加载法线
        for (int i = 0; normals != null && i < vertices.size(); i++) {
            vertices.get(i).setNormalIndex(i);
        }
    }

    /**
     * 加载蒙皮数据
     *
     * @param geometryId 几何体Id
     */
    private void loadSkinningData(String geometryId) {
        // link vertex to weight data 将顶点链接到权重数据
        for (int i = 0; i < this.vertices.size(); i++) {

            Vertex vertex = this.vertices.get(i);
            float[] positionV = new float[]{vertex.getPosition()[0], vertex.getPosition()[1], vertex.getPosition()[2], 1};

            // skinning data 蒙皮数据
            VertexSkinData weightsData = null;
            if (skinningDataMap != null) {
                weightsData = skinningDataMap.containsKey(geometryId) ?
                        skinningDataMap.get(geometryId).verticesSkinData.get(i) : null;

                // Log.v("GeometryLoader","weightsData: "+weightsData);

                // transform vertex according to bind_shape_matrix 根据bind_-shape_矩阵变换顶点
                if (skinningDataMap.containsKey(geometryId)) {
                    float[] bindShapeMatrix = skinningDataMap.get(geometryId).bindShapeMatrix;
                    float[] bindShaped = new float[16];
                    Matrix.multiplyMV(bindShaped, 0, bindShapeMatrix, 0, positionV, 0);
                    positionV = bindShaped;
                }

                // TODO: review this. meshId is never set on skeleton data so this will probably never work
                // 回顾一下。meshId永远不会设置在骨架数据上，因此这可能永远不会起作用
                if (weightsData == null && skeletonData != null) {
                    JointData jointData = getJointData(skeletonData.headJoint, geometryId);
                    if (jointData != null) {
                        weightsData = new VertexSkinData();
                        weightsData.addJointEffect(jointData.index, 1);
                        weightsData.limitJointNumber(3);
                    }
                }
            }
            vertex.setPosition(new float[]{positionV[0], positionV[1], positionV[2]});
            vertex.setWeightsData(weightsData);
        }
    }

    /**
     * 加载数据
     *
     * @param list  列表
     * @param node  节点
     * @param input 输入节点
     * @param size  大小
     */
    private static void loadData(List<float[]> list, XmlNode node, XmlNode input, int size) {
        // no input, no data
        if (input == null) {
            return;
        }

        // get source data 获取源数据
        String sourceId = input.getAttribute("source").substring(1);
        XmlNode source = node.getChildWithAttribute("source", "id", sourceId);
        XmlNode data = source.getChild("float_array");
        int count = Integer.parseInt(data.getAttribute("count"));

        // accessor 存取器
        int stride = 4;
        XmlNode technique = source.getChild("technique_common");
        if (technique != null && technique.getChild("accessor") != null) {
            stride = Integer.parseInt(technique.getChild("accessor").getAttribute("stride"));
        }

        // parse floats 解析浮动
        Log.i("GeometryLoader", "Loading data. count: " + count + ", stride: " + stride);
        String[] floatData = data.getData().trim().split("\\s+");
        for (int i = 0; i < count; i += stride) {
            float[] f = new float[size];
            for (int j = 0; j < size; j++) {
                float val = 1;
                if (j < stride) {
                    val = Float.parseFloat(floatData[i + j]);
                }
                f[j] = val;
            }
            list.add(f);
        }
    }

    /**
     * 装配顶点
     *
     * @param primitive
     */
    private void assembleVertices(XmlNode primitive) {

        // offsets
        int vertexOffset = 0;
        int normalOffset = -1;
        int colorOffset = -1;
        int texOffset = -1;

        // get max offset
        int maxOffset = 0;
        for (XmlNode input : primitive.getChildren("input")) {
            String semantic = input.getAttribute("semantic");
            int offset = Integer.valueOf(input.getAttribute("offset"));
            if ("VERTEX".equals(semantic)) {
                vertexOffset = offset;
            } else if ("COLOR".equals(semantic)) {
                colorOffset = offset;
            } else if ("TEXCOORD".equals(semantic)) {
                // only parse set=1
                if (texOffset == -1) {
                    texOffset = offset;
                }
            } else if ("NORMAL".equals(semantic)) {
                normalOffset = offset;
            }
            if (offset > maxOffset) {
                maxOffset = offset;
            }
        }

        // stride
        int stride = maxOffset + 1;
        Log.i("GeometryLoader", "Loading primitive. Stride: " + stride);
        Log.i("GeometryLoader", "Primitive offsets " + vertexOffset + "," + normalOffset + "," + texOffset);

        // update vertex info 更新顶点信息
        String[] indexData = primitive.getChild("p").getData().trim().split("\\s+");
        for (int i = 0; i < indexData.length; i += stride) {

            // get vertex
            int positionIndex = Integer.parseInt(indexData[i + vertexOffset]);
            Vertex currentVertex = vertices.get(positionIndex);

            // parse normal if available 解析正常（如果可用）
            int normalIndex = -1;
            if (normalOffset >= 0) {
                normalIndex = Integer.parseInt(indexData[i + normalOffset]);
                currentVertex.setNormalIndex(normalIndex);
            }

            // parse color if available 分析颜色（如果可用）
            int colorIndex = -1;
            if (colorOffset >= 0) {
                colorIndex = Integer.parseInt(indexData[i + colorOffset]);
                currentVertex.setColorIndex(colorIndex);
            }

            // parse texture if available 分析纹理（如果可用）
            int texCoordIndex = -1;
            if (texOffset >= 0) {
                texCoordIndex = Integer.parseInt(indexData[i + texOffset]);
                currentVertex.setTextureIndex(texCoordIndex);
            }

            // update vertex info 更新顶点信息
            indices.add(positionIndex);
        }
    }

    /**
     * 获得材质颜色和纹理
     *
     * @param material
     * @return
     */
    private Object[] getMaterialColorAndTexture(String material) {
        Object[] ret = new Object[2];
        try {
            XmlNode materialNode = materialsData.getChildWithAttribute("material", "id", material);
            if (materialNode == null) {
                Log.e("GeometryLoader", "No material with id '" + material + "' found");
                return ret;
            }
            XmlNode instanceEffectNode = materialNode.getChild("instance_effect");
            String instanceEffectId = instanceEffectNode.getAttribute("url").substring(1);
            XmlNode effect = effectsData.getChildWithAttribute("effect", "id", instanceEffectId);
            XmlNode profile_common = effect.getChild("profile_COMMON");

            // get technique implementation 获取技术实现
            XmlNode technique = profile_common.getChild("technique");
            XmlNode techniqueImpl = null;
            if (technique.getChild("lambert") != null) {
                techniqueImpl = technique.getChild("lambert");
            } else if (technique.getChild("phong") != null) {
                techniqueImpl = technique.getChild("phong");
            } else if (technique.getChild("blinn") != null) {
                techniqueImpl = technique.getChild("blinn");
            }

            // get ambient
			/*XmlNode colorAmbientNode = null;
			if (techniqueImpl != null){
				XmlNode ambient = techniqueImpl.getChild("ambient");
				if (ambient != null){
					colorAmbientNode = ambient.getChild("color");
				}
			}*/

            // get diffuse
            XmlNode diffuse = null;
            if (techniqueImpl != null) {
                diffuse = techniqueImpl.getChild("diffuse");
            }

            // get color & texture 获取颜色和纹理
            XmlNode colorNode = null;
            XmlNode textureNode = null;
            if (diffuse != null) {
                colorNode = diffuse.getChild("color");
                textureNode = diffuse.getChild("texture");
            }

            // got color? 有颜色吗？
            if (colorNode != null) {
                String colorString = colorNode.getData().trim();
                String[] color = colorString.split("\\s+");
                ret[0] = new float[]{Float.valueOf(color[0]), Float.valueOf(color[1]), Float.valueOf(color[2]), Float
                        .valueOf(color[3])};
                Log.d("GeometryLoader", "Color '" + colorString + "'");
            }

            // fallback to ambient color 回退到环境色
			/*if (colorAmbientNode != null){
				String colorString = colorAmbientNode.getData().trim();
				String[] color = colorString.split("\\s+");
				ret[0] = new float[]{Float.valueOf(color[0]), Float.valueOf(color[1]), Float.valueOf(color[2]), Float
						.valueOf(color[3])};
				Log.d("GeometryLoader","Ambient color '"+colorString+"'");
			}*/

            // get texture image 获取纹理图像
            String textureFile = null;
            if (textureNode != null) {
                String texture = textureNode.getAttribute("texture");
                XmlNode newParamNode = profile_common.getChildWithAttribute("newparam", "sid", texture);
                if (newParamNode != null) {
                    String surface = newParamNode.getChild("sampler2D").getChild("source").getData();
                    newParamNode = profile_common.getChildWithAttribute("newparam", "sid", surface);
                    String imageRef = newParamNode.getChildWithAttribute("surface", "type", "2D").getChild("init_from").getData();
                    textureFile = imagesNode.getChildWithAttribute("image", "id", imageRef).getChild("init_from")
                            .getData();
                } else {
                    // TODO: is this ok?
                    textureFile = imagesNode.getChildWithAttribute("image", "id", texture).getChild("init_from").getData();
                }
            }
            Log.i("GeometryLoader", "Texture '" + textureFile + "'");
            ret[1] = textureFile;
        } catch (Exception ex) {
            Log.e("GeometryLoader", "Error reading material '" + material + "'", ex);
        }
        return ret;
    }

    /**
     * 将索引列表转换为数组
     *
     * @return 结果数组
     */
    private int[] convertIndicesListToArray() {
        this.indicesArray = new int[indices.size()];
        for (int i = 0; i < indicesArray.length; i++) {
            indicesArray[i] = indices.get(i);
        }
        return indicesArray;
    }

    /**
     * 将数据转换为数组
     *
     * @return 结果数组
     */
    private float convertDataToArrays() {

        Log.i("GeometryLoader", "vertices: " + vertices.size() +
                ", textures: " + (textures != null ? textures.size() : 0) +
                ", colors: " + (colors != null ? colors.size() : 0));

        float furthestPoint = 0;
        int gw = 0, gj = 0; // global weights, global joints
        for (int i = 0; i < vertices.size(); i++) {
            Vertex currentVertex = vertices.get(i);
            if (currentVertex.getLength() > furthestPoint) {
                furthestPoint = currentVertex.getLength();
            }
            float[] position = currentVertex.getPosition();
            if (textures != null && !textures.isEmpty()) {
                float[] textureCoord = textures.get(currentVertex.getTextureIndex());
                texturesArray[i * 2] = textureCoord[0];
                texturesArray[i * 2 + 1] = 1 - textureCoord[1];
            }
            verticesArray[i * 3] = position[0];
            verticesArray[i * 3 + 1] = position[1];
            verticesArray[i * 3 + 2] = position[2];

            if (normals != null && !normals.isEmpty()) {
                float[] normalVector = normals.get(currentVertex.getNormalIndex());
                normalsArray[i * 3] = normalVector[0];
                normalsArray[i * 3 + 1] = normalVector[1];
                normalsArray[i * 3 + 2] = normalVector[2];
            }
            if (colors != null && currentVertex.getColorIndex() >= 0) {
                float[] color = colors.get(currentVertex.getColorIndex());
                colorsBuffer.put(color);
            }

            VertexSkinData weights = currentVertex.getWeightsData();
            if (weights != null) {
                for (int j = 0; j < weights.jointIds.size(); j++) {
                    jointIdsArray[gj++] = weights.jointIds.get(j);
                }
                for (int w = 0; w < weights.weights.size(); w++) {
                    weightsArray[gw++] = weights.weights.get(w);
                }
            }
        }
        return furthestPoint;
    }

    private void initArrays(String geometryId) {
        this.verticesArray = new float[vertices.size() * 3];
        if (textures != null && !textures.isEmpty()) {
            this.texturesArray = new float[vertices.size() * 2];
        }
        this.normalsArray = new float[vertices.size() * 3];
        if (skinningDataMap != null && skinningDataMap.containsKey(geometryId) || vertices.get(0).getWeightsData() != null) {
            this.jointIdsArray = new int[vertices.size() * vertices.get(0).getWeightsData().jointIds.size()];
            this.weightsArray = new float[vertices.size() * vertices.get(0).getWeightsData().weights.size()];
        }
        if (!colors.isEmpty()) {
            this.colorsBuffer = createNativeByteBuffer(colors.size() * 4 * 4).asFloatBuffer();
        }
    }

    /**
     * 创建本机字节缓冲区
     *
     * @param length
     * @return
     */
    private static ByteBuffer createNativeByteBuffer(int length) {
        // initialize vertex byte buffer for shape coordinates
        // 初始化形状坐标的顶点字节缓冲区
        ByteBuffer bb = ByteBuffer.allocateDirect(length);
        // use the device hardware's native byte order
        // 使用设备硬件的本机字节顺序
        bb.order(ByteOrder.nativeOrder());
        return bb;
    }
}