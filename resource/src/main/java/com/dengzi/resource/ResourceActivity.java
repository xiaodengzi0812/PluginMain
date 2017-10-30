package com.dengzi.resource;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dengzi.resource.util.SkinResource;

import java.io.File;

public class ResourceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //  1、获取外部apk（也就是它自己的apk放到内存卡），然后加载外部apk中的资源
        // 因为这个插件资源apk加载到别的apk中，它里面的资源加载就相当于加载外部apk的一个资源了
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "resource.apk";
        SkinResource skinResource = new SkinResource(this, path);
        // 2、根据本类的名字来加载外部apk的布局文件
        XmlResourceParser layout = skinResource.getLayout("activity_resource");
        View view = LayoutInflater.from(this).inflate(layout, (ViewGroup) getWindow().getDecorView(), false);
        super.onCreate(savedInstanceState);
        // 这里设置的就是加载外部资源的一个布局，这样操作之的再加载到插件主程序中才不会崩溃
        setContentView(view);
        initData();
    }

    private void initData() {
        // 测试获取插件主程序传来的参数值
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        if (!TextUtils.isEmpty(name)) {
            Toast.makeText(this, "插件传参=" + name, Toast.LENGTH_SHORT).show();
        }
    }
}
