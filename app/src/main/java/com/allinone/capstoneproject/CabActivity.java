package com.allinone.capstoneproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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


public class CabActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = getClass().getSimpleName();
    private final String CLIENT_ID = BuildConfig.CLIENT_ID;
    private final String CLIENT_SECRET = BuildConfig.CLIENT_SECRET;
    private final String SERVER_TOKEN = BuildConfig.SERVER_TOKEN;
    private final Context mContext = this;
    android.location.LocationListener locationListener;
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public AddressResultReceiver mResultReceiver;
    private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cab);

        //Defining the content authority
        String CONTENT_AUTHORITY = "com.allinone.capstoneproject";

        //Defining the Base Content Uri
        final String PATH_UBER = "uberRedirect";
        final Uri BASE_CONTENT_URI = Uri.parse("https://" + CONTENT_AUTHORITY);
        final Uri REDIRECT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_UBER).build();

        //Firstly check for google play services & then establish a connection with GoogleApiClient
        if (checkGooglePlayServices()) {
            buildGoogleApiClient();
        }


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
                                acquireAccessToken(m.group(1));
                                alertDialog.dismiss();
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

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Access the user current location
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.d(TAG, "onLocationChanged: " + location);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, latitude+"");
                Log.d(TAG, longitude+ "");
                mResultReceiver = new AddressResultReceiver(null);
                Intent intent = new Intent(mContext, FetchAddressIntentService.class);
                intent.putExtra(Constants.RECEIVER, mResultReceiver);
                intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
                startService(intent);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 100, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 100, locationListener);

    }


    private boolean checkGooglePlayServices() {

        int checkGooglePlayServices = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
			/*
			* google play services is missing or update is required
			*  return code could be
			* SUCCESS,
			* SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
			* SERVICE_DISABLED, SERVICE_INVALID.
			*/
            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
                    this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();

            return false;
        }

        return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RECOVER_PLAY_SERVICES) {

            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Google Play Services must be installed.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            Log.d(TAG, mAddressOutput);
//            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(mContext, getString(R.string.address_found), Toast.LENGTH_LONG).show();
            }

        }
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

                    //Storing the access token in shared preferences
                    SharedPreferences sharedPref = getSharedPreferences("key",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("uberAccessToken", accessToken);
                    editor.putString("uberRefreshToken", refreshToken);
                    editor.apply();
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
