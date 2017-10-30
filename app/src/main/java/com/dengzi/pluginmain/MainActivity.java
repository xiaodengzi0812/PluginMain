package com.dengzi.pluginmain;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.dengzi.pluginmain.util.HookStartActivityUtil;
import com.dengzi.pluginmain.util.hotfix.HotFixManager;

import java.io.File;

/**
 * @Title: 插件化开发的主程序，用此主程序来加载外部的插件apk
 * @Author: djk
 * @Time: 2017/10/30
 * @Version:1.0.0
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 加载外部插件的apk文件，运用热修复
     *
     * @param view
     */
    public void loadPluginApk(View view) {
        try {
            // 外部apk的路径
            String akpPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "resource.apk";
            // 热修复技术，将插件apk资源插入到系统资源dexlist前面
            HotFixManager hotFixManager = new HotFixManager(this);
            hotFixManager.addDex(akpPath);
            Toast.makeText(this, "插件Apk加载成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
        }
    }

    /**
     * 启动一个插件apk，运用绕过manifests检测，来启动一个activity
     *
     * @param view
     */
    public void startPluginApk(View view) {
        try {
            // 加载一个插件资源的class
            Class<?> targetClass = Class.forName("com.dengzi.resource.ResourceActivity");
            // 启动一个插件
            Intent intent = new Intent(MainActivity.this, targetClass);
            intent.putExtra("name", "Dengzi");
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}

