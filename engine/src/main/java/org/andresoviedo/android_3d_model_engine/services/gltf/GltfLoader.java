package org.andresoviedo.android_3d_model_engine.services.gltf;

import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AccessorModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.GltfModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.ImageModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.MeshModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.MeshPrimitiveModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.NodeModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.SceneModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.TextureModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.io.GltfModelReader;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load glTF model data to inner data structure
 * 将glTF模型数据加载到内部数据结构中
 *
 * @author wenlinm
 */
public class GltfLoader {

    private static Map<String, Integer> keyHandleMap = new HashMap<>();
    // 纹理关键字
    private static String[] textureKeys = {"baseColorTexture"
            , "emissiveTexture"
            , "occlusionTexture"
            , "normalTexture"};

    private static Map<NodeModel, List<AnimatedModel>> nodeMap = new HashMap<>();

    // read model data and fill in data in Object3DData structure
    // 读取模型数据，在Object3DData结构中填充数据
    public static Object[] buildAnimatedModel(URI uri) throws IOException {


        GltfModelReader gltfModelReader = new GltfModelReader();
        GltfModel gltfModel = gltfModelReader.read(uri);

        Log.d("GltfLoaderTask", uri.toString());

        //convert all primitives to Object3DData object
        // 将所有原语转换为Object3DData对象
        List<Object3DData> ret = new ArrayList<>();
//        bindKeyHandle();

        int index = 1;

        // traverse all scene and for each root node, do dfs
        // 遍历所有场景，并对每个根节点执行DFS
        for (SceneModel scene : gltfModel.getSceneModels()) {
            for (NodeModel node : scene.getNodeModels()) {
                traverseNode(node, ret, index);
                index++;
            }
        }

        return new Object[]{gltfModel, ret};

    }

    private static void traverseNode(NodeModel node, List<Object3DData> ret, int index) {
        int i = 1;
        for (MeshModel mesh : node.getMeshModels()) {
            for (MeshPrimitiveModel meshPrimitive : mesh.getMeshPrimitiveModels()) {
                // for each mesh primitive initialize animated model object which
                // inherited from Object3DData to hold data
                // 对于每个网格原语初始化从Object3DData继承来的动画模型对象来保存数据
                AnimatedModel data3D = new AnimatedModel();
                Map<String, AccessorModel> attriMap = meshPrimitive.getAttributes();

                // for debug identification
                // 调试识别
                if (mesh.getName() != null) {
                    data3D.setId(mesh.getName() + i);
                    i++;
                } else {
                    data3D.setId("Triangle" + index);
                }

                // TODO: refactor to abstract this part

                // for each mesh primitive, check each keywords and deal with the data correspondingly
                // 对于每个网格原语，检查每个关键字，并对数据进行相应处理
                for (String key : attriMap.keySet()) {
                    if (!isKeyValid(key)) {
                        continue;
                    }

                    // get accessor, bufferview, and buffer for vertex buffer, normal buffer  texture coordinate buffer
                    // 获取访问器，bufferview，和缓冲区的顶点缓冲区，普通缓冲区纹理坐标缓冲区
                    AccessorModel accessor = attriMap.get(key);

                    // TODO: add read of other types of buffer if needed 如果需要，添加其他类型的缓冲区读取
                    Buffer dataB = accessor.getCorrBufferData();
                    FloatBuffer dataFB = null;
                    if (dataB instanceof FloatBuffer) {
                        dataFB = (FloatBuffer) dataB;
                    } else if (dataB instanceof ShortBuffer) {
                        ShortBuffer dataSB = (ShortBuffer) dataB;
                        short[] shortArr = new short[dataSB.capacity()];
                        dataSB.get(shortArr);
                        ByteBuffer bb = ByteBuffer.allocate(shortArr.length * 2);
                        bb.asShortBuffer().put(shortArr);
                        dataFB = bb.asFloatBuffer();
                    }

//                    FloatBuffer dataFB = accessor.getCorrBufferData();
                    if (key.equals("POSITION")) {
                        transformVertices(data3D, node, dataFB);
                        data3D.setVertexArrayBuffer(dataFB);
                        float[] test = new float[dataFB.capacity()];
                        dataFB.get(test);
                        data3D.setVertices(test);
                    } else if (key.equals("NORMAL")) {
                        data3D.setVertexNormalsArrayBuffer(dataFB);
                    } else if (key.startsWith("TEXCOORD_")) {
                        data3D.addTextureCoords(key, dataFB);
                    } else if (key.startsWith("COLOR_")) {
                        data3D.setVertexColorsArrayBuffer(dataFB);
                    } else if (key.startsWith("JOINTS_")) {
                        data3D.setJointIds(dataFB);
                    } else if (key.startsWith("WEIGHTS_")) {
                        data3D.setVertexWeights(dataFB);
                    }
                }

                // if this mesh primitive describe indexed geometry, store draw order buffer
                // 如果这个网格原语描述了索引几何体，存储绘制顺序缓冲区
                AccessorModel indices = meshPrimitive.getIndices();
                if (indices != null) {
                    Buffer indexBuffer = indices.getCorrBufferData();

                    data3D.setDrawOrder(indexBuffer);
                    data3D.setDrawOrderBufferType(indices.getComponentType());
                    data3D.setDrawUsingArrays(false);
                }

                // TODO: add technique model to shader for realistic PBR 为现实的PBR添加技术模型着色器
//                TechniqueModel test2 = meshPrimitive.getMaterialModel().getTechniqueModel();

                data3D.setGltfMaterial(meshPrimitive.getMaterialModel());

                data3D.setDrawMode(meshPrimitive.getMode());
                WavefrontLoader.ModelDimensions modelDimensions = new WavefrontLoader.ModelDimensions();
                data3D.setDimensions(modelDimensions);

                ret.add(data3D);
                // add Object3DData correspond with node for adding animation in
                // populatedAnimatedModel
                if (!nodeMap.containsKey(node)) {
                    List<AnimatedModel> data3dList = new ArrayList<>();
                    nodeMap.put(node, data3dList);
                }
                nodeMap.get(node).add(data3D);
            }
        }

        if (node.getChildren().isEmpty() == true) {
            return;
        } else {
            for (NodeModel child : node.getChildren()) {
                traverseNode(child, ret, ++index);
            }
        }
    }


    public static void populateAnimatedModel(URL url, List<Object3DData> datas, GltfModel modelData) {

        for (int i = 0; i < datas.size(); i++) {
            Object3DData data = datas.get(i);

            FloatBuffer normalsBuffer = data.getVertexNormalsArrayBuffer();
            FloatBuffer vertexBuffer = data.getVertexArrayBuffer();
            Buffer indexBuffer = data.getDrawOrderBuffer();

            WavefrontLoader.ModelDimensions modelDimensions = data.getDimensions();

            boolean first = true;
            for (int counter = 0; counter < data.getVertices().length - 3; counter += 3) {

                // update model dimensions
                if (first) {
                    modelDimensions.set(data.getVertices()[counter], data.getVertices()[counter + 1], data.getVertices()[counter + 2]);
                    first = false;
                }
                modelDimensions.update(data.getVertices()[counter], data.getVertices()[counter + 1], data.getVertices()[counter + 2]);

            }

            bindTexture(data, modelData);
            data.setFaces(new WavefrontLoader.Faces(vertexBuffer.capacity() / 3));
            data.setDrawOrder(indexBuffer);
        }

        // TODO: Iterate through all channels, map target's node and change all object3dData associated
        // TODO: with that node's model matrix based on path, record the time since the object is
        // TODO: rendered and calculate the interpolated value for each transformation
//        for (AnimationModel animation : modelData.getAnimationModels()){
//            for (AnimationModel.Channel channel : animation.getChannels()){
//                List<AnimatedModel> data3DList = nodeMap.get(channel.getNodeModel());
//                for (AnimatedModel data3D : data3DList){
//                    data3D.doGltfAnimation(animation);
//                }
//            }
//        }
    }


    private static void bindTexture(Object3DData data, GltfModel gltfModel) {
        List<TextureModel> textures = gltfModel.getTextureModels();
        Map<String, Object> materialValueMap = data.getGltfMaterial().getValues();

        // Default Texture
        if (materialValueMap.get("baseColorTexture") != null) {
            Integer index = (Integer) materialValueMap.get("baseColorTexture");
            String texCordKey = (String) materialValueMap.get("baseColorTexCoord");
            data.setTextureCoordsArrayBuffer(data.getTextureCoords(texCordKey));
            TextureModel baseColorTexture = textures.get(index);
            ImageModel image = baseColorTexture.getImageModel();
            // Faster way
            data.setTextureFile(null);
            ByteBuffer imageByteBuffer = image.getImageData();
            byte[] imageByte = byteBufferToByte(imageByteBuffer);
            data.setTextureData(imageByte);
            data.setFilter(baseColorTexture.getMinFilter(), baseColorTexture.getMagFilter());
            data.setTextureWrap(baseColorTexture.getWrapS(), baseColorTexture.getWrapT());
        }

        // Emissive Texture
        if (materialValueMap.get("emissiveTexture") != null) {
            Integer index = (Integer) materialValueMap.get("emissiveTexture");
            String texCordKey = (String) materialValueMap.get("emissiveTexCoord");
            data.setEmissiveTextureCoordsArrayBuffer(data.getTextureCoords(texCordKey));
            TextureModel emissiveTexture = textures.get(index);
            ImageModel image = emissiveTexture.getImageModel();
            // Faster way
            ByteBuffer imageByteBuffer = image.getImageData();
            byte[] imageByte = byteBufferToByte(imageByteBuffer);
            data.setEmissiveTextureData(imageByte);
            data.setEmissiveFilter(emissiveTexture.getMinFilter(), emissiveTexture.getMagFilter());
            data.setEmissiveTextureWrap(emissiveTexture.getWrapS(), emissiveTexture.getWrapT());
        }

        if (materialValueMap.get("occlusionTexture") != null) {

        }

        if (materialValueMap.get("normalTexture") != null) {

        }
        data.setColor((float[]) materialValueMap.get("baseColorFactor"));
        data.setIsDoubleSided((Integer) materialValueMap.get("isDoubleSided"));
    }

    private static FloatBuffer transformVertices(Object3DData obj, NodeModel node, FloatBuffer dataFB) {

        // model matrix of the node
        // 节点的模型矩阵
        obj.setModelMatrix(node.getMatrix());

        // if model matrix is not assigned, use translation, rotation, and scale attribute
        // 如果没有分配模型矩阵，使用平移、旋转和缩放属性
        obj.setScale(node.getScale());
        obj.setRotation(node.getRotation());
        obj.setPosition(node.getTranslation());

        float[] modelMatrix = obj.getModelMatrix();

        for (int i = 0; i < dataFB.capacity(); i += 3) {
            float[] ver = {dataFB.get(i), dataFB.get(i + 1), dataFB.get(i + 2), 1};
            Matrix.multiplyMV(ver, 0, modelMatrix, 0, ver, 0);
            dataFB.put(i, ver[0]);
            dataFB.put(i + 1, ver[1]);
            dataFB.put(i + 2, ver[2]);
        }

        return dataFB;
    }

    private static boolean isKeyValid(String key) {
        return key.equals("POSITION") || key.equals("NORMAL")
                || key.startsWith("TEXCOORD_") || key.startsWith("COLOR_")
                || key.startsWith("JOINTS_") || key.startsWith("WEIGHTS_");
    }


    private static byte[] byteBufferToByte(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        byte[] arr = new byte[byteBuffer.remaining()];
        byteBuffer.get(arr);

        return arr;
    }

}
