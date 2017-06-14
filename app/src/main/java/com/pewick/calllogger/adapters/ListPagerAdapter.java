package com.pewick.calllogger.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.pewick.calllogger.fragments.NumbersFragment;
import com.pewick.calllogger.fragments.HistoryFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Chris on 5/17/2017.
 */
public class ListPagerAdapter extends FragmentPagerAdapter {

    private final String TAG = getClass().getSimpleName();

    private Map<Integer, String> fragmentTags;
    private Fragment currentFragment;

    public ListPagerAdapter(FragmentManager fm) {
        super(fm);
        fragmentTags = new HashMap<>();
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
//        return Fragment.instantiate(, AFragment.class.getName(), null);

        if(position == 0){
            NumbersFragment numbersFragment = new NumbersFragment();
            String tag = numbersFragment.getTag();
            fragmentTags.put(0,tag);
            return numbersFragment;
        } else{
            HistoryFragment historyFragment = new HistoryFragment();
            String tag = historyFragment.getTag();
            fragmentTags.put(1, tag);
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
