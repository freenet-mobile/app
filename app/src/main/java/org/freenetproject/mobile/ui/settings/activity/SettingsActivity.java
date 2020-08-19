package org.freenetproject.mobile.ui.settings.activity;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.proxy.Simple;

public class SettingsActivity extends AppCompatActivity {

    private static String localIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        localIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SwitchPreferenceCompat editPref = findPreference("web_access");
            editPref.setSummary(
                    getActivity().getApplicationContext().getString(
                            R.string.web_access_summary,
                            localIp,
                            Simple.defaultLocalPort
                    )
            );
        }
    }
}