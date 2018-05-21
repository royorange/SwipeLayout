package com.royorange.swipeoptionlayout.adapter;

/**
 * Created by Roy on 2018/4/26.
 */

public class FlowViewModel {
    private String title;
    private String summary;
    private boolean isLike;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isLike() {
        return isLike;
    }

    public void setLike(boolean like) {
        isLike = like;
    }
}
