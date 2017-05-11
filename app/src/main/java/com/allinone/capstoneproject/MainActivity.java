package com.allinone.capstoneproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

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
        final Context mContext= this;

        cab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, CabActivity.class);
                startActivity(intent);
            }
        });



    }


}
