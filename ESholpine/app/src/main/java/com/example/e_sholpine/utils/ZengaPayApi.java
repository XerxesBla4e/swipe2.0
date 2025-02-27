package com.example.e_sholpine.utils;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;



// Define the API request structure
public interface ZengaPayApi {


    @Headers({
            "Authorization: Bearer ZPYPUBK-2dc39841062ed9f2a2e0d3b379cee2dd98b4c08d031da877e71f868a780ff48d",
            "Content-Type: application/json"
    })
    @POST("v1/collections")
    Call<ZengaPayResponse> collectPayment(@Body ZengaPayRequest request);
}
