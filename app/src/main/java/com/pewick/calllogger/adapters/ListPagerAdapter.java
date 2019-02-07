package com.pewick.calllogger.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.view.ViewGroup;

import com.pewick.calllogger.fragments.NumbersFragment;
import com.pewick.calllogger.fragments.HistoryFragment;

/**
 * Custom FragmentPagerAdapter for the view pager containing the NumbersFragment and the
 * HistoryFragment within the main activity.
 */
public class ListPagerAdapter extends FragmentPagerAdapter {
    private final String TAG = getClass().getSimpleName();
    private Fragment currentFragment;
    private NumbersFragment numbersFragment;
    private HistoryFragment historyFragment;

    public ListPagerAdapter(FragmentManager fm) {
        super(fm);
        numbersFragment = new NumbersFragment();
        historyFragment = new HistoryFragment();
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0){
            return numbersFragment;
        } else{
            return historyFragment;
        }
    }

    public Fragment getCurrentFragment(){
        return currentFragment;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            currentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }
}
