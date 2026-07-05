package com.shoplens.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shoplens.ai.model.SearchKeyword;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Unit tests for Admin Insight data model and date key formatting.
 */
public class AdminInsightTest {

    @Test
    public void testSearchKeywordModel() {
        SearchKeyword keyword = new SearchKeyword("Sữa rửa mặt", 15);
        assertEquals("Sữa rửa mặt", keyword.getKeyword());
        assertEquals(15, keyword.getCount());
    }

    @Test
    public void testDateKeyFormatting() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateKey = df.format(new Date());
        assertNotNull(dateKey);
        assertEquals(10, dateKey.length());
        assertTrue(dateKey.matches("\\d{4}-\\d{2}-\\d{2}"));
    }
}
