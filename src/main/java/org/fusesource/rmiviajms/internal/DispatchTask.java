/**
 * ====================================================================
 * Copyright (C) 2008 Progress Software, Inc. All rights reserved.
 * http://fusesource.com
 * ====================================================================
 * The software in this package is published under the terms of 
 * the AGPL license a copy of which has been included with this 
 * distribution in the license.txt file.
 * ====================================================================
 */
package org.fusesource.rmiviajms.internal;

import java.rmi.NoSuchObjectException;

import javax.jms.ObjectMessage;
import javax.jms.JMSException;

/**
 * This task demarshalls an received message, invokes the exported object and
 * sends the response via the sender thread.
 */
final class DispatchTask implements Runnable {
    private final ObjectMessage msg;
    private final boolean oneway;
    private final JMSRemoteSystem remoteSystem;

    public DispatchTask(JMSRemoteSystem remoteSystem, ObjectMessage msg, boolean oneway) {
        this.remoteSystem = remoteSystem;
        this.msg = msg;
        this.oneway = oneway;
        //System.out.println("Created DispatchTask" + msg);
    }

    public void run() {
        try {
            //System.out.println("Executing DispatchTask" + msg);
            long oid = msg.getLongProperty("object");
            Skeleton exportedObject = remoteSystem.exportedSkeletonsById.get(oid);
            Response response = null;
            Request request = null;
            if (exportedObject == null) {
                try {
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    request = (Request) (msg).getObject();
                    response = new Response(request.requestId, null, new NoSuchObjectException("" + oid));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Thread.currentThread().setContextClassLoader(exportedObject.getClass().getClassLoader());
                request = (Request) (msg).getObject();
                response = exportedObject.invoke(request);
            }
            
            if (!oneway) {
                remoteSystem.sendResponse(msg, request, response);
            } else {
                if (response.exception != null) {
                    response.exception.printStackTrace();
                }
            }
        }
        catch (JMSException e) {
            // The request message must not have been properly created.. ignore for now.
            e.printStackTrace();
        }
        catch (Throwable thrown)
        {
            thrown.printStackTrace();
        }
    }
}