package org.freenetproject.mobile.ui.about.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.freenetproject.mobile.BuildConfig;
import org.freenetproject.mobile.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView aboutText = findViewById(R.id.textview_about);
        aboutText.setText(Html.fromHtml(getString(R.string.text_about, BuildConfig.VERSION_NAME)));
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
    }
}