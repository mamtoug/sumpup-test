package com.mamtoug.sumpuptest;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.Toast;
import android.util.Log;

public class SumpupTest extends CordovaPlugin {

    private static final String TAG = "SumpupTest";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "Action called: " + action);

        switch (action) {
            case "showMessage":
                String message = args.getString(0);
                this.showMessage(message, callbackContext);
                return true;

            case "getDeviceInfo":
                this.getDeviceInfo(callbackContext);
                return true;

            case "initSumUp":
                String apiKey = args.getString(0);
                this.initSumUp(apiKey, callbackContext);
                return true;

            case "testPayment":
                double amount = args.getDouble(0);
                String currency = args.getString(1);
                this.testPayment(amount, currency, callbackContext);
                return true;

            default:
                return false;
        }
    }

    private void showMessage(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            // Show toast message on UI thread
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(cordova.getActivity(), message, Toast.LENGTH_LONG).show();
                }
            });

            // Return success response
            try {
                JSONObject result = new JSONObject();
                result.put("message", message);
                result.put("status", "success");
                result.put("platform", "Android");
                callbackContext.success(result);
            } catch (JSONException e) {
                callbackContext.error("Error creating response: " + e.getMessage());
            }
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void getDeviceInfo(CallbackContext callbackContext) {
        try {
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("platform", "Android");
            deviceInfo.put("model", android.os.Build.MODEL);
            deviceInfo.put("manufacturer", android.os.Build.MANUFACTURER);
            deviceInfo.put("version", android.os.Build.VERSION.RELEASE);
            deviceInfo.put("sdk", android.os.Build.VERSION.SDK_INT);
            deviceInfo.put("brand", android.os.Build.BRAND);

            callbackContext.success(deviceInfo);
        } catch (JSONException e) {
            callbackContext.error("Error getting device info: " + e.getMessage());
        }
    }

    private void initSumUp(String apiKey, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Initializing SumUp with API key: " + (apiKey != null ? "***" : "null"));

            // This is a placeholder for actual SumUp SDK initialization
            JSONObject result = new JSONObject();
            result.put("status", "initialized");
            result.put("message", "SumUp SDK initialized (simulated)");
            result.put("apiKey", apiKey != null ? "provided" : "missing");

            callbackContext.success(result);
        } catch (JSONException e) {
            callbackContext.error("Error initializing SumUp: " + e.getMessage());
        }
    }

    private void testPayment(double amount, String currency, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Processing test payment: " + amount + " " + currency);

            // This is a placeholder for actual SumUp payment processing
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "Payment processed (simulated)");
            result.put("amount", amount);
            result.put("currency", currency);
            result.put("transactionId", "TEST_" + System.currentTimeMillis());
            result.put("timestamp", System.currentTimeMillis());

            callbackContext.success(result);
        } catch (JSONException e) {
            callbackContext.error("Error processing payment: " + e.getMessage());
        }
    }
}
