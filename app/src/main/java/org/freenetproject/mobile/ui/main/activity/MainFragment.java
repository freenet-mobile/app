package org.freenetproject.mobile.ui.main.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.viewmodel.MainViewModel;
import org.freenetproject.mobile.ui.settings.activity.SettingsActivity;

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
        updateSettings(mainViewModel, view);
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
                statusText.setText(R.string.node_running);
            } else if (status.equals(MainViewModel.Status.STARTING_UP)) {
                statusText.setText(R.string.node_starting_up);
            } else if (status.equals(MainViewModel.Status.STOPPING)) {
                statusText.setText(R.string.node_shutting_down);
            } else if (status.equals(MainViewModel.Status.ERROR)) {
                statusText.setText(R.string.error_starting_up);
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
                detailText.setText(R.string.tap_to_navigate);
                detailText.setOnClickListener(view12 -> {
                    startActivity(
                        new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("http://127.0.0.1:8888/"))
                    );
                });
            } else if (status.equals(MainViewModel.Status.STARTING_UP)) {
                detailText.setText(R.string.may_take_a_while);
            } else if (status.equals(MainViewModel.Status.ERROR)) {
                detailText.setText(R.string.error_detail);
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

    private void updateSettings(MainViewModel vm, View view) {
        FloatingActionButton settings = view.findViewById(R.id.settingsButton);
        settings.setOnClickListener(view1 -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
    }
}