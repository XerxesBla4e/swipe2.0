package com.example.e_sholpine.utils;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;



// Define the API request structure
public interface ZengaPayApi {


    @Headers({
            "Authorization: Bearer ZPYPUBK-4a0906412986165fe12d382fcba9e06fc7d58f92c24f669c8d4d929147d6f619",
            "Content-Type: application/json"
    })
    @POST("v1/collections")
    Call<ZengaPayResponse> collectPayment(@Body ZengaPayRequest request);
}
