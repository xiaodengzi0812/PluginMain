package com.dengzi.pluginmain;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * @Title: 启动代理Activity，用此Activity来代替真正的Activity去绕过manifests的检测
 * @Author: djk
 * @Time: 2017/10/30
 * @Version:1.0.0
 */
public class ProxyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}

