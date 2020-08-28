package org.freenetproject.mobile.ui.acknowledgement.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.proxy.Simple;
import org.freenetproject.mobile.ui.main.activity.MainActivity;
import org.freenetproject.mobile.ui.settings.activity.SettingsActivity;

public class AcknowledgementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acknowledgement);
    }
}