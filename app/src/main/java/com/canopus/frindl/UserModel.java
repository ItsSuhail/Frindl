package com.canopus.frindl;

import java.util.ArrayList;

public class UserModel {
    private String displayName;
    private String uEmail;
    private ArrayList<String> librariesId = new ArrayList<>();
    private ArrayList<String> booksId = new ArrayList<>();

    public UserModel(String displayName, String uEmail){
        this.displayName = displayName;
        this.uEmail = uEmail;
    }

    public UserModel(){

    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUEmail() {
        return uEmail;
    }

    public void setUEmail(String uEmail) {
        this.uEmail = uEmail;
    }

    public ArrayList<String> getLibraries() {
        return librariesId;
    }

    public void setLibraries(ArrayList<String> librariesId) {
        this.librariesId = librariesId;
    }

    public ArrayList<String> getBooks() {
        return booksId;
    }

    public void setBooks(ArrayList<String> booksId) {
        this.booksId = booksId;
    }
}
