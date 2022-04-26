package org.andresoviedo.android_3d_model_engine.animation;

import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.Joint;

import java.util.HashMap;
import java.util.Map;


/**
 * This class contains all the functionality to apply an animation to an
 * animated entity. An Animator instance is associated with just one
 * {@link AnimatedModel}. It also keeps track of the running time (in seconds)
 * of the current animation, along with a reference to the currently playing
 * animation for the corresponding entity.
 * <p>
 * 此类包含将动画应用于动画实体的所有功能。Animator实例仅与一个{@link AnimatedModel}关联。
 * 它还跟踪当前动画的运行时间（以秒为单位），以及对相应实体当前播放动画的引用。
 * <p>
 * An Animator instance needs to be updated every frame, in order for it to keep
 * updating the animation pose of the associated entity. The currently playing
 * animation can be changed at any time using the doAnimation() method. The
 * Animator will keep looping the current animation until a new animation is
 * chosen.
 * <p>
 * 动画师实例需要每帧更新一次，以使其不断更新关联实体的动画姿势。
 * 可以随时使用doAnimation（）方法更改当前播放的动画。动画师将继续循环当前动画，直到选择新动画。
 * <p>
 * The Animator calculates the desired current animation pose by interpolating
 * between the previous and next keyframes of the animation (based on the
 * current animation time). The Animator then updates the transforms all of the
 * joints each frame to match the current desired animation pose.
 * <p>
 * 动画师通过在动画的上一个关键帧和下一个关键帧之间插值（基于当前动画时间），计算所需的当前动画姿势。
 * 然后，动画师更新每个帧的所有关节的变换，以匹配当前所需的动画姿势。
 *
 * @author Karl
 */
public class Animator {

    private float animationTime = 0;

    private final float IDENTITY_MATRIX[] = new float[16];

    // TODO: implement slower/faster speed
    // TODO: 实施较慢/较快的速度
    private float speed = 1f;

    private final Map<String, float[]> cache = new HashMap<>();

    public Animator() {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    /**
     * This method should be called each frame to update the animation currently
     * being played. This increases the animation time (and loops it back to
     * zero if necessary), finds the pose that the entity should be in at that
     * time of the animation, and then applies that pose to all the model's
     * joints by setting the joint transforms.
     * <p>
     * 应在每一帧调用此方法，以更新当前正在播放的动画。
     * 这会增加动画时间（并在必要时将其循环回零），找到实体在动画时应处于的姿势，然后通过设置关节变换将该姿势应用于模型的所有关节。
     */
    public void update(Object3DData obj) {
        this.update(obj, false);
    }

    public void update(Object3DData obj, boolean bindPoseOnly) {
        if (!(obj instanceof AnimatedModel)) {
            return;
        }

        // if (true) return;
        AnimatedModel animatedModel = (AnimatedModel) obj;
        if (animatedModel.getAnimation() == null) {
            return;
        }

        if (!bindPoseOnly) {
            // add missing key transformations
            // 添加缺少的关键点转换
            initAnimation(animatedModel);

            // increase time to progress animation
            // 增加动画进度的时间
            increaseAnimationTime((AnimatedModel) obj);

            Map<String, float[]> currentPose = calculateCurrentAnimationPose(animatedModel);

            applyPoseToJoints(currentPose, (animatedModel).getRootJoint(), IDENTITY_MATRIX, 0);
        } else {
            bindPose((animatedModel).getRootJoint(), IDENTITY_MATRIX);
        }
    }

    private void bindPose(Joint joint, final float[] parentTransform) {

        // performance optimization - reuse buffers
        // 性能优化-重用缓冲区
        float[] currentTransform = cache.get(joint.getName());
        if (currentTransform == null) {
            currentTransform = new float[16];
            cache.put(joint.getName(), currentTransform);
        }

        // apply joint local transform to current (parent) transform
        // 将联合局部变换应用于当前（父）变换
        Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);

        // apply calculated transform to inverse matrix for joints only
        // 仅对关节的逆矩阵应用计算出的变换
        if (joint.getIndex() >= 0) {
            Matrix.multiplyMM(joint.getAnimatedTransform(), 0, currentTransform, 0,
                    joint.getInverseBindTransform(), 0);
        }

        // apply transform for joint child  transform children
        // 将变换应用于关节子对象变换子对象
        for (int i = 0; i < joint.getChildren().size(); i++) {
            Joint childJoint = joint.getChildren().get(i);
            bindPose(childJoint, currentTransform);
        }
    }

    private void initAnimation(AnimatedModel animatedModel) {
        if (animatedModel.getAnimation().isInitialized()) {
            return;
        }
        KeyFrame[] keyFrames = animatedModel.getAnimation().getKeyFrames();
        Log.i("Animator", "Initializing " + animatedModel.getId() + ". " + keyFrames.length + " key frames...");
        for (int i = 0; i < keyFrames.length; i++) {
            int j = (i + 1) % keyFrames.length;
            KeyFrame keyFramePrevious = keyFrames[i];
            KeyFrame keyFrameNext = keyFrames[j];
            Map<String, JointTransform> jointTransforms = keyFramePrevious.getJointKeyFrames();
            for (Map.Entry<String, JointTransform> transform : jointTransforms.entrySet()) {
                String jointId = transform.getKey();
                if (keyFrameNext.getJointKeyFrames().containsKey(jointId)) {
                    continue;
                }
                JointTransform keyFramePreviousTransform = keyFramePrevious.getJointKeyFrames().get(jointId);
                JointTransform keyFrameNextTransform;
                KeyFrame keyFrameNextNext;
                int k = (j + 1) % keyFrames.length;
                do {
                    keyFrameNextNext = keyFrames[k];
                    keyFrameNextTransform = keyFrameNextNext.getJointKeyFrames().get(jointId);
                    k = (k + 1) % keyFrames.length;
                } while (keyFrameNextTransform == null);
                this.animationTime = keyFrameNext.getTimeStamp();
                float progression = calculateProgression(keyFramePrevious, keyFrameNextNext);
                JointTransform missingFrameTransform = JointTransform.interpolate(keyFramePreviousTransform, keyFrameNextTransform, progression);
                keyFrameNext.getJointKeyFrames().put(jointId, missingFrameTransform);
                Log.i("Animator", "Added missing key transform for " + jointId);
            }
        }
        animatedModel.getAnimation().setInitialized(true);
        Log.i("Animator", "Initialized " + animatedModel.getId() + ". " + keyFrames.length + " key frames");
    }

    /**
     * Increases the current animation time which allows the animation to
     * progress. If the current animation has reached the end then the timer is
     * reset, causing the animation to loop.
     * <p>
     * 增加当前动画时间，以允许动画进行。如果当前动画已结束，则计时器将重置，从而导致动画循环。
     */
    private void increaseAnimationTime(AnimatedModel obj) {
        this.animationTime = SystemClock.uptimeMillis() / 1000f * speed;
        this.animationTime %= obj.getAnimation().getLength();
    }

    /**
     * This method returns the current animation pose of the entity. It returns
     * the desired local-space transforms for all the joints in a map, indexed
     * by the name of the joint that they correspond to.
     * <p>
     * 此方法返回实体的当前动画姿势。它为贴图中的所有关节返回所需的局部空间变换，并根据它们对应的关节的名称进行索引。
     * <p>
     * The pose is calculated based on the previous and next keyframes in the
     * current animation. Each keyframe provides the desired pose at a certain
     * time in the animation, so the animated pose for the current time can be
     * calculated by interpolating between the previous and next keyframe.
     * <p>
     * 基于当前动画中的上一个关键帧和下一个关键帧计算姿势。每个关键帧在动画中的特定时间提供所需的姿势，
     * 因此可以通过在上一个关键帧和下一个关键帧之间插值来计算当前时间的动画姿势。
     * <p>
     * This method first finds the preious and next keyframe, calculates how far
     * between the two the current animation is, and then calculated the pose
     * for the current animation time by interpolating between the transforms at
     * those keyframes.
     * 该方法首先查找前一个关键帧和下一个关键帧，计算当前动画在这两个关键帧之间的距离，
     * 然后通过在这些关键帧处的变换之间插值来计算当前动画时间的姿势。
     *
     * @return The current pose as a map of the desired local-space transforms
     * for all the joints. The transforms are indexed by the name ID of
     * the joint that they should be applied to.
     * 当前姿势作为所有关节所需局部空间变换的贴图。变换将根据其应应用于的关节的名称ID进行索引。
     */
    private Map<String, float[]> calculateCurrentAnimationPose(AnimatedModel obj) {
        KeyFrame[] frames = getPreviousAndNextFrames(obj);
        float progression = calculateProgression(frames[0], frames[1]);
        return interpolatePoses(frames[0], frames[1], progression);
    }

    /**
     * This is the method where the animator calculates and sets those all-
     * important "joint transforms" that I talked about so much in the tutorial.
     * 这就是动画师计算和设置那些非常重要的“关节变换”的方法，我在教程中讨论了很多。
     * <p>
     * This method applies the current pose to a given joint, and all of its
     * descendants. It does this by getting the desired local-transform for the
     * current joint, before applying it to the joint. Before applying the
     * transformations it needs to be converted from local-space to model-space
     * (so that they are relative to the model's origin, rather than relative to
     * the parent joint). This can be done by multiplying the local-transform of
     * the joint with the model-space transform of the parent joint.
     * <p>
     * 此方法将当前姿势应用于给定关节及其所有子体。在将其应用于关节之前，它通过获取当前关节所需的局部变换来实现这一点。
     * 在应用变换之前，需要将其从局部空间转换到模型空间（以便它们相对于模型的原点，而不是相对于父关节）。
     * 这可以通过将关节的局部变换与父关节的模型空间变换相乘来实现。
     * <p>
     * The same thing is then done to all the child joints.
     * <p>
     * 然后对所有子关节执行相同的操作。
     * <p>
     * Finally the inverse of the joint's bind transform is multiplied with the
     * model-space transform of the joint. This basically "subtracts" the
     * joint's original bind (no animation applied) transform from the desired
     * pose transform. The result of this is then the transform required to move
     * the joint from its original model-space transform to it's desired
     * model-space posed transform. This is the transform that needs to be
     * loaded up to the vertex shader and used to transform the vertices into
     * the current pose.
     * <p>
     * 最后，将关节绑定变换的逆与关节的模型空间变换相乘。
     * 这基本上是从所需的姿势变换中“减去”关节的原始绑定（未应用动画）变换。
     * 这样做的结果就是将关节从原始模型空间变换移动到所需模型空间变换所需的变换。
     * 这是需要加载到顶点着色器并用于将顶点变换为当前姿势的变换。
     *
     * @param currentPose     - a map of the local-space transforms for all the joints for
     *                        the desired pose. The map is indexed by the name of the joint
     *                        which the transform corresponds to.
     *                        针对所需姿势的所有关节的局部空间变换贴图。贴图由变换对应的关节的名称索引。
     * @param joint           - the current joint which the pose should be applied to.
     *                        应应用姿势的当前关节。
     * @param parentTransform - the desired model-space transform of the parent joint for
     *                        the pose.
     *                        姿势的父关节的所需模型空间变换。
     */
    private void applyPoseToJoints(Map<String, float[]> currentPose, Joint joint, float[] parentTransform, int limit) {

        float[] currentTransform = cache.get(joint.getName());
        if (currentTransform == null) {
            currentTransform = new float[16];
            cache.put(joint.getName(), currentTransform);
        }

        // TODO: implement bind pose
        if (limit <= 0) {
            if (currentPose.get(joint.getName()) != null) {
                Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, currentPose.get(joint.getName()), 0);
            } else {
                Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);
            }
        } else {
            Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);
        }

        // calculate animation only if its used by vertices
        //joint.calcInverseBindTransform2(parentTransform);
        if (joint.getIndex() >= 0) {
            Matrix.multiplyMM(joint.getAnimatedTransform(), 0, currentTransform, 0,
                    joint.getInverseBindTransform(), 0);
        }

        // transform children
        for (int i = 0; i < joint.getChildren().size(); i++) {
            Joint childJoint = joint.getChildren().get(i);
            applyPoseToJoints(currentPose, childJoint, currentTransform, limit - 1);
        }
    }

    /**
     * Finds the previous keyframe in the animation and the next keyframe in the
     * animation, and returns them in an array of length 2. If there is no
     * previous frame (perhaps current animation time is 0.5 and the first
     * keyframe is at time 1.5) then the first keyframe is used as both the
     * previous and next keyframe. The last keyframe is used for both next and
     * previous if there is no next keyframe.
     * <p>
     * 查找动画中的上一个关键帧和动画中的下一个关键帧，并以长度为2的数组返回它们。
     * 如果没有前一帧（可能当前动画时间为0.5，第一个关键帧在时间1.5），则第一个关键帧将用作前一个关键帧和下一个关键帧。
     * 如果没有下一个关键帧，则最后一个关键帧将用于下一个关键帧和上一个关键帧。
     *
     * @return The previous and next keyframes, in an array which therefore will
     * always have a length of 2.
     * 数组中的上一个和下一个关键帧，因此其长度始终为2。
     */
    private KeyFrame[] getPreviousAndNextFrames(AnimatedModel obj) {
        KeyFrame[] allFrames = obj.getAnimation().getKeyFrames();
        KeyFrame previousFrame = allFrames[0];
        KeyFrame nextFrame = allFrames[0];
        for (int i = 1; i < allFrames.length; i++) {
            nextFrame = allFrames[i];
            if (nextFrame.getTimeStamp() > animationTime) {
                break;
            }
            previousFrame = allFrames[i];
        }
        return new KeyFrame[]{previousFrame, nextFrame};
    }

    /**
     * Calculates how far between the previous and next keyframe the current
     * animation time is, and returns it as a value between 0 and 1.
     * 计算当前动画时间在上一个关键帧和下一个关键帧之间的距离，并将其作为介于0和1之间的值返回。
     *
     * @param previousFrame - the previous keyframe in the animation. 动画中的上一个关键帧。
     * @param nextFrame     - the next keyframe in the animation. 动画中的下一个关键帧。
     * @return A number between 0 and 1 indicating how far between the two
     * keyframes the current animation time is.
     * 介于0和1之间的数字，指示当前动画时间在两个关键帧之间的距离。
     */
    private float calculateProgression(KeyFrame previousFrame, KeyFrame nextFrame) {
        float totalTime = nextFrame.getTimeStamp() - previousFrame.getTimeStamp();
        float currentTime = animationTime - previousFrame.getTimeStamp();
        // TODO: implement key frame display
        //return 0;
        return currentTime / totalTime;
    }

    /**
     * Calculates all the local-space joint transforms for the desired current
     * pose by interpolating between the transforms at the previous and next
     * keyframes.
     * 通过在上一个关键帧和下一个关键帧的变换之间插值，计算所需当前姿势的所有局部空间关节变换。
     *
     * @param previousFrame - the previous keyframe in the animation. 动画中的上一个关键帧
     * @param nextFrame     - the next keyframe in the animation. 动画中的下一个关键帧
     * @param progression   - a number between 0 and 1 indicating how far between the
     *                      previous and next keyframes the current animation time is.
     *                      介于0和1之间的数字，指示当前动画时间在上一个关键帧和下一个关键帧之间的距离。
     * @return The local-space transforms for all the joints for the desired
     * current pose. They are returned in a map, indexed by the name of
     * the joint to which they should be applied.
     * 为所需当前姿势的所有关节进行局部空间变换。它们将在映射中返回，并根据应用它们的关节的名称进行索引。
     */
    private Map<String, float[]> interpolatePoses(KeyFrame previousFrame, KeyFrame nextFrame, float progression) {
        // TODO: optimize this (memory allocation)
        Map<String, float[]> currentPose = new HashMap<>();
        for (String jointName : previousFrame.getJointKeyFrames().keySet()) {
            JointTransform previousTransform = previousFrame.getJointKeyFrames().get(jointName);
            if (Math.signum(progression) == 0) {
                currentPose.put(jointName, previousTransform.getLocalTransform());
            } else {
                // memory optimization
                float[] jointPose = cache.get(jointName);
                if (jointPose == null) {
                    jointPose = new float[16];
                    cache.put(jointName, jointPose);
                }
                float[] jointPoseRot = cache.get("___rotation___interpolation___");
                if (jointPoseRot == null) {
                    jointPoseRot = new float[16];
                    cache.put("___rotation___interpolation___", jointPoseRot);
                }
                // calculate interpolation
                JointTransform nextTransform = nextFrame.getJointKeyFrames().get(jointName);
                JointTransform.interpolate(previousTransform, nextTransform, progression, jointPose, jointPoseRot);
                currentPose.put(jointName, jointPose);
            }
        }
        return currentPose;
    }

}

