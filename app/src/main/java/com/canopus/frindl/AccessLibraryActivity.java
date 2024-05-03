package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccessLibraryActivity extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";

    // Views
    TextView libraryAccIdLbl, libraryAccNameLbl;
    LinearLayout libraryAccessLl;
    EditText libraryPasswordEt;
    Button accessBtn;
    ProgressBar accLibPb;

    // Firebase
    FirebaseAuth cAuth;
    FirebaseUser cUser;
    FirebaseDatabase db;
    DatabaseReference usersRef, libraryRef;
    ValueEventListener usersRefL, libraryRefL;

    Handler mHandler;
    Runnable cRunnable;

    // Vars
    String cEmail;
    String libName, libId, libPassword;

    String cUserPassword;

    volatile boolean hasChecked = false;
    boolean userHasRights = false;
    boolean fromPublic;
    Toast cToast;
    Intent cIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_library);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
                }
                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }

                if(fromPublic){
                    // Heading to Main page
                    Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                    mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainPage);
                    finish();
                }
                else{
                    // Heading to Manage Stuff Activity
                    Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
                    manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(manageStuffActivity);
                    finish();
                }

            }
        });

        // Defining views
        libraryAccIdLbl = findViewById(R.id.lblLibraryAccId);
        libraryAccNameLbl = findViewById(R.id.lblLibraryAccName);
        libraryPasswordEt = findViewById(R.id.etLibraryAccPassword);
        accessBtn = findViewById(R.id.btnAccess);
        accLibPb = findViewById(R.id.pbAccLib);
        libraryAccessLl = findViewById(R.id.llAccessLibrary);

        // Setting up ProgressBar
        accLibPb.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );


        // Firebase Auth
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();
        if(cUser==null){
            showToast(getApplicationContext(), "Please re-login into the app.", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        // Initializing Firebase Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        libraryRef = db.getReference("libraries");
        usersRef = db.getReference("users");


        cEmail = cUser.getEmail();
        cEmail = FirebaseUtil.encode(cEmail);

        cIntent = getIntent();
        if(cIntent.hasExtra("com.canopus.frindl.LibraryName") && cIntent.hasExtra("com.canopus.frindl.LibraryId") && cIntent.hasExtra("com.canopus.frindl.LibraryPassword") && cIntent.hasExtra("com.canopus.frindl.FromPublic")){
            libName = cIntent.getStringExtra("com.canopus.frindl.LibraryName");
            libId = cIntent.getStringExtra("com.canopus.frindl.LibraryId");
            libPassword = cIntent.getStringExtra("com.canopus.frindl.LibraryPassword");
            fromPublic = cIntent.getBooleanExtra("com.canopus.frindl.FromPublic",true);

            libraryAccIdLbl.setText(libId);
            libraryAccNameLbl.setText(libName);
            libraryAccNameLbl.setSelected(true);

//            Log.d(TAG, "Checking if user got the rights");
            showPb();
            hasChecked = false;
            userHasRights = false;

            if(fromPublic) {

                usersRefL = usersRef.child(cEmail).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "OnDataChange");
                        if (snapshot.exists()) {
                            hasChecked = true;
                            UserModel cUserModel = snapshot.getValue(UserModel.class);
                            if (cUserModel.getLibraries() != null) {
                                if (cUserModel.getLibraries().contains(libId)) {
                                    userHasRights = true;
                                    Log.d(TAG, "onDataChange: User got the rights");
                                }
                            }
                        } else {
                            showToast(getApplicationContext(), "Some error occurred while fetching profile! Please retry", 1);
                            cAuth.signOut();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast(getApplicationContext(), "Some error occurred while fetching profile! Please retry", 1);
                        // Heading to Main page
                        Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                        mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainPage);
                        finish();
                    }
                });
            }
            else{
                hasChecked = true;
                userHasRights = true;
            }

            mHandler = new Handler();
            mHandler.postDelayed(cRunnable = new Runnable() {
                @Override
                public void run() {
//                    Log.d(TAG, "Inside runnable");
                    mHandler.postDelayed(cRunnable, 500);
                    if(hasChecked){
                        hidePb();
                        mHandler.removeCallbacks(cRunnable);
                        if(libPassword.isEmpty()){
                            // Heading to Manage Library Activity
                            Intent manageLibrary = new Intent(getApplicationContext(), ManageLibrary.class);
                            manageLibrary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            manageLibrary.putExtra("com.canopus.frindl.LibraryId", libId);
                            manageLibrary.putExtra("com.canopus.frindl.LibraryName", libName);
                            manageLibrary.putExtra("com.canopus.frindl.UserRights", userHasRights);
                            manageLibrary.putExtra("com.canopus.frindl.FromPublic", fromPublic);
                            startActivity(manageLibrary);
                            finish();
                        }
                        else{
                            libraryAccessLl.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Password aint empty");

                        }
                    }
                }
            }, 500);

        }
        else{
            Log.d(TAG, "Some error occurred. No library found!");

            showToast(getApplicationContext(), "Some error occurred. Please retry!", 1);

            // Heading to MainPage
            Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
            mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainPage);
            finish();
        }

        accessBtn.setOnClickListener(v -> {
            cUserPassword = libraryPasswordEt.getText().toString();
            if(cUserPassword.equals(libPassword)){
                // Heading to Manage Library Activity
                Intent manageLibrary = new Intent(getApplicationContext(), ManageLibrary.class);
                manageLibrary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                manageLibrary.putExtra("com.canopus.frindl.LibraryId", libId);
                manageLibrary.putExtra("com.canopus.frindl.LibraryName", libName);
                manageLibrary.putExtra("com.canopus.frindl.UserRights", userHasRights);
                manageLibrary.putExtra("com.canopus.frindl.FromPublic", fromPublic);
                startActivity(manageLibrary);
                finish();
            }
            else{
                showToast(getApplicationContext(), "Incorrect password. Try again...", 1);
            }
        });


    }

    public void showPb(){
        accLibPb.setVisibility(View.VISIBLE);
    }

    public void hidePb(){
        accLibPb.setVisibility(View.GONE);
    }

    public void showToast(Context context, CharSequence message, int duration){
        if (cToast!=null){
            cToast.cancel();
        }

        cToast = Toast.makeText(context, message, duration);
        cToast.show();
    }
}