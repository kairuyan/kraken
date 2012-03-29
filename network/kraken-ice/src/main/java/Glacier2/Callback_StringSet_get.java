// **********************************************************************
//
// Copyright (c) 2003-2010 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.4.1

package Glacier2;

// <auto-generated>
//
// Generated from file `Session.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>


/**
 * Returns a sequence of strings describing the constraints in this
 * set.
 * 
 **/

public abstract class Callback_StringSet_get extends Ice.TwowayCallback
{
    public abstract void response(String[] __ret);

    public final void __completed(Ice.AsyncResult __result)
    {
        StringSetPrx __proxy = (StringSetPrx)__result.getProxy();
        String[] __ret = null;
        try
        {
            __ret = __proxy.end_get(__result);
        }
        catch(Ice.LocalException __ex)
        {
            exception(__ex);
            return;
        }
        response(__ret);
    }
}