package com.alex.licode_android;

/**
 * Created by dth
 * Des:
 * Date: 2019/2/25.
 */
public class CubeConfig {

    private boolean isHardwareDecoding = true;
    private int     cameraId           = 1;
    private String  audioCodec         = "OPUS";
    private String  videoCodec         = "VP8";
    private int     videoWidth         = 640;
    private int     videoHeight        = 480;
    private int     videoFps           = 18;
    private int     videoMaxBitrate    = 1600;//1M带宽即指1Mbps=1000Kbps=1000/8KBps=125KBps
    private int     audioMinBitrate    = 32;//1M带宽即指1Mbps=1000Kbps=1000/8KBps=125KBps
    private boolean isSupportH264 = false;
    private boolean                        videoCallEnabled = true;

    public CubeConfig() {}


    public boolean isHardwareDecoding() {
        return isHardwareDecoding;
    }

    public void setHardwareDecoding(boolean hardwareDecoding) {
        isHardwareDecoding = hardwareDecoding;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoFps() {
        return videoFps;
    }

    public void setVideoFps(int videoFps) {
        this.videoFps = videoFps;
    }

    public int getVideoMaxBitrate() {
        return videoMaxBitrate;
    }

    public void setVideoMaxBitrate(int videoMaxBitrate) {
        this.videoMaxBitrate = videoMaxBitrate;
    }

    public int getAudioMinBitrate() {
        return audioMinBitrate;
    }

    public void setAudioMinBitrate(int audioMinBitrate) {
        this.audioMinBitrate = audioMinBitrate;
    }

    public boolean isSupportH264() {
        return isSupportH264;
    }

    public void setSupportH264(boolean supportH264) {
        isSupportH264 = supportH264;
    }


    public boolean isVideoCallEnabled() {
        return videoCallEnabled;
    }

    public void setVideoCallEnabled(boolean videoCallEnabled) {
        this.videoCallEnabled = videoCallEnabled;
    }
}
