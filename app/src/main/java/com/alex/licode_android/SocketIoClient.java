package com.alex.licode_android;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

/**
 * Created by dth
 * Des:
 * Date: 2019/2/21.
 */
public class SocketIoClient implements LicodeSignalingService {

    private long mLocalStreamId;

    private enum ConnectionState {NEW, CONNECTING,CONNECTED, CLOSED, ERROR}

    private ConnectionState          roomState;
    private Handler                  handler;
    private SignalingEvents          events;
    private RoomConnectionParameters connectionParameters;
    private Socket                   mSocket;//socket.io 对象


    public SocketIoClient(SignalingEvents events) {
        this.events = events;
        roomState = ConnectionState.NEW;
        final HandlerThread handlerThread = new HandlerThread("SocketIoClient");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void connectToRoom(final RoomConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        handler.post(new Runnable() {
            @Override
            public void run() {
                connectToRoomInternal(connectionParameters);
            }
        });
    }

    @Override
    public void sendOfferSdp(String sdp,IStreamDescription streamDescription) {
        try {
            JSONObject wrapperJson = new JSONObject();
            JSONObject msgJson = new JSONObject();
            JSONObject configJson = new JSONObject();
            configJson.put("maxVideoBW", 300);
            msgJson.put("config",configJson);
            msgJson.put("type","offer");
            msgJson.put("sdp", sdp);
            wrapperJson.put("msg", msgJson);
            //            wrapperJson.put("browser", "chrome-stable");
            wrapperJson.put("streamId", streamDescription.getId());

            mSocket.emit("signaling_message", wrapperJson, null);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAnswerSdp(String sdp) {

    }

    @Override
    public void sendLocalIceCandidate(JSONObject jsonObject,IStreamDescription streamDescription) {

        try {
            JSONObject wrapperJson = new JSONObject();
            JSONObject msgJson = new JSONObject();
            msgJson.put("candidate", jsonObject);
            msgJson.put("type","candidate");
            wrapperJson.put("msg", msgJson);
            //            wrapperJson.put("browser", "chrome-stable");
            wrapperJson.put("streamId", streamDescription.getId());

            mSocket.emit("signaling_message", wrapperJson, null);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void disconnectFromRoom() {

    }

    // Connects to room - function runs on a local looper thread.
    private void connectToRoomInternal(RoomConnectionParameters parameters) {
        VLog.d("Connect to room: " + parameters.host);
        if (roomState == ConnectionState.CONNECTING || roomState == ConnectionState.CONNECTED) {
            VLog.w("socket.io: " + roomState);
            return;
        }
        roomState = ConnectionState.NEW;
        // set as an option
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.reconnection = true;
        opts.reconnectionAttempts = 3;
        opts.secure = parameters.secure;
        opts.transports = new String[]{WebSocket.NAME};
        try {
            mSocket = IO.socket(parameters.host, opts);
            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_CONNECT: ");
                    roomState = ConnectionState.CONNECTED;
                    handleSocketConnect(args);
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_DISCONNECT");
                    roomState = ConnectionState.CLOSED;
                }

            }).on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_CONNECTING: ");
                    roomState = ConnectionState.CONNECTING;
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_CONNECT_ERROR");
                    roomState = ConnectionState.ERROR;
                    onFailed("socket connect failed");
                }
            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_CONNECT_TIMEOUT");
                    roomState = ConnectionState.ERROR;
                    onFailed("socket connect timeout");

                }
            }).on(Socket.EVENT_RECONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_RECONNECT_ERROR");
                    roomState = ConnectionState.ERROR;
                }
            }).on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_RECONNECT_FAILED");
                    roomState = ConnectionState.ERROR;
                }
            }).on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_RECONNECTING");
                    roomState = ConnectionState.CONNECTING;
                }
            }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    VLog.i("Socket EVENT_MESSAGE");
                }
            });
            subscribeLicodeEvents();
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }



    private void handleSocketConnect(Object... args) {
        try {
            JSONObject jsonToken = new JSONObject(connectionParameters.token);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("singlePC", false);
            jsonObj.put("token", jsonToken);
            mSocket.emit("token", jsonObj, new Ack() {
                @Override
                public void call(Object... args) {
                    JSONArray result = transformJsonArray(args);
                    VLog.i("token: " + result);
                    try {
                        // ["success",{"streams":[{"id":915526718819328000,"audio":true,"video":true,"data":true,"label":"GxISR1uAS6xrU0AhWIGbZ3sJrnCwcgrJySaw","screen":""},{"id":789546779165138800,"audio":true,"video":true,"data":true,"label":"BZ3Jxusi35rcTtY24ekAGdp7TpdNw3RmBWTN","screen":""},{"id":847426050597384200,"audio":true,"video":true,"data":false,"label":"fbs6IIKpwrLrkk5606mWYRHwQ6bfLBP27P5w","attributes":{"cube":"10255988"}},{"id":960787325312862300,"audio":true,"video":true,"data":false,"label":"SJO05v8cvAghuLWpmBPZthXPh6l5zK2VSvV5","attributes":{"cube":"10255987"}},{"id":770559376548028000,"audio":true,"video":true,"data":false,"label":"CTgau97IL73C5wPdR312HEIj6UEJqj5tIQiZ","attributes":{"cube":"10256000"}}],"id":"5c6b63bfb61b7c020214c82d","clientId":"b0a96c5b-fcff-49d6-af81-6e91c88e0c1b","singlePC":false,"defaultVideoBW":300,"maxVideoBW":300,"iceServers":[{"url":"stun:stun.l.google.com:19302"}]}]
                        if (!"success".equalsIgnoreCase(result.getString(0))) {
                            return;
                        }

                        JSONObject jsonObject = result.getJSONObject(1);

                        LicodeSignalingParams.TokenParams tokenParams = new LicodeSignalingParams.TokenParams();
                        if (jsonObject.has("iceServers")) {
                            JSONArray iceServers = jsonObject.optJSONArray("iceServers");
                            if (iceServers != null) {
                                for (int i = 0; i < iceServers.length(); i++) {
                                    JSONObject obj = (JSONObject) iceServers.get(i);
                                    String url = obj.optString("url");
                                    if (!TextUtils.isEmpty(url)) {
                                        tokenParams.iceServers.add(new PeerConnection.IceServer(url));
                                    }
                                }
                            }
                        }
                        if (jsonObject.has("defaultVideoBW")) {
                            tokenParams.defaultVideoBW = jsonObject.optInt("defaultVideoBW");
                        }
                        if (jsonObject.has("maxVideoBW")) {
                            tokenParams.maxVideoBW = jsonObject.optInt("maxVideoBW");
                        }

                        List<IStreamDescription> streams = new ArrayList<>();
                        if (jsonObject.has("streams")) {
                            JSONArray streamArray = jsonObject.optJSONArray("streams");
                            for (int i = 0; i < streamArray.length(); i++) {
                                JSONObject streamObj = streamArray.getJSONObject(i);
                                IStreamDescription streamDescription = StreamDescription.parseJson(streamObj, false);
                                streams.add(streamDescription);
                            }
                        }

                        tokenParams.id = jsonObject.optLong("id");
                        tokenParams.clientId = jsonObject.optString("clientId");

                        if (events != null) {
                            events.onConnectedToRoom(tokenParams);
                        }

                        sendPublish();

                        for (IStreamDescription stream : streams) {
                            sendSubscribe(stream);
                        }


                    } catch (JSONException e) {
                    }
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送publish
     * @throws JSONException
     */
    private void sendPublish() throws JSONException {
        JSONObject publishJson = new JSONObject();
        publishJson.put("state","erizo");
        publishJson.put("label","ARDAMS");
        publishJson.put("data",true);
        publishJson.put("audio",true);
        publishJson.put("video",true);
        JSONObject muteJson = new JSONObject();
        muteJson.put("audio",false);
        muteJson.put("video",false);
        publishJson.put("muteStream", muteJson);
        publishJson.put("minVideoBW", 0);
        mSocket.emit("publish", publishJson, null, new Ack() {
            @Override
            public void call(Object... args) {

                JSONArray jsonArray = transformJsonArray(args);
                VLog.i("publish: " + jsonArray);
                LicodeSignalingParams.PublishParams publishParams = new LicodeSignalingParams.PublishParams();

                publishParams.id = jsonArray.optLong(0);
                publishParams.clientId = jsonArray.optString(1);
                mLocalStreamId = publishParams.id;
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", publishParams.id);
                    IStreamDescription streamDescription = StreamDescription.parseJson(jsonObject, true);
                    events.onPublish(streamDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendUnpublish() {
        JSONObject unpublishJson = new JSONObject();
        try {
            unpublishJson.put("streamId", mLocalStreamId);

            mSocket.emit("unpublish", unpublishJson, new Ack() {
                @Override
                public void call(Object... args) {

                    JSONArray array = transformJsonArray(args);
                    VLog.i("unpublish: " +array);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendSubscribe(IStreamDescription params) throws JSONException {
        JSONObject subscribeJson = new JSONObject();
        subscribeJson.put("streamId",params.getId());
        subscribeJson.put("data",params.isData());
        subscribeJson.put("audio",params.isAudio());
        subscribeJson.put("video",params.isVideo());
        JSONObject muteJson = new JSONObject();
        muteJson.put("audio",false);
        muteJson.put("video",false);
        subscribeJson.put("muteStream", muteJson);
        subscribeJson.put("minVideoBW", 0);

        VLog.d("sendSubscribe: "  + subscribeJson);
        mSocket.emit("subscribe", subscribeJson, null, new Ack() {
            @Override
            public void call(Object... args) {

                JSONArray jsonArray = transformJsonArray(args);
                VLog.i("subscribe: " + jsonArray);
                if (events != null) {
                    events.onSubscribe(params);
                }
            }
        });
    }

    private JSONArray transformJsonArray(Object... args) {
        JSONArray jsonArray = new JSONArray();
        if (args == null) {
            return jsonArray;
        }

        for (Object arg : args) {
            jsonArray.put(arg);
        }

        return jsonArray;
    }

    @Override
    public void close() {
        if (mSocket != null) {
            mSocket.close();
            events = null;
            mSocket = null;
        }
        mLocalStreamId = 0;
        roomState = ConnectionState.CLOSED;
    }

    /**
     * 订阅licode信令相关事件
     */
    private void subscribeLicodeEvents() {
        mSocket.on("signaling_message_erizo", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray jsonArray = transformJsonArray(args);
                VLog.i("signaling_message_erizo: "+jsonArray);

                try {

                    JSONObject jsonObject = jsonArray.optJSONObject(0);
                    JSONObject mess = jsonObject.optJSONObject("mess");
                    boolean answer = "answer".equals(mess.optString("type"));
                    if (!answer) {
                        VLog.i("expected ANSWER, got: " + mess.getString("type"));
                        return;
                    }
                    String sdp = mess.optString("sdp");

                    long streamId;
                    if (jsonObject.has("streamId")) {
                        streamId = jsonObject.optLong("streamId");
                    } else {
                        streamId = jsonObject.optLong("peerId");
                    }

                    if (events != null) {
                        events.onRemoteDescription(streamId,sdp);
                    }


                } catch (JSONException e1) {
                }
            }
        });

        mSocket.on("onAddStream", new Emitter.Listener() {

            @Override
            public void call(Object... args) {

                JSONArray array = transformJsonArray(args);
                VLog.d("onAddStream: "+array);
                try {
                    JSONObject jsonObject = array.getJSONObject(0);
                    long id = jsonObject.optLong("id");
                    if (id == mLocalStreamId) {
                        VLog.d("不订阅自己的发布流");
                        return;
                    }
                    IStreamDescription streamDescription = StreamDescription.parseJson(jsonObject, false);
                    sendSubscribe(streamDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mSocket.on("onRemoveStream", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray data = transformJsonArray(args);
                VLog.d("onRemoveStream: "+data);
                JSONObject jsonObject = data.optJSONObject(0);
                long streamId = jsonObject.optLong("id");
                if (events != null) {
                    events.onUnSubscribe(streamId);
                }
            }
        });

        mSocket.on("onDataStream", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray data = transformJsonArray(args);
                VLog.d("onDataStream: "+data);
            }
        });

        mSocket.on("signaling_message_peer", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray data = transformJsonArray(args);
                VLog.d("signaling_message_peer: "+data);
            }
        });

        mSocket.on("onUpdateAttributeStream", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray data = transformJsonArray(args);
                VLog.d("onUpdateAttributeStream: "+data);
            }
        });

    }

    private void onFailed(String des) {
        if (events != null) {
            events.onChannelError(des);
        }
    }
}
