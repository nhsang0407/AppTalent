package com.shoplens.ai.model;

import java.io.Serializable;

/**
 * Represents a search keyword and the number of times it has been searched.
 */
public class SearchKeyword implements Serializable {
    private final String keyword;
    private final int count;

    public SearchKeyword(String keyword, int count) {
        this.keyword = keyword;
        this.count = count;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getCount() {
        return count;
    }
}
