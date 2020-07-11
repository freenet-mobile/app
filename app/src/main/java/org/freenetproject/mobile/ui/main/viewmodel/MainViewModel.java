package org.freenetproject.mobile.ui.main.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import org.freenetproject.mobile.services.node.Manager;

/**
 * Class responsible for exposing data to the UI. It also exposes methods for the UI to interact with,
 * such as startService and stopService.
 */
public class MainViewModel extends ViewModel {
    Manager manager = Manager.getInstance();
    private LiveData<Manager.Status> status = manager.getStatus();

    public MainViewModel() {
        super();
    }
}

