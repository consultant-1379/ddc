package com.ericsson.cifwk.diagmon.util.common;

import org.omg.CORBA.Any;

public class EnumHelper
{
    public static String toString( org.omg.CORBA.Any valueAny, String enumValues[] )
    {
	return toString((int)valueAny.extract_long(), enumValues );
    }

    public static String toString( int value, String enumValues[] )
    {
	if ( value >= 0 && value < enumValues.length )
	    return (enumValues[value] + " (" + String.valueOf(value) + ")");
	else
	    return String.valueOf(value);
    }
}    
