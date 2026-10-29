package com.example.smartdining;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class AiAssistantActivity extends AppCompatActivity {
    private int userId;
    private String userName;
    private EditText etApiKey, etMessage;
    private Button btnSend, btnSaveApiKey;
    private LinearLayout chatMessages;
    private ScrollView scrollChat;
    private DatabaseHelper helper;
    private final HashMap<Integer, FoodInfo> foods = new HashMap<>();
    private final ArrayList<PendingOrder> pendingOrders = new ArrayList<>();
    private double activeBudget = -1;
    private static class FoodInfo { int id, hotelId; String name, hotel, imageUrl; double price; FoodInfo(int i,int h,String n,String ht,double p,String image){id=i;hotelId=h;name=n;hotel=ht;price=p;imageUrl=image;} }
    private static class PendingOrder { int foodId, quantity; FoodInfo info; PendingOrder(int f,int q,FoodInfo i){foodId=f;quantity=q;info=i;} }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_ai_assistant);
        AppChrome.install(this, "Smart Dining AI", false, false);
        userId=getIntent().getIntExtra("user_id",-1); userName=getIntent().getStringExtra("user_name");
        helper=new DatabaseHelper(this);
        etApiKey=findViewById(R.id.etApiKey); etMessage=findViewById(R.id.etMessage); btnSend=findViewById(R.id.btnSend);
        btnSaveApiKey=findViewById(R.id.btnSaveApiKey);
        findViewById(R.id.tvApiHelp).setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/app/apikey"))); } catch(Exception ignored) { Toast.makeText(this,"Open Google AI Studio in your browser",Toast.LENGTH_SHORT).show(); } });
        chatMessages=findViewById(R.id.chatMessages); scrollChat=findViewById(R.id.scrollChat);
        SharedPreferences prefs=getSharedPreferences("smart_dining_ai",MODE_PRIVATE);
        etApiKey.setText(prefs.getString("gemini_api_key",""));
        btnSaveApiKey.setOnClickListener(v -> { String key=etApiKey.getText().toString().trim(); if(key.isEmpty()){Toast.makeText(this,"Enter a Gemini API key",Toast.LENGTH_SHORT).show();return;} prefs.edit().putString("gemini_api_key",key).apply(); Toast.makeText(this,"API key saved on this device",Toast.LENGTH_SHORT).show(); });
        btnSend.setOnClickListener(v -> sendMessage());
        addBubble("Hi "+(userName==null?"there":userName)+"! I can recommend food using your budget, previous orders, local time, menu and daily specials. You can also ask me to order one or more dishes.",false);
    }

    private void sendMessage() {
        String message=etMessage.getText().toString().trim(); if(message.isEmpty()) return;
        String typedKey=etApiKey.getText().toString().trim();
        if(!typedKey.isEmpty()) getSharedPreferences("smart_dining_ai",MODE_PRIVATE).edit().putString("gemini_api_key",typedKey).apply();
        String key=getSharedPreferences("smart_dining_ai",MODE_PRIVATE).getString("gemini_api_key","");
        if(key.isEmpty()){ Toast.makeText(this,"Enter your Gemini API key first",Toast.LENGTH_LONG).show(); etApiKey.requestFocus(); return; }
        activeBudget = extractBudget(message);
        etMessage.setText(""); addBubble(message,true); btnSend.setEnabled(false); btnSend.setText("...");
        new Thread(() -> {
            try {
                String context=buildContext(message);
                JSONObject payload=new JSONObject();
                JSONArray contents=new JSONArray(); JSONObject content=new JSONObject(); JSONArray parts=new JSONArray(); JSONObject part=new JSONObject();
                String prompt="You are SmartDining's food recommendation AND cart-ordering assistant. Use only food and hotel records in the supplied context. You ARE allowed to create cart clusters and add items to them through the app's order-action response; never tell the user you cannot create clusters. For recommendations, respect budget, availability, order history, local time, daily specials, price and variety. For order requests, identify every requested dish, its exact hotel and quantity. Return ONLY valid JSON with this schema: {\\\"reply\\\":\\\"short friendly answer\\\",\\\"recommendations\\\":[{\\\"food_id\\\":1,\\\"reason\\\":\\\"why\\\"}],\\\"orders\\\":[{\\\"food_id\\\":1,\\\"quantity\\\":2}],\\\"needs_clarification\\\":false,\\\"clarification_question\\\":\\\"\\\",\\\"cluster_action\\\":\\\"create_named|choose_existing|none\\\",\\\"cluster_name\\\":\\\"\\\"}. Return up to 5 recommendations. Include orders only when user explicitly requests ordering/adding/buying. Use food_id values from context only; quantities must be positive integers. IMPORTANT SAFETY/AMBIGUITY RULES: before any cart mutation, if any requested dish cannot be uniquely matched to a menu item, a hotel is ambiguous, quantity is missing/unclear in a way that affects the order, the requested cluster name is unclear, or any other material detail is uncertain, set needs_clarification=true, ask ONE concise specific question in reply and clarification_question, and return orders=[] and cluster_action=none. Never guess or partially order an ambiguous request. If user explicitly gives a new cluster name (e.g. 'create a cluster named Dinner'), set cluster_action=create_named and cluster_name to that exact name. If user explicitly names an existing cluster, use choose_existing and return the orders for the app to show the cluster chooser if needed. If the user asks to order but does not specify a cluster, use choose_existing so the app asks them to choose/create one. If no order request, cluster_action=none. Do not claim items were saved; the app will save them and confirm only after SQLite succeeds. Context:\\n"+context+"\\nUser message: "+message;
                part.put("text",prompt); parts.put(part); content.put("parts",parts); contents.put(content); payload.put("contents",contents);
                JSONObject generation=new JSONObject(); generation.put("temperature",0.25); generation.put("responseMimeType","application/json"); payload.put("generationConfig",generation);
                URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+key);
                HttpURLConnection conn=(HttpURLConnection)url.openConnection(); conn.setRequestMethod("POST"); conn.setConnectTimeout(12000); conn.setReadTimeout(25000); conn.setDoOutput(true); conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                byte[] body=payload.toString().getBytes(StandardCharsets.UTF_8); try(OutputStream os=conn.getOutputStream()){os.write(body);}
                int code=conn.getResponseCode(); InputStream stream=code>=200&&code<300?conn.getInputStream():conn.getErrorStream(); String response=readAll(stream); conn.disconnect();
                if(code<200||code>=300) throw new Exception("Gemini API error ("+code+"): "+shorten(response,250));
                JSONObject outer=new JSONObject(response); JSONArray candidates=outer.optJSONArray("candidates"); if(candidates==null||candidates.length()==0) throw new Exception("The AI returned no response. Try again.");
                JSONArray responseParts=candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts"); String raw="";
                for(int i=0;i<responseParts.length();i++){JSONObject responsePart=responseParts.optJSONObject(i);if(responsePart!=null&&!responsePart.optBoolean("thought",false)){String candidateText=responsePart.optString("text","").trim();if(!candidateText.isEmpty())raw=candidateText;}}
                if(raw.isEmpty()&&responseParts.length()>0)raw=responseParts.getJSONObject(responseParts.length()-1).optString("text","").trim();
                JSONObject result=parseJson(raw);
                runOnUiThread(() -> { btnSend.setEnabled(true); btnSend.setText("Send"); renderResult(result); });
            } catch(Exception e) { runOnUiThread(() -> { btnSend.setEnabled(true); btnSend.setText("Send"); addBubble("I couldn't reach the AI service: "+e.getMessage()+". Check your internet connection and API key.",false); }); }
        }).start();
    }

    private String buildContext(String userMessage) {
        foods.clear(); StringBuilder out=new StringBuilder();
        String lowerMessage=userMessage.toLowerCase(Locale.ROOT);
        boolean explicitOrder = !lowerMessage.contains("order history") && lowerMessage.matches("(?s).*(\\border\\b|\\badd\\b|\\bbuy\\b|\\bget me\\b|\\bplace an order\\b).*" );
        double budget = extractBudget(userMessage);
        if (!explicitOrder && budget >= 0) out.append("User budget constraint: ₹").append(budget).append(". The menu list below is already filtered to items at or below this budget.\n");
        String now=new SimpleDateFormat("EEEE, dd MMM yyyy, hh:mm a",Locale.getDefault()).format(new Date()); out.append("Local date/time: ").append(now).append("\nCustomer id: ").append(userId).append("\nAvailable hotels and food items (prices in INR):\n");
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date()); SQLiteDatabase db=helper.getReadableDatabase();
        Cursor c=db.rawQuery("SELECT f.food_id,f.hotel_id,f.name,h.name,f.price,f.category,f.description,f.availability,f.rating,f.image_url,CASE WHEN ds.special_id IS NULL THEN 0 ELSE 1 END FROM food f JOIN hotels h ON h.hotel_id=f.hotel_id LEFT JOIN daily_specials ds ON ds.food_id=f.food_id AND ds.hotel_id=f.hotel_id AND ds.date=? WHERE f.availability=1 ORDER BY h.name,f.name",new String[]{today});
        int count=0; while(c.moveToNext()&&count<180){int id=c.getInt(0),hid=c.getInt(1); String name=c.getString(2),hotel=c.getString(3);double price=c.getDouble(4); if(!explicitOrder && budget>=0 && price>budget) continue; foods.put(id,new FoodInfo(id,hid,name,hotel,price,c.isNull(9)?"":c.getString(9)));out.append("food_id=").append(id).append(" | ").append(name).append(" | hotel=").append(hotel).append(" (hotel_id=").append(hid).append(") | price=₹").append(price).append(" | category=").append(c.isNull(5)?"":c.getString(5)).append(" | description=").append(c.isNull(6)?"":c.getString(6)).append(" | rating=").append(c.getDouble(8)).append(" | daily_special=").append(c.getInt(10)==1?"YES":"no").append("\n");count++;} c.close();
        out.append("\nRecent customer orders (latest 12):\n"); Cursor h=db.rawQuery("SELECT f.name,ht.name,o.quantity,o.date FROM orders o JOIN food f ON f.food_id=o.food_id JOIN hotels ht ON ht.hotel_id=o.hotel_id WHERE o.user_id=? ORDER BY o.order_id DESC LIMIT 12",new String[]{String.valueOf(userId)}); int history=0;while(h.moveToNext()){out.append(h.getString(0)).append(" from ").append(h.getString(1)).append(" x").append(h.getInt(2)).append(" on ").append(h.getString(3)).append("\n");history++;} if(history==0)out.append("No previous orders.\n");h.close();
        Cursor pref=db.rawQuery("SELECT preferences FROM users WHERE user_id=?",new String[]{String.valueOf(userId)}); if(pref.moveToFirst()&&!pref.isNull(0))out.append("Saved food preferences: ").append(pref.getString(0)).append("\n");pref.close();db.close();
        if(count==0)out.append("No available menu items currently. Do not invent food.\n"); return out.toString();
    }
    private double extractBudget(String message) {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(?:₹|rs\\.?\\s*|inr\\s*)([0-9]{1,6})").matcher(message);
        if(m.find()) return Double.parseDouble(m.group(1));
        m=java.util.regex.Pattern.compile("(?i)([0-9]{1,6})\\s*(?:rs\\.?|rupees?)").matcher(message);
        if(m.find()) return Double.parseDouble(m.group(1));
        m=java.util.regex.Pattern.compile("(?i)budget\\D{0,20}([0-9]{1,6})").matcher(message);
        if(m.find()) return Double.parseDouble(m.group(1));
        return -1;
    }
    private JSONObject parseJson(String raw) throws Exception { String s=raw; if(s.startsWith("```")){s=s.replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");} int a=s.indexOf('{'),b=s.lastIndexOf('}'); if(a<0||b<a)throw new Exception("AI response wasn't valid JSON. Please try again."); return new JSONObject(s.substring(a,b+1)); }
    private void renderResult(JSONObject result) {
        addBubble(result.optString("reply","Here are some options."),false);
        if (result.optBoolean("needs_clarification", false)) {
            String question = result.optString("clarification_question", "").trim();
            if (!question.isEmpty() && !question.equals(result.optString("reply", ""))) addBubble(question, false);
            pendingOrders.clear();
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
            return;
        }
        JSONArray recs=result.optJSONArray("recommendations");
        if(recs!=null) for(int i=0;i<recs.length();i++){JSONObject rec=recs.optJSONObject(i);if(rec==null)continue;int id=rec.optInt("food_id",-1);FoodInfo f=foods.get(id);if(f==null)continue;addRecommendation(f,rec.optString("reason","Recommended for you"));}
        JSONArray orders=result.optJSONArray("orders"); pendingOrders.clear();
        if(orders!=null) for(int i=0;i<orders.length();i++){JSONObject order=orders.optJSONObject(i);if(order==null)continue;int id=order.optInt("food_id",-1),q=Math.max(1,Math.min(99,order.optInt("quantity",1)));if(foods.containsKey(id)){boolean merged=false;for(PendingOrder po:pendingOrders)if(po.foodId==id){po.quantity=Math.min(99,po.quantity+q);merged=true;break;}if(!merged)pendingOrders.add(new PendingOrder(id,q,foods.get(id)));}}
        if(!pendingOrders.isEmpty() && activeBudget >= 0) {
            double requestedTotal=0; for(PendingOrder p:pendingOrders){if(p.info!=null) requestedTotal += p.info.price*p.quantity;}
            if(requestedTotal > activeBudget) {
                addBubble("Those items total ₹"+String.format(Locale.getDefault(),"%.2f",requestedTotal)+", which exceeds your stated budget of ₹"+String.format(Locale.getDefault(),"%.2f",activeBudget)+". I haven't added them. Ask me for alternatives within budget.",false);
                pendingOrders.clear(); scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN)); return;
            }
        }
        if(!pendingOrders.isEmpty()) {
            if (orders == null || orders.length() != pendingOrders.size()) {
                pendingOrders.clear();
                addBubble("I couldn't match every requested item confidently. Which exact dish and hotel did you mean?", false);
                return;
            }
            String clusterAction = result.optString("cluster_action", "choose_existing");
            String clusterName = result.optString("cluster_name", "").trim();
            ArrayList<PendingOrder> snapshot = new ArrayList<>();
            for (PendingOrder p : pendingOrders) snapshot.add(new PendingOrder(p.foodId, p.quantity, p.info));
            if ("create_named".equals(clusterAction)) {
                if (clusterName.isEmpty()) {
                    pendingOrders.clear();
                    addBubble("What name would you like for the new cart cluster?", false);
                    return;
                }
                StringBuilder summary = new StringBuilder("I'll create \"").append(clusterName).append("\" with:\\n");
                double total = 0;
                for (PendingOrder p : snapshot) {
                    summary.append("• ").append(p.info.name).append(" × ").append(p.quantity)
                           .append(" from ").append(p.info.hotel).append("\\n");
                    total += p.info.price * p.quantity;
                }
                summary.append("Estimated total: ₹").append(String.format(Locale.getDefault(), "%.2f", total));
                addBubble(summary.toString(), false);
                // The user explicitly named a new cluster and the AI resolved every item.
                // Execute the requested action; the database method reports success only after saving.
                createNamedClusterAndAdd(clusterName, snapshot);
            } else {
                StringBuilder summary = new StringBuilder("Order request:\\n");
                double total = 0;
                for (PendingOrder p : snapshot) {
                    FoodInfo f = p.info;
                    summary.append("• ").append(f.name).append(" × ").append(p.quantity)
                           .append(" — ₹").append(String.format(Locale.getDefault(), "%.2f", f.price * p.quantity)).append("\\n");
                    total += f.price * p.quantity;
                }
                summary.append("Estimated total: ₹").append(String.format(Locale.getDefault(), "%.2f", total))
                       .append("\\nChoose a cluster to add these items.");
                addBubble(summary.toString(), false);
                Button add = new Button(this);
                add.setText("Add all items to a cart cluster");
                add.setAllCaps(false);
                chatMessages.addView(add);
                add.setOnClickListener(v -> chooseClusterForOrders(snapshot));
            }
        }
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }
    private void addRecommendation(FoodInfo f,String reason){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(14,12,14,12);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(18);bg.setStroke(2,Color.rgb(213,224,237));card.setBackground(bg);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,6,0,6);chatMessages.addView(card,cp);ImageView image=new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP); card.addView(image,new LinearLayout.LayoutParams(-1,AppChrome.dp(this,130))); FoodImageLoader.load(f.imageUrl,image,false); TextView title=new TextView(this);title.setText(f.name+"  ·  ₹"+String.format(Locale.getDefault(),"%.2f",f.price));title.setTextSize(16);title.setTextColor(Color.rgb(23,50,77));title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=AppChrome.dp(this,8);card.addView(title,tp);TextView why=new TextView(this);why.setText(f.hotel+"\n"+reason);why.setTextSize(13);why.setTextColor(Color.rgb(85,102,119));card.addView(why);Button open=new Button(this);open.setText("View item");open.setAllCaps(false);card.addView(open);open.setOnClickListener(v->{Intent intent=new Intent(this,FoodDetailsActivity.class);intent.putExtra("food_id",f.id);intent.putExtra("hotel_id",f.hotelId);intent.putExtra("user_id",userId);startActivity(intent);});}
    private void addBubble(String text,boolean user){TextView bubble=new TextView(this);bubble.setText(text);bubble.setTextSize(15);bubble.setTextColor(user?Color.WHITE:Color.rgb(32,50,68));bubble.setPadding(14,12,14,12);GradientDrawable bg=new GradientDrawable();bg.setColor(user?Color.rgb(43,103,145):Color.WHITE);bg.setCornerRadius(18);bubble.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(user?getResources().getDisplayMetrics().widthPixels*3/4:-1,-2);p.setMargins(user?AppChrome.dp(this,42):0,AppChrome.dp(this,5),user?0:AppChrome.dp(this,25),AppChrome.dp(this,5));p.gravity=user?Gravity.RIGHT:Gravity.LEFT;chatMessages.addView(bubble,p);scrollChat.post(()->scrollChat.fullScroll(View.FOCUS_DOWN));}
    private void createNamedClusterAndAdd(String name, ArrayList<PendingOrder> orderSnapshot) {
        if (userId <= 0 || name == null || name.trim().isEmpty() || orderSnapshot == null || orderSnapshot.isEmpty()) {
            addBubble("I need a valid cluster name and at least one confirmed item before creating the cluster.", false);
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long clusterId = -1;
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("cluster_name", name.trim());
            clusterId = db.insertOrThrow("cart_clusters", null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            addBubble("I couldn't create that cluster: " + e.getMessage() + ". No items were added.", false);
            return;
        } finally {
            db.endTransaction();
            db.close();
        }
        if (clusterId > 0) addOrdersToCluster((int) clusterId, orderSnapshot);
        else addBubble("I couldn't create the cluster, so no items were added.", false);
    }

    private void chooseClusterForOrders(ArrayList<PendingOrder> orderSnapshot) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT cluster_id, cluster_name FROM cart_clusters WHERE user_id=? ORDER BY cluster_name",
                    new String[]{String.valueOf(userId)});
            while (c.moveToNext()) {
                ids.add(c.getInt(0));
                names.add(c.getString(1));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not load your cart clusters. Please update/reinstall the app and try again.", Toast.LENGTH_LONG).show();
            return;
        } finally {
            if (c != null) c.close();
            db.close();
        }
        names.add("+ Create new cluster");
        new AlertDialog.Builder(this)
                .setTitle("Add order to which cluster?")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    if (which == names.size() - 1) askClusterName(orderSnapshot);
                    else if (which >= 0 && which < ids.size()) addOrdersToCluster(ids.get(which), orderSnapshot);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askClusterName(ArrayList<PendingOrder> orderSnapshot) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("e.g. Dinner, Weekend");
        new AlertDialog.Builder(this)
                .setTitle("Create cart cluster")
                .setMessage("Give this group of items a name")
                .setView(input)
                .setPositiveButton("Create and add items", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Enter a cluster name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SQLiteDatabase db = helper.getWritableDatabase();
                    long id = -1;
                    try {
                        ContentValues values = new ContentValues();
                        values.put("user_id", userId);
                        values.put("cluster_name", name);
                        id = db.insert("cart_clusters", null, values);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not create cluster: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        db.close();
                    }
                    if (id > 0) addOrdersToCluster((int) id, orderSnapshot);
                    else Toast.makeText(this, "Could not create cluster. Please try another name.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addOrdersToCluster(int clusterId, ArrayList<PendingOrder> orderSnapshot) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int added = 0;
        try {
            // Verify the selected cluster belongs to the signed-in customer.
            Cursor owner = db.rawQuery("SELECT cluster_id FROM cart_clusters WHERE cluster_id=? AND user_id=?",
                    new String[]{String.valueOf(clusterId), String.valueOf(userId)});
            boolean validCluster = owner.moveToFirst();
            owner.close();
            if (!validCluster) {
                Toast.makeText(this, "That cart cluster doesn't belong to this account. Please choose again.", Toast.LENGTH_LONG).show();
                return;
            }

            db.beginTransaction();
            try {
                for (PendingOrder pending : orderSnapshot) {
                    FoodInfo food = pending.info != null ? pending.info : foods.get(pending.foodId);
                    if (food == null || pending.quantity < 1) continue;
                    Cursor existing = null;
                    try {
                        existing = db.rawQuery("SELECT item_id, quantity FROM cart_items WHERE cluster_id=? AND food_id=? AND hotel_id=?",
                                new String[]{String.valueOf(clusterId), String.valueOf(food.id), String.valueOf(food.hotelId)});
                        if (existing.moveToFirst()) {
                            ContentValues values = new ContentValues();
                            values.put("quantity", Math.min(999, existing.getInt(1) + pending.quantity));
                            db.update("cart_items", values, "item_id=?", new String[]{String.valueOf(existing.getInt(0))});
                        } else {
                            ContentValues values = new ContentValues();
                            values.put("cluster_id", clusterId);
                            values.put("food_id", food.id);
                            values.put("hotel_id", food.hotelId);
                            values.put("quantity", pending.quantity);
                            values.put("unit_price", food.price);
                            long inserted = db.insertOrThrow("cart_items", null, values);
                            if (inserted < 0) throw new Exception("Database rejected " + food.name);
                        }
                        added++;
                    } finally {
                        if (existing != null) existing.close();
                    }
                }
                if (added == 0) throw new Exception("No valid food items were found in the AI response.");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            Toast.makeText(this, "Added " + added + " item(s) to your cart cluster", Toast.LENGTH_LONG).show();
            addBubble("Done — all requested items were added to your selected cluster.", false);
            pendingOrders.clear();
        } catch (Exception e) {
            Toast.makeText(this, "Could not add items to the cluster: " + e.getMessage(), Toast.LENGTH_LONG).show();
            addBubble("I couldn't save those items to the cluster. Your cart was not partially updated. Please try again.", false);
        } finally {
            db.close();
        }
    }

    private String readAll(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)b.append(line);}return b.toString();}
    private String shorten(String s,int n){return s.length()>n?s.substring(0,n):s;}
}
