package com.alex.licode_android;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Created by dth
 * Des:
 * Date: 2019/3/6.
 */
public class ApiManager {
    private static final int    READ_TIME_OUT    = 15;
    private static final int    CONNECT_TIME_OUT = 15;
    private static final String BASE_URL         = "https://www.google.com/";

    private        Retrofit     mRetrofit;
    private        OkHttpClient mOkHttpClient;
    private        ApiService   mApiService;

    public static ApiManager getInstance() {
        return Holder.instance;
    }

    private static class Holder{
        private static ApiManager   instance = new ApiManager();
    }

    private ApiManager() {
        this.initOkHttp();
        this.initRetrofit();
        this.initApiService();
    }

    private void initOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS);
        builder.readTimeout(READ_TIME_OUT, TimeUnit.SECONDS);
        builder.writeTimeout(READ_TIME_OUT, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        builder.sslSocketFactory(SSLSocketClient.getSSLSocketFactory());
        builder.hostnameVerifier(SSLSocketClient.getHostnameVerifier());
        this.mOkHttpClient = builder.build();
    }


    private void initRetrofit() {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl(BASE_URL);
        builder.client(this.mOkHttpClient);
        this.mRetrofit = builder.build();
    }

    private void initApiService() {
        this.mApiService = mRetrofit.create(ApiService.class);
    }

    ApiService getApiService() {
        return this.mApiService;
    }

    public static class SSLSocketClient {
        public static SSLSocketFactory getSSLSocketFactory() {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, getTrustManager(), new SecureRandom());
                return sslContext.getSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static TrustManager[] getTrustManager() {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }};
            return trustAllCerts;
        }

        public static HostnameVerifier getHostnameVerifier() {
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            };
            return hostnameVerifier;
        }
    }
}
