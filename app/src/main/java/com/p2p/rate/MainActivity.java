package com.p2p.rate;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    EditText amt;
    TextView out;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("P2P USDT Rate (INR)");
        title.setTextSize(22);
        root.addView(title);

        amt = new EditText(this);
        amt.setInputType(InputType.TYPE_CLASS_NUMBER);
        amt.setText("10000");
        amt.setHint("Kitne Rs ka trade?");
        root.addView(amt);

        Button btn = new Button(this);
        btn.setText("Rate Refresh");
        root.addView(btn);

        out = new TextView(this);
        out.setTextSize(18);
        out.setPadding(0, 30, 0, 0);
        root.addView(out);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        btn.setOnClickListener(v -> fetch());
        fetch();
    }

    void fetch() {
        out.setText("Load ho raha hai...");
        final String a = amt.getText().toString().trim();
        new Thread(() -> {
            String res;
            try {
                double amount = Double.parseDouble(a);
                double buy = price("BUY", a);
                double sell = price("SELL", a);
                double usdt = amount / buy;
                double sellv = usdt * sell;
                double gross = sellv - amount;
                double tax = gross > 0 ? gross * 0.312 : 0;
                double net = gross - tax;
                double tds = sellv * 0.01;
                res = String.format(
                    "Buy rate: Rs %.2f\nSell rate: Rs %.2f\nSpread: Rs %.2f\n\n"
                    + "Kachcha profit: Rs %.2f\nTax (30%% + cess): Rs %.2f\n"
                    + "Tax ke baad profit: Rs %.2f\n1%% TDS (cash fasega): Rs %.2f\n\n"
                    + "Ye sirf andaza hai. Trade khud karo.",
                    buy, sell, sell - buy, gross, tax, net, tds);
            } catch (Exception e) {
                res = "Error: " + e;
            }
            final String r = res;
            runOnUiThread(() -> out.setText(r));
        }).start();
    }

    double price(String type, String amount) throws Exception {
        URL u = new URL("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search");
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setDoOutput(true);
        String body = "{\"fiat\":\"INR\",\"page\":1,\"rows\":5,\"tradeType\":\"" + type
            + "\",\"asset\":\"USDT\",\"countries\":[],\"proMerchantAds\":false,"
            + "\"shieldMerchantAds\":false,\"publisherType\":null,\"payTypes\":[],"
            + "\"transAmount\":\"" + amount + "\"}";
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        InputStream is = c.getInputStream();
        Scanner s = new Scanner(is).useDelimiter("\\A");
        String txt = s.hasNext() ? s.next() : "";
        JSONArray d = new JSONObject(txt).getJSONArray("data");
        return Double.parseDouble(d.getJSONObject(0).getJSONObject("adv").getString("price"));
    }
}
