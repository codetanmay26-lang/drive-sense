package com.drivesense.app.utils;

import android.app.Activity;
import android.view.View;

import com.drivesense.app.R;

public final class BackNavigation {
    private BackNavigation() {}

    public static void attach(Activity activity) {
        View backButton = activity.findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> activity.finish());
        }
    }
}
