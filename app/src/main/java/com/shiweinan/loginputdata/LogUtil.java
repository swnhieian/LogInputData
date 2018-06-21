package com.shiweinan.loginputdata;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

class LogUtil {
    private static boolean LOGV = true;
    private static boolean LOGD = true;
    private static boolean LOGI = true;
    private static boolean LOGW = true;
    private static boolean LOGE = true;

    private static String getTag() {
        StackTraceElement[] trace = new Throwable().fillInStackTrace()
            .getStackTrace();
        String callingClass = "";
        for (int i = 2; i < trace.length; i++) {
            Class<?> clazz = trace[i].getClass();
            if (!clazz.equals(LogUtil.class)) {
                callingClass = trace[i].getClassName();
                callingClass = callingClass.substring(callingClass
                    .lastIndexOf('.') + 1);
                break;
            }
        }
        return callingClass;
    }

    private static String buildMessage(String msg) {
        StackTraceElement[] trace = new Throwable().fillInStackTrace()
            .getStackTrace();
        String caller = "";
        for (int i = 2; i < trace.length; i++) {
            Class<?> clazz = trace[i].getClass();
            if (!clazz.equals(LogUtil.class)) {
                caller = trace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%d] %s: %s", Thread.currentThread()
            .getId(), caller, msg);
    }

    public static void v(String mess) {
        if (LOGV) { Log.v(getTag(), buildMessage(mess)); }
    }
    public static void d(String mess) {
        if (LOGD) { Log.d(getTag(), buildMessage(mess)); }
    }
    public static void i(String mess) {
        if (LOGI) { Log.i(getTag(), buildMessage(mess)); }
    }
    public static void w(String mess) {
        if (LOGW) { Log.w(getTag(), buildMessage(mess)); }
    }
    public static void e(String mess) {
        if (LOGE) { Log.e(getTag(), buildMessage(mess)); }
    }
    public static BufferedWriter writer = null;
    public static BufferedWriter sensorWriter = null;
    public static void finish() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e("Writer close failed!");
            }
        }
        if (sensorWriter != null) {
            try {
                sensorWriter.flush();
                sensorWriter.close();
                sensorWriter = null;
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e("Sensor writer close failed!");
            }
        }
    }

    public static void init() { // create log file to write
        Date timeNow = new Date();
        CharSequence timeS = android.text.format.DateFormat.format("yy-MM-dd-HH-mm-ss-", timeNow.getTime());
        String timeStr = timeS.toString() + Config.posture + ".txt";
        //String timeStr = String.format(Locale.CHINA, "%tm-%te-%tH-%tM-%tS.txt", timeNow);
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String folderName = "gazeLog";
        try {
            String fileName = filePath + File.separatorChar;
            File file = new File(fileName);
            if (!file.exists()) {
                file.mkdir();
            }
            fileName = filePath + File.separatorChar+ folderName + File.separatorChar;
            file = new File(fileName);
            if (!file.exists()) {
                file.mkdir();
            }
            fileName = fileName + timeStr;
            file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            writer = new BufferedWriter(new FileWriter(file));
            fileName = filePath + File.separatorChar+ folderName + File.separatorChar + timeS.toString() + Config.posture + "-sensor.txt";
            file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            sensorWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String mess) { // log info to file
        long time = System.currentTimeMillis();
        long time2 = SystemClock.uptimeMillis();
        if (writer == null) {
            LogUtil.e("log writer is NULL");
        } else {
            try {
                writer.write(time + "," + time2 + "," + mess + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void logSensor(String mess) { // log sensor info to file
        long time = System.currentTimeMillis();
        long time2 = SystemClock.uptimeMillis();
        if (sensorWriter == null) {
            //LogUtil.e("sensor writer is NULL");
        } else {
            try {
                sensorWriter.write(time + "," + time2 + "," + mess + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
