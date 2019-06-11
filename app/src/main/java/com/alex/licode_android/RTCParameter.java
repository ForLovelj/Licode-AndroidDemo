package com.alex.licode_android;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * RTC配置参数
 */
public class RTCParameter {
    public boolean                        videoCallEnabled;
    public boolean                        loopback;
    public boolean                        tracing;
    public int                            cameraId;
    public int                            videoWidth;
    public int                            videoHeight;
    public int                            videoFps;
    public int                            videoMaxBitrate;
    public int                            videoMinBitrate;
    public String                         videoCodec;
    public boolean                        videoCodecHwAcceleration;
    public boolean                        videoFlexfecEnabled;
    public int                            audioStartBitrate;
    public String                         audioCodec;
    public boolean                        noAudioProcessing;
    public boolean                        aecDump;
    public boolean                        saveInputAudioToFile;
    public boolean                        useOpenSLES;
    public boolean                        disableBuiltInAEC;
    public boolean                        disableBuiltInAGC;
    public boolean                        disableBuiltInNS;
    public boolean                        disableWebRtcAGCAndHPF;
    public boolean                        enableRtcEventLog;
    public List<PeerConnection.IceServer> iceServers = new ArrayList<>();
}
