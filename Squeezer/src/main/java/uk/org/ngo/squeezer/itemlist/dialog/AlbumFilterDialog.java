package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.GenreSpinner;
import uk.org.ngo.squeezer.itemlist.YearSpinner;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Year;

public class AlbumFilterDialog extends BaseFilterDialog {

    private AlbumListActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (AlbumListActivity) getActivity();

        editText.setHint(getString(R.string.filter_text_hint,
                activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        new GenreSpinner(activity, activity, genreSpinnerView);
        new YearSpinner(activity, activity, yearSpinnerView);

        if (activity.getSong() != null) {
            track.setText(activity.getSong().getName());
            trackView.setVisibility(View.VISIBLE);
        }
        if (activity.getArtist() != null) {
            artist.setText(activity.getArtist().getName());
            artistView.setVisibility(View.VISIBLE);
        }

        return dialog;
    }

    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((Genre) genreSpinnerView.getSelectedItem());
        activity.setYear((Year) yearSpinnerView.getSelectedItem());
        activity.clearAndReOrderItems();
    }

}
