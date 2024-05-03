package com.canopus.frindl;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class PublicLibraryAdaptor extends RecyclerView.Adapter<PublicLibraryAdaptor.ViewHolder> {

    private String[] libDataNames;
    private String[] libDataIds;
    private LibraryModel[] libDataLibraries;
    private final OnItemClickListener listener;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    public interface OnItemClickListener {
        void onItemClick(String libName, String libId, LibraryModel libraryModel);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView libraryNameLbl;
        private final TextView libraryIdLbl;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            libraryNameLbl = (TextView) view.findViewById(R.id.lblLibraryName);
            libraryIdLbl = (TextView) view.findViewById(R.id.lblLibraryId);
        }

        public TextView[] getTextView() {
            return new TextView[]{libraryNameLbl, libraryIdLbl};
        }

        public void bind(final String lName, final String lId, final LibraryModel lLib, final OnItemClickListener listener){
            getTextView()[0].setText(lName);
            getTextView()[1].setText(lId);
            getTextView()[0].setSelected(true);
//            getTextView().setText(Html.fromHtml(""+lName+"<sup>"+lId+"</sup>",1));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(lName, lId, lLib);
                }
            });
        }

    }

    public PublicLibraryAdaptor(String[] dataSetNames, String [] dataSetIds, LibraryModel[] dataSetLibraries, OnItemClickListener l) {
        libDataNames = dataSetNames;
        libDataIds = dataSetIds;
        libDataLibraries = dataSetLibraries;
        listener = l;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.libraries_view, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(libDataNames[position], libDataIds[position], libDataLibraries[position], listener);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return libDataNames.length;
    }
}
