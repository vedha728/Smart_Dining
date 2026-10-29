package com.example.smartdining;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class FoodRowAdapter extends BaseAdapter {
    public static class FoodRow { public int id; public String name, detail, imageUrl; public boolean special;
        public FoodRow(int id,String name,String detail,String imageUrl,boolean special){this.id=id;this.name=name;this.detail=detail;this.imageUrl=imageUrl;this.special=special;} }
    private final Activity activity; private final List<FoodRow> rows;
    public FoodRowAdapter(Activity a,List<FoodRow> r){activity=a;rows=r;}
    public int getCount(){return rows.size();} public Object getItem(int p){return rows.get(p);} public long getItemId(int p){return rows.get(p).id;}
    public View getView(int position, View convert, ViewGroup parent) {
        FoodRow row=rows.get(position); LinearLayout card=new LinearLayout(activity); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(12,10,12,10);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(20); bg.setStroke(1,Color.rgb(229,235,241)); card.setBackground(bg);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,5,0,5); card.setLayoutParams(cp);
        ImageView image=new ImageView(activity); int size=AppChrome.dp(activity,64); LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(size,size); ip.setMargins(0,0,AppChrome.dp(activity,12),0); image.setScaleType(ImageView.ScaleType.CENTER_CROP); card.addView(image,ip); FoodImageLoader.load(row.imageUrl,image,true);
        LinearLayout textBox=new LinearLayout(activity); textBox.setOrientation(LinearLayout.VERTICAL); textBox.setGravity(Gravity.CENTER_VERTICAL); card.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=new TextView(activity); name.setText(row.special ? "★  "+row.name : row.name); name.setTextColor(row.special?Color.rgb(185,85,20):Color.rgb(28,48,68)); name.setTextSize(16); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); textBox.addView(name);
        TextView detail=new TextView(activity); detail.setText(row.detail); detail.setTextColor(Color.rgb(94,108,122)); detail.setTextSize(13); LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2); dp.topMargin=AppChrome.dp(activity,4); textBox.addView(detail,dp);
        TextView arrow=new TextView(activity); arrow.setText("›"); arrow.setTextSize(28); arrow.setTextColor(Color.rgb(105,126,145)); card.addView(arrow,new LinearLayout.LayoutParams(-2,-2)); return card;
    }
}
