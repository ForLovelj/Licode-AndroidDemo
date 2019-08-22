package com.alex.licode_android;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.LegacyAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by dth
 * Des:
 * Date: 2019/2/28.
 */
public class PeerConnectionPool {
    public static final  String VIDEO_TRACK_ID                        = "ARDAMSv0";
    public static final  String AUDIO_TRACK_ID                        = "ARDAMSa0";
    public static final  String VIDEO_TRACK_TYPE                      = "video";
    private static final String TAG                                   = "PeerConnectionPool";
    private static final String VIDEO_CODEC_VP8                       = "VP8";
    private static final String VIDEO_CODEC_VP9                       = "VP9";
    private static final String VIDEO_CODEC_H264                      = "H264";
    private static final String VIDEO_CODEC_H264_BASELINE             = "H264 Baseline";
    private static final String VIDEO_CODEC_H264_HIGH                 = "H264 High";
    private static final String AUDIO_CODEC_OPUS                      = "opus";
    private static final String AUDIO_CODEC_ISAC                      = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE       = "x-google-start-bitrate";
    private static final String VIDEO_FLEXFEC_FIELDTRIAL              = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    private static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    private static final String DISABLE_WEBRTC_AGC_FIELDTRIAL         = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    private static final String AUDIO_CODEC_PARAM_BITRATE             = "maxaveragebitrate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT    = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT    = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT     = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT    = "googNoiseSuppression";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT    = "DtlsSrtpKeyAgreement";
    private static final int    HD_VIDEO_WIDTH                        = 1280;
    private static final int    HD_VIDEO_HEIGHT                       = 720;
    private static final int    BPS_IN_KBPS                           = 1000;
    private static final String RTCEVENTLOG_OUTPUT_DIR_NAME           = "rtc_event_log";

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private static final ExecutorService                               executor = Executors.newSingleThreadExecutor();
    private final        Timer                                         statsTimer  = new Timer();
    private final        EglBase                                       rootEglBase;
    private final        Context                                       appContext;
    private final        PeerConnectionEvents.PeerConnectionParameters peerConnectionParameters;
    private final        PeerConnectionEvents                          events;
    private final boolean                                              dataChannelEnabled;
    private int                                                        videoWidth;
    private int                                                        videoHeight;
    private int                                                        videoFps;
    private MediaConstraints                                           audioConstraints;
    private MediaConstraints                                           sdpMediaConstraints;
    @Nullable
    private PeerConnectionFactory                                  factory;
    private boolean                                                isInitFactory;
    // Implements the WebRtcAudioRecordSamplesReadyCallback interface and writes
    // recorded audio samples to an output file.
    @Nullable
    private SurfaceTextureHelper                                   surfaceTextureHelper;
    private boolean                                                preferIsac;
    private             ConcurrentHashMap<Long, StreamDescription> mStreamDesMap           = new ConcurrentHashMap<>();
    // enableVideo is set to true if video should be rendered and sent.
    private boolean                                                renderVideo             = true;
    private boolean                                                enableAudio             = true;
    private VideoTrack                                             mLocalVideoTrack;
    private AudioTrack                                             mLocalAudioTrack;
    private VideoSource                                            mVideoSource;
    private AudioSource                                            mAudioSource;
    private DataChannel                                            dataChannel;
    // Enable RtcEventLog.
    private VideoCapturer mVideoCapturer;
    private RtpSender localVideoSender;


    /**
     * Create a PeerConnectionClient with the specified parameters. PeerConnectionClient takes
     * ownership of |eglBase|.
     */
    public PeerConnectionPool(Context appContext, EglBase eglBase, PeerConnectionEvents.PeerConnectionParameters peerConnectionParameters, PeerConnectionEvents events) {
        this.rootEglBase = eglBase;
        this.appContext = appContext;
        this.events = events;
        this.peerConnectionParameters = peerConnectionParameters;
        this.dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null;

        Log.d(TAG, "Preferred video codec: " + getSdpVideoCodecName(peerConnectionParameters));

        final String fieldTrials = getFieldTrials(peerConnectionParameters);
        executor.execute(() -> {
            Log.d(TAG, "Initialize WebRTC. Field trials: " + fieldTrials + " Enable video HW acceleration: " + peerConnectionParameters.videoCodecHwAcceleration);
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                            .setFieldTrials(fieldTrials)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions());
            createMediaConstraintsInternal();
            //            maybeCreateAndStartRtcEventLog();
        });
    }

    /**
     * This function should only be called once.
     */
    public void createPeerConnectionFactory(PeerConnectionFactory.Options options) {
        executor.execute(() -> createPeerConnectionFactoryInternal(options));
    }

    private void createPeerConnectionFactoryInternal(PeerConnectionFactory.Options options) {
        if (factory != null) {
            Log.e(TAG, "PeerConnectionFactory has already been constructed");
            return;
        }

        if (peerConnectionParameters.tracing) {
            PeerConnectionFactory.startInternalTracingCapture(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "webrtc-trace.txt");
        }

        // Check if ISAC is used by default.
        preferIsac = peerConnectionParameters.audioCodec != null && peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC);

        // It is possible to save a copy in raw PCM format on a file by checking
        // the "Save input audio to file" checkbox in the Settings UI. A callback
        // interface is set when this flag is enabled. As a result, a copy of recorded
        // audio samples are provided to this client directly from the native audio
        // layer in Java.

        final AudioDeviceModule adm = createJavaAudioDevice();

        // Create peer connection factory.
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
        }
        final boolean enableH264HighProfile = VIDEO_CODEC_H264_HIGH.equals(peerConnectionParameters.videoCodec);
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        if (peerConnectionParameters.videoCodecHwAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
            decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        }
        else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        isInitFactory = true;
        Log.d(TAG, "Peer connection factory created.");
        adm.release();
    }

    public boolean isInitFactory() {
        return isInitFactory;
    }

    public EglBase.Context getRenderContext() {
        return rootEglBase.getEglBaseContext();
    }

    public View getSurfaceView(long streamId) {
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription != null) {
            return streamDescription.getSurfaceView();
        }
        return null;
    }

    private void createMediaConstraintsInternal() {
        // Create video constraints if video call is enabled.
        videoWidth = peerConnectionParameters.videoWidth;
        videoHeight = peerConnectionParameters.videoHeight;
        videoFps = peerConnectionParameters.videoFps;

        // If video resolution is not specified, default to HD.
        if (videoWidth == 0 || videoHeight == 0) {
            videoWidth = HD_VIDEO_WIDTH;
            videoHeight = HD_VIDEO_HEIGHT;
        }

        // If fps is not specified, default to 30.
        if (videoFps == 0) {
            videoFps = 30;
        }
        Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps);

        // Create audio constraints.
        audioConstraints = new MediaConstraints();
        // added for audio performance measurements
        if (peerConnectionParameters.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }
        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", Boolean.toString(isVideoCallEnabled())));
    }

    public void createPeerConnection(final VideoSink localRender, final List<VideoSink> remoteSinks, final VideoCapturer videoCapturer, final List<PeerConnection.IceServer> iceServers, StreamDescription streamDescription, StreamDescription.StreamDesState streamDesState) {
        if (peerConnectionParameters == null) {
            Log.e(TAG, "Creating peer connection without initializing factory.");
            return;
        }

        executor.execute(() -> {
            try {
                createPeerConnectionInternal(localRender,remoteSinks,videoCapturer,iceServers,streamDescription,streamDesState);
            } catch (Exception e) {
                reportError(streamDescription.getId(),"Failed to create peer connection: " + e.getMessage());
                throw e;
            }
        });
    }

    private void createPeerConnectionInternal(VideoSink localRender, final List<VideoSink> remoteSinks, final VideoCapturer videoCapturer, final List<PeerConnection.IceServer> iceServers,StreamDescription streamDescription,StreamDescription.StreamDesState streamDesState) {
        if (factory == null) {
            Log.e(TAG, "Peerconnection factory is not created");
            return;
        }

        if (streamDescription == null) {
            Log.e(TAG, "streamDescription is null");
            return;
        }


        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        //rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY; //fldy sip fs 无法使用
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = !peerConnectionParameters.loopback;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PCObserver  pcObserver  = new PCObserver(streamDescription);
        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, pcObserver);
        streamDescription.initPC(peerConnection);
        mStreamDesMap.put(streamDescription.getId(), streamDescription);

        if (dataChannelEnabled) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = peerConnectionParameters.dataChannelParameters.ordered;
            init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
            init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
            init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
            init.id = peerConnectionParameters.dataChannelParameters.id;
            init.protocol = peerConnectionParameters.dataChannelParameters.protocol;
            dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
        }

        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);

        if (streamDescription.isLocal()) {
            List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
            VideoTrack remoteVideoTrack = null;
            if (isVideoCallEnabled()) {
                // We can add the renderers right away because we don't need to wait for an
                // answer to get the remote track.
                if (mLocalVideoTrack == null) {
                    mLocalVideoTrack = createVideoTrack(videoCapturer, localRender);
                }
                peerConnection.addTrack(mLocalVideoTrack, mediaStreamLabels);

                findVideoSender(streamDescription.getId());
                // We can add the renderers right away because we don't need to wait for an
                // answer to get the remote track.
                remoteVideoTrack = getRemoteVideoTrack(streamDescription.getId());
                remoteVideoTrack.setEnabled(true);
                for (VideoSink remoteSink : remoteSinks) {
                    remoteVideoTrack.addSink(remoteSink);
                }
            }
            if (mLocalAudioTrack == null) {
                mLocalAudioTrack = createAudioTrack();
            }
            peerConnection.addTrack(mLocalAudioTrack, mediaStreamLabels);

            streamDescription.setAudioTrack(mLocalAudioTrack);
            streamDescription.setVideoTrack(mLocalVideoTrack);
        } else {

        }
        streamDescription.initEvent(remoteSinks,sdpMediaConstraints, streamDesState, events);

    }

    private PeerConnection getPC(long streamId) {
        return getStreamDes(streamId).getPc();
    }

    public StreamDescription getStreamDes(long streamId) {
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription == null) {
            throw new IllegalStateException("not found streamDescription ---> streamId: " +streamId);
        }
        return streamDescription;
    }

    public boolean isStreamInit(long streamId) {
        return mStreamDesMap.containsKey(streamId);
    }


    private File createRtcEventLogOutputFile() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault());
        Date date = new Date();
        final String outputFileName = "event_log_" + dateFormat.format(date) + ".log";
        return new File(appContext.getDir(RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE), outputFileName);
    }

    public void close() {
        executor.execute(this::closeInternal);
    }

    public void close(long streamId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                StreamDescription streamDescription = mStreamDesMap.get(streamId);
                if (streamDescription != null) {
                    streamDescription.close();
                    mStreamDesMap.remove(streamId);
                    events.onPeerConnectionClosed(streamDescription);
                }
            }
        });
    }

    private void closeInternal() {
        if (factory != null && peerConnectionParameters.aecDump) {
            factory.stopAecDump();
        }
        Log.d(TAG, "Closing peer connection.");
        statsTimer.cancel();
        if (dataChannel != null) {
            dataChannel.dispose();
            dataChannel = null;
        }

        Set<Map.Entry<Long, StreamDescription>> entries = mStreamDesMap.entrySet();
        for (Map.Entry<Long, StreamDescription> entry : entries) {
            entry.getValue().close();
        }
        mStreamDesMap.clear();
        Log.d(TAG, "Closing audio source.");
        if (mAudioSource != null) {
            mAudioSource.dispose();
            mAudioSource = null;
        }
        Log.d(TAG, "Stopping capture.");
        if (mVideoCapturer != null) {
            try {
                mVideoCapturer.stopCapture();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mVideoCapturer.dispose();
            mVideoCapturer = null;
        }
        Log.d(TAG, "Closing video source.");
        if (mVideoSource != null) {
            mVideoSource.dispose();
            mVideoSource = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        Log.d(TAG, "Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        rootEglBase.release();
        Log.d(TAG, "Closing peer connection done.");
        events.onPeerConnectionPoolDestroy();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
    }

    public void setAudioEnabled(long streamId, final boolean enable) {
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription != null) {
            streamDescription.setAudioEnabled(enable);
        } else {
            Log.e(TAG, "setAudioEnabled: 未找到 streamId："+ streamId + "对应streamDescription");
        }
    }

    public void setVideoEnabled(long streamId, final boolean enable) {
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription != null) {
            streamDescription.setVideoEnabled(enable);
        } else {
            Log.e(TAG, "setVideoEnabled: 未找到 streamId："+ streamId + "对应streamDescription");
        }
    }

    public boolean isVideoEnabled(long streamId) {
        return getStreamDes(streamId).isVideoEnable();
    }

    public boolean isAudioEnabled(long streamId) {
        return getStreamDes(streamId).isAudioEnable();
    }


    public void setRemoteDescription(long streamId,String sdp) {
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription != null) {
            streamDescription.setRemoteDescription(sdp);
        } else {
            Log.e(TAG, "setRemoteDescription: 未找到 streamId："+ streamId + "对应streamDescription");
        }
    }

    public void createOffer(long streamId){
        StreamDescription streamDescription = mStreamDesMap.get(streamId);
        if (streamDescription != null) {
            streamDescription.createOffer();
        } else {
            Log.e(TAG, "setRemoteDescription: 未找到 streamId："+ streamId + "对应streamDescription");
        }
    }

    private static String getSdpVideoCodecName(PeerConnectionEvents.PeerConnectionParameters parameters) {
        switch (parameters.videoCodec) {
            case VIDEO_CODEC_VP8:
                return VIDEO_CODEC_VP8;
            case VIDEO_CODEC_VP9:
                return VIDEO_CODEC_VP9;
            case VIDEO_CODEC_H264_HIGH:
            case VIDEO_CODEC_H264_BASELINE:
                return VIDEO_CODEC_H264;
            default:
                return VIDEO_CODEC_VP8;
        }
    }

    private static String getFieldTrials(PeerConnectionEvents.PeerConnectionParameters peerConnectionParameters) {
        String fieldTrials = "";
        if (peerConnectionParameters.videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL;
            Log.d(TAG, "Enable FlexFEC field trial.");
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
        if (peerConnectionParameters.disableWebRtcAGCAndHPF) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL;
            Log.d(TAG, "Disable WebRTC AGC field trial.");
        }
        return fieldTrials;
    }

    // Returns the remote VideoTrack, assuming there is only one.
    private @Nullable VideoTrack getRemoteVideoTrack(long streamId) {
        for (RtpTransceiver transceiver : getPC(streamId).getTransceivers()) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (track instanceof VideoTrack) {
                return (VideoTrack) track;
            }
        }
        return null;
    }

    private @Nullable AudioTrack getRemoteAudioTrack(long streamId) {
        for (RtpTransceiver transceiver : getPC(streamId).getTransceivers()) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (track instanceof AudioTrack) {
                return (AudioTrack) track;
            }
        }
        return null;
    }

    private boolean isVideoCallEnabled() {
        return peerConnectionParameters.videoCallEnabled;
    }

    @Nullable
    private AudioTrack createAudioTrack() {
        mAudioSource = factory.createAudioSource(audioConstraints);
        AudioTrack localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, mAudioSource);
        localAudioTrack.setEnabled(enableAudio);
        return localAudioTrack;
    }

    @Nullable
    private VideoTrack createVideoTrack(VideoCapturer capturer,VideoSink localRender) {
        mVideoCapturer = capturer;
        surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        mVideoSource = factory.createVideoSource(capturer.isScreencast());
        capturer.initialize(surfaceTextureHelper, appContext, mVideoSource.getCapturerObserver());
        capturer.startCapture(videoWidth, videoHeight, videoFps);

        VideoTrack localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, mVideoSource);
        localVideoTrack.setEnabled(renderVideo);
        localVideoTrack.addSink(localRender);

        Logging.d(TAG, "createVideoTrack: " + videoWidth + "x" + videoHeight + "@" + videoFps);

        return localVideoTrack;
    }

    public void setRemoteDescription(long streamId,SessionDescription sdp,SdpObserver sdpObserver) {
        Log.d(TAG, "setRemoteDescription: ");
        executor.execute(() -> getPC(streamId).setRemoteDescription(sdpObserver,sdp));
    }

    public void setLocalDescription(long streamId, SessionDescription sdp,SdpObserver sdpObserver) {
        executor.execute(() -> {
            getPC(streamId).setLocalDescription(sdpObserver,sdp);
        });
    }

    AudioDeviceModule createLegacyAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters.useOpenSLES) {
            Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
        }
        else {
            Log.d(TAG, "Allow OpenSL ES audio if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }

        if (peerConnectionParameters.disableBuiltInAEC) {
            Log.d(TAG, "Disable built-in AEC even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        }
        else {
            Log.d(TAG, "Enable built-in AEC if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
        }

        if (peerConnectionParameters.disableBuiltInNS) {
            Log.d(TAG, "Disable built-in NS even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        }
        else {
            Log.d(TAG, "Enable built-in NS if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        }


        // Set audio record error callbacks.
        WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecord.WebRtcAudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                //reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(WebRtcAudioRecord.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                reportError(errorMessage);
            }
        });

        WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.ErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(WebRtcAudioTrack.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
                reportError(errorMessage);
            }
        });

        return new LegacyAudioDeviceModule();
    }

    AudioDeviceModule createJavaAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters.useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.");
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                //reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                reportError(errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
                reportError(errorMessage);
            }
        };

        return JavaAudioDeviceModule.builder(appContext).setSamplesReadyCallback(null).setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC).setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS).setAudioRecordErrorCallback(audioRecordErrorCallback).setAudioTrackErrorCallback(audioTrackErrorCallback).createAudioDeviceModule();
    }

    private void reportError(long streamId,final String errorMessage) {
        Log.e(TAG, "Peerconnection error: " + errorMessage);
        executor.execute(() -> {
            events.onPeerConnectionError(streamId,errorMessage);

        });
    }

    private void reportError(final String errorMessage) {
        reportError(-1,errorMessage);
    }

    private class PCObserver implements PeerConnection.Observer {


        private StreamDescription mStreamDescription;

        public PCObserver(StreamDescription streamDescription) {
            mStreamDescription = streamDescription;
        }


        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            executor.execute(() -> events.onIceCandidate(candidate,mStreamDescription));
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            executor.execute(() -> events.onIceCandidatesRemoved(candidates));
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            Log.d(TAG, "SignalingState: " + newState);
        }

        @Override
        public void onIceConnectionChange(final IceConnectionState newState) {
            executor.execute(() -> {
                Log.d(TAG, "IceConnectionState: " + newState);
                if (newState == IceConnectionState.CONNECTED) {
                    events.onIceConnected();
                }
                else if (newState == IceConnectionState.DISCONNECTED) {
                    events.onIceDisconnected();
                }
                else if (newState == IceConnectionState.FAILED) {
                    reportError(mStreamDescription.getId(),"ICE connection failed.");
                }
            });
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            Log.d(TAG, "IceGatheringState: " + newState);
            if (PeerConnection.IceGatheringState.COMPLETE == newState) {
                events.onIceCompleted(mStreamDescription);
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.d(TAG, "IceConnectionReceiving changed to " + receiving);
        }

        @Override
        public void onAddStream(final MediaStream stream) {
            Log.w(TAG, "onAddStream: ---");
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {}

        @Override
        public void onDataChannel(final DataChannel dc) {
            Log.d(TAG, "New Data channel " + dc.label());

            if (!dataChannelEnabled) {
                return;
            }

            dc.registerObserver(new DataChannel.Observer() {
                @Override
                public void onBufferedAmountChange(long previousAmount) {
                    Log.d(TAG, "Data channel buffered amount changed: " + dc.label() + ": " + dc.state());
                }

                @Override
                public void onStateChange() {
                    Log.d(TAG, "Data channel state changed: " + dc.label() + ": " + dc.state());
                }

                @Override
                public void onMessage(final DataChannel.Buffer buffer) {
                    if (buffer.binary) {
                        Log.d(TAG, "Received binary msg over " + dc);
                        return;
                    }
                    ByteBuffer data = buffer.data;
                    final byte[] bytes = new byte[data.capacity()];
                    data.get(bytes);
                    String strData = new String(bytes, Charset.forName("UTF-8"));
                    Log.d(TAG, "Got msg: " + strData + " over " + dc);
                }
            });
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded------");
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        @Override
        public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
            Log.w(TAG, "onAddTrack: ---");
            if(mStreamDescription.isLocal() || receiver == null)return;
            MediaStreamTrack track = receiver.track();
            if (track instanceof VideoTrack) {
                List<VideoSink> remoteSinks = mStreamDescription.getRemoteSinks();
                for (VideoSink remoteSink : remoteSinks) {
                    ((VideoTrack) track).addSink(remoteSink);
                }
                mStreamDescription.setVideoTrack((VideoTrack) track);

            }

            if (track instanceof AudioTrack) {
                mStreamDescription.setAudioTrack((AudioTrack) track);
            }
        }
    }


    public void setVideoMaxBitrate(long streamId,@Nullable final Integer maxBitrateKbps,final Integer minBitrateKbps) {
        executor.execute(() -> {
            if (getPC(streamId) == null) {
                return;
            }
            Log.d(TAG, "Requested max video bitrate: " + maxBitrateKbps);
            if (localVideoSender == null) {
                Log.w(TAG, "Sender is not ready.");
                return;
            }

            RtpParameters parameters = localVideoSender.getParameters();
            if (parameters.encodings.size() == 0) {
                Log.w(TAG, "RtpParameters are not ready.");
                return;
            }

            for (RtpParameters.Encoding encoding : parameters.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps = maxBitrateKbps == null ? null : maxBitrateKbps * BPS_IN_KBPS;
                encoding.minBitrateBps = minBitrateKbps == null ? null : minBitrateKbps * BPS_IN_KBPS;

            }
            if (!localVideoSender.setParameters(parameters)) {
                Log.e(TAG, "RtpSender.setParameters failed.");
            }
            Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
        });
    }

    private void findVideoSender(long streamId) {
        PeerConnection pc = getPC(streamId);
        for (RtpSender sender : pc.getSenders()) {
            if (sender.track() != null) {
                String trackType = sender.track().kind();
                if (trackType.equals(VIDEO_TRACK_TYPE)) {
                    Log.d(TAG, "Found video sender.");
                    localVideoSender = sender;
                }
            }
        }
    }

    @SuppressWarnings("StringSplitter")
    private static String setStartBitrate(
            String codec, boolean isVideoCodec, String sdpDescription, int bitrateKbps) {
        String[] lines = sdpDescription.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                rtpmapLineIndex = i;
                break;
            }
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec + " codec");
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex]);

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                Log.d(TAG, "Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Update remote SDP line: " + lines[i]);
                sdpFormatUpdated = true;
                break;
            }
        }

        StringBuilder newSdpDescription = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                String bitrateSet;
                if (isVideoCodec) {
                    bitrateSet =
                            "a=fmtp:" + codecRtpMap + " " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
                            + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Add remote SDP line: " + bitrateSet);
                newSdpDescription.append(bitrateSet).append("\r\n");
            }
        }
        return newSdpDescription.toString();
    }
}
