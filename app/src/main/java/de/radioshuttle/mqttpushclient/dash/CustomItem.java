/*
 *	$Id$
 *	This is an unpublished work copyright (c) 2019 HELIOS Software GmbH
 *	30827 Garbsen, Germany.
 */

package de.radioshuttle.mqttpushclient.dash;

import android.util.Base64;
import android.util.Log;

import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;

import de.radioshuttle.mqttpushclient.PushAccount;
import de.radioshuttle.utils.Utils;

public class CustomItem extends Item {

    public CustomItem() {
        super();
        data = Collections.synchronizedMap(data);
    }

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject o = super.toJSONObject();
        o.put("html", html == null ? "" : html);
        return o;
    }

    protected void setJSONData(JSONObject o) {
        super.setJSONData(o);
        html = o.optString("html");
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getHtml() {
        String h ;
        if (html == null) {
            h = "";
        } else {
            h = html;
        }
        return h;
    }

    public JSObject getWebInterface() {
        if (mWebviewInterface == null) {
            mWebviewInterface = new JSObject();
        }
        return mWebviewInterface;
    }

    /* see also cv_interface.js */

    public class JSObject {

        public JSObject() {
            view = new JSView();
        }

        @JavascriptInterface
        public JSView getView() {
            return view;
        }

        /* the "public" publish is added via cv_interface.js (to handle ArrayBuffer data type) */
        @JavascriptInterface
        public void _publishHex(String topic, String payloadHex, boolean retain) {
            Log.d(TAG, "_publish: " + payloadHex + " " + retain);
        }

        @JavascriptInterface
        public void _publishStr(String topic, String payload, boolean retain) {
            Log.d(TAG, "_publish: " + payload + " " + retain);
        }

        @JavascriptInterface
        public void log(String s) {
            if (s == null)
            s = "";
            Log.d(TAG, "webview: " + s);
        }

        JSView view;
    }

    public class JSView {

        @JavascriptInterface
        public void setError(String error) {
            String lastError = (String) data.get("error");
            if (!Utils.equals(error, lastError)) {
                data.put("error", (error == null ? "" : error));
                notifyDataChangedNonUIThread();
            }
        }

        @JavascriptInterface
        public void setTextColor(double color) {
            Double currVal = getTextColor();
            if (currVal != color) {
                data.put("textcolor", doubleToLong(color));
                notifyDataChangedNonUIThread();
            }
        }

        @JavascriptInterface
        public double getTextColor() {
            return longToDouble(CustomItem.this.getTextcolor());
        }

        @JavascriptInterface
        public void setBackgroundColor(double color) {
            Double currVal = getBackgroundColor();
            if (currVal != color) {
                data.put("background", doubleToLong(color));
                notifyDataChangedNonUIThread();
            }
        }

        @JavascriptInterface
        public double getBackgroundColor() {
            return longToDouble(CustomItem.this.getBackgroundColor());
        }

        protected double longToDouble(long i) {
            double v;
            if (i == DColor.CLEAR) {
                v = DColor.CLEAR;
            } else if (i == DColor.OS_DEFAULT) {
                v = DColor.OS_DEFAULT;
            } else {
                v = i & 0xFFFFFFFFL;
            }
            return v;
        }

        protected long doubleToLong(double d) {
            long v;
            if (d == (double) DColor.CLEAR) {
                v = DColor.CLEAR;
            } else if (d == (double) DColor.OS_DEFAULT) {
                v = DColor.OS_DEFAULT;
            } else {
                v = (long) d & 0xFFFFFFFFL;
            }
            return v;
        }

    }

    public boolean hasMessageData() {
        return  !Utils.isEmpty(topic_s) && data != null &&
                !Utils.isEmpty((String) data.get("msg.topic"));
    }


    /** build message call of _onMqttMessage as defined in custom_view.js */
    public static String build_onMqttMessageCall(CustomItem item) {
        StringBuilder js = new StringBuilder();
        if (item != null && item.data != null) {
            long paraWhen = item.data.get("msg.received") == null ? 0 : (Long) item.data.get("msg.received");
            String paraTopic = item.data.get("msg.topic") == null ? "" : (String) item.data.get("msg.topic");
            byte[] msgRaw = item.data.get("msg.raw") == null ? new byte[0] : (byte[]) item.data.get("msg.raw");
            String paraMsgRaw = Base64.encodeToString(msgRaw, Base64.NO_WRAP); //TODO: check there was a problem with Base64.Default
            String org;
            /*
            try {
                org = new String(Base64.decode(paraMsgRaw, Base64.DEFAULT), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            */
            String paraMsg = item.data.get("msg.text") == null ? "" : (String) item.data.get("msg.text");
            js.append("if (typeof window['onMqttMessage'] === 'function') _onMqttMessage(");
            js.append(paraWhen);
            js.append(",'");
            js.append(paraTopic);
            js.append("','");
            js.append(paraMsg);
            js.append("','");
            js.append(paraMsgRaw);
            js.append("');");
        }
        return js.toString();
    }

    public static String build_onMqttPushClientInitCall(PushAccount accountData, CustomItem item) {
        StringBuilder js = new StringBuilder();

        if (accountData != null && item != null) {
            js.append("MQTT.acc = new Object();");
            js.append("MQTT.acc.user = '");
            js.append(accountData.user == null ? "" : accountData.user);
            js.append("'; ");
            js.append("MQTT.acc.mqttServer = '");
            try {
                URI u = new URI(accountData.uri);
                js.append(u.getAuthority());
            } catch (Exception e) {
                Log.d(TAG, "URI parse error: ", e);
            }
            js.append("'; ");
            js.append("MQTT.acc.pushServer = '");
            js.append(accountData.pushserver == null ? "" : accountData.pushserver);
            js.append("'; ");

            js.append("if (typeof window['onMqttPushClientInit'] === 'function') onMqttPushClientInit(");
            js.append("MQTT.acc");
            js.append(',');
            js.append("MQTT.getView()");
            js.append("); ");
        }


        return js.toString();
    }

    /** the passed java script code will be executed when document in state complete */
    public static String buildJS_readyState(String src) {
        StringBuilder js = new StringBuilder();
        js.append("function _initMqttPushClient() {");
        js.append(src);
        js.append("}");

        js.append("if (document.readyState != 'loading') {");
        js.append("_initMqttPushClient()");
        js.append("} else {");
        js.append("document.addEventListener('readystatechange', function(e) {");
        js.append("if (e.target.readyState === 'complete') {");
        js.append("_initMqttPushClient()");
        js.append("}});");
        js.append("}");

        return js.toString();
    }

    private JSObject mWebviewInterface;
    private String html = "";

    //UI state
    public boolean isLoading;
}
