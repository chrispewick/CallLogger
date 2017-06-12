package com.pewick.calllogger.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.models.CallItem;
import com.pewick.calllogger.views.EditTextPlus;

/**
 * Created by Chris on 5/25/2017.
 */
public class CallDialogFragment extends DialogFragment {

    private final String TAG = getClass().getSimpleName();

    private CallItem callItem;

    private AlertDialog dialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NewCustomDialog);
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_call_dialog, null);

        Bundle args = getArguments();
        this.callItem = (CallItem) args.get("call_item");

        view.requestFocus();
        builder.setTitle(null).setView(view);
        dialog = builder.create();

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void configureDialogTextContent(){

    }


}
