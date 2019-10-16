/*
 *	$Id$
 *	This is an unpublished work copyright (c) 2019 HELIOS Software GmbH
 *	30827 Garbsen, Germany.
 */

package de.radioshuttle.mqttpushclient.dash;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import de.radioshuttle.mqttpushclient.R;
import de.radioshuttle.utils.Utils;

public class DashBoardAdapter extends RecyclerView.Adapter {

    public DashBoardAdapter(AppCompatActivity activity, int width, int spanCount, LinkedHashSet<Integer> selectedItems) {
        mInflater = activity.getLayoutInflater();
        mData = new ArrayList<>();
        mWidth = width;
        // setHasStableIds(true);

        mDefaultBackground = ContextCompat.getColor(activity, R.color.dashboad_item_background);
        mDefaultButtonTintColor = ContextCompat.getColor(activity, R.color.button_tint_default);
        Log.d(TAG, "default item bg: " + mDefaultBackground);

        mDefaultProgressColor = DColor.fetchAccentColor(activity);
        mDefaultButtonBackground = DColor.fetchColor(activity, R.attr.colorButtonNormal);
        spacing = activity.getResources().getDimensionPixelSize(R.dimen.dashboard_spacing);
        mSpanCnt = spanCount;
        mSelectedItems = selectedItems;

        try {
            m_custom_view_js = Utils.getRawStringResource(activity, "cv_interface", true);
        } catch (IOException e) {
            Log.d(TAG, "Error loading raw resource: custom_view_js", e);
        }
    }

    public void addListener(DashBoardActionListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, final int viewType) {
        // Log.d(TAG, "onCreateViewHolder: " );
        View view = null;
        TextView label = null;
        View contentContainer = null;
        TextView value = null;
        ImageView selectedImageView = null;
        ImageView errorImageView = null;
        ImageView errorImage2View = null;
        ProgressBar progressBar = null;
        Button button = null;
        ImageButton imageButton = null;
        int defaultColor = 0;
        if (viewType == TYPE_TEXT) {
            view = mInflater.inflate(R.layout.activity_dash_board_item_text, parent, false);
            label = view.findViewById(R.id.name);
            defaultColor = label.getTextColors().getDefaultColor();

            contentContainer = view.findViewById(R.id.textContent);
            value = view.findViewById(R.id.textContent);
            selectedImageView = view.findViewById(R.id.check);
            errorImageView = view.findViewById(R.id.errorImage);
            errorImage2View = view.findViewById(R.id.errorImage2);
        } else if (viewType == TYPE_PROGRESS) {
            view = mInflater.inflate(R.layout.activity_dash_board_item_progress, parent, false);
            label = view.findViewById(R.id.name);
            defaultColor = label.getTextColors().getDefaultColor();
            contentContainer = view.findViewById(R.id.progressBarContent);
            progressBar = view.findViewById(R.id.itemProgressBar);
            value = view.findViewById(R.id.textContent);
            selectedImageView = view.findViewById(R.id.check);
            errorImageView = view.findViewById(R.id.errorImage);
            errorImage2View = view.findViewById(R.id.errorImage2);
        } else if (viewType == TYPE_SWITCH) {
            view = mInflater.inflate(R.layout.activity_dash_board_item_switch, parent, false);
            label = view.findViewById(R.id.name);
            defaultColor = label.getTextColors().getDefaultColor();
            contentContainer = view.findViewById(R.id.switchContainer);
            button = view.findViewById(R.id.toggleButton);
            imageButton = view.findViewById(R.id.toggleImageButton);
            mDefaultButtonTextColor = mDefaultButtonTintColor;
            selectedImageView = view.findViewById(R.id.check);
            errorImageView = view.findViewById(R.id.errorImage);
            errorImage2View = view.findViewById(R.id.errorImage2);
        } else if (viewType == TYPE_GROUP) {
            view = mInflater.inflate(R.layout.activity_dash_board_item_group, parent, false);
            label = view.findViewById(R.id.name);
            defaultColor = label.getTextColors().getDefaultColor();
            selectedImageView = view.findViewById(R.id.check);
        } else if (viewType == TYPE_CUSTOM) {
            view = mInflater.inflate(R.layout.activity_dash_board_item_custom, parent, false);
            label = view.findViewById(R.id.name);
            defaultColor = label.getTextColors().getDefaultColor();
            contentContainer = view.findViewById(R.id.webContent);
            WebView webView = (WebView) contentContainer;
            webView.getSettings().setJavaScriptEnabled(true);
            // webView.getSettings().setUseWideViewPort(true); // viewport
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            selectedImageView = view.findViewById(R.id.check);
            errorImageView = view.findViewById(R.id.errorImage);
            errorImage2View = view.findViewById(R.id.errorImage2);
        } // TODO: handle unknown view type

        Log.d(TAG, "new custom component: " + viewType);

        final ViewHolder holder = new ViewHolder(view);
        holder.label = label; // item label
        holder.viewType = viewType;
        holder.contentContainer = contentContainer; // content for text items
        holder.progressBar = progressBar;
        holder.value = value;
        holder.selectedImageView = selectedImageView;
        holder.button = button;
        holder.imageButton = imageButton;
        holder.defaultColor = defaultColor;
        holder.errorImage = errorImageView;
        holder.errorImage2 = errorImage2View;

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        toggleSelection(pos);
                    }
                }
                return true;
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    if (mSelectedItems.size() > 0) {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            toggleSelection(pos);
                        }
                    } else {
                        if (viewType != TYPE_GROUP) {
                            mListener.onItemClicked(getItem(holder.getAdapterPosition()));
                        }
                    }
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Log.d(TAG, "onBindViewHolder: " );
        ViewHolder h = (ViewHolder) holder;
        Item item = mData.get(position);
        h.label.setText(item.label);

        if (mSelectedItems.contains(item.id)) {
            h.itemView.setActivated(true);
            if (h.selectedImageView != null && h.selectedImageView.getVisibility() != View.VISIBLE) {
                h.selectedImageView.setVisibility(View.VISIBLE);
            }
        } else {
            h.itemView.setActivated(false);
            if (h.selectedImageView != null && h.selectedImageView.getVisibility() != View.GONE) {
                h.selectedImageView.setVisibility(View.GONE);
            }
        }

        ViewGroup.LayoutParams lp = h.itemView.getLayoutParams();

        if (h.viewType != TYPE_GROUP) {
            lp = h.contentContainer.getLayoutParams();

            if (lp.width != mWidth || lp.height != mWidth) {
                lp.width = mWidth;
                lp.height = mWidth;
                h.contentContainer.setLayoutParams(lp);
            }
            item.setViewBackground(h.contentContainer, mDefaultBackground);
        } else { // if (h.viewType == TYPE_GROUP) {
            if (lp.width != mSpanCnt * mWidth + (mSpanCnt - 1) * spacing * 2) {
                lp.width = mSpanCnt * mWidth + (mSpanCnt - 1) * spacing * 2;
                h.itemView.setLayoutParams(lp);
            }

            item.setViewBackground(h.itemView, mDefaultBackground);
            item.setViewTextAppearance(h.label, h.defaultColor);
        }

        Object publishError = item.data.get("error2");
        if (h.errorImage2 != null) {
            if (publishError instanceof String) { // value set?
                int color;
                if (item.textcolor == DColor.OS_DEFAULT || item.textcolor == DColor.CLEAR) {
                    color = h.defaultColor;
                } else {
                    color = (int) item.textcolor;
                }
                ColorStateList csl = ColorStateList.valueOf(color);
                ImageViewCompat.setImageTintList(h.errorImage2, csl);

                if (h.errorImage2.getVisibility() != View.VISIBLE) {
                    h.errorImage2.setVisibility(View.VISIBLE);
                }
            } else {
                if (h.errorImage2.getVisibility() != View.GONE) {
                    h.errorImage2.setVisibility(View.GONE);
                }
            }
        }

        Object javaScriptError = item.data.get("error");

        if (h.progressBar != null) { // (h.viewType == TYPE_PROGRESS) {
            ProgressItem p = (ProgressItem) item;
            long pcolor = (p.data.get("ctrl_color") != null ? (Long) p.data.get("ctrl_color") : p.progresscolor);

            int color;
            if (pcolor ==  DColor.OS_DEFAULT || pcolor == DColor.CLEAR) {
                color = mDefaultProgressColor;
            } else {
                color = (int) pcolor;
            }

            if (Build.VERSION.SDK_INT >= 21) {
                /*
                Drawable d = h.progressBar.getProgressDrawable();
                if (d != null) {
                    DrawableCompat.setTint(d, p.progresscolor == 0 ? mDefaultProgressColor : p.progresscolor);
                }
                */
                ColorStateList pt = h.progressBar.getProgressTintList();
                if (pt == null || pt.getDefaultColor() != pcolor) {
                    h.progressBar.setProgressTintList(ColorStateList.valueOf(color));
                    h.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(color));
                }
            } else {
                Drawable d = h.progressBar.getProgressDrawable();
                if (d != null) {
                    d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            }

            int value = 0;
            /* if java script error, there is no valid data, set progress bar to 0 */
            if (javaScriptError instanceof String) {
            } else {
                String val = (String) item.data.get("content");
                if (!Utils.isEmpty(val)) {
                    try {
                        double v = Double.parseDouble(val);
                        if (p.range_min < p.range_max && v >= p.range_min && v <= p.range_max) {
                            double f = ProgressItem.calcProgessInPercent(v, p.range_min, p.range_max) / 100d;
                            value = (int) ((double) h.progressBar.getMax() * f);
                        }
                    } catch(Exception e) {}
                }
            }
            h.progressBar.setProgress(value);
        }
        if (h.errorImage != null) {
            if (javaScriptError instanceof String) { // TYPE_TEXT
                int color;
                if (item.textcolor == DColor.OS_DEFAULT || item.textcolor == DColor.CLEAR) {
                    color = h.defaultColor;
                } else {
                    color = (int) item.textcolor;
                }

                ColorStateList csl = ColorStateList.valueOf(color);
                ImageViewCompat.setImageTintList(h.errorImage, csl);

                if (h.errorImage.getVisibility() != View.VISIBLE) {
                    h.errorImage.setVisibility(View.VISIBLE);
                }
            } else {
                if (h.errorImage.getVisibility() != View.GONE) {
                    h.errorImage.setVisibility(View.GONE);
                }
            }
        }

        // if view for text content exists, set content
        // Log.d(TAG, "ui: " + item.label);
        if (h.value != null) { //TEXT
            String content = (String) item.data.get("content");
            if (h.viewType == TYPE_PROGRESS) {
                if (item.data.get("content.progress") instanceof String) {
                    content = (String) item.data.get("content.progress");
                }
            }

            item.setViewTextAppearance(h.value, h.defaultColor);
            h.value.setText(content);
        }

        // switch
        if (item instanceof Switch) {
            final Switch sw = (Switch) item;

            /* if stateless, show onValue */
            String val = null;
            long fcolor;
            long bcolor;
            boolean noTint;
            boolean isOnState = sw.isOnState();

            Drawable icon;

            if (isOnState) {
                val = sw.val;
                fcolor = sw.data.containsKey("ctrl_color") ? (Long) sw.data.get("ctrl_color") : sw.color;
                bcolor = sw.data.containsKey("ctrl_background") ? (Long) sw.data.get("ctrl_background") : sw.bgcolor;
                icon = (Drawable) sw.data.get("ctrl_image_blob");
                if (icon == null) {
                    icon = sw.image;
                }
                noTint = fcolor == DColor.CLEAR;
            } else {
                val = sw.valOff;
                fcolor = sw.data.containsKey("ctrl_color_off") ? (Long) sw.data.get("ctrl_color_off") : sw.colorOff;
                bcolor = sw.data.containsKey("ctrl_background_off") ? (Long) sw.data.get("ctrl_background_off") : sw.bgcolorOff;
                icon = (Drawable) sw.data.get("ctrl_image_off_blob");
                if (icon == null) {
                    icon = sw.imageOff;
                }
                noTint = fcolor == DColor.CLEAR;
            }
            ColorStateList csl;
            if (bcolor == DColor.OS_DEFAULT || bcolor == DColor.CLEAR) {
                csl = ColorStateList.valueOf(mDefaultButtonBackground);
            } else {
                csl = ColorStateList.valueOf((int) bcolor);
            }

            /* show button or image button */
            if (icon == null) {
                if (h.button.getVisibility() != View.VISIBLE) {
                    h.button.setVisibility(View.VISIBLE);
                }
                if (h.imageButton.getVisibility() != View.GONE) {
                    h.imageButton.setVisibility(View.GONE);;
                }
                /* if stateless, show onValue */
                h.button.setText(val);
                int color;
                if (fcolor == DColor.OS_DEFAULT || fcolor == DColor.CLEAR) {
                    color = mDefaultButtonTextColor;
                } else {
                    color = (int) fcolor;
                }
                h.button.setTextColor(color);
                ViewCompat.setBackgroundTintList(h.button, csl);

            } else {
                if (h.button.getVisibility() != View.GONE) {
                    h.button.setVisibility(View.GONE);
                }
                if (h.imageButton.getVisibility() != View.VISIBLE) {
                    h.imageButton.setVisibility(View.VISIBLE);;
                }
                if (h.imageButton.getDrawable() != icon) {
                    h.imageButton.setImageDrawable(icon);
                }

                ViewCompat.setBackgroundTintList(h.imageButton, csl);
                int color;
                if (fcolor == DColor.OS_DEFAULT) {
                    color = mDefaultButtonTintColor;
                } else {
                    color = (int) fcolor;
                }
                ColorStateList tcsl = ColorStateList.valueOf(color);
                if (noTint) {
                    ImageViewCompat.setImageTintList(h.imageButton, null);
                } else {
                    ImageViewCompat.setImageTintList(h.imageButton, tcsl);
                }
            }
        }

        if (item instanceof CustomItem) {
            final WebView webView = (WebView) h.contentContainer;
            final CustomItem citem = (CustomItem) item;

            if (!Utils.equals(h.html, citem.getHtml())) { // load html, if not already done or changed
                h.html = citem.getHtml();
                citem.isLoading = true;
                webView.addJavascriptInterface(new CustomItem.JSObject(), "PushApp");
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        citem.isLoading = false;
                        Log.d(TAG, "on page finished.");
                        StringBuilder js = new StringBuilder();
                        if (Build.VERSION.SDK_INT < 19) {
                            js.append("javascript:");
                        }
                        js.append(m_custom_view_js);
                        js.append(' ');
                        js.append(CustomItem.build_onMqttMessageCall(citem));

                        if (Build.VERSION.SDK_INT >= 19) {
                            webView.evaluateJavascript(js.toString(), null);
                        } else {
                            webView.loadUrl("javascript:" + js.toString());
                        }

                    }
                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);
                        //TODO: implement. this is only vorking for initial load errors
                    }
                });
                String encodedHtml = Base64.encodeToString(h.html.getBytes(), Base64.NO_PADDING);
                webView.loadData(encodedHtml, "text/html", "base64");
            } else {
                if (!citem.isLoading) {
                    String jsOnMqttMessageCall = CustomItem.build_onMqttMessageCall(citem);

                    if (Build.VERSION.SDK_INT >= 19) {
                        webView.evaluateJavascript(jsOnMqttMessageCall, null);
                    } else {
                        webView.loadUrl(jsOnMqttMessageCall);
                    }
                }
            }
        }
        // Log.d(TAG, "width: " + lp.width);
        // Log.d(TAG, "height: " + lp.height);
    }

    @Override
    public int getItemCount() {
        return (mData == null ? 0 : mData.size());
    }

    public void setData(List<Item> data) {
        //TODO: consider using DiffUtil to improve performance
        mData = data;
        if (mData == null) {
            mData = new ArrayList<>();
        }

        /* if items have been deleted the selected items hashmap must be updated */
        if (mSelectedItems != null && mSelectedItems.size() > 0) {
            int o = mSelectedItems.size();
            HashSet<Integer> dataKeys = new HashSet<>();
            for (Item a : data) {
                dataKeys.add(a.id);
            }
            mSelectedItems.retainAll(dataKeys);
            int n = mSelectedItems.size();
            if (o != n && mListener != null) {
                mListener.onSelectionChange(o, n);
            }
        }

        notifyDataSetChanged();
    }

    public List<Item> getData() {
        return mData;
    }


    public void setItemWidth(int width, int spanCnt) {
        mSpanCnt = spanCnt;
        mWidth = width;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
        TextView label;
        View contentContainer;
        TextView value;
        ProgressBar progressBar;
        ImageView selectedImageView;
        ImageView errorImage;
        ImageView errorImage2;
        Button button;
        ImageButton imageButton;
        int defaultColor;
        int viewType;
        String html;
    }

    @Override
    public int getItemViewType(int position) {
        int type = 0;
        if (mData != null && position < mData.size()) {
            Item item = mData.get(position);
            if (item instanceof GroupItem) {
                type = TYPE_GROUP;
            } else if (item instanceof TextItem) {
                type = TYPE_TEXT;
            } else  if (item instanceof ProgressItem) {
                type = TYPE_PROGRESS;
            } else if (item instanceof Switch) {
                type = TYPE_SWITCH;
            } else if (item instanceof CustomItem) {
                type = TYPE_CUSTOM;
            }
        }
        return type;
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).id;
    }


    public Item getItem(int position) {
        Item item = null;
        if (mData != null && position < mData.size()) {
            item = mData.get(position);
        }
        return item;
    }

    protected void toggleSelection(int pos) {
        int noOfSelectedItemsBefore = mSelectedItems.size();
        int noOfSelectedItems = noOfSelectedItemsBefore;

        Integer e = mData.get(pos).id;
        if (mSelectedItems.contains(e)) {
            mSelectedItems.remove(e);
            noOfSelectedItems--;
        } else {
            mSelectedItems.add(e);
            noOfSelectedItems++;
        }
        notifyItemChanged(pos, mData.get(pos));
        if (mListener != null) {
            mListener.onSelectionChange(noOfSelectedItemsBefore, noOfSelectedItems);
        }
    }

    public void clearSelection() {
        int noOfSelectedItemsBefore = mSelectedItems.size();
        mSelectedItems.clear();

        if (mListener != null) {
            mListener.onSelectionChange(noOfSelectedItemsBefore, 0);
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return mSelectedItems != null && mSelectedItems.size() > 0;
    }

    public HashSet<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public Integer getLastSelectedItem() {
        Integer lastSel = null;
        if (mSelectedItems != null && mSelectedItems.size() > 0) {
            ArrayList<Integer> tmp = new ArrayList<>(mSelectedItems);
            lastSel = tmp.get(tmp.size() - 1);
        }
        return lastSel;
    }

    private String m_custom_view_js;

    private LinkedHashSet<Integer> mSelectedItems;
    private DashBoardActionListener mListener;
    private int mDefaultBackground;
    private int mDefaultProgressColor;
    private int mDefaultButtonTextColor;
    private int mDefaultButtonTintColor;
    private int mDefaultButtonBackground;
    private int mWidth;
    private int mSpanCnt;
    private int spacing;
    private LayoutInflater mInflater;
    private List<Item> mData;

    public final static int TYPE_GROUP = 0;
    public final static int TYPE_TEXT = 1;
    public final static int TYPE_PROGRESS = 2;
    public final static int TYPE_SWITCH = 3;
    public final static int TYPE_CUSTOM = 4;

    private final static String TAG = DashBoardAdapter.class.getSimpleName();
}
