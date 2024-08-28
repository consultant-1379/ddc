package com.ericsson.cifwk.diagmon.util.common;

public interface IAttributeHandler
{
   public Object extract( org.omg.CORBA.NameValuePair attribVal[] );
   public Object extract( String attribs[] );
}
