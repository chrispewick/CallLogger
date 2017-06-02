package com.pewick.calllogger.activity;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pewick.calllogger.R;
import com.pewick.calllogger.adapters.ListPagerAdapter;
import com.pewick.calllogger.fragments.HistoryFragment;
import com.pewick.calllogger.fragments.NumbersFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private ViewPager pager;
    private ListPagerAdapter pagerAdapter;

    private LinearLayout navigationLayout;
    private TextView numbersListButton;
    private TextView historyListButton;

    private LinearLayout searchLayout;
    private EditText searchField;
    private ImageView searchIcon;
    private ImageView clearSearchIcon;
    private InputMethodManager inputMethodManager;

    private ImageView optionsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        this.handlePermissions();
        this.setUpNavigationBar();
        this.setUpViewPager();
        this.setUpSearch();
//        this.setUpViewPager();
    }

    private void setUpOptionsMenu(){
        this.optionsIcon = (ImageView)findViewById(R.id.options_icon);

        optionsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Options Icon clicked");

                //Show a different menu, depending on which page is displayed
            }
        });
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    private void setUpSearch(){
        searchLayout = (LinearLayout)findViewById(R.id.search_layout);
        searchField = (EditText)findViewById(R.id.search_field);
        searchIcon = (ImageView)findViewById(R.id.search_icon);
        clearSearchIcon = (ImageView)findViewById(R.id.clear_search_icon);

        searchLayout.setVisibility(View.GONE);

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(searchLayout.getVisibility() == View.GONE){
                    //Start search
                    startSearch();
                } else{
                    //End search
                    endSearch();
                }
            }
        });

        searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(searchField,InputMethodManager.SHOW_IMPLICIT);
//                    inputMethodManager.showSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                } else {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    inputMethodManager.hideSoftInput(searchField,InputMethodManager.SHOW_IMPLICIT);
                    if(getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                }
            }
        });

        clearSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchField.setText("");
            }
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //NOTE: The application crashes in onStart if this is not declared after clearSearchIcon.setOnClickListener
                Log.i(TAG, "Search, afterTextChanged");
                String text = searchField.getText().toString().toLowerCase();

                //Now, make a call to the appropriate fragment, passing the text to filter the list
                if(pager.getCurrentItem() == 0){
                    ((NumbersFragment)((ListPagerAdapter) pager.getAdapter()).getCurrentFragment()).filterList(text);
                } else {
                    ((HistoryFragment)((ListPagerAdapter) pager.getAdapter()).getCurrentFragment()).filterList(text);
                }
            }
        });
    }

    public void closeKeyboard(){
        //Used by fragments when user begins to scroll after searching
        if(getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void startSearch(){
        searchLayout.setVisibility(View.VISIBLE);
        navigationLayout.setVisibility(View.GONE);
        searchField.requestFocus();
        inputMethodManager.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
    }

    private void endSearch(){
        navigationLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
        searchField.setText("");
        searchField.clearFocus();
        if(getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void setUpViewPager(){

        pager = (ViewPager)findViewById(R.id.view_pager);
        pagerAdapter = new ListPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(0);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(pager.getCurrentItem() == 0){
                    //Then on Numbers page
                    numbersListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                    historyListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white_transparent, null));
                } else{
                    //Then on History page
                    numbersListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white_transparent, null));
                    historyListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                }

                if(searchLayout.getVisibility() == View.VISIBLE){
                    endSearch();
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


    }

    @Override
    public void onBackPressed() {
        if(searchLayout.getVisibility() == View.VISIBLE){
            endSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void handlePermissions(){
        boolean state = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean processOutgoing = ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED;
        boolean readContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        ArrayList<String> permissionRequests = new ArrayList<>();
        if(!state){
            permissionRequests.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(!processOutgoing){
            permissionRequests.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        }
        if(!readContacts){
            permissionRequests.add(Manifest.permission.READ_CONTACTS);
        }

        if(!permissionRequests.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionRequests.toArray(new String[permissionRequests.size()]),
                    123);
        }
    }

    private void setUpNavigationBar(){
//        final TextView numbersListButton = (TextView) findViewById(R.id.numbers_list_button);
//        final TextView historyListButton = (TextView) findViewById(R.id.history_list_button);

        navigationLayout = (LinearLayout)findViewById(R.id.navigation_layout);
        numbersListButton = (TextView) findViewById(R.id.numbers_list_button);
        historyListButton = (TextView) findViewById(R.id.history_list_button);

        numbersListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));

        numbersListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO:Handle color change
                pager.setCurrentItem(0);
                numbersListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                historyListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white_transparent, null));
            }
        });

        historyListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: Handle color change
                pager.setCurrentItem(1);
                historyListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                numbersListButton.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white_transparent, null));
            }
        });
    }
}
