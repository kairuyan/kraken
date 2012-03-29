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
 * A client-visible session object, which is tied to the lifecycle of
 * a {@link Router}.
 * 
 * @see Router
 * @see SessionManager
 * 
 **/
public interface SessionPrx extends Ice.ObjectPrx
{
    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     **/
    public void destroy();

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __ctx The Context map to send with the invocation.
     **/
    public void destroy(java.util.Map<String, String> __ctx);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy();

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __ctx The Context map to send with the invocation.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy(java.util.Map<String, String> __ctx);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy(Ice.Callback __cb);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy(java.util.Map<String, String> __ctx, Ice.Callback __cb);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy(Callback_Session_destroy __cb);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __ctx The Context map to send with the invocation.
     * @param __cb The asynchronous callback object.
     * @return The asynchronous result object.
     **/
    public Ice.AsyncResult begin_destroy(java.util.Map<String, String> __ctx, Callback_Session_destroy __cb);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __result The asynchronous result object.
     **/
    public void end_destroy(Ice.AsyncResult __result);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __cb The callback object for the operation.
     **/
    public boolean destroy_async(AMI_Session_destroy __cb);

    /**
     * Destroy the session. This is called automatically when the
     * {@link Router} is destroyed.
     * 
     * @param __cb The callback object for the operation.
     * @param __ctx The Context map to send with the invocation.
     **/
    public boolean destroy_async(AMI_Session_destroy __cb, java.util.Map<String, String> __ctx);
}