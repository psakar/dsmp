/*
 * Copyright 2002-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.pdark.dsmp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Wait for connections from somewhere and pass them on to <code>RequestHandler</code>
 * for processing.
 * 
 * @author digulla
 *
 */
public class Server
{
    public static final Logger log = LogManager.getLogger(Server.class);
    
    private final int port;
    private final Config config;
    private final ServerSocket socket;
    
    public Server (Config config) throws IOException
    {
        port = config.getPort();
        this.config = config;

        log.info("Opening connection on port "+port);
        socket = new ServerSocket (port);
    }

    private static volatile boolean run = true;
    
    public void terminateAll ()
    {
        // TODO Implement a way to gracefully stop the proxy
        run = false;
    }
    
    public void handleRequests ()
    {
        while (run)
        {
            // config.reload ();
            try {
                new RequestHandler(socket.accept(), config).start();
            } catch (IOException e) {
                log.error("Error acception connection from client - " + e.getMessage(), e);
            }
        }
        
        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            log.error("Error closing server socket - " + e.getMessage(), e);
        }
    }
    
}
