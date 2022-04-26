package org.andresoviedo.app.model3D.demo;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.collision.CollisionDetection;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.Object3DBuilder;
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.gltf.GltfLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.andresoviedo.app.model3D.view.ModelActivity;
import org.andresoviedo.app.model3D.view.ModelRenderer;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class loads a 3D scena as an example of what can be done with the app
 * 这个类加载一个3D场景作为应用程序的示例
 *
 * @author andresoviedo
 */
public class SceneLoader implements LoaderTask.Callback {

    /**
     * Default model color: yellow
     * 默认模型颜色：黄色
     */
    private static float[] DEFAULT_COLOR = {1.0f, 1.0f, 0, 1.0f};
    /**
     * Parent component
     * 父组件
     */
    protected final ModelActivity parent;
    /**
     * List of data objects containing info for building the opengl objects
     * 包含用于构建opengl对象的信息的数据对象列表
     */
    private List<Object3DData> objects = new ArrayList<>();
    /**
     * Show axis or not
     * 是否显示轴
     */
    private boolean drawAxis = false;
    /**
     * Point of view camera
     * 视角摄像机
     */
    private Camera camera;
    /**
     * Enable or disable blending (transparency)
     * 启用或禁用混合（透明度）
     */
    private boolean isBlendingEnabled = true;
    /**
     * Whether to draw objects as wireframes
     * 是否将对象绘制为线框
     */
    private boolean drawWireframe = false;
    /**
     * Whether to draw using points
     * 是否使用点绘制
     */
    private boolean drawingPoints = false;
    /**
     * Whether to draw bounding boxes around objects
     * 是否在对象周围绘制边界框
     */
    private boolean drawBoundingBox = false;
    /**
     * Whether to draw face normals. Normally used to debug models
     * 是否绘制面法线。通常用于调试模型
     */
    private boolean drawNormals = false;
    /**
     * Whether to draw using textures
     * 是否使用纹理绘制
     */
    private boolean drawTextures = true;
    /**
     * Whether to draw using colors or use default white color
     * 是使用颜色还是使用默认白色绘制
     */
    private boolean drawColors = true;
    /**
     * Light toggle feature: we have 3 states: no light, light, light + rotation
     * 灯光切换功能：我们有三种状态：无灯光、灯光、灯光+旋转
     */
    private boolean rotatingLight = true;
    /**
     * Light toggle feature: whether to draw using lights
     * 灯光切换功能：是否使用灯光绘制
     */
    private boolean drawLighting = true;
    /**
     * Animate model (dae only) or not
     * 是否设置模型动画（仅限dae）
     */
    private boolean doAnimation = true;
    /**
     * show bind pose only
     * 仅显示绑定姿势
     */
    private boolean showBindPose = false;
    /**
     * Draw skeleton or not
     * 画不画骨架
     */
    private boolean drawSkeleton = false;
    /**
     * Toggle collision detection
     * 切换碰撞检测
     */
    private boolean isCollision = false;
    /**
     * Toggle 3d
     * 切换3d
     */
    private boolean isStereoscopic = false;
    /**
     * Toggle 3d anaglyph (red, blue glasses)
     * 切换3d浮雕（红色、蓝色眼镜）
     */
    private boolean isAnaglyph = false;
    /**
     * Toggle 3d VR glasses
     * 切换3d虚拟现实眼镜
     */
    private boolean isVRGlasses = false;
    /**
     * Object selected by the user
     * 切换3d虚拟现实眼镜
     */
    private Object3DData selectedObject = null;
    /**
     * Initial light position
     * 初始灯光位置
     */
    private final float[] lightPosition = new float[]{0, 0, 6, 1};
    /**
     * Light bulb 3d data
     * 灯泡3d数据
     */
    private final Object3DData lightPoint = Object3DBuilder.buildPoint(lightPosition).setId("light");
    /**
     * Animator
     * 动画师
     */
    private Animator animator = new Animator();
    /**
     * Did the user touched the model for the first time?
     * 用户是第一次触摸模型吗？
     */
    private boolean userHasInteracted;
    /**
     * time when model loading has started (for stats)
     * 开始加载模型的时间（用于统计）
     */
    private long startTime;

    public SceneLoader(ModelActivity main) {
        this.parent = main;
    }

    public void init() {
        // Camera to show a point of view
        // 显示视角的摄像机
        camera = new Camera();
        // force first draw
        // 强制先绘制
        camera.setChanged(true);

        if (parent.getParamUri() == null) {
            return;
        }

        startTime = SystemClock.uptimeMillis();
        Uri uri = parent.getParamUri();
        // 根据不同的文件后缀，使用不容的3D模型加载器
        Log.i("Object3DBuilder", "Loading model " + uri + ". async and parallel..");
        if (uri.toString().toLowerCase().endsWith(".obj") || parent.getParamType() == 0) {
            new WavefrontLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".stl") || parent.getParamType() == 1) {
            Log.i("Object3DBuilder", "Loading STL object from: " + uri);
            new STLLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".dae") || parent.getParamType() == 2) {
            Log.i("Object3DBuilder", "Loading Collada object from: " + uri);
            new ColladaLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".gltf") || parent.getParamType() == 3) {
            Log.i("Object3DBuilder", "Loading GLtf object from: " + uri);
            new GltfLoaderTask(parent, uri, this).execute();
        }
    }

    public boolean isDrawAxis() {
        return drawAxis;
    }

    public void setDrawAxis(boolean drawAxis) {
        this.drawAxis = drawAxis;
    }

    public Camera getCamera() {
        return camera;
    }

    private void makeToastText(final String text, final int toastDuration) {
        parent.runOnUiThread(() -> Toast.makeText(parent.getApplicationContext(), text, toastDuration).show());
    }

    public Object3DData getLightBulb() {
        return lightPoint;
    }

    public float[] getLightPosition() {
        return lightPosition;
    }

    /**
     * Hook for animating the objects before the rendering
     * 用于在渲染之前设置对象动画的挂钩
     */
    public void onDrawFrame() {

        animateLight();

        // smooth camera transition
        //平滑相机过渡
        camera.animate();

        // initial camera animation. animate if user didn't touch the screen
        // 初始相机动画。如果用户未触摸屏幕，则设置动画
        if (!userHasInteracted) {
            animateCamera();
        }

        if (objects.isEmpty()) {
            return;
        }

        if (doAnimation) {
            for (int i = 0; i < objects.size(); i++) {
                Object3DData obj = objects.get(i);
                animator.update(obj, isShowBindPose());
            }
        }
    }

    private void animateLight() {
        if (!rotatingLight) {
            return;
        }

        // animate light - Do a complete rotation every 5 seconds.
        // 设置灯光动画-每5秒做一次完整的旋转。
        long time = SystemClock.uptimeMillis() % 5000L;
        float angleInDegrees = (360.0f / 5000.0f) * ((int) time);
        lightPoint.setRotationY(angleInDegrees);
    }

    private void animateCamera() {
        camera.translateCamera(0.0025f, 0f);
    }

    synchronized void addObject(Object3DData obj) {
        List<Object3DData> newList = new ArrayList<Object3DData>(objects);
        newList.add(obj);
        this.objects = newList;
        requestRender();
    }

    private void requestRender() {
        // request render only if GL view is already initialized
        //仅当总账视图已初始化时请求渲染
        if (parent.getGLView() != null) {
            parent.getGLView().requestRender();
        }
    }

    public synchronized List<Object3DData> getObjects() {
        return objects;
    }

    public void toggleWireframe() {
        if (!this.drawWireframe && !this.drawingPoints && !this.drawSkeleton) {
            this.drawWireframe = true;
            makeToastText("Wireframe", Toast.LENGTH_SHORT);
        } else if (!this.drawingPoints && !this.drawSkeleton) {
            this.drawWireframe = false;
            this.drawingPoints = true;
            makeToastText("Points", Toast.LENGTH_SHORT);
        } else if (!this.drawSkeleton) {
            this.drawingPoints = false;
            this.drawSkeleton = true;
            makeToastText("Skeleton", Toast.LENGTH_SHORT);
        } else {
            this.drawSkeleton = false;
            makeToastText("Faces", Toast.LENGTH_SHORT);
        }
        requestRender();
    }

    public boolean isDrawWireframe() {
        return this.drawWireframe;
    }

    public boolean isDrawPoints() {
        return this.drawingPoints;
    }

    public void toggleBoundingBox() {
        this.drawBoundingBox = !drawBoundingBox;
        requestRender();
    }

    public boolean isDrawBoundingBox() {
        return drawBoundingBox;
    }

    public boolean isDrawNormals() {
        return drawNormals;
    }

    public void toggleTextures() {
        if (drawTextures && drawColors) {
            this.drawTextures = false;
            this.drawColors = true;
            makeToastText("Texture off", Toast.LENGTH_SHORT);
        } else if (drawColors) {
            this.drawTextures = false;
            this.drawColors = false;
            makeToastText("Colors off", Toast.LENGTH_SHORT);
        } else {
            this.drawTextures = true;
            this.drawColors = true;
            makeToastText("Textures on", Toast.LENGTH_SHORT);
        }
    }

    public void toggleLighting() {
        if (this.drawLighting && this.rotatingLight) {
            this.rotatingLight = false;
            makeToastText("Light stopped", Toast.LENGTH_SHORT);
        } else if (this.drawLighting && !this.rotatingLight) {
            this.drawLighting = false;
            makeToastText("Lights off", Toast.LENGTH_SHORT);
        } else {
            this.drawLighting = true;
            this.rotatingLight = true;
            makeToastText("Light on", Toast.LENGTH_SHORT);
        }
        requestRender();
    }

    public void toggleAnimation() {
        if (!this.doAnimation && !this.showBindPose) {
            this.doAnimation = true;
            makeToastText("Animation on", Toast.LENGTH_SHORT);
        } else if (!this.showBindPose) {
            this.doAnimation = true;
            this.showBindPose = true;
            makeToastText("Bind pose", Toast.LENGTH_SHORT);
        } else {
            this.doAnimation = false;
            this.showBindPose = false;
            makeToastText("Animation off", Toast.LENGTH_SHORT);
        }
    }

    public boolean isDoAnimation() {
        return doAnimation;
    }

    public boolean isShowBindPose() {
        return showBindPose;
    }

    public void toggleCollision() {
        this.isCollision = !isCollision;
        makeToastText("Collisions: " + isCollision, Toast.LENGTH_SHORT);
    }

    public void toggleStereoscopic() {
        if (!this.isStereoscopic) {
            this.isStereoscopic = true;
            this.isAnaglyph = true;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic Anaplygh", Toast.LENGTH_SHORT);
        } else if (this.isAnaglyph) {
            this.isAnaglyph = false;
            this.isVRGlasses = true;
            // move object automatically cause with VR glasses we still have no way of moving object
            //自动移动物体，因为使用虚拟现实眼镜，我们仍然无法移动物体
            this.userHasInteracted = false;
            makeToastText("Stereoscopic VR Glasses", Toast.LENGTH_SHORT);
        } else {
            this.isStereoscopic = false;
            this.isAnaglyph = false;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic disabled", Toast.LENGTH_SHORT);
        }
        // recalculate camera
        //重新计算相机
        this.camera.setChanged(true);
    }

    public boolean isVRGlasses() {
        return isVRGlasses;
    }

    public boolean isDrawTextures() {
        return drawTextures;
    }

    public boolean isDrawColors() {
        return drawColors;
    }

    public boolean isDrawLighting() {
        return drawLighting;
    }

    public boolean isDrawSkeleton() {
        return drawSkeleton;
    }

    public boolean isCollision() {
        return isCollision;
    }

    public boolean isStereoscopic() {
        return isStereoscopic;
    }

    public boolean isAnaglyph() {
        return isAnaglyph;
    }

    public void toggleBlending() {
        this.isBlendingEnabled = !isBlendingEnabled;
        makeToastText("Blending " + isBlendingEnabled, Toast.LENGTH_SHORT);
    }

    public boolean isBlendingEnabled() {
        return isBlendingEnabled;
    }

    @Override
    public void onStart() {
        ContentUtils.setThreadActivity(parent);
    }

    @Override
    public void onLoadComplete(List<Object3DData> datas) {
        // TODO: move texture load to LoaderTask
        //TODO:将纹理加载移动到LoaderTask
        for (Object3DData data : datas) {
            if (data.getTextureData() == null && data.getTextureFile() != null) {
                Log.i("LoaderTask", "Loading texture... " + data.getTextureFile());
                try (InputStream stream = ContentUtils.getInputStream(data.getTextureFile())) {
                    if (stream != null) {
                        data.setTextureData(IOUtils.read(stream));
                    }
                } catch (IOException ex) {
                    data.addError("Problem loading texture " + data.getTextureFile());
                }
            }
        }

        // TODO: move error alert to LoaderTask
        // TODO: 将错误警报移至LoaderTask
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : datas) {
            addObject(data);
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Build complete (" + elapsed + ")", Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e("SceneLoader", ex.getMessage(), ex);
        makeToastText("There was a problem building the model: " + ex.getMessage(), Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    private void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
    }

    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Unavailable", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));
        this.drawTextures = true;
    }

    public void processTouch(float x, float y) {
        ModelRenderer mr = parent.getGLView().getModelRenderer();
        Object3DData objectToSelect = CollisionDetection.getBoxIntersection(getObjects(), mr.getWidth(), mr.getHeight
                (), mr.getModelViewMatrix(), mr.getModelProjectionMatrix(), x, y);
        if (objectToSelect != null) {
            if (getSelectedObject() == objectToSelect) {
                Log.i("SceneLoader", "Unselected object " + objectToSelect.getId());
                setSelectedObject(null);
            } else {
                Log.i("SceneLoader", "Selected object " + objectToSelect.getId());
                setSelectedObject(objectToSelect);
            }
            if (isCollision()) {
                Log.d("SceneLoader", "Detecting collision...");

                float[] point = CollisionDetection.getTriangleIntersection(getObjects(), mr.getWidth(), mr.getHeight
                        (), mr.getModelViewMatrix(), mr.getModelProjectionMatrix(), x, y);
                if (point != null) {
                    Log.i("SceneLoader", "Drawing intersection point: " + Arrays.toString(point));
                    addObject(Object3DBuilder.buildPoint(point).setColor(new float[]{1.0f, 0f, 0f, 1f}));
                }
            }
        }
    }

    public void processMove(float dx1, float dy1) {
        userHasInteracted = true;
    }
}
