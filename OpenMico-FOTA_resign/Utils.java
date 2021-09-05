package com.xiaomi.mico.romupdate;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Pair;
import com.elvishew.xlog.XLog;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class Utils {
    public static NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) RomUpdateApplication.appContext.getSystemService("connectivity");
        if (connectivityManager == null) {
            return null;
        }
        return connectivityManager.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String getBuildModel() {
        return Build.MODEL;
    }

    @SuppressLint({"MissingPermission"})
    public static String getSn() {
        return Build.getSerial();
    }

    public static String getParamHash(String str, String str2, String str3, String str4, String str5, String str6) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new Pair(Constants.PRODUCT_MODEL, str));
        arrayList.add(new Pair(Constants.CURRENT_ROM_VERSION, str2));
        arrayList.add(new Pair(Constants.CURRENT_ROM_CHANNEL, str3));
        arrayList.add(new Pair(Constants.PRODUCT_FILTER_ID, str4));
        arrayList.add(new Pair(Constants.LOCALE, str5));
        arrayList.add(new Pair(Constants.TIMESTAMP, str6));
        Collections.sort(arrayList, new Comparator<Pair<String, String>>() {
            /* class com.xiaomi.mico.romupdate.Utils.AnonymousClass1 */

            public int compare(Pair<String, String> pair, Pair<String, String> pair2) {
                return ((String) pair.first).compareTo((String) pair2.first);
            }
        });
        StringBuilder sb = new StringBuilder();
        Iterator it = arrayList.iterator();
        boolean z = true;
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            if (!z) {
                sb.append("&");
            }
            sb.append((String) pair.first);
            sb.append("=");
            sb.append((String) pair.second);
            z = false;
        }
        sb.append("&");
        sb.append("8007236f-a2d6-4847-ac83-c49395ad6d65");
        String sb2 = sb.toString();
        XLog.d("params before hashing: " + sb2);
        byte[] encode = Base64.encode(getBytes(sb2), 2);
        XLog.d("params after base64 encoding: " + new String(encode));
        String md5Digest = getMd5Digest(encode);
        XLog.d("params md5: " + md5Digest);
        return md5Digest;
    }

    public static byte[] getBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException unused) {
            return str.getBytes();
        }
    }

    public static String getMd5Digest(byte[] bArr) {
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(bArr);
            return String.format("%1$032X", new BigInteger(1, instance.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getExternalStoragePath(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = context.getFilesDir();
        }
        return externalFilesDir.getAbsolutePath();
    }

    public static String getLogDirectory(Context context) {
        return getExternalStoragePath(context) + File.separator + "log";
    }

    public static boolean isEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    public static long getMidnightTimeStamp() {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(System.currentTimeMillis());
        instance.set(11, 0);
        instance.set(12, 0);
        instance.set(13, 0);
        instance.set(14, 0);
        return instance.getTimeInMillis();
    }

    public static ActivityManager.RunningAppProcessInfo getTopProcessInfo() {
        Field field;
        Integer num;
        try {
            field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (Exception unused) {
            field = null;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ((ActivityManager) RomUpdateApplication.appContext.getSystemService("activity")).getRunningAppProcesses()) {
            if (runningAppProcessInfo.importance == 100 && runningAppProcessInfo.importanceReasonCode == 0) {
                try {
                    num = Integer.valueOf(field.getInt(runningAppProcessInfo));
                } catch (Exception unused2) {
                    XLog.e("Exception in getting 'processState' field from ActivityManager.RunningAppProcessInfo.");
                    num = null;
                }
                if (num != null && (num.intValue() == 2 || num.intValue() == 1)) {
                    XLog.d("Top app process is " + runningAppProcessInfo.processName + ", process state is " + num);
                    return runningAppProcessInfo;
                }
            }
        }
        return null;
    }

    public static ComponentName getCurrentActivity() {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) RomUpdateApplication.appContext.getSystemService("activity")).getRunningTasks(1);
        return new ComponentName(runningTasks.get(0).topActivity.getPackageName(), runningTasks.get(0).topActivity.getClassName());
    }

    public static boolean currentIsHome() {
        ComponentName currentActivity = getCurrentActivity();
        XLog.d("Current component is " + currentActivity.getPackageName() + "/" + currentActivity.getClassName());
        return "com.xiaomi.micolauncher".equals(currentActivity.getPackageName()) && ("com.xiaomi.micolauncher.Launcher".equals(currentActivity.getClassName()) || "com.xiaomi.micolauncher.module.lockscreen.LockScreenActivity".equals(currentActivity.getClassName()));
    }
}
