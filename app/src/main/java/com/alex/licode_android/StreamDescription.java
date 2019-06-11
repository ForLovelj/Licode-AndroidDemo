package com.alex.licode_android;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dth
 * Des: licode stream
 * Date: 2019/2/27.
 */
public class StreamDescription extends IStreamDescription {


    public static final String            TAG = "StreamDescription";
    private              PeerConnection   mPc;
    private              AudioTrack       mAudioTrack;
    private              VideoTrack       mVideoTrack;
    private              SDPObserver      mSdpObserver;
    private              MediaConstraints mMediaConstraints;
    private ExecutorService               executor;
    private StreamDesState                mStreamDesState;
    private SessionDescription            localSdp;
    private PeerConnectionEvents          mEvents;
    private SurfaceViewRenderer           mLocalRender;
    private SurfaceViewRenderer           mRemoteRender;
    private List<VideoSink>               mRemoteSinks;

    public static StreamDescription parseJson(JSONObject streamObj,boolean isLocal) {
        String cubeId = null;
        String conferenceId = null;

        boolean audio = streamObj.optBoolean("audio",true);
        boolean video = streamObj.optBoolean("video",true);
        boolean data = streamObj.optBoolean("data",true);
        long id = streamObj.optLong("id");
        String label = streamObj.optString("label");
        String screen = streamObj.optString("screen");
        JSONObject attr = streamObj.optJSONObject("attributes");
        if (attr != null) {
            cubeId = attr.optString("cubeId");
            conferenceId = attr.optString("conferenceId");
        }

        return new StreamDescription(id, data, video, audio, screen, attr,label, cubeId,conferenceId,isLocal);
    }

    public StreamDescription(long id, boolean data, boolean video, boolean audio, String screen, JSONObject attr, String label, String cubeId,String conferenceId, boolean isLocal) {
        mId = id;
        mData = data;
        mVideo = video;
        mAudio = audio;
        mScreen = screen;
        mLabel = label;
//        mCubeId = cubeId;
//        mConferenceId = conferenceId;
        this.isLocal = isLocal;
        if (attr != null) {
            mAttributes = attr;
        }
    }


    public void initPC(PeerConnection pc) {
        if (mPc != null) {
            return;
        }
        executor = Executors.newSingleThreadExecutor();
        mPc = pc;
    }

    public void initEvent(List<VideoSink> remoteSinks,MediaConstraints mediaConstraints, StreamDesState streamDesState, PeerConnectionEvents events) {
        mRemoteSinks = remoteSinks;
        mSdpObserver = new SDPObserver();
        mMediaConstraints = mediaConstraints;
        mStreamDesState = streamDesState;
        mEvents = events;
        if (mStreamDesState != null) {
            mStreamDesState.onStreamDesInit();
        }
        createOffer();
    }

    @Override
    public void createOffer() {
        Log.d(TAG, "createOffer: ");
        localSdp = null;
        getPc().createOffer(mSdpObserver, mMediaConstraints);
        //        executor.execute(() -> getPc().createOffer(mSdpObserver, mMediaConstraints));
    }

    @Override
    public void createAnswer() {
        getPc().createAnswer(mSdpObserver, mMediaConstraints);
        //        executor.execute(() -> getPc().createAnswer(mSdpObserver, mMediaConstraints));
    }

    @Override
    public void setRemoteDescription(String sdp) {
        Log.d(TAG, "setRemoteDescription: ");
        SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        getPc().setRemoteDescription(mSdpObserver,remoteSdp);
        //        executor.execute(() -> {
        //            SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        //            getPc().setRemoteDescription(mSdpObserver,remoteSdp);
        //        });
    }

    public void setLocalDescription(SessionDescription sdp) {
        getPc().setLocalDescription(mSdpObserver,sdp);
        //        executor.execute(() -> {
        //            getPc().setLocalDescription(mSdpObserver,sdp);
        //        });
    }

    public void setAudioEnabled(final boolean enable) {
        if (mAudioTrack != null) {
            mAudioTrack.setEnabled(enable);
        }
        //        executor.execute(() -> {
        //            if (mAudioTrack != null) {
        //                mAudioTrack.setEnabled(enable);
        //            }
        //        });
    }

    public void setVideoEnabled(final boolean enable) {
        if (mVideoTrack != null) {
            mVideoTrack.setEnabled(enable);
        }
        //        executor.execute(() -> {
        //            if (mVideoTrack != null) {
        //                mVideoTrack.setEnabled(enable);
        //            }
        //        });
    }


    public List<VideoSink> getRemoteSinks() {
        return mRemoteSinks;
    }

    public void close() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        if (mLocalRender != null) {
            mLocalRender.release();
            mLocalRender = null;
        }
        if (mRemoteRender != null) {
            mRemoteRender.release();
            mRemoteRender = null;
        }
        if (mPc != null) {
            getPc().dispose();
            mPc = null;
        }

        mAudioTrack = null;
        mVideoTrack = null;
        mRemoteSinks = null;
        mSdpObserver = null;
        mEvents = null;
        mStreamDesState = null;
    }

    public void bindView(SurfaceViewRenderer localRender,SurfaceViewRenderer remoteRender) {
        mLocalRender = localRender;
        mRemoteRender = remoteRender;
    }

    @Override
    public View getSurfaceView() {
        if (isLocal) {
            if (mLocalRender != null) {
                ViewGroup parent = (ViewGroup) mLocalRender.getParent();
                if (parent != null) {
                    parent.removeView(mLocalRender);
                }
            }
            return mLocalRender;
        } else {
            if (mRemoteRender != null) {
                ViewGroup parent = (ViewGroup) mRemoteRender.getParent();
                if (parent != null) {
                    parent.removeView(mRemoteRender);
                }
            }
            return mRemoteRender;
        }
    }

    public void setVideoTrack(VideoTrack videoTrack) {
        mVideoTrack = videoTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
    }

    public class SDPObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            if (localSdp != null) {
                return;
            }
            localSdp = origSdp;
            setLocalDescription(origSdp);
        }

        @Override
        public void onSetSuccess() {
            executor.execute(() -> {
                if (getPc() == null) {
                    return;
                }

                // For offering peer connection we first create offer and set
                // local SDP, then after receiving answer set remote SDP.
                if (getPc().getRemoteDescription() == null) {
                    // We've just set our local SDP so time to send it.
                    Log.d(TAG, "Local SDP set succesfully");
                    mEvents.onLocalDescription(localSdp,StreamDescription.this);
                }
                else {
                    // We've just set remote description, so drain remote
                    // and send local ICE candidates.
                    Log.d(TAG, "Remote SDP set succesfully");
                }

            });
        }

        @Override
        public void onCreateFailure(final String error) {
            Log.e(TAG, "onCreateFailure: " +error);
        }

        @Override
        public void onSetFailure(final String error) {
            Log.e(TAG, "onSetFailure: " +error);
        }
    }

    public PeerConnection getPc() {
        if (mPc == null) {
            throw new IllegalStateException("StreamDescription not init!");
        }
        return mPc;
    }

    public boolean isVideoEnable() {
        return mVideoTrack != null && mVideoTrack.enabled();
    }

    public boolean isAudioEnable() {
        return mAudioTrack != null && mAudioTrack.enabled();
    }

}
