/**
 * 
 */
package com.ericsson.cifwk.diagmon.e2e;

/**
 * @author eswavin
 * 
 */
public class Utility {

    public static String trimQuotes(final String stringToTrim) {
        String result = null;
        if (stringIsValid(stringToTrim)) {
            result = stringToTrim.replaceAll("(^['|\"])|(['|\"]$)", "");
        }
        return result;
    }

    private static boolean stringIsValid(final String inputString) {
        return inputString != null && inputString.length() > 0;
    }
}
