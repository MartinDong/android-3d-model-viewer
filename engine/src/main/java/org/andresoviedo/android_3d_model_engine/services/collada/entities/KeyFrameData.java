package org.andresoviedo.android_3d_model_engine.services.collada.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键帧数据
 *
 * @author mogoauto
 */
public class KeyFrameData {

    public final float time;
    public final List<JointTransformData> jointTransforms = new ArrayList<>();

    public KeyFrameData(float time) {
        this.time = time;
    }

    public void addJointTransform(JointTransformData transform) {
        jointTransforms.add(transform);
    }

}
