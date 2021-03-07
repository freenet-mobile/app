package org.freenetproject.mobile.ui.bootstrap.activity;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import org.freenetproject.mobile.R;

public class BootstrapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);

        WebView wv = (WebView) findViewById(R.id.bootstrap_webview);
        wv.loadUrl("file:///android_asset/index.html");

    }
}
