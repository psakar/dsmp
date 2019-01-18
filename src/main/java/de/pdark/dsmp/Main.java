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

/**
 * The main class.
 * 
 * @author digulla
 *
 */
public class Main
{
    public static final Logger log = LogManager.getLogger(Main.class);
    public static final String VERSION = "1.1";
    
    public static void main (String[] args)
    {
        try
        {
            if (args.length == 0)
                throw new IllegalArgumentException ("Usage: $0 home-directory");
            
            Config config = new Config(args[0]);
            config.reload();
            
            Server server = new Server (config);
            log.info("Dead Stupid Maven Proxy "+VERSION+" is ready.");
            log.debug ("Debugging is enabled.");
            server.handleRequests ();
        }
        catch (Exception e)
        {
            log.error ("Fatal Error in proxy", e);
        }
        LogManager.shutdown();
    }


}
