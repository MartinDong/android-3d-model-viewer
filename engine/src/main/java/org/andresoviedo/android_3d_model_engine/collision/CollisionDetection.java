package org.andresoviedo.android_3d_model_engine.collision;

import android.opengl.GLU;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.BoundingBox;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.math.Math3DUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Class that encapsulates all the logic for the collision detection algorithm.
 * 类，该类封装了碰撞检测算法的所有逻辑。
 *
 * @author andresoviedo
 */
public class CollisionDetection {

    /**
     * Get the nearest object intersected by the specified window coordinates
     * 获取与指定窗口坐标相交的最近对象
     *
     * @param objects               the list of objects to test     要测试的对象列表
     * @param height                viewport height                 视口高度
     * @param width                 viewport width                  视口宽度
     * @param modelViewMatrix       model view matrix               模型视图矩阵
     * @param modelProjectionMatrix model projection matrix         模型投影矩阵
     * @param windowX               the window x coordinate         窗口x坐标
     * @param windowY               the window y coordinate         窗口的y坐标
     * @return the nearest object intersected by the specified coordinates or null 与指定坐标或空坐标相交的最近对象
     */
    public static Object3DData getBoxIntersection(List<Object3DData> objects, int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix, float windowX, float windowY) {
        float[] nearHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 0);
        float[] farHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        return getBoxIntersection(objects, nearHit, direction);
    }

    /**
     * Get the nearest object intersected by the specified ray or null if no object is intersected
     * 获取与指定光线相交的最近对象，如果没有对象相交，则获取null
     *
     * @param objects   the list of objects to test 要测试的对象列表
     * @param p1        the ray start point         光线的起点
     * @param direction the ray direction           射线方向
     * @return the object intersected by the specified ray 与指定光线相交的对象
     */
    private static Object3DData getBoxIntersection(List<Object3DData> objects, float[] p1, float[] direction) {
        float min = Float.MAX_VALUE;
        Object3DData ret = null;
        for (Object3DData obj : objects) {
            if ("Point".equals(obj.getId()) || "Line".equals(obj.getId())) {
                continue;
            }
            BoundingBox box = obj.getBoundingBox();
            float[] intersection = getBoxIntersection(p1, direction, box);
            if (intersection[0] > 0 && intersection[0] <= intersection[1] && intersection[0] < min) {
                min = intersection[0];
                ret = obj;
            }
        }
        if (ret != null) {
            Log.i("CollisionDetection", "Collision detected '" + ret.getId() + "' distance: " + min);
        }
        return ret;
    }

    /*
     * Get the entry and exit point of the ray intersecting the nearest object or null if no object is intersected
     * 获取与最近对象相交的光线的入口和出口点，如果没有对象相交，则获取null
     * @param objects list of objects to test
     * @param p1      ray start point
     * @param p2      ray end point
     * @return the entry and exit point of the ray intersecting the nearest object
     */
    /*public static float[] getBoxIntersectionPoint(List<Object3DData> objects, float[] p1, float[] p2) {
        float[] direction = Math3DUtils.substract(p2, p1);
        Math3DUtils.normalize(direction);
        float min = Float.MAX_VALUE;
        float[] intersection2 = null;
        Object3DData ret = null;
        for (Object3DData obj : objects) {
            BoundingBoxBuilder box = obj.getBoundingBox();
            float[] intersection = getBoxIntersection(p1, direction, box);
            if (intersection[0] > 0 && intersection[0] <= intersection[1] && intersection[0] < min) {
                min = intersection[0];
                ret = obj;
                intersection2 = intersection;
            }
        }
        if (ret != null) {
            Log.i("CollisionDetection", "Collision detected '" + ret.getId() + "' distance: " + min);
            return new float[]{p1[0] + direction[0] * min, p1[1] + direction[1] * min, p1[2] + direction[2] * min};
        }
        return null;
    }*/

    /**
     * Return true if the specified ray intersects the bounding box
     * 如果指定的光线与边界框相交，则返回true
     *
     * @param origin origin of the ray      射线的起源
     * @param dir    direction of the ray   射线的方向
     * @param b      bounding box           包围盒
     * @return true if the specified ray intersects the bounding box, false otherwise 如果指定的光线与边界框相交，则为true，否则为false
     */
    private static boolean isBoxIntersection(float[] origin, float[] dir, BoundingBox b) {
        float[] intersection = getBoxIntersection(origin, dir, b);
        return intersection[0] > 0 && intersection[0] < intersection[1];
    }

    /**
     * Get the intersection points of the near and far plane for the specified ray and bounding box
     * 获取指定光线和边界框的近平面和远平面的交点
     *
     * @param origin the ray origin         射线起源
     * @param dir    the ray direction      射线方向
     * @param b      the bounding box       边界框
     * @return the intersection points of the near and far plane 近平面和远平面的交点
     */
    private static float[] getBoxIntersection(float[] origin, float[] dir, BoundingBox b) {
        float[] tMin = Math3DUtils.divide(Math3DUtils.substract(b.getMin(), origin), dir);
        float[] tMax = Math3DUtils.divide(Math3DUtils.substract(b.getMax(), origin), dir);
        float[] t1 = Math3DUtils.min(tMin, tMax);
        float[] t2 = Math3DUtils.max(tMin, tMax);
        float tNear = Math.max(Math.max(t1[0], t1[1]), t1[2]);
        float tFar = Math.min(Math.min(t2[0], t2[1]), t2[2]);
        return new float[]{tNear, tFar};
    }

    /**
     * Map window coordinates to object coordinates.
     * 将窗口坐标映射到对象坐标。
     *
     * @param height                viewport height         视口高度
     * @param width                 viewport width          视口宽度
     * @param modelViewMatrix       model view matrix       模型视图矩阵
     * @param modelProjectionMatrix model projection matrix 模型投影矩阵
     * @param rx                    x point                 x点
     * @param ry                    y point                 y点
     * @param rz                    z point                 z点
     * @return the corresponding near and far vertex for the specified window coordinates 指定窗口坐标对应的近顶点和远顶点
     */
    private static float[] unProject(int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix,
                                     float rx, float ry, float rz) {
        float[] xyzw = {0, 0, 0, 0};
        ry = (float) height - ry;
        int[] viewport = {0, 0, width, height};
        GLU.gluUnProject(rx, ry, rz, modelViewMatrix, 0, modelProjectionMatrix, 0,
                viewport, 0, xyzw, 0);
        xyzw[0] /= xyzw[3];
        xyzw[1] /= xyzw[3];
        xyzw[2] /= xyzw[3];
        xyzw[3] = 1;
        return xyzw;
    }

    /*public static float[] getTriangleIntersection(List<Object3DData> objects, ModelRenderer mRenderer, float
            windowX, float windowY) {
        float[] nearHit = unProject(mRenderer, windowX, windowY, 0);
        float[] farHit = unProject(mRenderer, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        Object3DData intersected = getBoxIntersection(objects, nearHit, direction);
        if (intersected != null) {
            Log.d("CollisionDetection", "intersected:" + intersected.getId() + ", rayOrigin:" + Arrays.toString(nearHit) + ", rayVector:" + Arrays.toString(direction));
            FloatBuffer buffer = intersected.getVertexArrayBuffer().asReadOnlyBuffer();
            float[] modelMatrix = intersected.getModelMatrix();
            buffer.position(0);
            float[] selectedv1 = null;
            float[] selectedv2 = null;
            float[] selectedv3 = null;
            float min = Float.MAX_VALUE;
            for (int i = 0; i < buffer.capacity(); i += 9) {
                float[] v1 = new float[]{buffer.get(), buffer.get(), buffer.get(), 1};
                float[] v2 = new float[]{buffer.get(), buffer.get(), buffer.get(), 1};
                float[] v3 = new float[]{buffer.get(), buffer.get(), buffer.get(), 1};
                Matrix.multiplyMV(v1, 0, modelMatrix, 0, v1, 0);
                Matrix.multiplyMV(v2, 0, modelMatrix, 0, v2, 0);
                Matrix.multiplyMV(v3, 0, modelMatrix, 0, v3, 0);
                float t = getTriangleIntersection(nearHit, direction, v1, v2, v3);
                if (t != -1 && t < min) {
                    min = t;
                    selectedv1 = v1;
                    selectedv2 = v2;
                    selectedv3 = v3;
                }
            }
            if (selectedv1 != null) {
                float[] outIntersectionPoint = Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, min));
                return outIntersectionPoint;
            }
        }
        return null;
    }*/

    /**
     * 得到三角形交点
     *
     * @param objects
     * @param width
     * @param height
     * @param modelViewMatrix
     * @param modelProjectionMatrix
     * @param windowX
     * @param windowY
     * @return
     */
    public static float[] getTriangleIntersection(List<Object3DData> objects, int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix, float windowX, float windowY) {
        float[] nearHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 0);
        float[] farHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        Object3DData intersected = getBoxIntersection(objects, nearHit, direction);
        if (intersected != null) {
            Log.d("CollisionDetection", "intersected: " + intersected.getId());
            Octree octree;
            synchronized (intersected) {
                octree = intersected.getOctree();
                if (octree == null) {
                    octree = Octree.build(intersected);
                    intersected.setOctree(octree);
                }
            }
            float intersection = getTriangleIntersectionForOctree(octree, nearHit, direction);
            if (intersection != -1) {
                float[] intersectionPoint = Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection));
                Log.d("CollisionDetection", "Interaction point: " + Arrays.toString(intersectionPoint));
                return intersectionPoint;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * 求八叉树的三角形交点
     *
     * @param octree
     * @param rayOrigin
     * @param rayDirection
     * @return
     */
    private static float getTriangleIntersectionForOctree(Octree octree, float[] rayOrigin, float[] rayDirection) {
        //Log.v("CollisionDetection","Testing octree "+octree);
        if (!isBoxIntersection(rayOrigin, rayDirection, octree.boundingBox)) {
            Log.d("CollisionDetection", "No octree intersection");
            return -1;
        }
        Octree selected = null;
        float min = Float.MAX_VALUE;
        for (Octree child : octree.getChildren()) {
            if (child == null) {
                continue;
            }
            float intersection = getTriangleIntersectionForOctree(child, rayOrigin, rayDirection);
            if (intersection != -1 && intersection < min) {
                Log.d("CollisionDetection", "Octree intersection: " + intersection);
                min = intersection;
                selected = child;
            }
        }
        float[] selectedTriangle = null;
        for (float[] triangle : octree.getTriangles()) {
            float[] vertex0 = new float[]{triangle[0], triangle[1], triangle[2]};
            float[] vertex1 = new float[]{triangle[4], triangle[5], triangle[6]};
            float[] vertex2 = new float[]{triangle[8], triangle[9], triangle[10]};
            float intersection = getTriangleIntersection(rayOrigin, rayDirection, vertex0, vertex1, vertex2);
            if (intersection != -1 && intersection < min) {
                min = intersection;
                selectedTriangle = triangle;
                selected = octree;

            }
        }
        if (min != Float.MAX_VALUE) {
            Log.d("CollisionDetection", "Intersection at distance: " + min);
            Log.d("CollisionDetection", "Intersection at triangle: " + Arrays.toString(selectedTriangle));
            Log.d("CollisionDetection", "Intersection at octree: " + selected);
            return min;
        }
        return -1;
    }

    /**
     * 得到三角形交点
     *
     * @param rayOrigin
     * @param rayVector
     * @param vertex0
     * @param vertex1
     * @param vertex2
     * @return
     */
    private static float getTriangleIntersection(float[] rayOrigin,
                                                 float[] rayVector,
                                                 float[] vertex0, float[] vertex1, float[] vertex2) {
        float EPSILON = 0.0000001f;
        float[] edge1, edge2, h, s, q;
        float a, f, u, v;
        edge1 = Math3DUtils.substract(vertex1, vertex0);
        edge2 = Math3DUtils.substract(vertex2, vertex0);
        h = Math3DUtils.crossProduct(rayVector, edge2);
        a = Math3DUtils.dotProduct(edge1, h);
        if (a > -EPSILON && a < EPSILON) {
            return -1;
        }
        f = 1 / a;
        s = Math3DUtils.substract(rayOrigin, vertex0);
        u = f * Math3DUtils.dotProduct(s, h);
        if (u < 0.0 || u > 1.0) {
            return -1;
        }
        q = Math3DUtils.crossProduct(s, edge1);
        v = f * Math3DUtils.dotProduct(rayVector, q);
        if (v < 0.0 || u + v > 1.0) {
            return -1;
        }
        // At this stage we can compute t to find out where the intersection point is on the line.
        //在这个阶段，我们可以计算t来找出交点在这条线上的位置。
        float t = f * Math3DUtils.dotProduct(edge2, q);
        if (t > EPSILON)
        // ray intersection
        //射线交叉
        {
            Log.d("CollisionDetection", "Triangle intersection at: " + t);
            return t;
        } else
        // This means that there is a line intersection but not a ray intersection.
        //这意味着存在直线交点，但不存在光线交点。
        {
            return -1;
        }
    }
}

