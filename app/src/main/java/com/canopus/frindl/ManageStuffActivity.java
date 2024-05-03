package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ManageStuffActivity extends AppCompatActivity {

    String TAG = "com.canopus.frindl.logs";

    // Firebase Utils
    FirebaseAuth cAuth;
    FirebaseUser cUser;
    FirebaseDatabase db;
    DatabaseReference usersRef, libraryRef, bookRef;
    ValueEventListener usersRefL, usersBookRefL, libraryRefL, bookRefL;

    // Views
    EditText searchUserLibsEt;
    ImageView addLibraryIv, addBookIv, logoutIv, addStuffIv;
    EditText searchUserCategoryEt;
    TextView statusManagePageLbl, titleLbl;
    RecyclerView userLibrariesRv;
    LinearLayout exploreLl;

    String cEmail;
    ArrayList<String> cUserLibs = new ArrayList<String>();
    ArrayList<String> cUserBooks = new ArrayList<String>();
    ArrayList<LibraryModel> userLibraries = new ArrayList<LibraryModel>();
    ArrayList<BookModel> userBooks = new ArrayList<BookModel>();

    Toast cToast;

    Handler mHandler = new Handler();
    Runnable cRunnable;
    int delay = 500;
    String pSearch = "-1";
    String cSearchCategory = "Libraries";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_stuff);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
                }
                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }
                if(usersBookRefL!=null){
                    usersRef.removeEventListener(usersBookRefL);
                }
                if(bookRefL!=null){
                    bookRef.removeEventListener(bookRefL);
                }
                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                // Heading to Main page
                Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
                mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainPage);
                finish();
            }
        });

        // Defining Views
        searchUserLibsEt = findViewById(R.id.etSearchUserLibs);
//        addLibraryIv = findViewById(R.id.ivAddLibrary);
//        addBookIv = findViewById(R.id.ivAddBook);
        addStuffIv = findViewById(R.id.ivAddStuff);
        searchUserCategoryEt = findViewById(R.id.etSearchUserCategory);
        statusManagePageLbl = findViewById(R.id.lblStatusManagePage);
        userLibrariesRv = findViewById(R.id.rvUserLibraries);
        exploreLl = findViewById(R.id.llExplore);
        logoutIv = findViewById(R.id.ivLogout);
        titleLbl = findViewById(R.id.lblTitle5);
        titleLbl.setSelected(true);

        // Initializing Firebase Authentication
        cAuth = FirebaseAuth.getInstance();
        cUser = cAuth.getCurrentUser();

        if(cUser==null){
            showToast(getApplicationContext(), "Please re-login into the app.", 1);
            // Signing out
            cAuth.signOut();
            // Exiting app
            finish();
        }

        cEmail = cUser.getEmail();

        // Initializing Firebase Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        libraryRef = db.getReference("libraries");
        usersRef = db.getReference("users");
        bookRef = db.getReference("docs");
        usersRefL = usersRef.child(FirebaseUtil.encode(cEmail)).child("libraries").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    cUserLibs.clear();
                    for(DataSnapshot snap: snapshot.getChildren()){
                        cUserLibs.add(snap.getValue().toString());
                    }

                    // Now finding all the libs with ids present in cUserLibs
                    libraryRefL = libraryRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()) {
                                userLibraries.clear();
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    LibraryModel cLibrary = snap.getValue(LibraryModel.class);
                                    if (cUserLibs.contains(cLibrary.getLibId())) {
                                        userLibraries.add(cLibrary);
                                    }
                                }
                            }
                            else{
                                Log.e(TAG, "No children in library reference! No snapshot exists...");
                                showToast(getApplicationContext(), "Some error occurred! No libraries exist...", 1);

                                if(cRunnable != null){
                                    mHandler.removeCallbacks(cRunnable);
                                }

                                if(usersRefL!=null){
                                    usersRef.removeEventListener(usersRefL);
                                }
                                if(usersBookRefL!=null){
                                    usersRef.removeEventListener(usersBookRefL);
                                }
                                if(bookRefL!=null){
                                    bookRef.removeEventListener(bookRefL);
                                }
                                if(libraryRefL!=null){
                                    libraryRef.removeEventListener(libraryRefL);
                                }

                                finish();

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "onCancelled: "+error.toString());

                            if(cRunnable != null){
                                mHandler.removeCallbacks(cRunnable);
                            }

                            if(usersRefL!=null){
                                usersRef.removeEventListener(usersRefL);
                            }
                            if(usersBookRefL!=null){
                                usersRef.removeEventListener(usersBookRefL);
                            }
                            if(bookRefL!=null){
                                bookRef.removeEventListener(bookRefL);
                            }
                            if(libraryRefL!=null){
                                libraryRef.removeEventListener(libraryRefL);
                            }

                            showToast(getApplicationContext(), "Some error occurred while fetching the libraries!", 1);

                            finish();
                        }
                    });
                }
//                else{
//                    showToast(getApplicationContext(), "Create a library", 1);
//                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: "+error.toString());

                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }
                if(usersBookRefL!=null){
                    usersRef.removeEventListener(usersBookRefL);
                }
                if(bookRefL!=null){
                    bookRef.removeEventListener(bookRefL);
                }
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
                }

                showToast(getApplicationContext(), "Some error occurred while fetching the libraries!", 1);

                finish();
            }
        });

        usersBookRefL = usersRef.child(FirebaseUtil.encode(cEmail)).child("books").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    cUserBooks.clear();
                    for(DataSnapshot snap: snapshot.getChildren()){
                        cUserBooks.add(snap.getValue().toString());
                    }

                    // Now finding all the libs with ids present in cUserBooks
                    bookRefL = bookRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()) {
                                userBooks.clear();
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    BookModel cBook = snap.getValue(BookModel.class);
                                    if (cUserBooks.contains(cBook.getBookId())) {
                                        userBooks.add(cBook);
                                    }
                                }
                            }
                            else{
                                Log.e(TAG, "No children in book reference! No snapshot exists...");
                                showToast(getApplicationContext(), "Some error occurred! No books exist...", 1);

                                if(cRunnable != null){
                                    mHandler.removeCallbacks(cRunnable);
                                }

                                if(usersRefL!=null){
                                    usersRef.removeEventListener(usersRefL);
                                }
                                if(usersBookRefL!=null){
                                    usersRef.removeEventListener(usersBookRefL);
                                }
                                if(bookRefL!=null){
                                    bookRef.removeEventListener(bookRefL);
                                }
                                if(libraryRefL!=null){
                                    libraryRef.removeEventListener(libraryRefL);
                                }

                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "onCancelled: "+error.toString());

                            if(cRunnable != null){
                                mHandler.removeCallbacks(cRunnable);
                            }

                            if(usersRefL!=null){
                                usersRef.removeEventListener(usersRefL);
                            }
                            if(usersBookRefL!=null){
                                usersRef.removeEventListener(usersBookRefL);
                            }
                            if(bookRefL!=null){
                                bookRef.removeEventListener(bookRefL);
                            }
                            if(libraryRefL!=null){
                                libraryRef.removeEventListener(libraryRefL);
                            }

                            showToast(getApplicationContext(), "Some error occurred while fetching the books!", 1);

                            finish();
                        }
                    });
                }
//                else{
//                    showToast(getApplicationContext(), "Add a book", 1);
//                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: "+error.toString());

                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                if(usersRefL!=null){
                    usersRef.removeEventListener(usersRefL);
                }
                if(usersBookRefL!=null){
                    usersRef.removeEventListener(usersBookRefL);
                }
                if(bookRefL!=null){
                    bookRef.removeEventListener(bookRefL);
                }
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
                }

                showToast(getApplicationContext(), "Some error occurred while fetching the books!", 1);

                finish();
            }
        });

        searchUserCategoryEt.setOnClickListener(v -> {
            String[] categories = {"Libraries", "Books"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select type:")
                    .setItems(categories, (dialog, which) -> {
                        cSearchCategory = categories[which];
                        pSearch = "-1";
                        searchUserCategoryEt.setText(cSearchCategory);
                    }).show();
        });

        logoutIv.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ManageStuffActivity.this);
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setMessage("Are you sure you want to Logout?");
            builder.setTitle("Logout");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cAuth.signOut();
                    Toast.makeText(ManageStuffActivity.this, "Successfully logged out.", Toast.LENGTH_SHORT).show();

                    if(cRunnable != null){
                        mHandler.removeCallbacks(cRunnable);
                    }

                    if(usersRefL!=null){
                        usersRef.removeEventListener(usersRefL);
                    }
                    if(usersBookRefL!=null){
                        usersRef.removeEventListener(usersBookRefL);
                    }
                    if(bookRefL!=null){
                        bookRef.removeEventListener(bookRefL);
                    }
                    if(libraryRefL!=null){
                        libraryRef.removeEventListener(libraryRefL);
                    }

                    // Heading to Login Activity
                    Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
                    loginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginActivity);
                    finish();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        });

//        // When pressed on add library iv
//        addLibraryIv.setOnClickListener(v -> {
//            if(cRunnable != null){
//                mHandler.removeCallbacks(cRunnable);
//            }
//
//            if(usersRefL!=null){
//                usersRef.removeEventListener(usersRefL);
//            }
//            if(usersBookRefL!=null){
//                usersRef.removeEventListener(usersBookRefL);
//            }
//            if(bookRefL!=null){
//                bookRef.removeEventListener(bookRefL);
//            }
//            if(libraryRefL!=null){
//                libraryRef.removeEventListener(libraryRefL);
//            }
//
//            // Heading to Add Library page
//            Intent addLibraryActivity = new Intent(getApplicationContext(), AddLibraryActivity.class);
//            addLibraryActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(addLibraryActivity);
//            finish();
//        });
//
//        // When pressed on add book iv
//        addBookIv.setOnClickListener(v -> {
//            if(cRunnable != null){
//                mHandler.removeCallbacks(cRunnable);
//            }
//
//            if(usersRefL!=null){
//                usersRef.removeEventListener(usersRefL);
//            }
//            if(usersBookRefL!=null){
//                usersRef.removeEventListener(usersBookRefL);
//            }
//            if(bookRefL!=null){
//                bookRef.removeEventListener(bookRefL);
//            }
//            if(libraryRefL!=null){
//                libraryRef.removeEventListener(libraryRefL);
//            }
//
//            // Heading to Add Book page
//            Intent addBookActivity = new Intent(getApplicationContext(), AddBookActivity.class);
//            addBookActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(addBookActivity);
//            finish();
//        });

        addStuffIv.setOnClickListener(v -> {
            String[] choices = {"Create a library", "Add a book"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose:")
                    .setCancelable(true)
                    .setItems(choices, (dialog, which) -> {
                        if(which == 0){
                            if(cRunnable != null){
                                mHandler.removeCallbacks(cRunnable);
                            }

                            if(usersRefL!=null){
                                usersRef.removeEventListener(usersRefL);
                            }
                            if(usersBookRefL!=null){
                                usersRef.removeEventListener(usersBookRefL);
                            }
                            if(bookRefL!=null){
                                bookRef.removeEventListener(bookRefL);
                            }
                            if(libraryRefL!=null){
                                libraryRef.removeEventListener(libraryRefL);
                            }

                            // Heading to Add Library page
                            Intent addLibraryActivity = new Intent(getApplicationContext(), AddLibraryActivity.class);
                            addLibraryActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(addLibraryActivity);
                            finish();
                        }
                        else if(which == 1){
                            if(cRunnable != null){
                                mHandler.removeCallbacks(cRunnable);
                            }

                            if(usersRefL!=null){
                                usersRef.removeEventListener(usersRefL);
                            }
                            if(usersBookRefL!=null){
                                usersRef.removeEventListener(usersBookRefL);
                            }
                            if(bookRefL!=null){
                                bookRef.removeEventListener(bookRefL);
                            }
                            if(libraryRefL!=null){
                                libraryRef.removeEventListener(libraryRefL);
                            }

                            // Heading to Add Book page
                            Intent addBookActivity = new Intent(getApplicationContext(), AddBookActivity.class);
                            addBookActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(addBookActivity);
                            finish();
                        }
                    }).show();
        });

        // When pressed on explore ll
        exploreLl.setOnClickListener(v -> {
            if(cRunnable != null){
                mHandler.removeCallbacks(cRunnable);
            }

            if(usersRefL!=null){
                usersRef.removeEventListener(usersRefL);
            }
            if(usersBookRefL!=null){
                usersRef.removeEventListener(usersBookRefL);
            }
            if(bookRefL!=null){
                bookRef.removeEventListener(bookRefL);
            }
            if(libraryRefL!=null){
                libraryRef.removeEventListener(libraryRefL);
            }

            // Heading to Main page
            Intent mainPage = new Intent(getApplicationContext(), MainPage.class);
            mainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainPage);
            finish();
        });

        // Check every two seconds
        mHandler.postDelayed(cRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(cRunnable, delay);
                if(cSearchCategory.equals("Libraries")) {
                    if (userLibraries.size() != 0) {
                        String cSearch = searchUserLibsEt.getText().toString();
                        if (!pSearch.equals(cSearch)) {
                            pSearch = cSearch;

                            ArrayList<String> allLibIds = new ArrayList<>();
                            ArrayList<String> allLibNames = new ArrayList<>();
                            ArrayList<LibraryModel> allLibModels = new ArrayList<>();
                            for (LibraryModel libraryModel : userLibraries) {
                                if (cSearch.trim().length() != 0) {
                                    if (libraryModel.getLibId().contains(cSearch) || libraryModel.getLibName().toLowerCase().contains(cSearch.toLowerCase())) {
                                        allLibIds.add(libraryModel.getLibId());
                                        allLibNames.add(libraryModel.getLibName());
                                        allLibModels.add(libraryModel);
                                    }
                                } else {
                                    allLibIds.add(libraryModel.getLibId());
                                    allLibNames.add(libraryModel.getLibName());
                                    allLibModels.add(libraryModel);
                                }
                            }

                            String[] libIds = new String[allLibIds.size()];
                            String[] libNames = new String[allLibNames.size()];
                            LibraryModel[] libraryModels = new LibraryModel[allLibModels.size()];

                            for (int i = 0; i < allLibIds.size(); i++) {
                                libIds[i] = allLibIds.get(i);
                                libNames[i] = allLibNames.get(i);
                                libraryModels[i] = allLibModels.get(i);
                            }

                            statusManagePageLbl.setVisibility(View.GONE);
                            userLibrariesRv.setVisibility(View.VISIBLE);
                            userLibrariesRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            PublicLibraryAdaptor adaptor = new PublicLibraryAdaptor(libNames, libIds, libraryModels, new PublicLibraryAdaptor.OnItemClickListener() {
                                @Override
                                public void onItemClick(String libName, String libId, LibraryModel libraryModel) {
                                    mHandler.removeCallbacks(cRunnable);

                                    if(libraryRefL!=null){
                                        libraryRef.removeEventListener(libraryRefL);
                                    }
                                    if(usersRefL!=null){
                                        usersRef.removeEventListener(usersRefL);
                                    }
                                    if(usersBookRefL!=null){
                                        usersRef.removeEventListener(usersBookRefL);
                                    }
                                    if(bookRefL!=null){
                                        bookRef.removeEventListener(bookRefL);
                                    }

                                    // Heading to Access Library page
                                    Intent accessLibraryActivity = new Intent(getApplicationContext(), AccessLibraryActivity.class);
                                    accessLibraryActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryId", libId);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryPassword", libraryModel.getPassword());
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryName", libName);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.FromPublic", false);

                                    startActivity(accessLibraryActivity);
                                    finish();
                                }
                            });

                            userLibrariesRv.setAdapter(adaptor);
                        }
                    }
                    else{
                        statusManagePageLbl.setVisibility(View.VISIBLE);
                        userLibrariesRv.setVisibility(View.GONE);
                        statusManagePageLbl.setText("No Libraries Found!");
                    }
                }
                // If book category is considered
                else{
                    if (userBooks.size() != 0) {
                        String cSearch = searchUserLibsEt.getText().toString();
                        if (!pSearch.equals(cSearch)) {
                            pSearch = cSearch;

                            ArrayList<String> allBookIds = new ArrayList<>();
                            ArrayList<String> allBookNames = new ArrayList<>();
                            ArrayList<BookModel> allBookModels = new ArrayList<>();
                            for (BookModel bookModel : userBooks) {
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
                            }

                            statusManagePageLbl.setVisibility(View.GONE);
                            userLibrariesRv.setVisibility(View.VISIBLE);
                            userLibrariesRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            PublicBookAdaptor adaptor = new PublicBookAdaptor(bookNames, bookIds, bookModels, new PublicBookAdaptor.OnItemClickListener() {
                                @Override
                                public void onItemClick(String bookName, String bookId, BookModel bookModel) {
                                    String[] options = {"Download book"};

                                    AlertDialog.Builder builder = new AlertDialog.Builder(ManageStuffActivity.this);
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
                                                    if(usersBookRefL!=null){
                                                        usersRef.removeEventListener(usersBookRefL);
                                                    }
                                                    if(usersRefL!=null){
                                                        usersRef.removeEventListener(usersRefL);
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

                            userLibrariesRv.setAdapter(adaptor);
                        }
                    }
                    else{
                        statusManagePageLbl.setVisibility(View.VISIBLE);
                        userLibrariesRv.setVisibility(View.GONE);
                        statusManagePageLbl.setText("No Books Found!");
                    }
                }
            }

        }, delay);


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

        if(usersRefL!=null){
            usersRef.removeEventListener(usersRefL);
        }
        if(usersBookRefL!=null){
            usersRef.removeEventListener(usersBookRefL);
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