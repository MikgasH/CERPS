package com.example.cerps.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonLibraryTest {

    @Test
    void testGetVersion() {
        assertEquals("0.0.1-SNAPSHOT", CommonLibrary.getVersion());
    }
}
