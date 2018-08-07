package com.dengzi.pluginmain.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * @author Djk
 * @Title: 开启一个没有在manifests中配置的activity
 * @Time: 2017/10/26.
 * @Version:1.0.0
 */
public class HookStartActivityUtil {
    private Context mContext;
    private Class<?> mProxyClass;
    private String ORIGIN_INTENT = "ORIGIN_INTENT";

    public HookStartActivityUtil(Context context, Class<?> proxyClass) {
        this.mContext = context.getApplicationContext();
        this.mProxyClass = proxyClass;
    }

    /**
     * 开始执行绕过manifests的配置检测
     */
    public void execute() {
        try {
            hookStartActivity();
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 动态代理hook启动activity，用来绕过manifests的配置检测
     *
     * @throws Exception
     */
    private void hookStartActivity() throws Exception {
        // android 8.0以上源码有变动
        Field gDefaultField;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Class<?> ActivityManagerNativeClass = Class.forName("android.app.ActivityManager");
            gDefaultField = ActivityManagerNativeClass.getDeclaredField("IActivityManagerSingleton");
        } else {
            // 我们从startActivity()中一路查看源码，最终定位到ActivityManagerNative中的gDefault属性
            Class<?> ActivityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            gDefaultField = ActivityManagerNativeClass.getDeclaredField("gDefault");
        }
        gDefaultField.setAccessible(true);
        Object gDefault = gDefaultField.get(null);
        // 从Singleton中获取IActivityManager
        Class<?> singleTonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = singleTonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        Object IActivityManager = mInstanceField.get(gDefault);

        IActivityManager = newProxyInstance(
                HookStartActivityUtil.class.getClassLoader(),
                IActivityManager.getClass().getInterfaces(),
                new HookStartActivityInvocationHandler(IActivityManager)
        );
        // 将代理的IActivityManager设置回Singleton的属性中
        mInstanceField.set(gDefault, IActivityManager);
    }

    /**
     * 动态代理InvocationHandler，用来绕过manifests的配置检测
     * 将原先的intent替换为代理intent，以此来绕过manifests的配置检测
     */
    private class HookStartActivityInvocationHandler implements InvocationHandler {
        private Object mObject;

        HookStartActivityInvocationHandler(Object object) {
            this.mObject = object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 如果hook到startActivity，则替换为我们能检证通过的activity
            /* 如果hook到startActivity,我们找到在IActivityManager类中的startActivity方法
                public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
                String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags,
                ProfilerInfo profilerInfo, Bundle options) throws RemoteException;
             */
            if ("startActivity".equals(method.getName())) {
                // 获取原来的intent，上面注释中，第2个参数就是Intent
                Intent originIntent = (Intent) args[2];
                // 创建一个代理intent，用来去过安检（这个代理的intent里的activity是在manifests中配置过的）
                Intent proxyIntent = new Intent(mContext, mProxyClass);
                // 将原来的intent当做参数保存到代理intent中
                proxyIntent.putExtra(ORIGIN_INTENT, originIntent);
                // 将代理intent重新设置回args参数中
                args[2] = proxyIntent;
            }
            return method.invoke(mObject, args);
        }
    }

    /*--------------------------------------------------------------------------------*/

    /**
     * 在创建activity的时候，hook到handleLaunchActivity，并替换intent
     * 将代理intent替换为我们原先的intent，以些来创建真正想要的activity
     *
     * @throws Exception
     */
    private void hookLaunchActivity() throws Exception {
        Class<?> ActivityThreadClass = Class.forName("android.app.ActivityThread");
        Field sCurrentActivityThreadField = ActivityThreadClass.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        Object sCurrentActivityThread = sCurrentActivityThreadField.get(null);

        Field mHField = ActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Object mH = mHField.get(sCurrentActivityThread);

        Class<?> handlerClass = Handler.class;
        Field mCallbackField = handlerClass.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        mCallbackField.set(mH, new MyHanderCallBack());
    }


    /**
     * 创建一个handler的Callback
     */
    private class MyHanderCallBack implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            //  int LAUNCH_ACTIVITY = 100;  启动时的LAUNCH_ACTIVITY
            if (msg.what == 100) {
                startLaunchActivity(msg);
            }
            return false;
        }

        // 开始创建并启动Activity
        private void startLaunchActivity(Message msg) {
            try {
                Object record = msg.obj;
                Field intentField = record.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                // 这个拿到的是传来的代理Intent
                Intent proxyIntent = (Intent) intentField.get(record);
                // 从代理Intent获取之前传入的原来intent参数
                Intent originIntent = proxyIntent.getParcelableExtra(ORIGIN_INTENT);
                // 将我们原始想要的intent再设置回来
                if (originIntent != null) {
                    intentField.set(record, originIntent);
                    dealAppCompatBug();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 解决兼容AppCompatActivity报错问题
        private void dealAppCompatBug() throws Exception {
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThread = field.get(null);
            Method getPackageManager = activityThread.getClass().getDeclaredMethod("getPackageManager");
            Object iPackageManager = getPackageManager.invoke(activityThread);

            PackageManagerHandler handler = new PackageManagerHandler(iPackageManager);
            Class<?> iPackageManagerIntercept = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iPackageManagerIntercept}, handler);

            // 获取 sPackageManager 属性
            Field iPackageManagerField = activityThread.getClass().getDeclaredField("sPackageManager");
            iPackageManagerField.setAccessible(true);
            iPackageManagerField.set(activityThread, proxy);
        }
    }

    /**
     * 我们这次动态代理是解决继承自AppCompatActivity时，在启动时系统又来检测一次的问题
     */
    private class PackageManagerHandler implements InvocationHandler {
        private Object iPackageManager;

        PackageManagerHandler(Object iPackageManager) {
            this.iPackageManager = iPackageManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getActivityInfo".equals(method.getName())) {
                ComponentName componentName = new ComponentName(mContext, mProxyClass);
                args[0] = componentName;
            }
            return method.invoke(iPackageManager, args);
        }
    }

}
