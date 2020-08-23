package org.freenetproject.mobile.ui.acknowledgement.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.activity.MainActivity;

public class AcknowledgementFragment extends Fragment {
    private SharedPreferences prefs;
    public static final String ACKNOWLEDGEMENT_KEY = "acknowledgement";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        prefs = getContext().getSharedPreferences(
                getContext().getPackageName(), Context.MODE_PRIVATE
        );

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_acknowledgement, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatCheckBox acknowledgement = view.findViewById(R.id.acknowledgement);
        acknowledgement.setText(Html.fromHtml(getString(R.string.acknowledgement_text)));
        acknowledgement.setMovementMethod(LinkMovementMethod.getInstance());

        acknowledgement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit()
                .putBoolean(ACKNOWLEDGEMENT_KEY, isChecked)
                .apply();

            if (isChecked) {
                activateMainActivity();
            }
        });
    }

    private void activateMainActivity() {
        Intent intent = new Intent(getContext(), MainActivity.class);
        startActivity(intent);
    }
}