package com.example.smartdining;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Adds a consistent toolbar to the existing XML screens. */
public final class AppChrome {
    private AppChrome() { }

    public static void install(Activity activity, String title, boolean home, boolean logout) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0 || content.getTag() != null) return;
        View oldRoot = content.getChildAt(0);
        content.removeView(oldRoot);
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.rgb(247, 249, 252));
        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        toolbar.setBackgroundColor(Color.rgb(23, 50, 77));
        if (!home) {
            Button back = new Button(activity);
            back.setText("‹"); back.setTextSize(30); back.setTextColor(Color.WHITE);
            back.setAllCaps(false); back.setMinWidth(dp(activity, 44));
            back.setBackgroundColor(Color.TRANSPARENT);
            toolbar.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
            back.setOnClickListener(v -> activity.finish());
        }
        TextView heading = new TextView(activity);
        heading.setText(title); heading.setTextColor(Color.WHITE); heading.setTextSize(19);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        hp.leftMargin = dp(activity, 8); toolbar.addView(heading, hp);
        if (logout) {
            Button out = new Button(activity); out.setText("Logout"); out.setTextSize(12); out.setAllCaps(false);
            out.setTextColor(Color.WHITE); out.setBackground(tintBackground());
            toolbar.addView(out, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 42)));
            out.setOnClickListener(v -> {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent); activity.finish();
            });
        }
        wrapper.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 60)));
        ViewGroup.LayoutParams oldParams = oldRoot.getLayoutParams();
        wrapper.addView(oldRoot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(wrapper, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.setTag(Boolean.TRUE);
    }
    private static GradientDrawable tintBackground() {
        GradientDrawable d = new GradientDrawable(); d.setColor(Color.rgb(44, 92, 130)); d.setCornerRadius(24); return d;
    }
    public static int dp(Activity a, int v) { return (int)(v * a.getResources().getDisplayMetrics().density + 0.5f); }
}
