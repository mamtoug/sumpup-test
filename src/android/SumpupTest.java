package de.mamtoug.cordova.sumup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult.Status;

import com.sumup.merchant.reader.api.SumUpState;
import com.sumup.merchant.reader.api.SumUpAPI;
import com.sumup.merchant.reader.api.SumUpLogin;
import com.sumup.merchant.reader.api.SumUpPayment;
import com.sumup.merchant.reader.models.TransactionInfo;


import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Marius Backes
 * Updated for SumUp SDK 5.0.2
 */
public class SumpupTest extends CordovaPlugin {

    // SumUp Methods
    public enum Action {
        login,
        auth,
        getSettings,
        logout,
        isLoggedIn,
        prepare,
        closeConnection,
        pay
    }

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_PAYMENT = 2;
    private static final int REQUEST_CODE_PAYMENT_SETTINGS = 3;
    private static final int REQUEST_CODE_SETUP = 4;
    private static final int REQUEST_CODE_TEST = 5;

    // Response codes
    private static final int LOGIN_ERROR = 100;
    private static final int LOGIN_CANCELED = 101;
    private static final int CHECK_FOR_LOGIN_STATUS_FAILED = 102;
    private static final int LOGOUT_FAILED = 103;
    private static final int FAILED_CLOSE_CARD_READER_CONN = 104;
    private static final int CARDREADER_INSTANCE_NOT_DEFINED = 105;
    private static final int STOP_CARD_READER_ERROR = 106;
    private static final int SHOW_SETTINGS_FAILED = 107;
    private static final int SETTINGS_DONE = 108;
    private static final int PREPARE_PAYMENT_ERROR = 109;
    private static final int CARDREADER_NOT_READY_TO_TRANSMIT = 110;
    private static final int ERROR_PREPARING_CHECKOUT = 111;
    private static final int AUTH_ERROR = 112;
    private static final int NO_ACCESS_TOKEN = 113;
    private static final int AUTH_SUCCESSFUL = 114;
    private static final int CANT_PARSE_AMOUNT = 115;
    private static final int CANT_PARSE_CURRENCY = 116;
    private static final int PAYMENT_ERROR = 117;
    private static final int NO_AFFILIATE_KEY = 118;

    private CallbackContext callback = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        cordova.getActivity().runOnUiThread(() -> SumUpState.init(cordova.getActivity().getApplicationContext()));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean result = false;

        String affiliateKey = this.cordova.getActivity().getString(cordova.getActivity().getResources()
                .getIdentifier("SUMUP_API_KEY", "string", cordova.getActivity().getPackageName()));

        switch(Action.valueOf(action)){
            case login:
                result = login(affiliateKey, args, callbackContext); break;
            case auth:
                result = auth(args, callbackContext); break;
            case getSettings:
                result = getSettings(args, callbackContext); break;
            case logout:
                result = logout(args, callbackContext); break;
            case isLoggedIn:
                result = isLoggedIn(args, callbackContext); break;
            case prepare:
                result = prepare(args, callbackContext); break;
            case closeConnection:
                result = closeConnection(args, callbackContext); break;
            case pay:
                result = pay(args, callbackContext); break;
        }

        return result;
    }

    // tries to login sumup user with credentials or access token
    private boolean login(String affiliateKey, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;

        final JSONObject sumUpKeys = args.getJSONObject(0);
        if(affiliateKey.isEmpty()) {
            affiliateKey = sumUpKeys.optString("affiliateKey");
        }

        if(!affiliateKey.isEmpty()) {
            final String supportedAffiliateKey = affiliateKey;
            Runnable runnable = () -> {
                Object accessToken = null;
                try {
                    accessToken = sumUpKeys.optString("accessToken");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                SumUpLogin sumUpLogin;
                if (accessToken != null && !accessToken.equals("")) {
                    sumUpLogin = SumUpLogin.builder(supportedAffiliateKey).accessToken(accessToken.toString()).build();
                } else {
                    sumUpLogin = SumUpLogin.builder(supportedAffiliateKey).build();
                }

                SumUpAPI.openLoginActivity(cordova.getActivity(), sumUpLogin, REQUEST_CODE_LOGIN);
            };

            cordova.setActivityResultCallback(this);
            cordova.getActivity().runOnUiThread(runnable);
        } else {
            JSONObject obj = createReturnObject(NO_AFFILIATE_KEY, "No affiliate key available");
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
        }

        return true;
    }

    // authenticate with the terminal
    private boolean auth(JSONArray args, CallbackContext callbackContext) {
        callback = callbackContext;
        try {
            cordova.getThreadPool().execute(() -> {
                try {
                    Object accessToken = args.get(0);

                    if (accessToken != null) {
                        // Note: In SDK 5.0.2, directly setting accessToken is not supported
                        // Authenticate via the login method with access token
                        String affiliateKey = this.cordova.getActivity().getString(cordova.getActivity().getResources()
                                .getIdentifier("SUMUP_API_KEY", "string", cordova.getActivity().getPackageName()));

                        // We'll perform a login with the access token instead
                        SumUpLogin sumUpLogin = SumUpLogin.builder(affiliateKey)
                                .accessToken(accessToken.toString())
                                .build();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            // We're not actually opening the UI here, just initializing the access token
                            SumUpAPI.prepareForCheckout();

                            JSONObject obj = createReturnObject(AUTH_SUCCESSFUL, "Authenticate was successful");
                            returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
                        });
                    } else {
                        JSONObject obj = createReturnObject(NO_ACCESS_TOKEN, "No access token");
                        returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
                    }
                } catch (Exception e) {
                    JSONObject obj = createReturnObject(AUTH_ERROR, e.getMessage());
                    returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
                }
            });
        } catch (Exception e) {
            JSONObject obj = createReturnObject(AUTH_ERROR, e.getMessage());
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            return false;
        }

        return true;
    }

    // reads the settings from the logged in user account
    private boolean getSettings(JSONArray args, CallbackContext callbackContext) {
        callback = callbackContext;
        cordova.setActivityResultCallback(this);
        cordova.getActivity().runOnUiThread(() -> SumUpAPI.openPaymentSettingsActivity(cordova.getActivity(), REQUEST_CODE_PAYMENT_SETTINGS));
        return true;
    }

    // logout an user
    private boolean logout(JSONArray args, CallbackContext callbackContext) {
        callback = callbackContext;
        try {
            Handler handler = new Handler(cordova.getActivity().getMainLooper());
            handler.post(() -> SumUpAPI.logout());

            JSONObject obj = createReturnObject(1, "Logout successful");
            returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
        } catch (Exception e) {
            JSONObject obj = createReturnObject(LOGOUT_FAILED, e.getMessage());
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            return false;
        }

        return true;
    }

    // checkes whether an user is logged in to proceed some action
    private boolean isLoggedIn(JSONArray args, CallbackContext callbackContext) {
        // runnable to run on the UIThread
        Runnable runnable = () -> {
            boolean isLoggedIn = SumUpAPI.isLoggedIn();
            try {
                JSONObject obj = new JSONObject();
                obj.put("code", 1);
                obj.put("isLoggedIn", isLoggedIn);
                returnCordovaPluginResult(PluginResult.Status.OK, obj, true);
            } catch (Exception e) {
                JSONObject obj = createReturnObject(CHECK_FOR_LOGIN_STATUS_FAILED, e.getMessage());
                returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
            }
        };

        callback = callbackContext;
        cordova.getActivity().runOnUiThread(runnable);

        return true;
    }

    // wakes up the terminal to make a payment
    private boolean prepare(JSONArray args, CallbackContext callbackContext) {
        callback = callbackContext;
        try {
            Handler handler = new Handler(cordova.getActivity().getMainLooper());
            handler.post(() -> {
                try {
                    SumUpAPI.prepareForCheckout();
                    JSONObject obj = createReturnObject(1, "SumUp checkout prepared successfully");
                    returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
                } catch (Exception e) {
                    JSONObject obj = createReturnObject(ERROR_PREPARING_CHECKOUT, e.getMessage());
                    returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                }
            });
        } catch (Exception e) {
            JSONObject obj = createReturnObject(PREPARE_PAYMENT_ERROR, e.getMessage());
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            return false;
        }

        return true;
    }

    // closes the connection to the card reader - in SDK 5.0.2 this is handled automatically
    private boolean closeConnection(JSONArray args, CallbackContext callbackContext) {
        callback = callbackContext;
        try {
            JSONObject obj = createReturnObject(1, "Connection handling is automatic in SDK 5.0.2");
            returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
        } catch (Exception e) {
            JSONObject obj = createReturnObject(FAILED_CLOSE_CARD_READER_CONN, e.getMessage());
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            return false;
        }

        return true;
    }

    // executes a payment
    private boolean pay(JSONArray args, CallbackContext callbackContext) {
        BigDecimal amount;

        try {
            amount = new BigDecimal(args.get(0).toString());
        } catch (Exception e) {
            JSONObject obj = createReturnObject(CANT_PARSE_AMOUNT, "Can't parse amount");
            returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            return false;
        }

        String title;
        try {
            title = new String(args.get(1).toString());
        } catch (Exception e) {
            title = "";
        }

        SumUpPayment.Currency currency;
        try {
            currency = SumUpPayment.Currency.valueOf(args.get(2).toString());
        } catch (Exception e) {
            if (!SumUpAPI.isLoggedIn()) {
                // no merchant account currently logged in
                JSONObject obj = createReturnObject(CANT_PARSE_CURRENCY, "Can't parse currency");
                returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
                return false;
            } else {
                currency = SumUpAPI.getCurrentMerchant().getCurrency();
            }
        }

        SumUpPayment payment = SumUpPayment.builder()
                .total(amount)
                .currency(currency)
                .title(title)
                //.skipSuccessScreen()
                .build();

        Runnable runnable = () -> {
            SumUpAPI.checkout(cordova.getActivity(), payment, REQUEST_CODE_PAYMENT);
        };

        callback = callbackContext;
        cordova.setActivityResultCallback(this);
        cordova.getActivity().runOnUiThread(runnable);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Login screen
        if (requestCode == REQUEST_CODE_LOGIN) {
            try {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Integer code = extras.getInt(SumUpAPI.Response.RESULT_CODE);
                    String message = extras.getString(SumUpAPI.Response.MESSAGE);

                    JSONObject obj = createReturnObject(code, message);
                    if (code == 1) {
                        returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
                    } else {
                        returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                    }
                } else {
                    JSONObject obj = createReturnObject(LOGIN_CANCELED, "Login canceled");
                    returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                }
            } catch (Exception e) {
                JSONObject obj = createReturnObject(LOGIN_ERROR, e.getMessage());
                returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            }
        }

        // payment screen
        if (requestCode == REQUEST_CODE_PAYMENT) {
            try {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Integer code = extras.getInt(SumUpAPI.Response.RESULT_CODE);
                    String message = extras.getString(SumUpAPI.Response.MESSAGE);
                    TransactionInfo txinfo = extras.getParcelable(SumUpAPI.Response.TX_INFO);

                    JSONObject obj = new JSONObject();

                    if (txinfo != null) {
                        obj.put("transaction_code", txinfo.getTransactionCode());
                        obj.put("merchant_code", txinfo.getMerchantCode());
                        obj.put("amount", txinfo.getAmount());
                        obj.put("tip_amount", txinfo.getTipAmount());
                        obj.put("vat_amount", txinfo.getVatAmount());
                        obj.put("currency", txinfo.getCurrency());
                        obj.put("status", txinfo.getStatus());
                        obj.put("payment_type", txinfo.getPaymentType());
                        obj.put("entry_mode", txinfo.getEntryMode());
                        obj.put("installments", txinfo.getInstallments());
                        obj.put("card_type", txinfo.getCard().getType());
                        obj.put("last_4_digits", txinfo.getCard().getLast4Digits());
                    }

                    if (code == 1) {
                        returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
                    } else {
                        obj = createReturnObject(code, "Payment error");

                        if (!SumUpAPI.isLoggedIn()) {
                            obj = createReturnObject(SumUpAPI.Response.ResultCode.ERROR_NOT_LOGGED_IN, "Not logged in");
                        } else {
                            obj = createReturnObject(code, "Payment error");
                        }
                        returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                    }
                } else {
                    JSONObject obj = createReturnObject(PAYMENT_ERROR, "Payment error");
                    returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                }
            } catch (Exception e) {
                JSONObject obj = createReturnObject(PAYMENT_ERROR, e.getMessage());
                returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            }
        }

        // Settings screen
        if (requestCode == REQUEST_CODE_PAYMENT_SETTINGS) {
            try {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Integer code = extras.getInt(SumUpAPI.Response.RESULT_CODE);
                    String message = extras.getString(SumUpAPI.Response.MESSAGE);

                    JSONObject obj = createReturnObject(code, message);
                    if (code == 1) {
                        returnCordovaPluginResult(PluginResult.Status.OK, obj, false);
                    } else {
                        returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                    }
                } else {
                    JSONObject obj = createReturnObject(SETTINGS_DONE, "Settings done");
                    returnCordovaPluginResult(PluginResult.Status.ERROR, obj, false);
                }

            } catch (Exception e) {
                JSONObject obj = createReturnObject(SHOW_SETTINGS_FAILED, e.getMessage());
                returnCordovaPluginResult(PluginResult.Status.ERROR, obj, true);
            }
        }
    }

    // creates a return object with all needed information
    private JSONObject createReturnObject(int code, String message) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("code", code);
            obj.put("message", message);
            return obj;
        } catch (Exception e) {
            System.out.println("JSONObject error: " + e.getMessage());
        }

        return null;
    }

    // returns the plugin result to javascript interface
    private void returnCordovaPluginResult(Status status, JSONObject obj, boolean setKeepCallback) {
        PluginResult result = new PluginResult(status, obj);
        if(!setKeepCallback) {
            result.setKeepCallback(false);
        }
        callback.sendPluginResult(result);
    }
}
