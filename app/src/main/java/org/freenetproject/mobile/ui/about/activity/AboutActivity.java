package org.freenetproject.mobile.ui.about.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.freenetproject.mobile.BuildConfig;
import org.freenetproject.mobile.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView wv = (WebView) findViewById(R.id.about_webview);
        wv.loadData(
                getResources().getString(R.string.text_about, BuildConfig.VERSION_NAME),
                "text/html; charset=utf-8",
                null
        );
    }
}