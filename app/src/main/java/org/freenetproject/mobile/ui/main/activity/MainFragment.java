package org.freenetproject.mobile.ui.main.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.viewmodel.MainViewModel;

public class MainFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        mainViewModel.init(
            sharedPref.getBoolean("isRunning", false) ? MainViewModel.Status.STARTED : MainViewModel.Status.STOPPED
        );

        updateSharedPreferences(mainViewModel, view);
        updateControls(mainViewModel, view);
        updateStatus(mainViewModel, view);
        updateStatusDetail(mainViewModel, view);
    }

    private void updateControls(MainViewModel vm, View view) {
        ToggleButton controlButton = view.findViewById(R.id.controlButton);
        controlButton.setOnClickListener(view1 -> {
            // HACK: Disable toggle button automatic check on click
            controlButton.setChecked(!controlButton.isChecked());

            new Thread(() -> {
                // Return right aways if for some reason it is still working (either
                // starting up or stopping).
                if (vm.isTransitioning()) {
                    return;
                }

                if (vm.isRunning()) {
                    vm.stopService(view.getContext());
                } else {
                    vm.startService(view.getContext());
                }
            }).start();
        });

        vm.getStatus().observe(getViewLifecycleOwner(), status -> {
            controlButton
                .setEnabled(
                    !vm.isTransitioning()
                );
            controlButton
                .setChecked(
                    vm.isRunning()
                );
        });
    }

    private void updateStatus(MainViewModel vm, View view) {
        TextView statusText = view.findViewById(R.id.freenetStatus);
        vm.getStatus().observe(getViewLifecycleOwner(), status -> {
            if (status.equals(MainViewModel.Status.STARTED)) {
                statusText.setText("Freenet is running");
            } else if (status.equals(MainViewModel.Status.STARTING_UP)) {
                statusText.setText("Starting up");
            } else if (status.equals(MainViewModel.Status.STOPPING)) {
                statusText.setText("Shutting down");
            } else if (status.equals(MainViewModel.Status.ERROR)) {
                statusText.setText("Error starting Freenet.");
            } else {
                statusText.setText("");
            }
        });
    }

    private void updateStatusDetail(MainViewModel vm, View view) {
        TextView detailText = view.findViewById(R.id.detailText);
        vm.getStatus().observe(getViewLifecycleOwner(), status -> {
            detailText.setOnClickListener(null);
            if (status.equals(MainViewModel.Status.STARTED)) {
                detailText.setText("Tap here to Navigate");
                detailText.setOnClickListener(view12 -> {
                    startActivity(
                        new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("http://127.0.0.1:8888/"))
                    );
                });
            } else if (status.equals(MainViewModel.Status.STARTING_UP)) {
                detailText.setText("This may take a while.");
            } else if (status.equals(MainViewModel.Status.ERROR)) {
                detailText.setText("Restart the application and try again.\nIf the error persist reinstall the application.");
            } else {
                detailText.setText("");
            }
        });
    }

    private void updateSharedPreferences(MainViewModel vm, View view) {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        vm.getStatus().observe(getViewLifecycleOwner(), status -> {
            editor.putBoolean("isRunning", vm.isRunning());
            editor.apply();
        });
    }
}