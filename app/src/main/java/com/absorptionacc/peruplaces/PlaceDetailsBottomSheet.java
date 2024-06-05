package com.absorptionacc.peruplaces;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaceDetailsBottomSheet extends BottomSheetDialogFragment {

    private String placeName;
    private String placeRating;
    private String placeAddress;
    private String placeDescription;

    public PlaceDetailsBottomSheet() {
        // Constructor vacío necesario
    }

    public PlaceDetailsBottomSheet(String placeName, String placeRating, String placeAddress, String placeDescription) {
        this.placeName = placeName;
        this.placeRating = placeRating;
        this.placeAddress = placeAddress;
        this.placeDescription = placeDescription;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_place_details, container, false);

        TextView tvPlaceName = view.findViewById(R.id.tvPlaceName);
        TextView tvPlaceRating = view.findViewById(R.id.tvPlaceRating);
        TextView tvPlaceAddress = view.findViewById(R.id.tvPlaceAddress);
        TextView tvPlaceDescription = view.findViewById(R.id.tvPlaceDescription);

        tvPlaceName.setText(placeName);
        tvPlaceRating.setText(placeRating);
        tvPlaceAddress.setText(placeAddress);
        tvPlaceDescription.setText(placeDescription);

        return view;
    }
}
