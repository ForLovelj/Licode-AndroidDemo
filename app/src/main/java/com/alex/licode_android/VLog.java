package com.alex.licode_android;

import android.util.Log;

/**
 * Created by dth
 * Des:
 * Date: 2018/12/5.
 */
public class VLog {
    private static String sTag = "VLog";
    private static int sLevel = Log.DEBUG;

    public static String getTag() {
        return sTag;
    }

    public static void setTag(String tag) {
        sTag = tag;
    }

    public static int getLevel() {
        return sLevel;
    }

    public static void setLevel(int level) {
        sLevel = level;
    }

    public static void v(String format, Object... args) {
        if (Log.VERBOSE >= sLevel) {
            Log.v(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void verbose(String tag, String format, Object... args) {
        if (Log.VERBOSE >= sLevel) {
            Log.v(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void d(String format, Object... args) {
        if (Log.DEBUG >= sLevel) {
            Log.d(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void debug(String tag, String format, Object... args) {
        if (Log.DEBUG >= sLevel) {
            Log.d(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void i(String format, Object... args) {
        if (Log.INFO >= sLevel) {
            Log.i(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void info(String tag, String format, Object... args) {
        if (Log.INFO >= sLevel) {
            Log.i(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void w(String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void warn(String tag, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void w(Throwable throwable, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(sTag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void warn(String tag, Throwable throwable, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(tag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void e(String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void error(String tag, String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void e(Throwable throwable, String msg, Object... format) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(String.format(msg, format)), throwable);
        }
    }

    public static void error(String tag, Throwable throwable, String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(tag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void e(Throwable e) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(getStackTraceString(e)));
        }
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    public static void printStackTrace(String tag) {
        if (Log.VERBOSE >= sLevel) {
            Log.e(tag, buildMsg(getStackTraceString(new Exception())));
        }
    }

    private static String buildMsg(String msg) {
        StringBuilder buffer = new StringBuilder();

        final StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];

        buffer.append("[ (");
        buffer.append(stackTraceElement.getFileName());
        buffer.append(":");
        buffer.append(stackTraceElement.getLineNumber());
        buffer.append(")# ");
        buffer.append(stackTraceElement.getMethodName());
        buffer.append(" -> ");
        buffer.append(Thread.currentThread().getName());
        buffer.append(" ] ");

        buffer.append(msg);

        return buffer.toString();
    }
}
