package com.allinone.capstoneproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.auth.OAuth2Credentials;
import com.uber.sdk.rides.client.SessionConfiguration;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Cab booking menu button
        Button cab = (Button) findViewById(R.id.cab);

        //Food menu button
        Button food = (Button) findViewById(R.id.food);

        //News menu button
        Button news = (Button) findViewById(R.id.news);



        //Defining the content authority
        String CONTENT_AUTHORITY="com.allinone.capstoneproject";
        final Context mContext= this;

        //Defining the Base Content Uri
        final Uri BASE_CONTENT_URI= Uri.parse("content://"+CONTENT_AUTHORITY);

        SessionConfiguration config = new SessionConfiguration.Builder()
                // mandatory
                .setClientId("FPXHFmTeAyQclj2kSHSkOMRQsRRe3G0h")
                // required for enhanced button features
                .setServerToken("Snxi8mV7MP61JyoaEFuJkkJpPAZPt4gPrFKvanl8")
                //required for authorization
                .setClientSecret("WvnHIdKTnFNRYYKpmh38xS9u--YwiefnL4Ln2S4T")
                // required for implicit grant authentication
                .setRedirectUri((BASE_CONTENT_URI).toString())
                // required scope for Ride Request Widget features
                .setScopes(Arrays.asList(Scope.RIDE_WIDGETS, Scope.PROFILE))
                // optional: set Sandbox as operating environment
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .build();


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
            Log.d("Main", authorizationUrl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        final String finalAuthorizationUrl = authorizationUrl;
        Log.d("Url", finalAuthorizationUrl);
        cab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Launch cab activity
                 */
                Intent intent = new Intent(mContext, CabActivity.class);
                intent.putExtra("Url", finalAuthorizationUrl);
                startActivity(intent);
            }
        });



    }


}
