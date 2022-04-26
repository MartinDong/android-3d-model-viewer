package org.andresoviedo.android_3d_model_engine.services.collada.entities;

/**
 * This contains the data for a transformation of one joint, at a certain time
 * in an animation. It has the name of the joint that it refers to, and the
 * local transform of the joint in the pose position.
 * 其中包含动画中某个特定时间一个关节的变换数据。它具有所指关节的名称，以及该关节在姿势位置的局部变换。
 *
 * @author Karl
 */
public class JointTransformData {

    public final String jointNameId;
    public final float[] jointLocalTransform;

    public JointTransformData(String jointNameId, float[] jointLocalTransform) {
        this.jointNameId = jointNameId;
        this.jointLocalTransform = jointLocalTransform;
    }
}
