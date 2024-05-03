package com.canopus.frindl;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class PublicBookAdaptor extends RecyclerView.Adapter<PublicBookAdaptor.ViewHolder> {

    private String[] bookDataNames;
    private String[] bookDataIds;
    private BookModel[] bookDataBooks;
    private final OnItemClickListener listener;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    public interface OnItemClickListener {
        void onItemClick(String bookName, String bookId, BookModel bookModel);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView bookNameLbl;
        private final TextView bookIdLbl;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            bookNameLbl = (TextView) view.findViewById(R.id.lblBookName);
            bookIdLbl = (TextView) view.findViewById(R.id.lblBookId);
        }

        public TextView[] getTextView() {
            return new TextView[]{bookNameLbl, bookIdLbl};
        }

        public void bind(final String bName, final String bId, final BookModel bBook, final OnItemClickListener listener){
            getTextView()[0].setText(bName);
            getTextView()[1].setText(bId);
            getTextView()[0].setSelected(true);
//            getTextView().setText(Html.fromHtml(""+lName+"<sup>"+lId+"</sup>",1));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(bName, bId, bBook);
                }
            });
        }

    }

    public PublicBookAdaptor(String[] dataSetNames, String [] dataSetIds, BookModel[] dataSetBooks, OnItemClickListener l) {
        bookDataNames = dataSetNames;
        bookDataIds = dataSetIds;
        bookDataBooks = dataSetBooks;
        listener = l;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.books_view, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(bookDataNames[position], bookDataIds[position], bookDataBooks[position], listener);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return bookDataNames.length;
    }
}
