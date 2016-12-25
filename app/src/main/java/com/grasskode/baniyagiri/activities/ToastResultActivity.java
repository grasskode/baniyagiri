package com.grasskode.baniyagiri.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by karan on 29/7/16.
 */
public abstract class ToastResultActivity extends AppCompatActivity {

    protected static final int REQ_CODE_EDIT_EXPENSE = 1;
    protected static final int REQ_CODE_ANALYTICS = 2;
    protected static final int REQ_CODE_MANAGE_GROUPS = 3;
    protected static final int REQ_CODE_SETTINGS = 4;
    protected static final int REQ_CODE_EDIT_GROUP = 5;
    protected static final int REQ_CODE_IMPORT = 6;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // check if a toast is requested
            String toast = data.getStringExtra("toast");
            if(toast != null && toast.length() > 0) {
                Toast.makeText(this, toast, Toast.LENGTH_LONG)
                        .show();
            }
        } catch (NullPointerException e) {
            // no toast to be shown
            // ignore
        }

        // pass to super
        super.onActivityResult(requestCode, resultCode, data);
    }
}
