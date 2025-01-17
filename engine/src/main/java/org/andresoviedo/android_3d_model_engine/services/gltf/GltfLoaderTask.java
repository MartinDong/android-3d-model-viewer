package org.andresoviedo.android_3d_model_engine.services.gltf;

import android.app.Activity;
import android.net.Uri;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.GltfModel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * gltf 装载器
 * gltf 就是一种规范的数据格式，也就是说不管什么样的数据格式，后缀名，
 * 都可以转换成gltf这种格式来进行加载。gltf立志成为一种通用格式。抹平各个格式之间的差异。
 * https://blog.csdn.net/lz5211314121/article/details/117959088
 *
 * @author mogoauto
 */
public class GltfLoaderTask extends LoaderTask {

    GltfModel modelData;

    public GltfLoaderTask(Activity parent, Uri uri, Callback callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws IOException, URISyntaxException {

        Object[] ret = GltfLoader.buildAnimatedModel(new URI(uri.toString()));
        List<Object3DData> datas = (List<Object3DData>) ret[1];
        modelData = (GltfModel) ret[0];

        return datas;
    }

    @Override
    protected void build(List<Object3DData> datas) throws Exception {
        GltfLoader.populateAnimatedModel(new URL(uri.toString()), datas, modelData);
        if (datas.size() == 1) {
            datas.get(0).centerAndScale(5, new float[]{0, 0, 0});
        } else {
            Object3DData.centerAndScale(datas, 5, new float[]{0, 0, 0});
        }
    }

}
