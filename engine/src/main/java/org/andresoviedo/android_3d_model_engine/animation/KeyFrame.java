package org.andresoviedo.android_3d_model_engine.animation;

import java.util.Map;

/**
 * Represents one keyframe of an animation. This contains the timestamp of the
 * keyframe, which is the time (in seconds) from the start of the animation when
 * this keyframe occurs.
 * <p>
 * 表示动画的一个关键帧。其中包含关键帧的时间戳，即从该关键帧出现时动画开始算起的时间（以秒为单位）。
 * <p>
 * It also contains the desired bone-space transforms of all of the joints in
 * the animated entity at this keyframe in the animation (i.e. it contains all
 * the joint transforms for the "pose" at this time of the animation.). The
 * joint transforms are stored in a map, indexed by the name of the joint that
 * they should be applied to.
 * <p>
 * 它还包含动画中该关键帧处动画实体中所有关节的所需骨骼空间变换（即，它包含动画此时“姿势”的所有关节变换）。
 * 关节变换存储在一个映射中，由它们应该应用到的关节的名称索引。
 *
 * @author Karl
 */
public class KeyFrame {

    private final float timeStamp;
    private final Map<String, JointTransform> pose;

    /**
     * @param timeStamp      - the time (in seconds) that this keyframe occurs during the
     *                       animation.
     *                       该关键帧在动画期间出现的时间（秒）。
     * @param jointKeyFrames - the local-space transforms for all the joints at this
     *                       keyframe, indexed by the name of the joint that they should be
     *                       applied to.
     *                       该关键帧处所有关节的局部空间变换，由它们应应用于的关节的名称索引。
     */
    public KeyFrame(float timeStamp, Map<String, JointTransform> jointKeyFrames) {
        this.timeStamp = timeStamp;
        this.pose = jointKeyFrames;
    }

    /**
     * @return The time in seconds of the keyframe in the animation.
     * 动画中关键帧的时间（以秒为单位）。
     */
    protected float getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return The desired bone-space transforms of all the joints at this
     * keyframe, of the animation, indexed by the name of the joint that
     * they correspond to. This basically represents the "pose" at this
     * keyframe.
     * 该关键帧处所有关节、动画的所需骨骼空间变换，由它们对应的关节名称索引。这基本上代表了这个关键帧的“姿势”。
     */
    protected Map<String, JointTransform> getJointKeyFrames() {
        return pose;
    }

}
