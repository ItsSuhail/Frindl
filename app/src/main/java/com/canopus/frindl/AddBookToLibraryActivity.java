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

import android.annotation.SuppressLint;
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
import android.widget.TextView;
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

public class AddBookToLibraryActivity extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";

    TextView bookLibNameLbl;
    EditText bookTitleEt, bookDescriptionEt, bookCategoryEt, bookUploadEt;
    CheckBox makeBookPublicCb;
    Button addBookBtn;
    ProgressBar addBookPb;

    // Firebase
    FirebaseAuth cAuth;
    FirebaseUser cUser;

    FirebaseDatabase db;
    DatabaseReference usersRef, libraryRef, bookRef;
    ValueEventListener usersRefL, libraryRefL;

    FirebaseStorage storage;
    StorageReference booksStorageRef;

    ActivityResultLauncher<Intent> pickDocumentLauncher;

    UserModel cUserModel;
    LibraryModel cLibModel;
    Uri cUri = null;
    boolean bookPublicState = false;
    boolean findUserBooks = true;
    boolean findLibBooks = true;
    boolean amidstProcess = false;
    String libName, libId;
    boolean fromPublic;
    ArrayList<String> cUserBooks = new ArrayList<>();
    ArrayList<String> cLibBooks = new ArrayList<>();
    String cEmail;
    String cCategory = "-1";
    Intent cIntent;
    Toast cToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book_to_library);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(!amidstProcess && cIntent!=null){
                    if(usersRefL!=null){
                        usersRef.removeEventListener(usersRefL);
                    }
                    if(libraryRefL!=null){
                        libraryRef.removeEventListener(libraryRefL);
                    }

                    // Heading to Manage Library page
                    Intent manageLibrary = new Intent(getApplicationContext(), ManageLibrary.class);
                    manageLibrary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    manageLibrary.putExtra("com.canopus.frindl.LibraryId", libId);
                    manageLibrary.putExtra("com.canopus.frindl.LibraryName", libName);
                    manageLibrary.putExtra("com.canopus.frindl.FromPublic", fromPublic);
                    manageLibrary.putExtra("com.canopus.frindl.UserRights", true);
                    startActivity(manageLibrary);
                    finish();
                }
            }
        });


        // Defining views
        bookLibNameLbl = findViewById(R.id.lblLibBookName);
        bookTitleEt = findViewById(R.id.etLibBookTitle);
        bookDescriptionEt = findViewById(R.id.etLibBookDescription);
        bookCategoryEt = findViewById(R.id.etLibBookCategory);
        makeBookPublicCb = findViewById(R.id.cbLibMakeBookPublic);
        bookUploadEt = findViewById(R.id.etLibBookUpload);
        addBookBtn = findViewById(R.id.btnLibAddBook);
//        addBookPb = findViewById(R.id.pbLibAddBook);

//        // Setting up ProgressBar
//        addBookPb.getIndeterminateDrawable()
//                .setColorFilter(ContextCompat.getColor(this, R.color.fuchsia), PorterDuff.Mode.SRC_IN );


        cIntent = getIntent();
        if(cIntent.hasExtra("com.canopus.frindl.LibraryName") && cIntent.hasExtra("com.canopus.frindl.LibraryId") && cIntent.hasExtra("com.canopus.frindl.FromPublic")) {
            libName = cIntent.getStringExtra("com.canopus.frindl.LibraryName");
            libId = cIntent.getStringExtra("com.canopus.frindl.LibraryId");
            fromPublic = cIntent.getBooleanExtra("com.canopus.frindl.FromPublic", true);

            bookLibNameLbl.setText(libName);
            bookLibNameLbl.setSelected(true);
        }
        else{
            Log.e(TAG, "Some error occurrred! Intent doesn't have extra data...");
            showToast(getApplicationContext(), "Some error occurred. Library not found!", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }


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
                showToast(getApplicationContext(), "Some error occurred!", 1);

                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
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

        findLibBooks = true;
        showPb("Loading...");
        libraryRefL = libraryRef.child(libId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(findLibBooks){
                    findLibBooks = false;
                    // If user exists
                    if(snapshot.exists()){
                        hidePb();

                        cLibModel = snapshot.getValue(LibraryModel.class);
                        cLibBooks = cLibModel.getBookIds();
                        if(cLibBooks == null){
                            cLibBooks = new ArrayList<>();
                        }
                    }
                    // If library doesn't exist for some stupid reason : |
                    else{
                        hidePb();
                        Toast.makeText(getApplicationContext(), "Library not found! Please retry", Toast.LENGTH_LONG).show();

                        if(usersRefL!=null){
                            usersRef.removeEventListener(usersRefL);
                        }
                        if(libraryRefL!=null){
                            libraryRef.removeEventListener(libraryRefL);
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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hidePb();
                Log.e(TAG, "Some error occurred while accessing the library db with provided id: "+error.getMessage());
                showToast(getApplicationContext(), "Some error occurred while accessing the library! Please retry", 1);

                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
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

        makeBookPublicCb.setOnCheckedChangeListener((buttonView, isChecked) -> bookPublicState = isChecked);


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
                    showToast(getApplicationContext(), "Some error occurred!", 1);
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
            if(!findUserBooks && !findLibBooks) {
                amidstProcess = true;
                addBookBtn.setEnabled(false);
                makeBookPublicCb.setEnabled(false);
                bookTitleEt.setEnabled(false);
                bookDescriptionEt.setEnabled(false);

                String cTitle = bookTitleEt.getText().toString().trim();
                String cDescription = bookDescriptionEt.getText().toString().trim();

                if (!FirebaseUtil.isValidName(cTitle)) {
                    amidstProcess = false;
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    showToast(getApplicationContext(), "Please type a valid book title.", 1);

                } else if (!FirebaseUtil.isValidName(cDescription)) {
                    amidstProcess = false;
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    showToast(getApplicationContext(), "Please type a valid book description.", 1);

                } else if (cCategory.equals("-1")) {
                    amidstProcess = false;
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
                    showToast(getApplicationContext(), "Select a book category", 1);

                } else if (cUri == null) {
                    amidstProcess = false;
                    addBookBtn.setEnabled(true);
                    makeBookPublicCb.setEnabled(true);
                    bookTitleEt.setEnabled(true);
                    bookDescriptionEt.setEnabled(true);
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
        BookModel cBookModel = new BookModel(bookId, title, description, category, downloadUrl, bookPublicState, true, libId);
        bookRef.child(cBookModel.getBookId()).setValue(cBookModel);
        cUserBooks.add(cBookModel.getBookId());
        cUserModel.setBooks(cUserBooks);
        usersRef.child(FirebaseUtil.encode(cEmail)).setValue(cUserModel);
        cLibBooks.add(bookId);
        cLibModel.setBookIds(cLibBooks);
        libraryRef.child(libId).setValue(cLibModel);

        amidstProcess = false;
        addBookBtn.setEnabled(true);
        makeBookPublicCb.setEnabled(true);
        bookTitleEt.setEnabled(true);
        bookDescriptionEt.setEnabled(true);
        hidePb();

        if(usersRefL!=null){
            usersRef.removeEventListener(usersRefL);
        }
        if(libraryRefL!=null){
            libraryRef.removeEventListener(libraryRefL);
        }

        showToast(getApplicationContext(), "Book uploaded successfully!", 1);

        // Heading to Manage Library page
        Intent manageLibrary = new Intent(getApplicationContext(), ManageLibrary.class);
        manageLibrary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        manageLibrary.putExtra("com.canopus.frindl.LibraryId", libId);
        manageLibrary.putExtra("com.canopus.frindl.LibraryName", libName);
        manageLibrary.putExtra("com.canopus.frindl.FromPublic", fromPublic);
        manageLibrary.putExtra("com.canopus.frindl.UserRights", true);
        startActivity(manageLibrary);
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
        ProgressHelper.showDialog(AddBookToLibraryActivity.this, message);
    }

    public void hidePb(){
        ProgressHelper.dismissDialog();
    }

    @Override
    protected void onPause() {
        if(usersRefL!=null){
            usersRef.removeEventListener(usersRefL);
        }
        if(libraryRefL!=null){
            libraryRef.removeEventListener(libraryRefL);
        }

        super.onPause();
    }
}