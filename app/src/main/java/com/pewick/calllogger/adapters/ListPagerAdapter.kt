package com.pewick.calllogger.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

import android.view.ViewGroup

import com.pewick.calllogger.fragments.NumbersFragment
import com.pewick.calllogger.fragments.HistoryFragment

/**
 * Custom FragmentPagerAdapter for the view pager containing the NumbersFragment and the
 * HistoryFragment within the main activity.
 */
class ListPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val TAG = javaClass.simpleName
    var currentFragment: Fragment? = null
        private set
    private val numbersFragment: NumbersFragment
    private val historyFragment: HistoryFragment

    init {
        numbersFragment = NumbersFragment()
        historyFragment = HistoryFragment()
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            numbersFragment
        } else {
            historyFragment
        }
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        if (currentFragment !== `object`) {
            currentFragment = `object` as Fragment
        }
        super.setPrimaryItem(container, position, `object`)
    }
}
