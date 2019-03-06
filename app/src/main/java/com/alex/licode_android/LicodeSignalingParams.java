package com.alex.licode_android;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dth
 * Des:
 * Date: 2019/2/21.
 */
public class LicodeSignalingParams {

    /**
     * room authentication
     */
    //["success",{"streams":[{"id":215898481627865100,"audio":true,"video":true,"data":true,"label":"4HnChuqVaObdIhnhn9FU8cMGF4vYjKobtLfJ","screen":""},{"id":791246166528064000,"audio":true,"video":true,"data":true,"label":"8Ol3LPrh9j9Zmambjuo829nZClHFBl488NvZ","screen":""},{"id":473197316807737300,"audio":true,"video":true,"data":true,"attributes":{"cube":"10250000"}}],"id":"5c44f931d67eec013a0cbcf6","clientId":"1f9edc12-0c95-4052-b5b6-d28fb8a7d6bb","singlePC":false,"defaultVideoBW":300,"maxVideoBW":300,"iceServers":[{"url":"stun:stun.l.google.com:19302"}]}]
    public static class TokenParams {
        public long                           id;
        public String                         clientId;
        public int                            defaultVideoBW;
        public int                            maxVideoBW;
        public List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    }

    /**
     * publish
     */
    //[343409328971682560, "be726d9b-22a7-70a3-07c6-35e997d89de8"]
    public static class PublishParams {
        public long id;
        public String  clientId;
    }

    /**
     * subscribe
     */
    //[{"id":897386974165988700,"audio":true,"video":true,"data":true,"label":"P6BcLkOael57ZZWi1kZdETgzGsvAAu9ZjxHX","screen":""}]
    public static class SubscribeParams {
        public boolean isSelf;
        public boolean audio;
        public boolean video;
        public boolean data;
        public long     id;
        public String  label;
        public String  screen;
    }

}
