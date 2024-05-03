package com.canopus.frindl;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    ImageView bookIv;
    FirebaseAuth cAuth;
    FirebaseUser cUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing Authentication
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if(cUser!=null){
                    Toast.makeText(MainActivity.this, "Welcome "+cUser.getDisplayName(), Toast.LENGTH_LONG).show();

                    // Heading to Main Page
                    Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                    mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainPage);
                    finish();
                }
                else{
                    // Heading to Login Activity
                    Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                    loginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginActivity);
                    finish();
                }
            }
        }, 2500);
    }
}