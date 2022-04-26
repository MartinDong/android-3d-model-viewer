package org.andresoviedo.android_3d_model_engine.animation;

import android.opengl.Matrix;

import org.andresoviedo.util.math.Quaternion;

/**
 * Represents the local bone-space transform of a joint at a certain keyframe
 * during an animation. This includes the position and rotation of the joint,
 * relative to the parent joint (for the root joint it's relative to the model's
 * origin, seeing as the root joint has no parent). The transform is stored as a
 * position vector and a quaternion (rotation) so that these values can be
 * easily interpolated, a functionality that this class also provides.
 * <p>
 * 表示动画期间特定关键帧处关节的局部骨骼空间变换。
 * 这包括关节相对于父关节的位置和旋转（对于根关节，它相对于模型的原点，因为根关节没有父关节）。
 * 变换存储为一个位置向量和一个四元数（旋转），因此这些值可以很容易地进行插值，这个类也提供了这一功能。
 *
 * @author Karl
 */

public class JointTransform {

    // remember, this position and rotation are relative to the parent bone!
    // 记住，这个位置和旋转是相对于父骨骼的！
    private final float[] matrix;
    private final float[] position;
    private final Quaternion rotation;

    public JointTransform(float[] matrix) {
        this.matrix = matrix;
        this.position = new float[]{matrix[12], matrix[13], matrix[14]};
        this.rotation = Quaternion.fromMatrix(matrix);
    }

    /**
     * @param position - the position of the joint relative to the parent joint
     *                 (bone-space) at a certain keyframe. For example, if this joint
     *                 is at (5, 12, 0) in the model's coordinate system, and the
     *                 parent of this joint is at (2, 8, 0), then the position of
     *                 this joint relative to the parent is (3, 4, 0).
     *                 关节在特定关键帧处相对于父关节（骨骼空间）的位置。
     *                 例如，如果该关节位于模型坐标系中的（5,12,0），
     *                 且该关节的父关节位于（2,8,0），则该关节相对于父关节的位置为（3,4,0）。
     * @param rotation - the rotation of the joint relative to the parent joint
     *                 (bone-space) at a certain keyframe.
     *                 关节在特定关键帧处相对于父关节（骨骼空间）的旋转。
     */
    public JointTransform(float[] position, Quaternion rotation) {
        this.matrix = null;
        this.position = position;
        this.rotation = rotation;
    }

    public float[] getPosition() {
        return position;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * In this method the bone-space transform matrix is constructed by
     * translating an identity matrix using the position variable and then
     * applying the rotation. The rotation is applied by first converting the
     * quaternion into a rotation matrix, which is then multiplied with the
     * transform matrix.
     * <p>
     * 在该方法中，通过使用位置变量转换单位矩阵，然后应用旋转来构造骨骼空间变换矩阵。
     * 通过首先将四元数转换为旋转矩阵，然后与变换矩阵相乘来应用旋转。
     *
     * @return This bone-space joint transform as a matrix. The exact same
     * transform as represented by the position and rotation in this
     * instance, just in matrix form.
     * 此骨骼空间将关节变换为矩阵。与本例中的位置和旋转表示的完全相同的变换，只是矩阵形式。
     */
    public float[] getLocalTransform() {
        if (matrix != null) {
            return matrix;
        }
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, position[0], position[1], position[2]);
        Matrix.multiplyMM(matrix, 0, matrix, 0, rotation.toRotationMatrix(new float[16]), 0);
        return matrix;
    }

    /**
     * Interpolates between two transforms based on the progression value. The
     * result is a new transform which is part way between the two original
     * transforms. The translation can simply be linearly interpolated, but the
     * rotation interpolation is slightly more complex, using a method called
     * "SLERP" to spherically-linearly interpolate between 2 quaternions
     * (rotations). This gives a much much better result than trying to linearly
     * interpolate between Euler rotations.
     * <p>
     * 基于级数值在两个变换之间插值。结果是一个新的变换，它是两个原始变换之间的一部分。
     * 平移可以简单地进行线性插值，但旋转插值稍微复杂一些，使用一种称为“SLERP”的方法在两个四元数（旋转）之间进行球面线性插值。
     * 这比尝试在Euler旋转之间进行线性插值的结果要好得多。
     *
     * @param frameA      - the previous transform  上一次转变
     * @param frameB      - the next transform      下一个转变
     * @param progression - a number between 0 and 1 indicating how far between the two
     *                    transforms to interpolate. A progression value of 0 would
     *                    return a transform equal to "frameA", a value of 1 would
     *                    return a transform equal to "frameB". Everything else gives a
     *                    transform somewhere in-between the two.
     *                    介于0和1之间的数字，指示要插值的两个变换之间的距离。
     *                    级数值为0将返回等于“frameA”的变换，值为1将返回等于“frameB”的变换。
     *                    其他一切都在两者之间的某个地方产生了变化。
     * @return
     */
    protected static JointTransform interpolate(JointTransform frameA, JointTransform frameB, float progression) {
        float[] pos = interpolate(frameA.position, frameB.position, progression);
        Quaternion rot = Quaternion.interpolate(frameA.rotation, frameB.rotation, progression);
        return new JointTransform(pos, rot);
    }

    protected static float[] interpolate(JointTransform frameA, JointTransform frameB, float progression, float[]
            matrix1, float[] matrix2) {
        float[] pos = interpolate(frameA.position, frameB.position, progression);
        Quaternion rot = Quaternion.interpolate(frameA.rotation, frameB.rotation, progression);
        Matrix.setIdentityM(matrix1, 0);
        Matrix.translateM(matrix1, 0, pos[0], pos[1], pos[2]);
        Matrix.multiplyMM(matrix1, 0, matrix1, 0, rot.toRotationMatrix(matrix2), 0);
        return matrix1;
    }

    /**
     * Linearly interpolates between two translations based on a "progression" value.
     * 基于“级数”值在两个平移之间线性插值。
     *
     * @param start       - the start translation.  开始翻译。
     * @param end         - the end translation.    最后的翻译。
     * @param progression - a value between 0 and 1 indicating how far to interpolate
     *                    between the two translations.
     *                    介于0和1之间的值，指示两个平移之间的插值距离。
     * @return
     */
    private static float[] interpolate(float[] start, float[] end, float progression) {
        float x = start[0] + (end[0] - start[0]) * progression;
        float y = start[1] + (end[1] - start[1]) * progression;
        float z = start[2] + (end[2] - start[2]) * progression;
        // TODO: optimize this (memory allocation)
        // TODO:优化（内存分配）
        return new float[]{x, y, z};
    }

}
