package com.example.e_sholpine.utils;

public class ZengaPayResponse {

    private String status;
    private String message;
    private String transactionReference;


    public ZengaPayResponse() {
    }

    public ZengaPayResponse(String status, String message, String transactionReference) {
        this.status = status;
        this.message = message;
        this.transactionReference = transactionReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
}
