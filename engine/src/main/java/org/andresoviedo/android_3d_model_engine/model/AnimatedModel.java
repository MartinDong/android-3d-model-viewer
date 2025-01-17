package org.andresoviedo.android_3d_model_engine.model;

import android.opengl.Matrix;

import org.andresoviedo.android_3d_model_engine.animation.Animation;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.Joint;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AnimationModel;

import java.nio.FloatBuffer;

/**
 * This class represents an entity in the world that can be animated. It
 * contains the model's VAO which contains the mesh data, the texture, and the
 * root joint of the joint hierarchy, or "skeleton". It also holds an int which
 * represents the number of joints that the model's skeleton contains, and has
 * its own {@link org.andresoviedo.android_3d_model_engine.animation.Animator} instance which can be used to apply animations to
 * this entity.
 * 此类表示世界上可以设置动画的实体。它包含模型的VAO，其中包含网格数据、纹理和关节层次或“骨架”的根关节。
 * 它还包含一个int，表示模型骨架包含的关节数，
 * 并有自己的{@link org.andresoviedo.android_3d_model_engine.animation.Animator}实例，可用于将动画应用于该实体。
 *
 * @author Karl
 */
public class AnimatedModel extends Object3DData {

    // skeleton
    private Joint rootJoint;
    private int jointCount;
    private int boneCount;
    private FloatBuffer jointIds;
    private FloatBuffer vertexWeigths;
    private Animation animation;
    private AnimationModel gltfAnimation = null;

    // cache
    private float[][] jointMatrices;

    public AnimatedModel() {
        super();
    }

    public AnimatedModel(FloatBuffer vertexArrayBuffer) {
        super(vertexArrayBuffer);
    }

    /**
     * Creates a new entity capable of animation. The inverse bind transform for
     * all joints is calculated in this constructor. The bind transform is
     * simply the original (no pose applied) transform of a joint in relation to
     * the model's origin (model-space). The inverse bind transform is simply
     * that but inverted.
     * 创建一个能够动画的新实体。在该构造函数中计算所有关节的反向绑定变换。
     * 绑定变换只是关节相对于模型原点（模型空间）的原始（未应用姿势）变换。
     * 反向绑定变换很简单，但却是反向的。
     *
     * @param rootJoint  - the root joint of the joint hierarchy which makes up the
     *                   "skeleton" of the entity.
     *                   构成实体“骨架”的关节层次的根关节。
     * @param jointCount - the number of joints in the joint hierarchy (skeleton) for
     *                   this entity.
     *                   此实体的关节层次（骨架）中的关节数。
     */
    public AnimatedModel setRootJoint(Joint rootJoint, int jointCount, int boneCount, boolean
            recalculateInverseBindTransforms) {
        this.rootJoint = rootJoint;
        this.jointCount = jointCount;
        this.boneCount = boneCount;
        float[] parentTransform = new float[16];
        Matrix.setIdentityM(parentTransform, 0);
        rootJoint.calcInverseBindTransform(parentTransform, recalculateInverseBindTransforms);
        this.jointMatrices = new float[boneCount][16];
        return this;
    }

    public int getJointCount() {
        return jointCount;
    }

    public int getBoneCount() {
        return boneCount;
    }

    public AnimatedModel setJointCount(int jointCount) {
        this.jointCount = jointCount;
        return this;
    }

    public AnimatedModel setJointIds(FloatBuffer jointIds) {
        this.jointIds = jointIds;
        return this;
    }

    public FloatBuffer getJointIds() {
        return jointIds;
    }

    public AnimatedModel setVertexWeights(FloatBuffer vertexWeigths) {
        this.vertexWeigths = vertexWeigths;
        return this;
    }

    public FloatBuffer getVertexWeights() {
        return vertexWeigths;
    }

    public AnimatedModel doAnimation(Animation animation) {
        this.animation = animation;
        return this;
    }

    public Animation getAnimation() {
        return animation;
    }

    public AnimatedModel doGltfAnimation(AnimationModel animation) {
        this.gltfAnimation = animation;
        return this;
    }

    public AnimationModel getGltfAnimation() {
        return gltfAnimation;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     * and every other joint in the skeleton is a descendant of this
     * joint.
     * 关节层次的根关节。该关节没有父关节，骨架中的每个其他关节都是该关节的后代。
     */
    public Joint getRootJoint() {
        return rootJoint;
    }

    /**
     * Gets an array of the all important model-space transforms of all the
     * joints (with the current animation pose applied) in the entity. The
     * joints are ordered in the array based on their joint index. The position
     * of each joint's transform in the array is equal to the joint's index.
     * 获取实体中所有关节（应用当前动画姿势）的所有重要模型空间变换的数组。
     * 关节在数组中根据其关节索引排序。每个关节的变换在数组中的位置等于关节的索引。
     *
     * @return The array of model-space transforms of the joints in the current
     * animation pose. 当前动画姿势中关节的模型空间变换数组。
     */
    public float[][] getJointTransforms() {
        addJointsToArray(rootJoint, jointMatrices);
        return jointMatrices;
    }

    /**
     * This adds the current model-space transform of a joint (and all of its
     * descendants) into an array of transforms. The joint's transform is added
     * into the array at the position equal to the joint's index.
     * 这会将关节（及其所有子体）的当前模型空间变换添加到变换数组中。关节的变换将添加到数组中与关节索引相等的位置。
     *
     * @param headJoint     - the current joint being added to the array. This method also
     *                      adds the transforms of all the descendents of this joint too.
     *                      正在添加到阵列的当前关节。该方法还添加了该关节的所有后代的变换。
     * @param jointMatrices - the array of joint transforms that is being filled.
     *                      正在填充的关节变换数组。
     */
    private void addJointsToArray(Joint headJoint, float[][] jointMatrices) {
        if (headJoint.getIndex() >= 0) {
            jointMatrices[headJoint.getIndex()] = headJoint.getAnimatedTransform();
        }
        for (int i = 0; i < headJoint.getChildren().size(); i++) {
            Joint childJoint = headJoint.getChildren().get(i);
            addJointsToArray(childJoint, jointMatrices);
        }
    }
}
