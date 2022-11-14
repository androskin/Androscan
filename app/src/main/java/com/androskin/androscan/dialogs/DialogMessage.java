package com.androskin.androscan.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.androskin.androscan.enums.EnumDialogValues;

public class DialogMessage  extends DialogFragment {
    private String title;
    private String message;

    private DialogMessage() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    public static void showMessage(FragmentManager fragmentManager, String title, String message) {
        DialogMessage myDialogFragment = new DialogMessage();
        myDialogFragment.title = title;
        myDialogFragment.message = message;
        myDialogFragment.show(fragmentManager, EnumDialogValues.MESSAGE.toString());
    }
}