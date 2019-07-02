/*
 *	$Id$
 *	This is an unpublished work copyright (c) 2019 HELIOS Software GmbH
 *	30827 Garbsen, Germany.
 */

package de.radioshuttle.mqttpushclient.dash;


import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import de.radioshuttle.mqttpushclient.R;
import de.radioshuttle.utils.Utils;

public abstract class Item {
    public Item() {
        data = new HashMap<>();
    }

    public int id;
    public int textcolor;
    public int background;
    public int textsize; // 0 - default, 1 - small, 2 - medium, 3 - large
    public String topic_s;
    public String script_f;

    public String topic_p;
    public String script_p;
    public boolean retain;

    public String label;

    public String outputScriptError;

    public JSONObject toJSONObject() throws JSONException {
        JSONObject o = new JSONObject();
        if (!(this instanceof GroupItem)) {
            o.put("topic_s", topic_s);
            o.put("script_f", (script_f == null ? "" : script_f));

            o.put("topic_p", topic_p);
            o.put("script_p", (script_p == null ? "" : script_p));

            o.put("retain", retain);
        }
        o.put("type", getType());
        o.put("label", label);
        o.put("textcolor", textcolor);
        o.put("background", background);
        o.put("textsize", textsize);
        o.put("id", id);

        return o;
    }

    protected void setJSONData(JSONObject o) {
        label = o.optString("label");
        background = o.optInt("background");
        textcolor = o.optInt("textcolor");
        textsize = o.optInt("textsize");
        topic_s = o.optString("topic_s");
        script_f = o.optString("script_f");
        topic_p = o.optString("topic_p");
        script_p = o.optString("script_p");
        retain = o.optBoolean("retain");
        id = o.optInt("id");
    }

    public static Item createItemFromJSONObject(JSONObject o) {
        Item item = null;
        String type = o.optString("type");
        switch (type) {
            case "text": item = new TextItem(); break;
            case "group": item = new GroupItem(); break;
        }
        if (item != null) {
            item.setJSONData(o);
        }
        return item;
    }

    /* helper for view component (used in adapter and detail dialog */

    public void setViewBackground(View v, Integer defalutBackgroundColor) {
        if (v != null) {
            int color;
            if (defalutBackgroundColor == null) {
                color = ContextCompat.getColor(v.getContext(), R.color.dashboad_item_background);
            } else {
                color = defalutBackgroundColor;
            }
            int bg = data.containsKey("background") ? (Integer) data.get("background") : background;
            int background = (bg == 0 ? color : bg);

            v.setBackgroundColor(background);
        }
    }

    /** set text color and text appearance */
    public void setViewTextAppearance(TextView v, int defaultColor) {
        if (v != null) {
            int textSizeIdx = (textsize <= 0 ? Item.DEFAULT_TEXTSIZE : textsize ) -1;
            if (textSizeIdx >= 0 && textSizeIdx < TEXTAPP.length) {
                TextViewCompat.setTextAppearance(v, TEXTAPP[textSizeIdx]);
            }

            int color = getTextcolor();
            if (color != 0) {
                v.setTextColor(color);
            } else if (defaultColor != 0) { // && color == 0
                v.setTextColor(defaultColor);
            }
        }
    }

    protected int getTextcolor() {
        int color = data.containsKey("textcolor") ? (Integer) data.get("textcolor") : textcolor;
        return color;
    }

    final static int[] TEXTAPP = new int[] {android.R.style.TextAppearance_Small, android.R.style.TextAppearance_Medium, android.R.style.TextAppearance_Large};

    public HashMap<String, Object> data;

    public abstract String getType();

    public final static int DEFAULT_TEXTSIZE = 1;

    protected static String TAG = Item.class.getSimpleName();
}
