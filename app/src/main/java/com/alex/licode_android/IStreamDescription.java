package com.alex.licode_android;

import android.view.View;

import org.json.JSONObject;

/**
 * Created by dth
 * Des:
 * Date: 2019/3/4.
 */
public abstract class IStreamDescription {

    protected JSONObject mAttributes;
    protected String mScreen;
    protected String mLabel;
    protected String mCube;
    protected boolean mData;
    protected boolean mVideo;
    protected boolean mAudio;
    protected long mId;
    protected               boolean                          isLocal  = true;//is local stream?

    public abstract void createOffer();

    public abstract void createAnswer();


    public abstract void setRemoteDescription(String sdp);

    public abstract View getSurfaceView();


    public JSONObject getAttributes() {
        return mAttributes;
    }

    public void setAttributes(JSONObject attributes) {
        mAttributes = attributes;
    }

    public String getScreen() {
        return mScreen;
    }

    public void setScreen(String screen) {
        mScreen = screen;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getCube() {
        return mCube;
    }

    public void setCube(String cube) {
        mCube = cube;
    }

    public boolean isData() {
        return mData;
    }

    public void setData(boolean data) {
        mData = data;
    }

    public boolean isVideo() {
        return mVideo;
    }

    public void setVideo(boolean video) {
        mVideo = video;
    }

    public boolean isAudio() {
        return mAudio;
    }

    public void setAudio(boolean audio) {
        mAudio = audio;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }


    public interface StreamDesState{
        void onStreamDesInit();
    }
}
