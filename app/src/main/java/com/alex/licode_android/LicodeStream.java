package com.alex.licode_android;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by dth
 * Des:
 * Date: 2019/2/25.
 */
public class LicodeStream implements LicodeSignalingService.SignalingEvents, PeerConnectionEvents {

    public static final String                                     TAG          = "LicodeStream";
    private             LicodeSignalingService                     mSocketIoClient;
    private             Context                                    mContext;
    private             RTCParameter                               mRTCParameter;
    private             int                                        mCameraId    = 1;
    private             ConcurrentHashMap<Long, SurfaceViewRenderer>         mViewMap     = new ConcurrentHashMap<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private StreamEvents mEvents;
    private VideoCapturer mVideoCapturer;
    private PeerConnectionPool mPCPool;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private LicodeStream() {

    }

    private static class holder{
        static final LicodeStream instance = new LicodeStream();
    }

    public static LicodeStream getInstance() {
        return holder.instance;
    }

    public LicodeStream init(Context context, CubeConfig config) {
        mContext = context;
        mRTCParameter = new RTCParameter();

        mSocketIoClient = new SocketIoClient(this);
        if (config != null) {
            boolean isSupportH264 = config.isSupportH264();
            mRTCParameter.videoCodecHwAcceleration = config.isHardwareDecoding();
            //主叫使用用户设置编码，被叫使用呼叫者编码
            mRTCParameter.videoCodec = config.getVideoCodec();


            mRTCParameter.audioCodec = config.getAudioCodec();
            mRTCParameter.videoFps = config.getVideoFps();

            //如果硬件设备没有摄像头，则不支持视频呼叫
            mRTCParameter.videoCallEnabled = config.isVideoCallEnabled();
            //兼容只有一个摄像头的设备
            if (Camera.getNumberOfCameras() < config.getCameraId() || Camera.getNumberOfCameras() == 1) {
                config.setCameraId(0);
            }
            mRTCParameter.videoMaxBitrate = config.getVideoMaxBitrate();
            mRTCParameter.cameraId = config.getCameraId();
            mCameraId = config.getCameraId();
            int videoW = 0;
            int videoH = 0;
            if (config.getVideoWidth() > 0 && config.getVideoHeight() > 0) {
                Point point = CameraUtil.findAdaptationVideoPreview(mCameraId, config.getVideoWidth(), config.getVideoHeight());
                videoW = point.x;
                videoH = point.y;
            }
            mRTCParameter.videoWidth = videoW;
            mRTCParameter.videoHeight = videoH;
        }
        mRTCParameter.audioStartBitrate = config.getAudioMinBitrate();


//        mPcClient = createPeerConnectionClient();
        mPCPool = createPeerConnectionPool();

        return this;
    }


    private PeerConnectionPool createPeerConnectionPool() {
        if (mRTCParameter == null) {
            throw new IllegalStateException("LicodeStream not init!");
        }
        PeerConnectionEvents.PeerConnectionParameters peerConnectionParameters = new PeerConnectionEvents.PeerConnectionParameters(mRTCParameter.videoCallEnabled, mRTCParameter.loopback, mRTCParameter.tracing, mRTCParameter.videoWidth, mRTCParameter.videoHeight, mRTCParameter.videoFps, mRTCParameter.videoMaxBitrate, mRTCParameter.videoCodec, mRTCParameter.videoCodecHwAcceleration, mRTCParameter.videoFlexfecEnabled, mRTCParameter.audioStartBitrate, mRTCParameter.audioCodec, mRTCParameter.noAudioProcessing, mRTCParameter.aecDump, mRTCParameter.saveInputAudioToFile, mRTCParameter.useOpenSLES, mRTCParameter.disableBuiltInAEC, mRTCParameter.disableBuiltInAGC, mRTCParameter.disableBuiltInNS, mRTCParameter.disableWebRtcAGCAndHPF, null);
        EglBase eglBase = EglBase.create();
        PeerConnectionPool pcPool = new PeerConnectionPool(mContext, eglBase, peerConnectionParameters, this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (mRTCParameter.loopback) {
            options.networkIgnoreMask = 0;
        }

        pcPool.createPeerConnectionFactory(options);
        return pcPool;
    }

    private void initVideoView(IStreamDescription streamDescription, StreamDescription.StreamDesState streamDesState) {
        if (mPCPool == null) {
            Log.e(TAG, "initRemoteVideoView: mPcClient = null !");
            return;
        }
        SurfaceViewRenderer localRender = new SurfaceViewRenderer(mContext);
        localRender.init(mPCPool.getRenderContext(), null);
        localRender.setZOrderMediaOverlay(true);
        localRender.setEnableHardwareScaler(true);
        localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        SurfaceViewRenderer remoteRender = new SurfaceViewRenderer(mContext);
        remoteRender.init(mPCPool.getRenderContext(), null);
        remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        remoteRender.setEnableHardwareScaler(false);

        ProxyRenderer remoteProxyVideoSink = new ProxyRenderer();
        ProxyRenderer localProxyVideoSink  = new ProxyRenderer();
        localProxyVideoSink.setTarget(localRender);
        remoteProxyVideoSink.setTarget(remoteRender);

        localRender.setMirror(false);
        remoteRender.setMirror(false);

        mVideoCapturer = createVideoCapturer();

        StreamDescription streamDes = (StreamDescription)streamDescription;
        streamDes.bindView(localRender,remoteRender);

        List<VideoRenderer.Callbacks> videoSinks = new ArrayList<>();
        videoSinks.add(remoteProxyVideoSink);
        mPCPool.createPeerConnection(localProxyVideoSink, videoSinks, mVideoCapturer, iceServers,  streamDes,streamDesState);
    }


    public void connectToRoom(LicodeSignalingService.RoomConnectionParameters connectionParameters) {
        mSocketIoClient.connectToRoom(connectionParameters);
    }



    public synchronized void close() {
        if (mPCPool != null) {
            mPCPool.close();
            mPCPool = null;
        }

        mVideoCapturer = null;
        mCameraId = 1;
        if (mSocketIoClient != null) {
            mSocketIoClient.close();
            mSocketIoClient = null;
        }
        if (mEvents != null) {
            mEvents.close();
            mEvents = null;
        }
    }

    public synchronized void close(long streamId) {
        mPCPool.close(streamId);
    }

    private class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        @Override
        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                VLog.d("Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }



    private VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        if (useCamera2()) {
            if (!captureToTexture()) {
                //reportError(getString(R.string.camera2_texture_only_error));
                return null;
            }

            Log.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(mContext));
        }
        else {
            Log.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            //reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(mContext);
    }

    private boolean captureToTexture() {
        return useCamera2();//getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras." + deviceNames.length + "+" + Arrays.asList(deviceNames).toString());

        for (String deviceName : deviceNames) {
            if (String.valueOf(mCameraId).equals(deviceName)) {
                Log.d(TAG, "Custom Creating camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }


    @Override
    public void onLocalDescription(SessionDescription sdp, StreamDescription streamDescription) {
        VLog.i("sdpOffer: \n" + sdp.description);
        //send offer
        mSocketIoClient.sendOfferSdp(sdp.description,streamDescription);
    }

    @Override
    public void onIceCandidate(IceCandidate candidate,StreamDescription streamDescription) {
        VLog.d("candidate: " + candidate.toString());
        JSONObject jsonObject = parseIceCandidate(candidate);
        mSocketIoClient.sendLocalIceCandidate(jsonObject,streamDescription);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        VLog.d("onIceCandidatesRemoved---");
    }

    @Override
    public void onIceConnected() {

        VLog.d("onIceConnected---");
    }

    @Override
    public void onIceDisconnected() {
        VLog.d("onIceDisconnected---");
    }

    @Override
    public void onIceCompleted() {
        VLog.d("onIceCompleted---");
    }

    @Override
    public void onPeerConnectionClosed(StreamDescription streamDescription) {
        VLog.d("onPeerConnectionError---"+streamDescription.getId());
    }

    @Override
    public void onPeerConnectionPoolDestroy() {

    }


    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        VLog.d("onPeerConnectionStatsReady---");
    }

    @Override
    public void onPeerConnectionError(long streamId, String description) {
        VLog.d("onPeerConnectionClosed---" + streamId);

    }



    //licode


    @Override
    public void onConnectedToRoom(LicodeSignalingParams.TokenParams params) {
        iceServers.clear();
        this.iceServers.addAll(params.iceServers);
    }

    @Override
    public void onRemoteDescription(long streamId, String sdp) {
        VLog.i("sdpAnswer: \n" + sdp);
        mPCPool.setRemoteDescription(streamId,sdp);
    }


    @Override
    public void onChannelError(String description) {

    }

    @Override
    public void onPublish(IStreamDescription streamDescription) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                initVideoView(streamDescription, new StreamDescription.StreamDesState() {
                    @Override
                    public void onStreamDesInit() {
                        streamDescription.createOffer();
                    }
                });
                View surfaceView = streamDescription.getSurfaceView();
                VLog.d("surfaceView: "+surfaceView + "   isLocal: "+streamDescription.isLocal());
                if (mEvents != null) {
                    mEvents.onViewCreate(streamDescription.getId(),surfaceView,streamDescription.isLocal());
                }
            }
        });
    }

    @Override
    public void onSubscribe(IStreamDescription streamDescription) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                initVideoView(streamDescription, new StreamDescription.StreamDesState() {
                    @Override
                    public void onStreamDesInit() {
                        streamDescription.createOffer();
                    }
                });
                View surfaceView = streamDescription.getSurfaceView();
                VLog.d("surfaceView: "+surfaceView + "   isLocal: "+streamDescription.isLocal());
                if (mEvents != null) {
                    mEvents.onViewCreate(streamDescription.getId(),surfaceView,streamDescription.isLocal());
                }
            }
        });
    }

    @Override
    public void onUnSubscribe(long streamId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                View surfaceView = mPCPool.getSurfaceView(streamId);
                if (mEvents != null) {
                    mEvents.onViewDestroy(streamId,surfaceView);
                }
                close(streamId);
            }
        });


    }

    private static JSONObject parseIceCandidate(IceCandidate candidate) {
        JSONObject json = new JSONObject();
        try {
            json.put("candidate", "a="+candidate.sdp);
            json.put("sdpMid", candidate.sdpMid);
            json.put("sdpMLineIndex", candidate.sdpMLineIndex);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return json;
    }


    public interface StreamEvents{
        void onViewCreate(long streamId,View surfaceView,boolean isLocal);

        void onViewDestroy(long streamId,View surfaceView);

        void close();
    }

    public void setStreamEvents(StreamEvents events) {
        mEvents = events;
    }
}
