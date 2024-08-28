package com.ericsson.cifwk.diagmon.util.common;

public class StringExtractor implements IAttributeHandler
{
   public Object extract( org.omg.CORBA.NameValuePair attribVal[] )
   {
      return attribVal[0].value.extract_string();
   }

    public Object extract( String attribs[] )
    {
	return attribs[0];
    }
}
