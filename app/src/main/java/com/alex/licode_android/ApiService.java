package com.alex.licode_android;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by dth
 * Des:
 * Date: 2019/3/6.
 */
public interface ApiService {

    @POST
    @FormUrlEncoded
    Call<ResponseBody> login(@FieldMap Map<String, String> params, @Url String url);
}
