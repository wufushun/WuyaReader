package com.wuya.reader.callback;

/**
 * Created by Wufushun on 2017/10/28.
 */

public interface SearchCallback {
    /**
     * 进度
     * @param progress
     */
    public void jumpTo(int progress);

    /**
     * 搜索内容
     * @param searchContent
     */
    public void search(String searchContent);
}
