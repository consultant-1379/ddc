package com.ericsson.cifwk.diagmon.e2e;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilityTest {

    @Test
    public void trimQuotesNullNegative() {
        assertEquals(null, Utility.trimQuotes(null));
    }

    @Test
    public void trimSingleQuotesEmptyStringsWithQuotesNegative() {
        assertEquals("", Utility.trimQuotes("''"));
    }

    @Test
    public void trimDoubleQuotesEmptyStringsWithQuotesNegative() {
        assertEquals("", Utility.trimQuotes("\"\""));
    }

    @Test
    public void trimQuotesEmptyStringsNegative() {
        assertEquals(null, Utility.trimQuotes(""));
    }

    @Test
    public void trimSingleQuotesToRemoveQuotesPostive() {
        assertEquals("Ericsson", Utility.trimQuotes("'Ericsson'"));
    }

    @Test
    public void trimSingleQuotesToRemoveQuotesPostiveWithApostrophe() {
        assertEquals("Ericsson's", Utility.trimQuotes("'Ericsson's'"));
    }

    @Test
    public void trimDoubleQuotesToRemoveQuotesPostive() {
        assertEquals("Ericsson", Utility.trimQuotes("\"Ericsson\""));
    }

}
