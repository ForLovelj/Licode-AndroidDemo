package com.alex.licode_android;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.alex.licode_android.runtimepermission.PermissionsManager;
import com.alex.licode_android.runtimepermission.PermissionsResultAction;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private String mTokenServerUrl = "https://192.168.1.200:3004/createToken/";

    private FrameLayout                                     mLocalContainer;
    private LicodeStream                                    mLicodeStream;
    private RecyclerView                                    mRecyclerView;
    private LicodeAdapter                                   mLicodeAdapter;
    public  boolean                                         isStarted;
    private Button                                          mButton;
    private LicodeSignalingService.RoomConnectionParameters mRoomConnectionParameters;
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        mLocalContainer = (FrameLayout) findViewById(R.id.local_container);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        closeDefaultAnimator(mRecyclerView);
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(v -> {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastClickTime < 3000) {
                return;
            }
            lastClickTime = currentTimeMillis;
            if (isStarted) {
                mLicodeStream.close();
                isStarted = false;
            } else {
                login();
            }
        });

        mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mLicodeAdapter = new LicodeAdapter();
        mRecyclerView.setAdapter(mLicodeAdapter);



    }

    private void initData(String token) {

        CubeConfig cubeConfig = new CubeConfig();
        mLicodeStream = LicodeStream.getInstance();
        if(mLicodeStream == null)return;
        mLicodeStream.init(this,cubeConfig);
        mLicodeStream.setStreamEvents(new LicodeStream.StreamEvents() {
            @Override
            public void onViewCreate(long streamId, View surfaceView,boolean isLocal) {
                if (isLocal) {
                    mLocalContainer.removeAllViews();
                    mLocalContainer.addView(surfaceView);
                } else {
                    mLicodeAdapter.addData(surfaceView);
                }
            }

            @Override
            public void onViewDestroy(long streamId, View surfaceView) {
                int position = findPosition(surfaceView);
                if (position != -1) {
                    mLicodeAdapter.remove(position);
                }
            }

            @Override
            public void close() {
                mLicodeAdapter.getData().clear();
                mLicodeAdapter.notifyDataSetChanged();
                mLocalContainer.removeAllViews();
                mButton.setText("start");
            }
        });

        try {
            JSONObject jsonToken = new JSONObject(token);
            String host = jsonToken.optString("host");
            String tokenId = jsonToken.optString("tokenId");
            String signature = jsonToken.optString("signature");
            boolean secure = jsonToken.optBoolean("secure");
            if (!host.startsWith("http://")) {
                host = "http://" + host;
            }
            mRoomConnectionParameters = new LicodeSignalingService.RoomConnectionParameters(token, host, tokenId, secure, signature);

            startCall();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int findPosition(View surfaceView) {
        List<View> data = mLicodeAdapter.getData();
        int position = data.indexOf(surfaceView);
        return position;
    }

    private void startCall() {
        mLicodeStream.connectToRoom(mRoomConnectionParameters);
        isStarted = true;
        mButton.setText("close");
    }


    class LicodeAdapter extends BaseQuickAdapter<View, BaseViewHolder> {

        public LicodeAdapter() {
            super(R.layout.item_licode);
        }

        @Override
        protected void convert(BaseViewHolder helper, View item) {
            FrameLayout frameLayout = helper.getView(R.id.container);
            ViewGroup parent = (ViewGroup) item.getParent();
            if (parent != null) {
                parent.removeView(item);
            }
            frameLayout.addView(item);
        }
    }

    private void login() {
        Map<String, String> params = new HashMap<>();
        params.put("username", "user");
        params.put("role", "presenter");
        params.put("room", "basicExampleRoom");
        params.put("type", "erizo");
        params.put("mediaConfiguration", "default");
        ApiManager.getInstance().getApiService().login(params,mTokenServerUrl).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.code() == 200) {

                        String string = response.body().string();
                        VLog.d("body: ---- " +string);
                        String decodeToken = decodeToken(string);
                        initData(decodeToken);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }

    private  String decodeToken(String result) {
        try {
            String token = new String(Base64.decode(result.getBytes(),
                    Base64.DEFAULT), "UTF-8");
            VLog.i("Licode token decoded: " + token);
            return token;
        } catch (UnsupportedEncodingException e) {
            VLog.i("Failed to decode token: " + e.getMessage());
        }
        return null;
    }

    @TargetApi(23)
    private void requestPermissions() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {

            }

            @Override
            public void onDenied(String permission) {
                VLog.d("permission: "+permission);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    public void closeDefaultAnimator(RecyclerView recyclerView) {
        recyclerView.getItemAnimator().setAddDuration(0);
        recyclerView.getItemAnimator().setChangeDuration(0);
        recyclerView.getItemAnimator().setMoveDuration(0);
        recyclerView.getItemAnimator().setRemoveDuration(0);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }
}
