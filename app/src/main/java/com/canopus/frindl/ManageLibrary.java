package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ManageLibrary extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";

    // Views
    TextView libraryManageNameLbl, statusManageLibLbl;
    EditText bookSearchBarEt;
    ImageView manageAddBookIv;
    RecyclerView booksRv;

    // Firebase Auth
    FirebaseAuth cAuth;
    FirebaseUser cUser;

    // Firebase Database
    FirebaseDatabase db;
    DatabaseReference libraryRef, bookRef;
    ValueEventListener libraryRefL, bookRefL;

    String manageLibName;
    String manageLibId;
    boolean fromPublic, userHasRights;
    LibraryModel cLib;
    ArrayList<String> cLibBooksId = new ArrayList<>();
    ArrayList<BookModel> cLibBooks = new ArrayList<>();
    Intent cIntent;
    Toast cToast;
    String pSearch = "-1";
    Handler mHandler = new Handler();
    Runnable cRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_library);


        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                if(bookRefL!=null){
                    bookRef.removeEventListener(bookRefL);
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

        // Defining views
        libraryManageNameLbl = findViewById(R.id.lblLibraryManageName);
        bookSearchBarEt = findViewById(R.id.etBookSearchBar);
        manageAddBookIv = findViewById(R.id.ivManageAddBook);
        statusManageLibLbl = findViewById(R.id.lblStatusManageLib);
        booksRv = findViewById(R.id.rvBooks);
        manageAddBookIv.setVisibility(View.GONE);

        // Initializing Firebase Auth
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();

        if(cUser == null){
            showToast(getApplicationContext(), "Please re-login into the app.", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        cIntent = getIntent();
        if(cIntent.hasExtra("com.canopus.frindl.LibraryName") && cIntent.hasExtra("com.canopus.frindl.LibraryId") && cIntent.hasExtra("com.canopus.frindl.UserRights") && cIntent.hasExtra("com.canopus.frindl.FromPublic")) {
            manageLibName = cIntent.getStringExtra("com.canopus.frindl.LibraryName");
            manageLibId = cIntent.getStringExtra("com.canopus.frindl.LibraryId");
            userHasRights = cIntent.getBooleanExtra("com.canopus.frindl.UserRights", false);
            fromPublic = cIntent.getBooleanExtra("com.canopus.frindl.FromPublic", true);

            libraryManageNameLbl.setText(manageLibName);
            libraryManageNameLbl.setSelected(true);
            if(userHasRights){
                manageAddBookIv.setVisibility(View.VISIBLE);
            }
        }
        else{
            Log.e(TAG, "Some error occurrred! Intent doesn't have extra data...");
            showToast(getApplicationContext(), "Some error occurred. Library not found!", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        // Initializing Firebase Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        libraryRef = db.getReference("libraries");
        bookRef = db.getReference("docs");

        libraryRefL = libraryRef.child(manageLibId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    Log.d(TAG, "Library snapshot exists");
                    cLibBooksId.clear();

                    cLib = snapshot.getValue(LibraryModel.class);
                    cLibBooksId = cLib.getBookIds();

                    if(cLibBooksId!=null){
                        bookRefL = bookRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
                                    for(DataSnapshot snap:snapshot.getChildren()){
                                        BookModel cBook = snap.getValue(BookModel.class);
                                        if(cLibBooksId.contains(cBook.getBookId())){
                                            if(cBook.isPublic()){
                                                cLibBooks.add(cBook);
                                            }
                                            else{
                                                if(userHasRights){
                                                    cLibBooks.add(cBook);
                                                }
                                            }
                                        }
                                    }
                                }
                                // No books exist
                                else{
                                    Log.e(TAG, "No children in book reference! No snapshot exists...");
                                    showToast(getApplicationContext(), "Some error occurred! No books exist...", 1);

                                    if(cRunnable != null){
                                        mHandler.removeCallbacks(cRunnable);
                                    }
                                    if(bookRefL!=null){
                                        bookRef.removeEventListener(bookRefL);
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

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Some error occurred while accessing the books db: "+error.getMessage());
                                showToast(getApplicationContext(), "Some error occurred while accessing the library! Please retry", 1);

                                if(cRunnable != null){
                                    mHandler.removeCallbacks(cRunnable);
                                }

                                if(bookRefL!=null){
                                    bookRef.removeEventListener(bookRefL);
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
                    }
                }
                else{
                    Log.e(TAG, "Error: Snapshot of library doesn't exist");
                    showToast(getApplicationContext(), "Some error occurred! Library doesn't exist...", 1);

                    if(cRunnable != null){
                        mHandler.removeCallbacks(cRunnable);
                    }

                    if(bookRefL!=null){
                        bookRef.removeEventListener(bookRefL);
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Some error occurred while accessing the library: "+error.getDetails());
                showToast(getApplicationContext(), "Some error occurred while accessing the library! Please retry", 1);

                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                if(bookRefL!=null){
                    bookRef.removeEventListener(bookRefL);
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

        manageAddBookIv.setOnClickListener(v -> {
            if(cRunnable != null){
                mHandler.removeCallbacks(cRunnable);
            }

            if(bookRefL!=null){
                bookRef.removeEventListener(bookRefL);
            }
            if(libraryRefL!=null){
                libraryRef.removeEventListener(libraryRefL);
            }
            // Heading to AddBookToLibrary page
            Intent addBookToLibraryActivity = new Intent(getApplicationContext(), AddBookToLibraryActivity.class);
            addBookToLibraryActivity.putExtra("com.canopus.frindl.LibraryId", manageLibId);
            addBookToLibraryActivity.putExtra("com.canopus.frindl.LibraryName", manageLibName);
            addBookToLibraryActivity.putExtra("com.canopus.frindl.FromPublic", fromPublic);
            addBookToLibraryActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(addBookToLibraryActivity);
            finish();
        });
        mHandler.postDelayed(cRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(cRunnable, 500);

                if (cLibBooks.size() != 0) {
                    Log.d(TAG, "run: Happenin");
                    String cSearch = bookSearchBarEt.getText().toString();
                    if(!pSearch.equals(cSearch)){
                        pSearch = cSearch;
                        ArrayList<String> allBookIds = new ArrayList<>();
                        ArrayList<String> allBookNames = new ArrayList<>();
                        ArrayList<BookModel> allBookModels = new ArrayList<>();
                        for (BookModel bookModel : cLibBooks) {
                            if (cSearch.trim().length() != 0) {
                                if (bookModel.getBookId().contains(cSearch) || bookModel.getBookTitle().toLowerCase().contains(cSearch.toLowerCase()) || bookModel.getBookDescription().toLowerCase().contains(cSearch.toLowerCase()) || bookModel.getCategory().toLowerCase().contains(cSearch.toLowerCase())) {
                                    allBookIds.add(bookModel.getBookId());
                                    allBookNames.add(bookModel.getBookTitle());
                                    allBookModels.add(bookModel);
                                }
                            } else {
                                allBookIds.add(bookModel.getBookId());
                                allBookNames.add(bookModel.getBookTitle());
                                allBookModels.add(bookModel);
                            }
                        }

                        String[] bookIds = new String[allBookIds.size()];
                        String[] bookNames = new String[allBookNames.size()];
                        BookModel[] bookModels = new BookModel[allBookModels.size()];

                        for (int i = 0; i < allBookIds.size(); i++) {
                            bookIds[i] = allBookIds.get(i);
                            bookNames[i] = allBookNames.get(i);
                            bookModels[i] = allBookModels.get(i);
//                    Log.d(TAG, "run: "+libIds[i] + libNames[i]);
                        }

                        statusManageLibLbl.setVisibility(View.GONE);
                        booksRv.setVisibility(View.VISIBLE);
                        booksRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        PublicBookAdaptor adaptor = new PublicBookAdaptor(bookNames, bookIds, bookModels, new PublicBookAdaptor.OnItemClickListener() {
                            @Override
                            public void onItemClick(String bookName, String bookId, BookModel bookModel) {
                                String[] options = {"Download book"};

                                AlertDialog.Builder builder = new AlertDialog.Builder(ManageLibrary.this);
                                builder.setTitle("Download "+bookName)
                                        .setItems(options, (dialog, which) -> {
                                            if(which == 0){
                                                String accessUrl = bookModel.getAccessUrl();
                                                if (!accessUrl.startsWith("http://") && !accessUrl.startsWith("https://")) {
                                                    accessUrl = "https://" + accessUrl;
                                                }

                                                mHandler.removeCallbacks(cRunnable);
                                                if(libraryRefL!=null){
                                                    libraryRef.removeEventListener(libraryRefL);
                                                }
                                                if(bookRefL != null){
                                                    bookRef.removeEventListener(bookRefL);
                                                }
                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(accessUrl));
                                                startActivity(browserIntent);
                                            }
                                }).show();
                            }
                        });

                        booksRv.setAdapter(adaptor);
                    }
                }
                else{
                    statusManageLibLbl.setVisibility(View.VISIBLE);
                    booksRv.setVisibility(View.GONE);
                    statusManageLibLbl.setText("No Books Found!");
                }


                // Run
            }
        }, 500);

    }

    public void showToast(Context context, CharSequence message, int duration){
        if (cToast!=null){
            cToast.cancel();
        }

        cToast = Toast.makeText(context, message, duration);
        cToast.show();
    }

    @Override
    protected void onPause() {
        if(cRunnable != null){
            mHandler.removeCallbacks(cRunnable);
        }
        if(bookRefL!=null){
            bookRef.removeEventListener(bookRefL);
        }
        if(libraryRefL!=null){
            libraryRef.removeEventListener(libraryRefL);
        }

        super.onPause();
    }
}