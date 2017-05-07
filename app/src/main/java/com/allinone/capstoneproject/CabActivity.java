package com.allinone.capstoneproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class CabActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context mContext = this;
        setContentView(R.layout.activity_cab);
        String authorizationUrl = null;
        Intent intent = getIntent();

        authorizationUrl = intent.getStringExtra("Url");

        Button loginButton = (Button) findViewById(R.id.button);
        final String finalAuthorizationUrl = authorizationUrl;
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(mContext);
                alert.setTitle("Login");

                WebView wv = new WebView(mContext);
                wv.loadUrl(finalAuthorizationUrl);
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);

                        return true;
                    }
                });

                alert.setView(wv);
                alert.show();

                wv.requestFocus(View.FOCUS_DOWN);
                wv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                                if (!v.hasFocus()) {
                                    v.requestFocus();
                                }
                                break;
                        }
                        return false;
                    }
                });


            }
        });



    }

}
