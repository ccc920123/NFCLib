package com.stardon.nfc.library.nfcexputils;

public class Nfc_const {
    public final static String KEY_DATA = "GET_DATA";

    public enum R_W_Methods {
        Fast_Mode, Polling_Mode, Error
    }

    public enum Task_Result {
        CANCEL,
        SUCCESS,
        WAIT_DATA_TIMEOUT,
        CONNET_TIMEOUT,
        SendFail,
        EXCEPTION,
    }

}
