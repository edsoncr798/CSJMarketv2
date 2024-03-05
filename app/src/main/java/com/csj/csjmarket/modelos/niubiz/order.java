package com.csj.csjmarket.modelos.niubiz;

public class order {
    private int Id;
    private Double amount;
    private String authorizationCode;
    private Double authorizedAmount;
    private String currency;
    private String externalTransactionId;
    private int installment;
    private String purchaseNumber;
    private String traceNumber;
    private String transactionDate;
    private String transactionId;

    public int getId() {
        return Id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public Double getAuthorizedAmount() {
        return authorizedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public int getInstallment() {
        return installment;
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public String getTraceNumber() {
        return traceNumber;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
