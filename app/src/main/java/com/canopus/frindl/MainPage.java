package com.canopus.frindl;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;

public class MainPage extends AppCompatActivity {
    String TAG = "com.canopus.frindl.logs";
    FirebaseAuth cAuth;
    FirebaseUser cUser;
    FirebaseDatabase db;
    DatabaseReference libraryRef, bookRef;
    ValueEventListener libraryRefL, bookRefL;

    // Views
    LinearLayout myStuffLl;
    TextView statusMainPageLbl;
    RecyclerView librariesRv;
    EditText searchBarEt, searchCategoryEt;

    // Storage
    ArrayList<LibraryModel> libraries = new ArrayList<LibraryModel>();
    ArrayList<BookModel> books = new ArrayList<BookModel>();
    Toast cToast;

    Handler mHandler = new Handler();
    Runnable cRunnable;
    int delay = 500;

    String pSearch = "-1";
    String cSearchCategory = "Libraries";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(libraryRefL!=null){
                    libraryRef.removeEventListener(libraryRefL);
                }
                if(bookRefL != null){
                    bookRef.removeEventListener(bookRefL);
                }
                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
                }

                finish();
            }
        });

        // Defining views
        myStuffLl = findViewById(R.id.llMyStuff);
        statusMainPageLbl = findViewById(R.id.lblStatusMainPage);
        librariesRv = findViewById(R.id.rvLibraries);
        searchBarEt = findViewById(R.id.etSearchBar);
        searchCategoryEt = findViewById(R.id.etSearchCategory);

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

        // Initializing Firebase Database
        db = FirebaseDatabase.getInstance("https://frindl-default-rtdb.asia-southeast1.firebasedatabase.app/");
        libraryRef = db.getReference("libraries");
        bookRef = db.getReference("docs");
        libraryRefL = libraryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Log.d(TAG, "onDataChange: "+snapshot.toString());
                if(snapshot.exists()) {
                    libraries.clear();

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        LibraryModel cLibrary = snap.getValue(LibraryModel.class);
                        if (cLibrary.isPublic()) {
                            libraries.add(cLibrary);
//                            Log.d(TAG, cLibrary.getLibName());
                        }
                    }
                }
//                else{
//                    showToast(getApplicationContext(), "No libraries exist", 1);
//                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: "+error.toString());

                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
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

        bookRefL = bookRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    books.clear();

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        BookModel cBook = snap.getValue(BookModel.class);
                        if (cBook.isPublic()) {
                            books.add(cBook);
                        }
                    }
                }
//                else{
//                    showToast(getApplicationContext(), "No books exist", 1);
//                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: "+error.toString());

                if(cRunnable != null){
                    mHandler.removeCallbacks(cRunnable);
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

        myStuffLl.setOnClickListener(v -> {
            if(cRunnable != null){
                mHandler.removeCallbacks(cRunnable);
            }
            if(bookRefL!=null){
                bookRef.removeEventListener(bookRefL);
            }
            if(libraryRefL!=null){
                libraryRef.removeEventListener(libraryRefL);
            }

            // Heading to Manage Stuff page
            Intent manageStuffActivity = new Intent(getApplicationContext(), ManageStuffActivity.class);
            manageStuffActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(manageStuffActivity);
            finish();
        });

        searchCategoryEt.setOnClickListener(v -> {
            String[] categories = {"Libraries", "Books"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select type:")
                    .setItems(categories, (dialog, which) -> {
                        cSearchCategory = categories[which];
                        pSearch = "-1";
                        searchCategoryEt.setText(cSearchCategory);
                    }).show();
        });

        mHandler.postDelayed(cRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(cRunnable, delay);
                if(cSearchCategory.equals("Libraries")){
                    if (libraries.size() != 0) {
//                    Log.d(TAG, "run: Happenin");
                        String cSearch = searchBarEt.getText().toString();
                        if(!pSearch.equals(cSearch)){
                            pSearch = cSearch;
                            ArrayList<String> allLibIds = new ArrayList<>();
                            ArrayList<String> allLibNames = new ArrayList<>();
                            ArrayList<LibraryModel> allLibModels = new ArrayList<>();
                            for (LibraryModel libraryModel : libraries) {
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


                            statusMainPageLbl.setVisibility(View.GONE);
                            librariesRv.setVisibility(View.VISIBLE);
                            librariesRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            PublicLibraryAdaptor adaptor = new PublicLibraryAdaptor(libNames, libIds, libraryModels, new PublicLibraryAdaptor.OnItemClickListener() {
                                @Override
                                public void onItemClick(String libName, String libId, LibraryModel libraryModel) {
                                    mHandler.removeCallbacks(cRunnable);

                                    if(libraryRefL!=null){
                                        libraryRef.removeEventListener(libraryRefL);
                                    }
                                    if(bookRefL != null){
                                        bookRef.removeEventListener(bookRefL);
                                    }

                                    // Heading to Access Library page
                                    Intent accessLibraryActivity = new Intent(getApplicationContext(), AccessLibraryActivity.class);
                                    accessLibraryActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryId", libId);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryPassword", libraryModel.getPassword());
                                    accessLibraryActivity.putExtra("com.canopus.frindl.LibraryName", libName);
                                    accessLibraryActivity.putExtra("com.canopus.frindl.FromPublic", true);
                                    startActivity(accessLibraryActivity);
                                    finish();
                                }
                            });

                            librariesRv.setAdapter(adaptor);
                        }
                    }
                    else{
                        statusMainPageLbl.setVisibility(View.VISIBLE);
                        librariesRv.setVisibility(View.GONE);
                        statusMainPageLbl.setText("No Libraries Found!");
                    }
                }
                else{
                    if (books.size() != 0) {
                        Log.d(TAG, "run: Happenin");
                        String cSearch = searchBarEt.getText().toString();
                        if(!pSearch.equals(cSearch)){
                            pSearch = cSearch;
                            ArrayList<String> allBookIds = new ArrayList<>();
                            ArrayList<String> allBookNames = new ArrayList<>();
                            ArrayList<BookModel> allBookModels = new ArrayList<>();
                            for (BookModel bookModel : books) {
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

                            statusMainPageLbl.setVisibility(View.GONE);
                            librariesRv.setVisibility(View.VISIBLE);
                            librariesRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            PublicBookAdaptor adaptor = new PublicBookAdaptor(bookNames, bookIds, bookModels, new PublicBookAdaptor.OnItemClickListener() {
                                @Override
                                public void onItemClick(String bookName, String bookId, BookModel bookModel) {
                                    String[] options = {"Download book"};

                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainPage.this);
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

                            librariesRv.setAdapter(adaptor);
                        }
                    }
                    else{
                        statusMainPageLbl.setVisibility(View.VISIBLE);
                        librariesRv.setVisibility(View.GONE);
                        statusMainPageLbl.setText("No Books Found!");
                    }
                }

            // Run
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
        if(bookRefL!=null){
            bookRef.removeEventListener(bookRefL);
        }
        if(libraryRefL!=null){
            libraryRef.removeEventListener(libraryRefL);
        }

        super.onPause();
    }
}