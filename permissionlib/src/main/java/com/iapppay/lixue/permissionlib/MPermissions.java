package com.iapppay.lixue.permissionlib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理类
 * Created by Administrator on 2016/4/25.
 */
public class MPermissions {
    private static final String TAG = "MPermissions";
    private static final String SUFFIX = "$$PermissionProxy";

    private static MPermissions mInstance;

    /**单例模式*/
    public static MPermissions getmInstance(Context context){
        if (mInstance == null){
            synchronized (MPermissions.class){
                if (mInstance == null){
                    mInstance = new MPermissions(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private Context mCtx;

    public MPermissions(Context context){
        this.mCtx = context;
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission, int requestCode) {
        PermissionProxy proxy = findPermissionProxy(activity);
        if (!proxy.needRational(requestCode)) return false;
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,permission)) {
            proxy.rational(activity, requestCode);
            return true;
        }
        return false;
    }

    public static void requestPermissions(Activity activity,int requestCode,String... permissions){
        _requestPermissions(activity,requestCode,permissions);
    }

    public static void requestPermissions(Fragment object, int requestCode, String... permissions) {
        _requestPermissions(object, requestCode, permissions);
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    private static void _requestPermissions(Object object, int requestCode, String... permissions) {
        if (!Utils.isOverMarshmallow()) {
            doExecuteSuccess(object, requestCode);
            return;
        }
        List<String> deniedPermissions = Utils.findDeniedPermissions(Utils.getActivity(object), permissions);

        if (deniedPermissions.size() > 0) {
            if (object instanceof Activity) {
                ((Activity) object).requestPermissions(deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
            } else if (object instanceof Fragment) {
                ((Fragment) object).requestPermissions(deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
            } else {
                throw new IllegalArgumentException(object.getClass().getName() + " is not supported!");
            }
        } else {
            doExecuteSuccess(object, requestCode);
        }
    }

    /**成功后处理**/
    private static void doExecuteSuccess(Object object,int requestCode){
        findPermissionProxy(object).grant(object, requestCode);
    }

    private static PermissionProxy findPermissionProxy(Object activity){
        try {
            Class clazz = activity.getClass();
            Class injectorClazz = Class.forName(clazz.getName() + SUFFIX);
            return (PermissionProxy) injectorClazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException(String.format("can not find %s , something when compiler.", activity.getClass().getSimpleName() + SUFFIX));
    }

    private static void doExecuteFail(Object activity, int requestCode) {
        findPermissionProxy(activity).denied(activity, requestCode);
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions,int[] grantResults) {
        requestResult(activity, requestCode, permissions, grantResults);
    }

    public static void onRequestPermissionsResult(Fragment fragment, int requestCode, String[] permissions,int[] grantResults) {
        requestResult(fragment, requestCode, permissions, grantResults);
    }

    private static void requestResult(Object obj, int requestCode, String[] permissions,int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }
        if (deniedPermissions.size() > 0) {
            doExecuteFail(obj, requestCode);
        } else {
            doExecuteSuccess(obj, requestCode);
        }
    }
}
