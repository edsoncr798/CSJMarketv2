package com.csj.csjmarket.modelos.niubiz;

import java.sql.Timestamp;

public class dataMap {
    private int id;
    private Timestamp hora;
    private String TERMINAL;
    private String BRAND_ACTION_CODE;
    private String BRAND_HOST_DATE_TIME;
    private String TRACE_NUMBER;
    private String CARD_TYPE;
    private String ECI_DESCRIPTION;
    private String SIGNATURE;
    private String CARD;
    private String MERCHANT;
    private String STATUS;
    private String ACTION_DESCRIPTION;
    private String ID_UNICO;
    private String AMOUNT;
    private String AUTHORIZATION_CODE;
    private String CURRENCY;
    private String TRANSACTION_DATE;
    private String ACTION_CODE;
    private String CVV2_VALIDATION_RESULT;
    private String CARD_TOKEN;
    private String ECI;
    private String ID_RESOLUTOR;
    private String BRAND;
    private String ADQUIRENTE;
    private String BRAND_NAME;
    private String PROCESS_CODE;
    private String VAULT_BLOCK;
    private String TRANSACTION_ID;

    public int getId() {
        return id;
    }

    public Timestamp getHora() {
        return hora;
    }

    public String getTERMINAL() {
        return TERMINAL;
    }

    public String getBRAND_ACTION_CODE() {
        return BRAND_ACTION_CODE;
    }

    public String getBRAND_HOST_DATE_TIME() {
        return BRAND_HOST_DATE_TIME;
    }

    public String getTRACE_NUMBER() {
        return TRACE_NUMBER;
    }

    public String getCARD_TYPE() {
        return CARD_TYPE;
    }

    public String getECI_DESCRIPTION() {
        return ECI_DESCRIPTION;
    }

    public String getSIGNATURE() {
        return SIGNATURE;
    }

    public String getCARD() {
        return CARD;
    }

    public String getMERCHANT() {
        return MERCHANT;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public String getACTION_DESCRIPTION() {
        return ACTION_DESCRIPTION;
    }

    public String getID_UNICO() {
        return ID_UNICO;
    }

    public String getAMOUNT() {
        return AMOUNT;
    }

    public String getAUTHORIZATION_CODE() {
        return AUTHORIZATION_CODE;
    }

    public String getCURRENCY() {
        return CURRENCY;
    }

    public String getTRANSACTION_DATE() {
        return TRANSACTION_DATE;
    }

    public String getACTION_CODE() {
        return ACTION_CODE;
    }

    public String getCVV2_VALIDATION_RESULT() {
        return CVV2_VALIDATION_RESULT;
    }

    public String getCARD_TOKEN() {
        return CARD_TOKEN;
    }

    public String getECI() {
        return ECI;
    }

    public String getID_RESOLUTOR() {
        return ID_RESOLUTOR;
    }

    public String getBRAND() {
        return BRAND;
    }

    public String getADQUIRENTE() {
        return ADQUIRENTE;
    }

    public String getBRAND_NAME() {
        return BRAND_NAME;
    }

    public String getPROCESS_CODE() {
        return PROCESS_CODE;
    }

    public String getVAULT_BLOCK() {
        return VAULT_BLOCK;
    }

    public String getTRANSACTION_ID() {
        return TRANSACTION_ID;
    }
}
