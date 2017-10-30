package com.dengzi.pluginmain.util.hotfix;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.BaseDexClassLoader;

/**
 * @author Djk
 * @Title: 热修复工具类
 * @Time: 2017/8/3.
 * @Version:1.0.0
 */
public class HotFixManager {
    private Context mContext;
    // 安卓系统存放dex文件目录
    private File sysDexFile;

    public HotFixManager(Context mContext) {
        this.mContext = mContext;
        /*获取系统存放有dex文件的目录*/
        sysDexFile = mContext.getDir("odex", Context.MODE_PRIVATE);
    }

    /**
     * 加载修复包dex文件
     *
     * @param fixDexPath 修复包文件路径
     */
    public void addDex(String fixDexPath) throws Exception {
        /*加载本地修复dex文件*/
        File fixDexFile = new File(fixDexPath);
        if (!fixDexFile.exists()) return;// 如果修复包不存在，则return
        /*定义复制文件的目录*/
        File targetDexFile = new File(sysDexFile, fixDexFile.getName());
//        if (targetDexFile.exists()) return;// 如果系统目录下已有对应的修复包，则return
        if (targetDexFile.exists()) {
            targetDexFile.delete();
        }
        /*将本地dex文件复制到系统目录下*/
        FileUtil.copyFile(fixDexFile, targetDexFile);
        /*将我们复制的文件添加到一个list中，为了与启动app时加载所有修复包兼容使用同一个方法*/
        List<File> hotFixDexList = new ArrayList<>();
        hotFixDexList.add(targetDexFile);
        loadDex(hotFixDexList);
    }

    /**
     * 加载所有修复包dex文件
     */
    public void loadDex() throws Exception {
        /*获取系统目录中已有的dex文件*/
        File[] dexFiles = sysDexFile.listFiles();
        List<File> hotFixDexList = new ArrayList<>();
        for (File dexFile : dexFiles) {
            if (dexFile.getName().endsWith(".dex") || dexFile.getName().endsWith(".apk")) {
                hotFixDexList.add(dexFile);
            }
        }
        loadDex(hotFixDexList);
    }

    /**
     * 加载修复包
     *
     * @param hotFixDexList
     */
    private void loadDex(List<File> hotFixDexList) throws Exception {
        ClassLoader sysClassLoader = mContext.getClassLoader();
        /*1、拿系统中的dexElements数组*/
        Object sysDexElements = getDexElementsFromClassLoader(sysClassLoader);

        /*2、拿我们要修复的dex文件中的dexElements数组*/
        /*new BaseDexClassLoader() 时要用的解压文件目录参数*/
        File optimizedDirectory = new File(sysDexFile, "dz_dex");
        /*目录不存在，则创建*/
        if (!optimizedDirectory.exists()) {
            optimizedDirectory.mkdirs();
        }
        for (File hotFixDexFile : hotFixDexList) {
            /*用我们自己的dex 创建一个我们自己的classLoader*/
            ClassLoader mClassLoader = new BaseDexClassLoader(
                    hotFixDexFile.getAbsolutePath(),
                    optimizedDirectory,
                    null,
                    sysClassLoader
            );
            /*从我们自己的classLoader中获取dexElements数组*/
            Object mDexElements = getDexElementsFromClassLoader(mClassLoader);
            /*把我们自己的dexElements数组插入到系统的dexElements数组前面*/
            sysDexElements = FileUtil.combineArray(mDexElements, sysDexElements);
        }
        /*3、把新的dexElement插到已运行的最前面*/
        injectDexElements(sysClassLoader, sysDexElements);
    }

    /**
     * 把新的dexElement插到已运行的最前面
     *
     * @param mClassLoader
     * @param sysDexElements
     */
    private void injectDexElements(ClassLoader mClassLoader, Object sysDexElements) throws Exception {
        Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(mClassLoader);

        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        dexElementsField.set(pathList, sysDexElements);
    }

    /**
     * 从classLoader中获取系统的dexElements
     *
     * @param mClassLoader
     */
    private Object getDexElementsFromClassLoader(ClassLoader mClassLoader) throws Exception {
        Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(mClassLoader);

        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        Object dexElements = dexElementsField.get(pathList);
        return dexElements;
    }

}
