package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AddLibraryActivity extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";

    EditText libraryNameEt;
    EditText libraryPasswordEt;
    CheckBox makePublicCb;
    Button addLibraryBtn;
    ProgressBar addLibraryPb;
    FirebaseAuth cAuth;
    FirebaseUser cUser;
    FirebaseDatabase db;
    DatabaseReference usersRef, libraryRef;
    ValueEventListener usersRefL;
    UserModel cUserModel;
    String cName;
    String cEmail;

    String cLibName;
    String cLibPassword;
    ArrayList<String> userLibs;
    boolean libPublicState = false;
    boolean findUserLibs = true;
    boolean amidstProcess = false;
    Toast cToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_library);


        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(!amidstProcess){
                    if(usersRefL!=null){
                        usersRef.removeEventListener(usersRefL);
                    }

                    // Heading to Manage Stuff Activity
                    Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
                    manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(manageStuffActivity);
                    finish();
                }
            }
        });

        // Defining views
        libraryNameEt = findViewById(R.id.etLibraryName);
        libraryPasswordEt = findViewById(R.id.etLibraryPassword);
        makePublicCb = findViewById(R.id.cbMakePublic);
        addLibraryBtn = findViewById(R.id.btnAddLibrary);
        addLibraryPb = findViewById(R.id.pbAddLibrary);

        // Setting up ProgressBar
        addLibraryPb.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );


        // Initializing FirebaseAuth
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();

        // Initializing Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        usersRef = db.getReference("users");
        libraryRef = db.getReference("libraries");

        if(cUser!=null){
            showPb();
            cEmail = cUser.getEmail();
            Log.d(TAG, "onCreate: "+cEmail);
            cEmail = FirebaseUtil.encode(cEmail);
            findUserLibs = true;

            usersRefL = usersRef.child(cEmail).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(findUserLibs){
                        findUserLibs = false;
                        // If user exists
                        if(snapshot.exists()){
                            hidePb();

                            cUserModel = snapshot.getValue(UserModel.class);
                            userLibs = cUserModel.getLibraries();
                        }
                        // If user doesn't exist for some stupid reason : |
                        else{
                            hidePb();

                            Toast.makeText(getApplicationContext(), "Profile not found! Please re-login into the app.", Toast.LENGTH_LONG).show();
                            // Signing out
                            cAuth.signOut();
                            // Exiting app
                            finish();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "onCancelled: "+error.toString());
                    hidePb();

                    if(usersRefL!=null){
                        usersRef.removeEventListener(usersRefL);
                    }

                    showToast(getApplicationContext(), "Some error occurred!", 1);

                    // Heading to Manage Stuff Activity
                    Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
                    manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(manageStuffActivity);
                    finish();
                }
            });
        }
        // If user ain't login
        else{
            showToast(getApplicationContext(), "Please re-login into the app.", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        makePublicCb.setOnCheckedChangeListener((buttonView, isChecked) -> libPublicState = isChecked);

        addLibraryBtn.setOnClickListener(v -> {
            // Check if we have user's library array
            if(!findUserLibs){
                addLibraryBtn.setEnabled(false);
                showPb();
                amidstProcess = true;

                cLibName = libraryNameEt.getText().toString();
                cLibPassword = libraryPasswordEt.getText().toString();
                if(FirebaseUtil.isValidName(cLibName)){
                    if(cLibPassword.length()>=6 || cLibPassword.length() == 0){
                        LibraryModel cLibrary = new LibraryModel(cLibName, cLibPassword, libPublicState);
                        userLibs.add(cLibrary.getLibId());
                        cUserModel.setLibraries(userLibs);
                        usersRef.child(cEmail).setValue(cUserModel);
                        libraryRef.child(cLibrary.getLibId()).setValue(cLibrary);

                        amidstProcess = false;
                        hidePb();

                        showToast(getApplicationContext(), "Library created successfully!", 1);

                        if(usersRefL!=null){usersRef.removeEventListener(usersRefL);}

                        // Heading to Manage Stuff Activity
                        Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
                        manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(manageStuffActivity);
                        finish();

                    }
                    else{
                        amidstProcess = false;
                        hidePb();
                        addLibraryBtn.setEnabled(true);

                        showToast(getApplicationContext(), "Please make sure the password has at least 6 characters", 1);
                    }

                }
                else{
                    amidstProcess = false;
                    hidePb();
                    addLibraryBtn.setEnabled(true);

                    showToast(getApplicationContext(), "Please use valid characters in Library name field.", 1);
                }
            }

            else{
                showToast(getApplicationContext(), "Please wait for some moment...", 1);
            }
        });
    }

    public void showToast(Context context, CharSequence message, int duration){
        if (cToast!=null){
            cToast.cancel();
        }

        cToast = Toast.makeText(context, message, duration);
        cToast.show();
    }

    public void showPb(){
        addLibraryPb.setVisibility(View.VISIBLE);
    }
    public void hidePb(){
        addLibraryPb.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        if(usersRefL!=null){
            usersRef.removeEventListener(usersRefL);
        }

        super.onPause();
    }
}