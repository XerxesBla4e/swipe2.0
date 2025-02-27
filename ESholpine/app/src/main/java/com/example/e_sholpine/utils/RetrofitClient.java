package com.example.e_sholpine.utils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance = null;
    private Retrofit retrofit;
    private static final String BASE_URL = "https://api.zengapay.com/";

    private RetrofitClient() {
        // Create the Retrofit instance
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())  // Use Gson to parse JSON
                .build();
    }

    // Singleton pattern to get the instance
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    // Expose the API Service interface
    public ZengaPayApi getApi() {
        return retrofit.create(ZengaPayApi.class);
    }
}
