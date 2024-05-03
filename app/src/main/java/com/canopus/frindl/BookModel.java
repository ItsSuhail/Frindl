package com.canopus.frindl;

public class BookModel{
    private String bookId;
    private String accessUrl;
    private boolean isPublic;

    private boolean inLibrary;
    private String bookTitle;
    private String bookDescription;
    private String category;

    private String libraryId;

    public BookModel(String bookId, String bookTitle, String bookDescription, String category, String accessUrl, boolean isPublic, boolean inLibrary, String libraryId) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookDescription = bookDescription;
        this.category = category;
        this.accessUrl = accessUrl;
        this.isPublic = isPublic;
        this.inLibrary = inLibrary;
        this.libraryId = libraryId;
    }

    public BookModel(){

    }
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isInLibrary() {
        return inLibrary;
    }

    public void setInLibrary(boolean inLibrary) {
        this.inLibrary = inLibrary;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookDescription() {
        return bookDescription;
    }

    public void setBookDescription(String bookDescription) {
        this.bookDescription = bookDescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }
}
