package com.example.e_sholpine.utils;

public class ZengaPayRequest {
    private String msisdn;
    private int amount;
    private String external_reference;
    private String narration;
    private boolean charge_customer;

    public ZengaPayRequest(String msisdn, int amount, String external_reference, String narration, boolean charge_customer) {
        this.msisdn = msisdn;
        this.amount = amount;
        this.external_reference = external_reference;
        this.narration = narration;
        this.charge_customer = charge_customer;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getExternal_reference() {
        return external_reference;
    }

    public void setExternal_reference(String external_reference) {
        this.external_reference = external_reference;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public boolean isCharge_customer() {
        return charge_customer;
    }

    public void setCharge_customer(boolean charge_customer) {
        this.charge_customer = charge_customer;
    }
}
