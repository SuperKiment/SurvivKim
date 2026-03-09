package com.superkiment.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    void getLastTerm_simpleClassName_returnsClassName() {
        assertEquals("EntitiesManager",
                StringUtils.GetLastTerm("com.superkiment.common.entities.EntitiesManager"));
    }

    @Test
    void getLastTerm_singleWord_returnsSameWord() {
        assertEquals("Foo", StringUtils.GetLastTerm("Foo"));
    }

    @Test
    void getLastTerm_twoTerms_returnsLastOne() {
        assertEquals("Bar", StringUtils.GetLastTerm("Foo.Bar"));
    }

    @Test
    void getLastTerm_deeplyNested_returnsLast() {
        assertEquals("Z", StringUtils.GetLastTerm("a.b.c.d.Z"));
    }
}
