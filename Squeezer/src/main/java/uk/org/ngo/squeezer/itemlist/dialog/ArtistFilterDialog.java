package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.GenreSpinner;
import uk.org.ngo.squeezer.model.Genre;

public class ArtistFilterDialog extends BaseFilterDialog {

    private ArtistListActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (ArtistListActivity) getActivity();
        editText.setHint(getString(R.string.filter_text_hint,
                activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        yearView.setVisibility(View.GONE);
        new GenreSpinner(activity, activity, genreSpinnerView);

        if (activity.getAlbum() != null) {
            album.setText(activity.getAlbum().getName());
            albumView.setVisibility(View.VISIBLE);
        }

        return dialog;
    }

    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((Genre) genreSpinnerView.getSelectedItem());
        activity.clearAndReOrderItems();
    }

}
