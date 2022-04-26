package org.andresoviedo.android_3d_model_engine.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;

import java.util.List;

/**
 * This component allows loading the model without blocking the UI.
 * 该组件允许在不阻塞UI的情况下加载模型。
 *
 * @author andresoviedo
 */
public abstract class LoaderTask extends AsyncTask<Void, Integer, List<Object3DData>> {

    /**
     * URL to the 3D model
     * 3D模型URL
     */
    protected final Uri uri;
    /**
     * Callback to notify of events
     * 回调以通知事件
     */
    private final Callback callback;
    /**
     * The dialog that will show the progress of the loading
     * 显示加载进度的对话框
     */
    private final ProgressDialog dialog;

    /**
     * Build a new progress dialog for loading the data model asynchronously
     * 构建一个新的进度对话框，用于异步加载数据模型
     *
     * @param uri the URL pointing to the 3d model 3D模型URL
     */
    public LoaderTask(Activity parent, Uri uri, Callback callback) {
        this.uri = uri;
        // this.dialog = ProgressDialog.show(this.parent, "Please wait ...", "Loading model data...", true);
        // this.dialog.setTitle(modelId);
        this.dialog = new ProgressDialog(parent);
        this.callback = callback;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.dialog.setMessage("加载中...");
        this.dialog.setCancelable(false);
        this.dialog.show();
    }


    @Override
    protected List<Object3DData> doInBackground(Void... params) {
        try {
            callback.onStart();
            List<Object3DData> data = build();
            build(data);
            callback.onLoadComplete(data);
            return data;
        } catch (Exception ex) {
            callback.onLoadError(ex);
            return null;
        }
    }

    protected abstract List<Object3DData> build() throws Exception;

    protected abstract void build(List<Object3DData> data) throws Exception;

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        switch (values[0]) {
            case 0:
                this.dialog.setMessage("分析模型……");
                break;
            case 1:
                this.dialog.setMessage("分配内存……");
                break;
            case 2:
                this.dialog.setMessage("加载数据…");
                break;
            case 3:
                this.dialog.setMessage("缩放对象……");
                break;
            case 4:
                this.dialog.setMessage("建筑三维模型……");
                break;
            case 5:
                // Toast.makeText(parent, modelId + " Build!", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onPostExecute(List<Object3DData> data) {
        super.onPostExecute(data);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }


    public interface Callback {

        void onStart();

        void onLoadError(Exception ex);

        void onLoadComplete(List<Object3DData> data);
    }
}