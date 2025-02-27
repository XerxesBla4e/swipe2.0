package com.example.e_sholpine.utils;

import com.example.e_sholpine.model.TransactionResponse;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ZengaPayApiService {

    @GET("collections/{transactionReference}")
    Call<TransactionResponse> getTransactionDetails(
            @Path("transactionReference") String transactionReference
    );
}
