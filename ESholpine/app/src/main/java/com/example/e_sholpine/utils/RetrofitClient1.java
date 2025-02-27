package com.example.e_sholpine.utils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient1 {
    private static final String BASE_URL = "https://api.zengapay.com/v1/";
    private static RetrofitClient1 instance;
    private Retrofit retrofit;

    // Private constructor for Singleton pattern
    private RetrofitClient1() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // Singleton instance method
    public static synchronized RetrofitClient1 getInstance() {
        if (instance == null) {
            instance = new RetrofitClient1();
        }
        return instance;
    }

    // Method to get the API service
    public ZengaPayApiService getApiService() {
        return retrofit.create(ZengaPayApiService.class);
    }
}
