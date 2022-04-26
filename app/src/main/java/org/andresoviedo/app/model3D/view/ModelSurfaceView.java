package org.andresoviedo.app.model3D.view;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import org.andresoviedo.app.model3D.controller.TouchController;

import java.io.IOException;

/**
 * This is the actual opengl view. From here we can detect touch gestures for example
 * 这是实际的opengl视图。例如，从这里我们可以检测触摸手势
 *
 * @author andresoviedo
 */
public class ModelSurfaceView extends GLSurfaceView {

    private ModelActivity parent;
    private ModelRenderer mRenderer;
    private TouchController touchHandler;

    public ModelSurfaceView(ModelActivity parent) throws IllegalAccessException, IOException {
        super(parent);

        // parent component
        //父组件
        this.parent = parent;

        // Create an OpenGL ES 2.0 context.
        //创建OpenGL ES 3.0上下文。
        setEGLContextClientVersion(3);

        // This is the actual renderer of the 3D space
        // 这是3D空间的实际渲染器
        mRenderer = new ModelRenderer(this);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        // 仅当图形数据发生更改时渲染视图
        // TODO: enable this?
        // setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        touchHandler = new TouchController(this, mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchHandler.onTouchEvent(event);
    }

    public ModelActivity getModelActivity() {
        return parent;
    }

    public ModelRenderer getModelRenderer() {
        return mRenderer;
    }

}