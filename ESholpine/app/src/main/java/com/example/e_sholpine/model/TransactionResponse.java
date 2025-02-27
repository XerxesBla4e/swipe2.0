package com.example.e_sholpine.model;

import com.google.gson.annotations.SerializedName;

public class TransactionResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private TransactionData data;

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public TransactionData getData() { return data; }
    public void setData(TransactionData data) { this.data = data; }

    // Inner class for the 'data' object
    public class TransactionData {

        @SerializedName("transactionSystemId")
        private String transactionSystemId;

        @SerializedName("transactionReference")
        private String transactionReference;

        @SerializedName("transactionStatus")
        private String transactionStatus;

        @SerializedName("amount")
        private int amount;

        @SerializedName("msisdn")
        private String msisdn;

        @SerializedName("channel")
        private String channel;

        @SerializedName("customerCharged")
        private boolean customerCharged;

        @SerializedName("currencyCode")
        private String currencyCode;

        @SerializedName("currencyName")
        private String currencyName;

        @SerializedName("MNOTransactionReferenceId")
        private String MNOTransactionReferenceId;

        @SerializedName("transactionExternalReference")
        private String transactionExternalReference;

        @SerializedName("transactionExternalNarrative")
        private String transactionExternalNarrative;

        @SerializedName("transactionInitiationDate")
        private String transactionInitiationDate;

        @SerializedName("transactionCompletionDate")
        private String transactionCompletionDate;

        @SerializedName("transactionEntryGeneralType")
        private String transactionEntryGeneralType;

        @SerializedName("transactionEntryDesignation")
        private String transactionEntryDesignation;

        @SerializedName("transactionEntrySpecificType")
        private String transactionEntrySpecificType;

        public String getTransactionSystemId() {
            return transactionSystemId;
        }

        public void setTransactionSystemId(String transactionSystemId) {
            this.transactionSystemId = transactionSystemId;
        }

        public String getTransactionReference() {
            return transactionReference;
        }

        public void setTransactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
        }

        public String getTransactionStatus() {
            return transactionStatus;
        }

        public void setTransactionStatus(String transactionStatus) {
            this.transactionStatus = transactionStatus;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getMsisdn() {
            return msisdn;
        }

        public void setMsisdn(String msisdn) {
            this.msisdn = msisdn;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public boolean isCustomerCharged() {
            return customerCharged;
        }

        public void setCustomerCharged(boolean customerCharged) {
            this.customerCharged = customerCharged;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public String getCurrencyName() {
            return currencyName;
        }

        public void setCurrencyName(String currencyName) {
            this.currencyName = currencyName;
        }

        public String getMNOTransactionReferenceId() {
            return MNOTransactionReferenceId;
        }

        public void setMNOTransactionReferenceId(String MNOTransactionReferenceId) {
            this.MNOTransactionReferenceId = MNOTransactionReferenceId;
        }

        public String getTransactionExternalReference() {
            return transactionExternalReference;
        }

        public void setTransactionExternalReference(String transactionExternalReference) {
            this.transactionExternalReference = transactionExternalReference;
        }

        public String getTransactionExternalNarrative() {
            return transactionExternalNarrative;
        }

        public void setTransactionExternalNarrative(String transactionExternalNarrative) {
            this.transactionExternalNarrative = transactionExternalNarrative;
        }

        public String getTransactionInitiationDate() {
            return transactionInitiationDate;
        }

        public void setTransactionInitiationDate(String transactionInitiationDate) {
            this.transactionInitiationDate = transactionInitiationDate;
        }

        public String getTransactionCompletionDate() {
            return transactionCompletionDate;
        }

        public void setTransactionCompletionDate(String transactionCompletionDate) {
            this.transactionCompletionDate = transactionCompletionDate;
        }

        public String getTransactionEntryGeneralType() {
            return transactionEntryGeneralType;
        }

        public void setTransactionEntryGeneralType(String transactionEntryGeneralType) {
            this.transactionEntryGeneralType = transactionEntryGeneralType;
        }

        public String getTransactionEntryDesignation() {
            return transactionEntryDesignation;
        }

        public void setTransactionEntryDesignation(String transactionEntryDesignation) {
            this.transactionEntryDesignation = transactionEntryDesignation;
        }

        public String getTransactionEntrySpecificType() {
            return transactionEntrySpecificType;
        }

        public void setTransactionEntrySpecificType(String transactionEntrySpecificType) {
            this.transactionEntrySpecificType = transactionEntrySpecificType;
        }
    }
}
