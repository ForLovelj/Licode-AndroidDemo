package com.alex.licode_android;

import org.json.JSONObject;

/**
 * Created by dth
 * Des:
 * Date: 2019/2/21.
 */
public interface LicodeSignalingService {

    /**
     * Struct holding the connection parameters of an AppRTC room.
     */
    class RoomConnectionParameters {
        public final String token;
        public final String host;
        public final String tokenId;
        public final boolean secure;
        public final String signature;
        public RoomConnectionParameters(String token,String host, String tokenId, boolean secure, String signature) {
            this.token = token;
            this.host = host;
            this.tokenId = tokenId;
            this.secure = secure;
            this.signature = signature;
        }
        public RoomConnectionParameters(String token) {
            this(token, null, null, false ,null/* urlParameters */);
        }
    }

    /**
     * Asynchronously connect to an AppRTC room URL using supplied connection
     * parameters. Once connection is established onConnectedToRoom()
     * callback with room parameters is invoked.
     */
    void connectToRoom(RoomConnectionParameters connectionParameters);

    /**
     * Send offer SDP to the other participant.
     */
    void sendOfferSdp(final String sdp, IStreamDescription streamDescription);

    /**
     * Send answer SDP to the other participant.
     */
    void sendAnswerSdp(final String sdp);

    /**
     * Send Ice candidate to the other participant.
     */
    void sendLocalIceCandidate(final JSONObject candidate, IStreamDescription streamDescription);

    /**
     * Disconnect from room.
     */
    void disconnectFromRoom();

    void close();


    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    interface SignalingEvents {
        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        void onConnectedToRoom(final LicodeSignalingParams.TokenParams params);

        /**
         * Callback fired once remote SDP is received.
         */
        void onRemoteDescription(long streamId, final String sdp);

        /**
         * Callback fired once channel error happened.
         */
        void onChannelError(final String description);

        void onPublish(IStreamDescription streamDescription);

        void onSubscribe(IStreamDescription streamDescription);

        void onUnSubscribe(long streamId);
    }
}
