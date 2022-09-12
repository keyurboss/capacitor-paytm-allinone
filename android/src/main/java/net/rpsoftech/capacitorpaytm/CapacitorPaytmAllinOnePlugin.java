package net.rpsoftech.capacitorpaytm;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.paytm.pgsdk.TransactionManager;

import org.json.JSONException;
import org.json.JSONObject;

@CapacitorPlugin(name = "CapacitorPaytmAllinOne")
public class CapacitorPaytmAllinOnePlugin extends Plugin {
protected static final int REQ_CODE = 20011;

    private static CapacitorPaytmAllinOnePlugin INSTANCE = null;

    public static CapacitorPaytmAllinOnePlugin getInstance() {
        return INSTANCE;
    }

    private String callbackId = null;

    @PluginMethod
    public void startTransaction(final PluginCall call) {
        INSTANCE = this;
        String orderId = call.getString("orderId");
        String mid = call.getString("mid");
        String txnToken = call.getString("txnToken");
        String amount = call.getString("amount");
        String callbackUrl = call.getString("callbackUrl");
        boolean isStaging = call.getBoolean("isStaging", false);
        boolean restrictAppInvoke = call.getBoolean("restrictAppInvoke", false);

        if (orderId == null || mid == null || txnToken == null || amount == null || orderId.isEmpty() || mid.isEmpty() || txnToken.isEmpty() || amount.isEmpty()) {
            if (txnToken == null || txnToken.isEmpty()) {
                setResult("txnToken error", call);
            } else {
                setResult("Please enter all field", call);
            }
            return;
        }
        callbackId = call.getCallbackId();
        bridge.saveCall(call);
        String host = "https://securegw.paytm.in/";
        if (isStaging) {
            host = "https://securegw-stage.paytm.in/";
        }

        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            callbackUrl = host + "theia/paytmCallback?ORDER_ID=" + orderId;
        }

        PaytmOrder paytmOrder = new PaytmOrder(orderId, mid, txnToken, amount, callbackUrl);
        TransactionManager transactionManager = new TransactionManager(paytmOrder, new PaytmPaymentTransactionCallback() {
            @Override
            public void onTransactionResponse(Bundle bundle) {
                Log.d("LOG", "Payment Transaction is successful " + bundle);
                if (bundle.getString("STATUS").equals("TXN_SUCCESS")) {
                    setResult(getJSONObjectFromBundle(bundle), call);
                } else {
                    setResult(bundle.getString("RESPMSG"), call);
                }
            }

            @Override
            public void networkNotAvailable() {
                setResult("Network Not Available", call);
            }

            @Override
            public void onErrorProceed(String s) {
                if (s != null) {
                    setResult(s, call);
                } else {
                    setResult("Error Proceed", call);
                }
            }

            @Override
            public void clientAuthenticationFailed(String s) {
                if (s != null) {
                    setResult(s, call);
                } else {
                    setResult("Client Authentication Failed", call);
                }
            }

            @Override
            public void someUIErrorOccurred(String s) {
                if (s != null) {
                    setResult(s, call);
                } else {
                    setResult("UI Error Occurred", call);
                }
            }

            @Override
            public void onErrorLoadingWebPage(int iniErrorCode, String inErrorMessage, String inFailingUrl) {
                setResult(inErrorMessage + " url: " + inFailingUrl, call);
            }

            @Override
            public void onBackPressedCancelTransaction() {
                setResult("Back Pressed Cancel Transaction", call);
            }

            @Override
            public void onTransactionCancel(String s, Bundle bundle) {
                if (bundle != null) {
                    if (bundle.get("STATUS") == "TXN_SUCCESS") {
                        setResult(getJSONObjectFromBundle(bundle), call);
                    } else {
                        setResult(bundle.getString("RESPMSG"), call);
                    }
                } else {
                    if (s != null) {
                        setResult(s, call);
                    } else {
                        setResult("Transaction Canceled", call);
                    }
                }
            }
        });
//        transactionManager.setCallingBridge("IonicCapacitor");
        if (restrictAppInvoke) {
            transactionManager.setAppInvokeEnabled(false);
        }
        transactionManager.setShowPaymentUrl(host + "theia/api/v1/showPaymentPage");
        transactionManager.startTransaction(getActivity(), REQ_CODE);
    }

    public void onActivityResult(int requestCode,Intent data) {
        if (requestCode == REQ_CODE  && data != null) {
            onRequestCallback(data);
        }
    }

    @ActivityCallback
    private void onRequestCallback(Intent data) {
        PluginCall call = bridge.getSavedCall(callbackId);
        String message = data.getStringExtra("nativeSdkForMerchantMessage");
        String response = data.getStringExtra("response");
        if (response != null && !response.isEmpty()) {
            try {
                JSONObject resultJson = new JSONObject(response);
                if (resultJson.getString("STATUS").equals("TXN_SUCCESS")) {
                    setResult(resultJson, call);
                } else {
                    setResult(resultJson.getString("RESPMSG"), call);
                }
            } catch (Exception e) {
                setResult(e.getMessage(), call);
            }
        } else if (!TextUtils.isEmpty(message)) {
            setResult(message, call);
        } else {
            setResult("Error fetching response", call);
        }
    }

    private void setResult(String message, PluginCall call) {
        if (call != null) {
            call.reject(message);
        }
        INSTANCE = null;
    }

    private void setResult(JSONObject object, PluginCall call) {
        if (call != null) {
            try {
                call.resolve(JSObject.fromJSONObject(object));
            } catch (JSONException e) {
                call.reject(e.getMessage());
            }
        }
        INSTANCE = null;
    }

    private JSONObject getJSONObjectFromBundle(Bundle bundle) {
        JSONObject object = new JSONObject();
        for (String key : bundle.keySet()) {
            try {
                object.put(key, bundle.get(key));
            } catch (Exception ignored) {
            }
        }
        return object;
    }
}
