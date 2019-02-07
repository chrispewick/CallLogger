package com.pewick.calllogger.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

import com.pewick.calllogger.R
import com.pewick.calllogger.adapters.ListPagerAdapter
import com.pewick.calllogger.fragments.HistoryFragment
import com.pewick.calllogger.fragments.NumbersFragment

import java.util.ArrayList
import java.util.HashMap

/**
 * The base activity for the application. Contains the ViewPager holding the two main fragments.
 * Handles the top bar behaviors throughout the application, such as search, filter, and switching
 * between the NumbersFragment and the HistoryFragment.
 */
class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName

    private var pager: ViewPager? = null
    private var navigationLayout: LinearLayout? = null
    private var numbersListButton: TextView? = null
    private var historyListButton: TextView? = null
    private var optionsIcon: ImageView? = null
    private var searchLayout: LinearLayout? = null
    private var searchField: EditText? = null
    private var inputMethodManager: InputMethodManager? = null
    private var numbersPopUp: PopupMenu? = null
    private var historyPopUp: PopupMenu? = null

    private var contactsNumberNamesMap: HashMap<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        this.handlePermissions()
        this.setUpNavigationBar()
        this.setUpViewPager()
        this.setUpSearch()
        this.setUpOptionsMenu()
        this.getAllContactNames()
    }

    private fun setUpOptionsMenu() {
        this.optionsIcon = findViewById<View>(R.id.options_icon) as ImageView
        this.setUpHistoryPopUp()
        this.setUpNumbersPopUp()

        optionsIcon!!.setOnClickListener {
            Log.i(TAG, "Options Icon clicked")
            //Show a different menu, depending on which page is displayed
            if (pager!!.currentItem == 0) {
                //On number fragment
                numbersPopUp!!.show()
            } else {
                //On history fragment
                historyPopUp!!.show()
            }
        }
    }

    private fun setUpHistoryPopUp() {
        historyPopUp = PopupMenu(this, optionsIcon)
        val inflater = historyPopUp!!.menuInflater
        inflater.inflate(R.menu.options_menu_history, historyPopUp!!.menu)
        historyPopUp!!.menu.getItem(0).isChecked = true
        historyPopUp!!.menu.getItem(1).isChecked = true
        historyPopUp!!.menu.getItem(2).isChecked = true
        historyPopUp!!.menu.getItem(3).isChecked = true
        historyPopUp!!.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.contacts ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
                R.id.non_contacts ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
                R.id.incoming ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
                R.id.outgoing ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
            }

            val text = searchField!!.text.toString()
            val contactsF = historyPopUp!!.menu.getItem(0).isChecked
            val noncontactsF = historyPopUp!!.menu.getItem(1).isChecked
            val incomingF = historyPopUp!!.menu.getItem(2).isChecked
            val outgoingF = historyPopUp!!.menu.getItem(3).isChecked
            ((pager!!.adapter as ListPagerAdapter).currentFragment as HistoryFragment)
                    .filterList(text, contactsF, noncontactsF, incomingF, outgoingF)

            // Keep the popup menu open
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            menuItem.actionView = View(applicationContext)
            menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return false
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    return false
                }
            })
            false
        }
    }

    private fun setUpNumbersPopUp() {
        numbersPopUp = PopupMenu(this, optionsIcon)
        val inflater = numbersPopUp!!.menuInflater
        inflater.inflate(R.menu.options_menu_numbers, numbersPopUp!!.menu)
        numbersPopUp!!.menu.getItem(0).isChecked = true
        numbersPopUp!!.menu.getItem(1).isChecked = true
        numbersPopUp!!.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.contacts ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
                R.id.non_contacts ->
                    //Filter by contacts
                    menuItem.isChecked = !menuItem.isChecked
            }

            val text = searchField!!.text.toString()
            val contactsF = numbersPopUp!!.menu.getItem(0).isChecked
            val noncontactsF = numbersPopUp!!.menu.getItem(1).isChecked
            ((pager!!.adapter as ListPagerAdapter).currentFragment as NumbersFragment)
                    .filterList(text, contactsF, noncontactsF)

            // Keep the popup menu open
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            menuItem.actionView = View(applicationContext)
            menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return false
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    return false
                }
            })
            false
        }
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
    }

    private fun setUpSearch() {
        searchLayout = findViewById<View>(R.id.search_layout) as LinearLayout
        searchField = findViewById<View>(R.id.search_field) as EditText
        val searchIcon = findViewById<View>(R.id.search_icon) as ImageView
        val clearSearchIcon = findViewById<View>(R.id.clear_search_icon) as ImageView

        searchLayout!!.visibility = View.GONE

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        searchIcon.setOnClickListener {
            if (searchLayout!!.visibility == View.GONE) {
                //Start search
                startSearch()
            } else {
                //End search
                endSearch()
            }
        }

        searchField!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT)
                //                    inputMethodManager.showSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } else {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                //                    inputMethodManager.hideSoftInput(searchField,InputMethodManager.SHOW_IMPLICIT);
                if (currentFocus != null) {
                    inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }
            }
        }

        clearSearchIcon.setOnClickListener {
            if (searchField!!.text.toString() == "") {
                endSearch()
            } else {
                searchField!!.setText("")
            }
        }

        searchField!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                //NOTE: The application crashes in onStart if this is not declared after clearSearchIcon.setOnClickListener
                Log.i(TAG, "Search, afterTextChanged")
                val text = searchField!!.text.toString().toLowerCase()

                //Now, make a call to the appropriate fragment, passing the text to filter the list
                if (pager!!.currentItem == 0) {
                    val contactsF = numbersPopUp!!.menu.getItem(0).isChecked
                    val noncontactsF = numbersPopUp!!.menu.getItem(1).isChecked
                    try {
                        //For some reason, this is called before the fragment is initialized,
                        // after the app has been in the background for some time.
                        ((pager!!.adapter as ListPagerAdapter).currentFragment as NumbersFragment)
                                .filterList(text, contactsF, noncontactsF)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }

                } else {
                    //                    ((HistoryFragment)((ListPagerAdapter) pager.getAdapter()).getCurrentFragment()).filterList(text);

                    val contactsF = historyPopUp!!.menu.getItem(0).isChecked
                    val noncontactsF = historyPopUp!!.menu.getItem(1).isChecked
                    val incomingF = historyPopUp!!.menu.getItem(2).isChecked
                    val outgoingF = historyPopUp!!.menu.getItem(3).isChecked
                    try {
                        ((pager!!.adapter as ListPagerAdapter).currentFragment as HistoryFragment)
                                .filterList(text, contactsF, noncontactsF, incomingF, outgoingF)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }

                }
            }
        })
    }

    fun closeKeyboard() {
        //Used by fragments when user begins to scroll after searching
        if (currentFocus != null) {
            inputMethodManager!!.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    private fun startSearch() {
        searchLayout!!.visibility = View.VISIBLE
        navigationLayout!!.visibility = View.GONE
        searchField!!.requestFocus()
        inputMethodManager!!.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun endSearch() {
        navigationLayout!!.visibility = View.VISIBLE
        searchLayout!!.visibility = View.GONE
        searchField!!.setText("")
        searchField!!.clearFocus()
        if (currentFocus != null) {
            inputMethodManager!!.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    private fun setUpViewPager() {
        pager = findViewById<View>(R.id.view_pager) as ViewPager
        val pagerAdapter = ListPagerAdapter(supportFragmentManager)
        pager!!.adapter = pagerAdapter
        pager!!.currentItem = 0
        pager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (pager!!.currentItem == 0) {
                    //Then on Numbers page
                    numbersListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                    historyListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white_transparent, null))
                } else {
                    //Then on History page
                    numbersListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white_transparent, null))
                    historyListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                }

                if (searchLayout!!.visibility == View.VISIBLE) {
                    endSearch()
                }
            }

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    override fun onBackPressed() {
        if (searchLayout!!.visibility == View.VISIBLE) {
            endSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun handlePermissions() {
        val state = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val processOutgoing = ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED
        val readContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

        val permissionRequests = ArrayList<String>()
        if (!state) {
            permissionRequests.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (!processOutgoing) {
            permissionRequests.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
        }
        if (!readContacts) {
            permissionRequests.add(Manifest.permission.READ_CONTACTS)
        }

        if (!permissionRequests.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionRequests.toTypedArray(),
                    123)
        }
    }

    private fun setUpNavigationBar() {
        navigationLayout = findViewById<View>(R.id.navigation_layout) as LinearLayout
        numbersListButton = findViewById<View>(R.id.numbers_list_button) as TextView
        historyListButton = findViewById<View>(R.id.history_list_button) as TextView

        numbersListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))

        numbersListButton!!.setOnClickListener {
            pager!!.currentItem = 0
            numbersListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            historyListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white_transparent, null))
        }

        historyListButton!!.setOnClickListener {
            pager!!.currentItem = 1
            historyListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            numbersListButton!!.setTextColor(ResourcesCompat.getColor(resources, R.color.white_transparent, null))
        }
    }

    fun refreshLists() {
        ((pager!!.adapter as ListPagerAdapter).getItem(0) as NumbersFragment).refreshList()
        ((pager!!.adapter as ListPagerAdapter).getItem(1) as HistoryFragment).refreshList()
    }

    private fun getAllContactNames() {
        contactsNumberNamesMap = HashMap()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

            val cursor = contentResolver.query(uri, projection, null, null, null)

            try {
                val indexName = cursor!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val indexNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                if (cursor.moveToFirst()) {
                    do {
                        val name = cursor.getString(indexName)
                        val number = removeLeadingOne(cursor.getString(indexNumber)) ?: ""

                        Log.i(TAG, "Contact name: $name")
                        Log.i(TAG, "Contact number: " + number)

                        contactsNumberNamesMap!![number] = name
                    } while (cursor.moveToNext())
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
    }

    private fun removeLeadingOne(number: String?): String? {
        return number?.substring(2)
    }

    fun getContactName(number: String): String? {
        return contactsNumberNamesMap!![number]
    }
}
