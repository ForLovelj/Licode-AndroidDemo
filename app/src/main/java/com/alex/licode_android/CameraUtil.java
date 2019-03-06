package com.alex.licode_android;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CameraUtil {
    private static final String TAG         = "CameraUtil";
    private static       float  previewRate = 1;

    public static Point findBestVideoPreview(int cameraId, int width, int height, Context context) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        Camera camera = Camera.open(cameraId);
        return findBestPreviewResolution(camera.getParameters().getPreviewSize(), camera.getParameters().getSupportedVideoSizes(), new Point(), ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation(), info.orientation);
    }

    public static Point findAdaptationVideoPreview(int cameraId, int width, int height) {
        Camera camera = Camera.open(cameraId);
        Point point = findAdaptationResolution(camera.getParameters().getPreviewSize(), camera.getParameters().getSupportedVideoSizes(), new Point(width, height));
        camera.release();
        return point;
    }

    /**
     * 找出最适合的预览界面分辨率
     *
     * @param defaultSize
     * @param rawSupportedSizes
     * @param screenResolution
     * @param screenOrientation
     * @param cameraOrientation
     *
     * @return
     */
    public static Point findBestPreviewResolution(Camera.Size defaultSize, List<Camera.Size> rawSupportedSizes, Point screenResolution, int screenOrientation, int cameraOrientation) {
        Log.d(TAG, "camera default resolution " + defaultSize.width + "x" + defaultSize.height);
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            return new Point(defaultSize.width, defaultSize.height);
        }
        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });
        //printlnSupportedPreviewSize(supportedPreviewResolutions);
        // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
        // 由于camera的分辨率是width>height，这里先判断我们的屏幕和相机的角度是不是相同的方向(横屏 or 竖屏),然后决定比较的时候要不要先交换宽高值
        final boolean isCandidatePortrait = screenOrientation % 180 != cameraOrientation % 180;
        final double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;
        // 移除不符合条件的分辨率
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;
            // 移除低于下限的分辨率，尽可能取高分辨率
            //if (width * height < MIN_PREVIEW_PIXELS) {
            //    it.remove();
            //    continue;
            //}
            //移除宽高比差异较大的
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > 0.15) {
                it.remove();
                continue;
            }
            // 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(width, height);
                Log.d(TAG, "found preview resolution exactly matching screen resolutions: " + exactPoint);
                return exactPoint;
            }
            //删掉宽高比比屏幕小的,防止左右出现白边
            if (aspectRatio - screenAspectRatio < 0) {
                it.remove();
                continue;
            }
        }
        // 如果没有找到合适的，并且还有候选的像素，则设置分辨率最大的
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            Point largestSize = new Point(largestPreview.width, largestPreview.height);
            Log.d(TAG, "using largest suitable preview resolution: " + largestSize);
            return largestSize;
        }
        //如果最后集合空了且本身支持640*480,则选择640*480
        if (supportedPreviewResolutions.isEmpty()) {
            it = rawSupportedSizes.iterator();
            while (it.hasNext()) {
                final Camera.Size next = it.next();
                if (next.width == 640 && next.height == 480) {
                    return new Point(next.width, next.height);
                }
            }
        }
        // 没有找到合适的，就返回默认的
        Point defaultResolution = new Point(defaultSize.width, defaultSize.height);
        Log.i(TAG, "No suitable preview resolutions, using default: " + defaultResolution);
        return defaultResolution;
    }

    /**
     * 找出最适合的预览界面分辨率
     *
     * @param defaultSize
     * @param rawSupportedSizes
     * @param screenResolution
     *
     * @return
     */
    public static Point findAdaptationResolution(Camera.Size defaultSize, List<Camera.Size> rawSupportedSizes, Point screenResolution) {
        Log.d(TAG, "camera default resolution " + defaultSize.width + "x" + defaultSize.height);
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            return new Point(defaultSize.width, defaultSize.height);
        }
        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;
            if (width > screenResolution.x + 80 || width < screenResolution.x - 80) {
                it.remove();
                continue;
            }
        }
        // 如果没有找到合适的，并且还有候选的像素，则设置分辨率最大的
        if (!supportedPreviewResolutions.isEmpty()) {
            Iterator<Camera.Size> it2 = supportedPreviewResolutions.iterator();
            while (it2.hasNext()) {
                Camera.Size supportedPreviewResolution = it2.next();
                int width = supportedPreviewResolution.width;
                int height = supportedPreviewResolution.height;
                if (width == screenResolution.x) {
                    return new Point(width, height);
                }
            }
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            Point largestSize = new Point(largestPreview.width, largestPreview.height);
            Log.d(TAG, "using largest suitable preview resolution: " + largestSize);
            return largestSize;
        }

        //如果最后集合空了且本身支持640*480,则选择640*480
        if (supportedPreviewResolutions.isEmpty()) {
            it = rawSupportedSizes.iterator();
            while (it.hasNext()) {
                final Camera.Size next = it.next();
                if (next.width == 640 && next.height == 480) {
                    return new Point(next.width, next.height);
                }
            }
        }
        // 没有找到合适的，就返回默认的
        Point defaultResolution = new Point(defaultSize.width, defaultSize.height);
        Log.i(TAG, "No suitable preview resolutions, using default: " + defaultResolution);
        return defaultResolution;
    }

    public static Point getScreenMetrics(Context context) {

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int w_screen = dm.widthPixels;
        int h_screen = dm.heightPixels;
        return new Point(w_screen, h_screen);
    }

    public static float getScreenRate(Context context) {
        Point P = getScreenMetrics(context);
        float H = P.y;
        float W = P.x;
        return (H / W);
    }
}
