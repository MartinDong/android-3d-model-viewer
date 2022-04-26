package org.andresoviedo.android_3d_model_engine.services.collada.entities;

/**
 * Contains the extracted data for an animation, which includes the length of
 * the entire animation and the data for all the keyframes of the animation.
 * 包含为动画提取的数据，其中包括整个动画的长度和动画的所有关键帧的数据。
 *
 * @author Karl
 */
public class AnimationData {

    public final float lengthSeconds;
    public final KeyFrameData[] keyFrames;

    public AnimationData(float lengthSeconds, KeyFrameData[] keyFrames) {
        this.lengthSeconds = lengthSeconds;
        this.keyFrames = keyFrames;
    }

}
