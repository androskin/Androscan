package com.androskin.androscan.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.androskin.androscan.R;
import com.androskin.androscan.enums.EnumDialogValues;

public class DialogYesNo extends DialogFragment {
    private final static String TAG = "GA_DialogYesNo";

    private EnumDialogValues key;
    private String title;
    private String message;

    private DialogYesNo() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "onClick_YES");
                getParentFragmentManager().setFragmentResult(key.toString(), new Bundle());
            }
        });
        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "onClick_NO");
                //do nothing
            }
        });
        builder.setCancelable(false);

        return builder.create();
    }

    public static void showDialog(FragmentManager fragmentManager, EnumDialogValues key, String title, String message) {
        DialogYesNo myDialogFragment = new DialogYesNo();
        myDialogFragment.key = key;
        myDialogFragment.title = title;
        myDialogFragment.message = message;
        myDialogFragment.show(fragmentManager, EnumDialogValues.MESSAGE.toString());
    }
}
