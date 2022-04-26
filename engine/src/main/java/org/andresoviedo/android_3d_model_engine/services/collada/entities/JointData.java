package org.andresoviedo.android_3d_model_engine.services.collada.entities;

import java.util.ArrayList;
import java.util.List;


/**
 * Contains the extracted data for a single joint in the model. This stores the
 * joint's index, name, and local bind transform.
 * 包含模型中单个关节的提取数据。这将存储关节的索引、名称和局部绑定变换。
 *
 * @author Karl
 */
public class JointData {

    public final int index;
    public final String nameId;
    public final float[] bindLocalTransform;
    public final float[] inverseBindTransform;
    public String meshId;

    public final List<JointData> children = new ArrayList<>();

    public JointData(int index, String nameId, float[] bindLocalTransform, float[] inverseBindTransform) {
        this.index = index;
        this.nameId = nameId;
        this.bindLocalTransform = bindLocalTransform;
        this.inverseBindTransform = inverseBindTransform;
    }

    public void addChild(JointData child) {
        children.add(child);
    }

    public JointData setMeshId(String meshId) {
        this.meshId = meshId;
        return this;
    }
}
