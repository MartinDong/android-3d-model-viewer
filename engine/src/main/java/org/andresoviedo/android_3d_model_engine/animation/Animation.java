package org.andresoviedo.android_3d_model_engine.animation;


/**
 * Represents an animation that can applied to an {@link org.andresoviedo.android_3d_model_engine.model.AnimatedModel} . It
 * contains the length of the animation in seconds, and a list of
 * 表示可应用于{@link org.andresoviedo.android_3d_model_engine.model.AnimatedModel}的动画。它包含动画的长度（以秒为单位），以及动画的列表
 * {@link KeyFrame}s.
 *
 * @author Karl
 */
public class Animation {

    private final float length;//in seconds
    private final KeyFrame[] keyFrames;
    private boolean initialized;

    /**
     * @param lengthInSeconds - the total length of the animation in seconds. 动画的总长度（以秒为单位）。
     * @param frames          - all the keyframes for the animation, ordered by time of appearance in the animation. 动画的所有关键帧，按动画中出现的时间排序。
     */
    public Animation(float lengthInSeconds, KeyFrame[] frames) {
        this.keyFrames = frames;
        this.length = lengthInSeconds;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return The length of the animation in seconds. 动画的长度（以秒为单位）。
     */
    public float getLength() {
        return length;
    }

    /**
     * @return An array of the animation's keyframes. The array is ordered based
     * on the order of the keyframes in the animation (first keyframe of
     * the animation in array position 0).
     * 动画关键帧的数组。阵列的顺序基于动画中关键帧的顺序（位于阵列位置0的动画的第一个关键帧）。
     */
    public KeyFrame[] getKeyFrames() {
        return keyFrames;
    }

}
