package org.andresoviedo.android_3d_model_engine.services.stl;

/**
 * 国际化管理
 *
 * @author andres
 * @date 17/04/17
 */
public class I18nManager {

    private static final I18nManager manager = new I18nManager();

    public static I18nManager getManager() {
        return manager;
    }

    public String getString(String unknownKeywordMsgProp) {
        return unknownKeywordMsgProp;
    }
}
