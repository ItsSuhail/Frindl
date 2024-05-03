package com.canopus.frindl;

import static com.canopus.frindl.FirebaseUtil.isValidEmail;
import static com.canopus.frindl.Utils.hideKeyboard;

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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";
    EditText emailEt;
    EditText passwordEt;
    Button loginBtn;
    TextView signupLbl;
    ProgressBar loginPb;
    FirebaseAuth cAuth;

    String cName;
    String cEmail;
    String cPassword;

    Toast cToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(signupLbl!=null && signupLbl.isEnabled()){
                    finish();
                }
            }
        });

        // Defining views
        emailEt = findViewById(R.id.etEmailLogin);
        passwordEt = findViewById(R.id.etPasswordLogin);
        loginBtn = findViewById(R.id.btnLogin);
        signupLbl = findViewById(R.id.lblSignup);
        loginPb = findViewById(R.id.pbLogin);

        // Setting up ProgressBar
        loginPb.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );

        // Initializing Authentication
        cAuth = FirebaseAuth.getInstance();
        cAuth.signOut();

        loginBtn.setOnClickListener(v -> {
            signupLbl.setEnabled(false);
            hideKeyboard(LoginActivity.this);
            showLoginPb();

            cEmail = emailEt.getText().toString();
            cPassword = passwordEt.getText().toString();
            boolean isEmailValid = isValidEmail(cEmail);
            boolean isPassEmpty = cPassword.isEmpty();

            if(isEmailValid && !isPassEmpty){
                cAuth.signInWithEmailAndPassword(cEmail, cPassword).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        FirebaseUser cUser = cAuth.getCurrentUser();
                        cName = cUser.getDisplayName();

                        showToast(getApplicationContext(), "Welcome "+cName, 1);

                        // Heading to MainPage
                        Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                        mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainPage);
                        finish();
                    }
                    // If task isn't successful
                    else {
                        hideLoginPb();
                        signupLbl.setEnabled(true);
                        Log.d(TAG, "Login not successful, Authentication error: " + String.valueOf(task.getException()));
                        if(task.getException() != null){
                            try {
                                throw task.getException();
                            }
                            catch(FirebaseTooManyRequestsException e){
                                showToast(getApplicationContext(), "Too many requests from this device. The device is temporarily blocked.", 1);
                            }
                            catch(FirebaseAuthInvalidUserException e){
                                showToast(getApplicationContext(), "Invalid login credentials. No profile found!", 1);
                            }
                            catch(FirebaseAuthInvalidCredentialsException e){
                                showToast(getApplicationContext(), "Invalid login credentials. Please check your email or password.", 1);
                            }
                            catch (Exception e){
                                showToast(getApplicationContext(), "An error occurred. Unable to Login. Please check your network!", 1);
                            }
                        }
                        else{
                            showToast(getApplicationContext(), "Login failed! Please retry...", 1);
                        }
                    }
                });
            }
            // Email ain't valid or password empty
            else {
                hideLoginPb();
                signupLbl.setEnabled(true);
                if(!isEmailValid && isPassEmpty){
                    showToast(getApplicationContext(), "Please enter a valid email and password.", 1);
                }
                else if(!isEmailValid){
                    showToast(getApplicationContext(), "Please enter a valid email.", 1);
                }
                else{
                    showToast(getApplicationContext(), "Please enter a valid password.", 1);
                }
            }
        });

        signupLbl.setOnClickListener(v -> {
            loginBtn.setEnabled(false);
            hideKeyboard(LoginActivity.this);

            // Heading to SignUpPage
            Intent signupActivity = new Intent(getApplicationContext(), SignupActivity.class);
            signupActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signupActivity);
            finish();
        });
    }

    public void showToast(Context context, CharSequence message, int duration){
        if(cToast!=null){
            cToast.cancel();
        }
        cToast = Toast.makeText(context, message, duration);
        cToast.show();
    }

    public void showLoginPb(){
        loginPb.setVisibility(View.VISIBLE);
    }
    public void hideLoginPb(){
        loginPb.setVisibility(View.GONE);
    }

}