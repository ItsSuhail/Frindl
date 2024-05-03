package com.canopus.frindl;

import java.util.ArrayList;

public class LibraryModel{
    private String libId;
    private String libName;
    private ArrayList<String> bookIds = new ArrayList<String>();
    private String password;

    private boolean isPublic;

    public LibraryModel(String libName, String password, boolean isPublic) {
        this.libId = FirebaseUtil.returnRandKey("L");
        this.libName = libName;
        this.password = password;
        this.isPublic = isPublic;
    }

    public LibraryModel(){

    }
    public String getLibId() {
        return libId;
    }

    public void setLibId(String libId) {
        this.libId = libId;
    }

    public String getLibName() {
        return libName;
    }

    public void setLibName(String libName) {
        this.libName = libName;
    }

    public ArrayList<String> getBookIds() {
        return bookIds;
    }

    public void setBookIds(ArrayList<String> bookIds) {
        this.bookIds = bookIds;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
