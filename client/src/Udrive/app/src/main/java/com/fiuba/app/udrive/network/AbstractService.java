package com.fiuba.app.udrive.network;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.fiuba.app.udrive.BuildConfig;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public abstract class AbstractService {

    protected Context mContext;

    private static RestAdapter.Builder builder;/* = new RestAdapter.Builder()
            .setEndpoint(ConnectionConfig.getConnectionURL(this.mContext))
            .setClient(new OkClient(new OkHttpClient()))
            .setLogLevel(BuildConfig.RETROFIT_LOGGING);*/

    public AbstractService(Context context) {
        this.mContext = context;
        //ConnectionConfig.clear(context);
        this.builder = new RestAdapter.Builder()
                .setEndpoint(ConnectionConfig.getConnectionURL(this.mContext))
                .setClient(new OkClient(new OkHttpClient()))
                .setLogLevel(BuildConfig.RETROFIT_LOGGING);
    }


    protected <T> T createService(Class<T> service, final String token) {
        if (token != null){
            builder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Accept", "application/json");
                    request.addHeader("Authorization", token);
                }
            });
        }
        return builder.build().create(service);
    }

    protected static String getURL(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences("connection", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String serverIP = sharedPref.getString("server_ip", "190.7.56.249");
        String serverPort = sharedPref.getString("server_port", "8055");
        return "http://"+serverIP+":"+serverPort;
    }

}
