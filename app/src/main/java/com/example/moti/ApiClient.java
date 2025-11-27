package com.example.moti;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ApiClient {

    private static OkHttpClient client;

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}
