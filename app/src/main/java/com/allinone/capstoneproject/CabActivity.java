package com.allinone.capstoneproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.auth.OAuth2Credentials;
import com.uber.sdk.rides.client.SessionConfiguration;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class CabActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private final String CLIENT_ID = BuildConfig.CLIENT_ID;
    private final String CLIENT_SECRET = BuildConfig.CLIENT_SECRET;
    private final String SERVER_TOKEN = BuildConfig.SERVER_TOKEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        final Context mContext = this;
        setContentView(R.layout.activity_cab);

        //Defining the content authority
        String CONTENT_AUTHORITY="com.allinone.capstoneproject";

        //Defining the Base Content Uri
        final String PATH_UBER="uberRedirect";
        final Uri BASE_CONTENT_URI= Uri.parse("https://"+CONTENT_AUTHORITY);
        final Uri REDIRECT_URI=  BASE_CONTENT_URI.buildUpon().appendPath(PATH_UBER).build();

        SessionConfiguration config = new SessionConfiguration.Builder()
                // mandatory
                .setClientId(CLIENT_ID)
                // required for enhanced button features
                .setServerToken(SERVER_TOKEN)
                //required for authorization
                .setClientSecret(CLIENT_SECRET)
                // required for implicit grant authentication
                .setRedirectUri((REDIRECT_URI).toString())
                // required scope for Ride Request Widget features
                .setScopes(Arrays.asList(Scope.RIDE_WIDGETS, Scope.PROFILE, Scope.REQUEST))
                // optional: set Sandbox as operating environment
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .build();

        UberSdk.initialize(config);


        /**
         * The Authorization Code flow is a two-step authorization process. The first step is having the user authorize your
         * app and the second involves requesting an OAuth 2.0 access token from Uber.
         * This process is mandatory if you want to take actions on behalf of a user or access their information.
         */
        OAuth2Credentials credentials = new OAuth2Credentials.Builder()
                .setSessionConfiguration(config)
                .build();
        String authorizationUrl = null;
        try {
            authorizationUrl = credentials.getAuthorizationUrl();
            Log.d(TAG, authorizationUrl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }



        final Button loginButton = (Button) findViewById(R.id.button);
        final String finalAuthorizationUrl = authorizationUrl;
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(mContext);
                final AlertDialog alertDialog = alert.create();
                alertDialog.setTitle("Login");

                WebView wv = new WebView(mContext){
                    @Override
                    public boolean onCheckIsTextEditor() {
                        return true;
                    }
                };

                wv.getSettings().setJavaScriptEnabled(true);
                wv.getSettings().setLoadWithOverviewMode(true);
                wv.getSettings().setUseWideViewPort(true);

                wv.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.d(TAG, "onPageStarted: Loading...");
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);

                        if (url.startsWith(REDIRECT_URI.toString())) {
                            Pattern p = Pattern.compile(".+code=(.+?(?=&|$))");
                            Matcher m = p.matcher(url);
                            if (m.matches()) {
                                alertDialog.dismiss();
                                acquireAccessToken(m.group(1));
                            }
                            return true; // we've handled the url
                        } else {
                            return false;
                        }

                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, url);
                        Log.d(TAG, "onPageFinished: Url Loaded");
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);
                        Log.d(TAG, "onReceivedError: "+error);

                    }
                });
                wv.loadUrl(finalAuthorizationUrl);

                alertDialog.setView(wv);
                alertDialog.show();


            }
        });



    }

    private void acquireAccessToken(String code) {

        final String BASE_URL="https://login.uber.com/oauth/v2/token?";
        String clienId="client_id";
        String clientSecret = "client_secret";
        String GRANT_TYPE = "grant_type";
        String REDIRECT_URI = "redirect_uri";
        String CODE = "code";

        Uri uri=Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter(clienId,CLIENT_ID)
                .appendQueryParameter(clientSecret, CLIENT_SECRET)
                .appendQueryParameter(GRANT_TYPE,"authorization_code")
                .appendQueryParameter(REDIRECT_URI,"https://com.allinone.capstoneproject/uberRedirect")
                .appendQueryParameter(CODE,code)
                .build();


        AsyncTask task = new AsyncTask<Object, Integer, String>() {
            @Override
            protected String doInBackground(Object[] urls) {
                return executeRequest((String) urls[0], "POST", "");
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    String accessToken = (String)json.get("access_token");
                    String refreshToken = (String) json.get("refresh_token");
                    Log.d(TAG, accessToken);
                    Log.d(TAG, refreshToken);
                } catch(Exception ex) {
                    Log.e("Cab Activity", "Request failed.", ex);
                }
            }
        };
        task.execute(uri.toString());
    }

    private String executeRequest(String url, String method, String content) {
        StringBuilder buffer = new StringBuilder();
        try {
            URL connUrl = new URL(url);

            HttpsURLConnection conn = (HttpsURLConnection)connUrl.openConnection();

            if (content != null) {
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(content.length()));
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                for (int i = 0; i < content.length(); i++)
                    writer.write(content.charAt(i));
            }

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            int c = reader.read();
            while (c != -1) {
                buffer.append((char)c);
                c = reader.read();
            }

            conn.disconnect();
        } catch (Exception ex) {
            Log.e("Cab", "Request failed.", ex);
        }
        return buffer.toString();
    }



}
