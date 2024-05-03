package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class AddBookActivity extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";

    EditText bookTitleEt, bookDescriptionEt, bookCategoryEt, bookUploadEt;
    CheckBox makeBookPublicCb;
    Button addBookBtn;
    ProgressBar addBookPb;

    // Firebase
    FirebaseAuth cAuth;
    FirebaseUser cUser;

    FirebaseDatabase db;
    DatabaseReference usersRef, libraryRef, bookRef;
    ValueEventListener usersRefL;

    FirebaseStorage storage;
    StorageReference booksStorageRef;

    ActivityResultLauncher<Intent> pickDocumentLauncher;

    UserModel cUserModel;
    Uri cUri = null;
    boolean bookPublicState = false;
    boolean findUserBooks = true;

    boolean amidstProcess = false;
    ArrayList<String> cUserBooks = new ArrayList<>();
    String cEmail;
    String cCategory = "-1";

    Toast cToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(!amidstProcess){
                    if(usersRefL!=null){
                        usersRef.removeEventListener(usersRefL);
                    }

                    // Heading to Main page
                    Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
                    manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(manageStuffActivity);
                    finish();
                }
            }
        });

        // Defining views
        bookTitleEt = findViewById(R.id.etBookTitle);
        bookDescriptionEt = findViewById(R.id.etBookDescription);
        bookCategoryEt = findViewById(R.id.etBookCategory);
        makeBookPublicCb = findViewById(R.id.cbMakeBookPublic);
        bookUploadEt = findViewById(R.id.etBookUpload);
        addBookBtn = findViewById(R.id.btnAddBook);
//        addBookPb = findViewById(R.id.pbAddBook);

//        // Setting up ProgressBar
//        addBookPb.getIndeterminateDrawable()
//                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );


        // Firebase Authentication
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();

        // Firebase Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        libraryRef = db.getReference("libraries");
        usersRef = db.getReference("users");
        bookRef = db.getReference("docs");

        // Firebase Storage
        storage = FirebaseStorage.getInstance();
        booksStorageRef = storage.getReference("docs");


        if(cUser==null){
            showToast(getApplicationContext(), "Please re-login into the app.", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        showPb("Loading...");
        cEmail = cUser.getEmail();
        cEmail = FirebaseUtil.encode(cEmail);

        findUserBooks = true;
        usersRefL = usersRef.child(cEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(findUserBooks){
                    findUserBooks = false;
                    // If user exists
                    if(snapshot.exists()){
                        hidePb();

                        cUserModel = snapshot.getValue(UserModel.class);
                        cUserBooks = cUserModel.getBooks();
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
                // Heading to Main page
                Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainPage);
                finish();
            }
        });

        makeBookPublicCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            bookPublicState = isChecked;
        });


        bookCategoryEt.setOnClickListener(v -> {
            if(!amidstProcess) {
                String[] categories = {"Science", "Mathematics", "Education", "Computer Science", "Horror", "Mystery", "Thriller", "Biography", "Ethnic & Cultural", "Fiction", "Non Fiction", "Fantasy", "Adventure", "Others"};

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose category")
                        .setItems(categories, (dialog, which) -> {
                            cCategory = categories[which];
                            bookCategoryEt.setText(cCategory);
                        }).show();
            }
        });

        pickDocumentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            Log.d(TAG, "Result code: "+o.getResultCode());
            if(o.getResultCode() == RESULT_OK){
                Intent cIntent = o.getData();
                if(cIntent!=null){
                    cUri = cIntent.getData();
                    try{
                        Cursor cursor = getContentResolver().query(cUri, null, null, null, null);
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        cursor.moveToFirst();

                        String filename = cursor.getString(nameIndex);
                        bookUploadEt.setText(filename);
                    }
                    catch(Exception e){
                        Log.e(TAG, "Some error occurred, "+e.getMessage());
                        showToast(getApplicationContext(), "Some error occurred!", 1);
                    }

                }
                else{
                    Log.e(TAG, "Null intent returned while picking document");
                }
                //Log.d(TAG, "Picked document: "+cUri);
            }
            else if(o.getResultCode() == RESULT_CANCELED){
                return;
            }
            else{
                Log.e(TAG, "Error occurred while picking document!");
                showToast(getApplicationContext(), "Some error occurred", 1);
            }
        });

        bookUploadEt.setOnClickListener(v -> {
            if(!amidstProcess) {
                String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                pickDocumentLauncher.launch(intent);
            }
        });

        addBookBtn.setOnClickListener(v -> {
            if(!findUserBooks) {
                amidstProcess = true;
                addBookBtn.setEnabled(false);
                makeBookPublicCb.setEnabled(false);
                bookTitleEt.setEnabled(false);
                bookDescriptionEt.setEnabled(false);

                String cTitle = bookTitleEt.getText().toString().trim();
                String cDescription = bookDescriptionEt.getText().toString().trim();

                if (!FirebaseUtil.isValidName(cTitle)) {
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    amidstProcess = false;

                    showToast(getApplicationContext(), "Please type a valid book title.", 1);

                } else if (!FirebaseUtil.isValidName(cDescription)) {
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    amidstProcess = false;
                    showToast(getApplicationContext(), "Please type a valid book description.", 1);

                } else if (cCategory.equals("-1")) {
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    amidstProcess = false;
                    showToast(getApplicationContext(), "Select a book category", 1);

                } else if (cUri == null) {
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    amidstProcess = false;
                    showToast(getApplicationContext(), "Please select a file.", 1);

                } else {
                    showPb("Uploading...");
                    uploadFileToFirebase(cTitle, cDescription, cCategory);
                }
            }
            else{
                showToast(getApplicationContext(), "Please wait...", 1);
            }
        });
    }

    public void uploadToDatabase(String title, String description, String category, String downloadUrl, String bookId){
        BookModel cBookModel = new BookModel(bookId, title, description, category, downloadUrl, bookPublicState, false, "-1");
        bookRef.child(cBookModel.getBookId()).setValue(cBookModel);
        cUserBooks.add(cBookModel.getBookId());
        cUserModel.setBooks(cUserBooks);
        usersRef.child(FirebaseUtil.encode(cEmail)).setValue(cUserModel);

        amidstProcess = false;
        addBookBtn.setEnabled(true);
        makeBookPublicCb.setEnabled(true);
        bookTitleEt.setEnabled(true);
        bookDescriptionEt.setEnabled(true);
        hidePb();

        if(usersRefL!=null){
            usersRef.removeEventListener(usersRefL);
        }

        showToast(getApplicationContext(), "Book uploaded successfully!", 1);

        // Heading to Manage Stuff Activity
        Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
        manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(manageStuffActivity);
        finish();
    }

    public void uploadFileToFirebase(String title, String description, String category){
        String filepathAndName = FirebaseUtil.returnRandKey("B");
        booksStorageRef.child(filepathAndName).putFile(cUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.d(TAG, "File uploaded successfully!");

                // Getting download url
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful()){
                    Log.d(TAG, "Inside while loop!");
                };
                String uploadUrl = uriTask.getResult().toString();
                Log.d(TAG, "onSuccess: "+uploadUrl);

                hidePb();
                showPb("Writing to database...");
                uploadToDatabase(title, description, category, uploadUrl, filepathAndName);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Some error occurred when uploading files", e);

                showToast(getApplicationContext(), "Some error occurred while uploading! Please retry", 1);

                amidstProcess = false;
                addBookBtn.setEnabled(true);
                makeBookPublicCb.setEnabled(true);
                bookTitleEt.setEnabled(true);
                bookDescriptionEt.setEnabled(true);
                hidePb();
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

//    public void showPb(){
//        addBookPb.setVisibility(View.VISIBLE);
//    }
//
//    public void hidePb(){
//        addBookPb.setVisibility(View.GONE);
//    }

    public void showPb(String message){
        ProgressHelper.showDialog(AddBookActivity.this, message);
    }

    public void hidePb(){
        ProgressHelper.dismissDialog();
    }
}