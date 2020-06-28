package org.freenetproject.mobilenode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

import freenet.support.Logger;

public class MainActivity extends AppCompatActivity {
    FreenetStatusReceiver receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiver = new FreenetStatusReceiver();
        registerReceiver(receiver, new IntentFilter("STATUS"));
    }

    @Override
    public void onStop()
    {
        super.onStop();
        this.startNode();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Logger.error(this, "Failed to unregister receiver");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startNode() {
        Intent serviceIntent = new Intent(this, FreenetService.class);
        this.startService(serviceIntent);
    }

    public void stopNode() {
        Intent serviceIntent = new Intent(this, FreenetService.class);
        this.stopService(serviceIntent);
    }

    class FreenetStatusReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            if (Objects.equals(intent.getAction(), "STATUS"))
            {
                TextView textView = (TextView) findViewById(R.id.freenetStatus);
                String status = intent.getStringExtra("STATUS_HUMAN_READABLE");
                textView.setText(status);

                TextView detailText = (TextView) findViewById(R.id.detailText);
                if (status.equals("Running")) {
                    detailText.setText("Navigate to 127.0.0.1:8888 to access Freenet");
                } else {
                    detailText.setText("");
                }
            }
        }
    }
}