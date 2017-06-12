package com.pewick.calllogger.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

import com.pewick.calllogger.R;

/**
 * Custom edit text used by the application.
 */
public class EditTextPlus extends EditText {

    /**
     * Constructor.
     * @param context the view's context.
     */
    public EditTextPlus(Context context) {
        super(context);
    }

    /**
     * Constructor.
     * @param context the view's context.
     * @param attrs additional attributes of the view.
     */
    public EditTextPlus(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor.
     * @param context the view's context.
     * @param attrs additional attributes of the view.
     * @param defStyle additional style attributes of the view.
     */
    public EditTextPlus(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        setCustomFont(context, attrs);
    }

    /**
     * Sets the custom font.
     * @param context the view's context.
     * @param attrs additional attributes of the view.
     */
    //TODO: Can be used later to add custom font
//    private void setCustomFont(Context context, AttributeSet attrs) {
//        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextViewPlus);
//        String customFont = typedArray.getString(R.styleable.TextViewPlus_customFont);
//        setCustomFont(context, customFont);
//        typedArray.recycle();
//    }

    /**
     * Sets the custom font.
     * @param context the view's context.
     * @param asset the custom font.
     * @return true if completed without exception, false otherwise.
     */
    private boolean setCustomFont(Context context, String asset) {
        Typeface customTypeface = null;
        try {
            customTypeface = TypeFaceDictionary.get(context, asset);
        }
        catch (Exception ex) {
            return false;
        }

        setTypeface(customTypeface);
        return true;
    }

    /**
     * Detects a focus change to or from the view.
     * @param focused True if the View has focus; false otherwise.
     * @param direction The direction focus has moved when requestFocus() is called to give this
     *                  view focus.
     * @param previouslyFocusedRect  The rectangle, in this view's coordinate system, of the
     *                               previously focused view.
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (keyboardListener != null)
            keyboardListener.onStateChanged(this,true);
    }

    /**
     * Handle a key event before it is processed by any input method associated with the view
     * hierarchy.
     * @param keyCode  The value in event.getKeyCode().
     * @param event Description of the key event.
     * @return True if event handled, false otherwise..
     */
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (keyboardListener != null)
                keyboardListener.onStateChanged(this,false);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    KeyboardListener keyboardListener;

    /**
     * Sets the keyboard listener.
     * @param listener the listener to set.
     */
    public void setOnKeyboardListener(KeyboardListener listener) {
        this.keyboardListener = listener;
    }

    /**
     * Interface for the keyboard listener.
     */
    public interface KeyboardListener {
        void onStateChanged(EditTextPlus editTextPlus, boolean showing);
    }
}