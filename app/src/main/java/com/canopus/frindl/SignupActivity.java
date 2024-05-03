package com.canopus.frindl;

import static com.canopus.frindl.FirebaseUtil.*;
import static com.canopus.frindl.Utils.hideKeyboard;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {
    /*
    DOCS:
        DOC1
        DOC2
        DOC3
        DOC4
        DOC5

    LIBRARIES:
        LIB1:
            DOC1
        LIB2:
            DOC1

    PUBLIC_LIBRARY:

    Basic functionalities:
    User->
        Enters name
        Enters email
        Enters password
        onPressSignup:
            if email not already present:
                authentication->register user
                login the user, go to MainPage
            else:
                error go boom
     */

    String TAG = "com.canopus.frindl.logs";
    EditText nameEt;
    EditText emailEt;
    EditText passwordEt;
    Button signupBtn;
    TextView loginLbl;
    ProgressBar signupPb;
    FirebaseAuth cAuth;

    String cName;
    String cEmail;
    String cPassword;

    Toast cToast;

    FirebaseDatabase db;
    DatabaseReference usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(loginLbl!=null && loginLbl.isEnabled()){
                    // Heading to Login page
                    Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                    loginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginActivity);
                    finish();
                }
            }
        });

        // Defining components
        nameEt = findViewById(R.id.etNameSignup);
        emailEt = findViewById(R.id.etEmailSignup);
        passwordEt = findViewById(R.id.etPasswordSignup);
        signupBtn = findViewById(R.id.btnSignup);
        loginLbl = findViewById(R.id.lblLogin);
        signupPb = findViewById(R.id.pbSignup);

        // Setting up ProgressBar
        signupPb.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );

        // Initializing Authentication
        cAuth = FirebaseAuth.getInstance();
        cAuth.signOut();

        // Initializing Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        usersRef = db.getReference("users");

        signupBtn.setOnClickListener(v -> {
            hideKeyboard(SignupActivity.this);
            showSignupPb();
            loginLbl.setEnabled(false);

            cName = nameEt.getText().toString();
            cEmail = emailEt.getText().toString().toLowerCase();
            cPassword = passwordEt.getText().toString();
            boolean isEmailValid = isValidEmail(cEmail);
            boolean isPassLong = cPassword.length()>=6;

            if(!cName.isEmpty() && !cEmail.isEmpty() && !cPassword.isEmpty()){
                if(isEmailValid && isPassLong){
                    // If name if valid
                    if(isValidName(cName)){
                        registerAccount(cEmail, cPassword, cName);
                    }
                    else{
                        hideSignupPb();
                        loginLbl.setEnabled(true);

                        showToast(this, "Use of invalid characters in name field!", 1);
                    }
                }
                // If email isn't valid or password.length() < 6
                else{
                    hideSignupPb();
                    loginLbl.setEnabled(true);

                    if(!isEmailValid && !isPassLong){
                        showToast(this, "Make sure you enter correct email and the password is at least 6 characters...", 1);
                    }
                    else if(!isEmailValid){
                        showToast(this, "Make sure you enter correct email.", 1);
                    }
                    else{
                        showToast(this, "Make sure your password is at least 6 characters long.", 1);
                    }
                }
            }
            else{
                hideSignupPb();
                loginLbl.setEnabled(true);

                showToast(this, "Please fill all the fields!", 1);
            }
        });

        loginLbl.setOnClickListener(v -> {
            signupBtn.setEnabled(false);
            hideKeyboard(SignupActivity.this);

            // Heading to Login page
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            loginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginActivity);
            finish();
        });
    }

    public void registerAccount(String uEmail, String uPassword, String displayName){
        cAuth.createUserWithEmailAndPassword(uEmail, uPassword).addOnCompleteListener(task -> {
            // If task is successful
            if(task.isSuccessful()) {
                UserProfileChangeRequest userNameChangeRequest = new UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build();

                FirebaseUser cUser = cAuth.getCurrentUser();
                assert cUser != null;
                cUser.updateProfile(userNameChangeRequest);

                addUser(displayName, uEmail);

                showToast(SignupActivity.this, "Registration successful! Welcome "+ displayName, 1);


                // Heading to Main Page
                Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainPage);
                finish();
            }
            else {
                hideSignupPb();
                loginLbl.setEnabled(true);

                Log.d(TAG, "Registration failed, Authentication error: " + String.valueOf(task.getException()));

                if (task.getException() != null) {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        showToast(SignupActivity.this, "Your password is weak", 1);
                    } catch (FirebaseAuthUserCollisionException e) {
                        showToast(SignupActivity.this, "Email is already in use!", 1);
                    } catch (Exception e) {
                        showToast(SignupActivity.this, "Authentication failed! Please check your network!", 1);
                    }
                }
                else {
                    showToast(SignupActivity.this, "Registration failed!", 1);
                }
            }
        });
    }

    public void addUser(String cDisplayName, String uEmail){
        String encodedUEmail = FirebaseUtil.encode(uEmail);
        UserModel userModel = new UserModel(cDisplayName, encodedUEmail);
        usersRef.child(encodedUEmail).setValue(userModel);
//        Log.d(TAG, userModel.getUEmail());
    }

    public void showToast(Context context, CharSequence message, int duration){
        if(cToast!=null){
            cToast.cancel();
        }
        cToast = Toast.makeText(context, message, duration);
        cToast.show();
    }

    public void showSignupPb(){
        signupPb.setVisibility(View.VISIBLE);
    }
    public void hideSignupPb(){
        signupPb.setVisibility(View.GONE);
    }
}