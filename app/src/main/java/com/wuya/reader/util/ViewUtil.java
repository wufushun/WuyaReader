package com.wuya.reader.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wuya.reader.R;
import com.wuya.reader.callback.SearchCallback;

/**
 * Created by Wufushun on 2017/10/28.
 */

public class ViewUtil {
    /**
     * 查找定位内容
     * @param context
     * @return
     */
    public static PopupWindow getSearchView(Context context, final SearchCallback callBack) {

        View view = LayoutInflater.from(context).inflate(R.layout.search_bar_view, null);

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //搜索框
        final EditText searchEditText = (EditText) view. findViewById(R.id.searchEditText);
        ImageButton searchButton = (ImageButton) view. findViewById(R.id.searchButton);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callBack.search(searchEditText.getText().toString());
            }
        };
        searchButton.setOnClickListener(listener);

        //进度条
        SeekBar progressSeekBar = (SeekBar) view.findViewById(R.id.progressSeekBar);
        final TextView progressTextView = (TextView) view.findViewById(R.id.progressTextView);

        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressTextView.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                callBack.jumpTo(seekBar.getProgress());
            }
        });

        PopupWindow popupWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);

        return popupWindow;
    }


    /**
     * 把popupWindow显示在view的上方
     * @param mContex
     * @param view
     * @param popupWindow
     */
    public static void showPopUp(Context mContex, View view, PopupWindow popupWindow) {

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        popupWindow.showAtLocation(view, Gravity.TOP, 0, location[1] - (int)mContex.getResources().getDimension(R.dimen.activity_wuya_menu_height));

    }

}
