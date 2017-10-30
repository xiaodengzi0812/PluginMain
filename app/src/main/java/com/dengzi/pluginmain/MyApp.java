package com.dengzi.pluginmain;

import android.app.Application;

import com.dengzi.pluginmain.util.HookStartActivityUtil;

/**
 * @author Djk
 * @Title:
 * @Time: 2017/10/30.
 * @Version:1.0.0
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 绕过manifests检测
        new HookStartActivityUtil(this, ProxyActivity.class).execute();
    }
}
