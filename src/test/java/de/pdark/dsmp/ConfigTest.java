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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class ConfigTest
{
    private Config config;

    @Test
    public void testGetProperties () throws Exception
    {
        config = new Config("NoSuchFile");
        
        try
        {
            config.reload ();
            fail ("No exceptionw as thrown");
        }
        catch (Error e)
        {
            // success
        }
    }

    @Test
    public void testGetPort () throws Exception
    {
        int port = config.getPort();
        assertEquals (1234, port);
    }

    @Test
    public void testGetPort2 () throws Exception
    {
        config = new Config("src/test/resources");
        System.setProperty("dsmp.conf", "illegal.conf");
        try
        {
            config.reload ();
            config.getPort();
            fail ("No exceptionw as thrown");
        }
        catch (RuntimeException e)
        {
            // success
        }
    }

    @Test
    public void testGetCacheDirectory () throws Exception
    {
        assertEquals ("cache-dir", config.getCacheDirectory().getName());
    }

    @Test
    public void testGetPatchesDirectory () throws Exception
    {
        assertEquals ("patches-dir", config.getPatchesDirectory().getName());
    }

    @Test
    public void testGetProxyUsername () throws Exception
    {
        assertEquals ("xxx", config.getProxyUsername());
    }

    @Test
    public void testGetProxyPassword () throws Exception
    {
        assertEquals ("yyy", config.getProxyPassword());
    }

    @Test
    public void testGetProxyHost () throws Exception
    {
        assertEquals ("proxy.server", config.getProxyHost());
    }

    @Test
    public void testGetProxyPort () throws Exception
    {
        assertEquals (234, config.getProxyPort());
    }

    @Test
    public void testGetNoProxy () throws Exception
    {
        String[] s = config.getNoProxy();
        assertEquals("a", s[0]);
        assertEquals("b", s[1]);
        assertEquals("c", s[2]);
    }

    @Test
    public void testNoProxy1 () throws Exception
    {
        assertFalse (config.useProxy(new URL ("http://b/x/y/z")));
    }

    @Test
    public void testNoProxy2 () throws Exception
    {
        assertTrue (config.useProxy(new URL ("http://some.doma.in/x/y/z")));
    }

    @Test
    public void testRedirect () throws Exception
    {
        assertEquals ("http://maven.sateh.com/maven2/org/apache/something",
                config.getMirror(new URL ("http://repo1.maven.org/maven2/org/apache/something")).toString());
    }

    @Test
    public void testRedirect2 () throws Exception
    {
        assertEquals ("http://maven.sateh.com/maven2/org/apache/something",
                config.getMirror(new URL ("http://maven.sateh.com/repository/org/apache/something")).toString());
    }

    @Test
    public void testRedirect3 () throws Exception
    {
        assertEquals ("http://maven.sateh.com/maven2/aopalliance/x",
                config.getMirror(new URL ("http://m2.safehaus.org/org/aopalliance/x")).toString());
    }

    @Test
    public void testRedirect4 () throws Exception
    {
        assertEquals ("http://maven.sateh.com/maven2/org/x",
                config.getMirror(new URL ("http://m2.safehaus.org/org/x")).toString());
    }

    @Test
    public void testIsAllowed () throws Exception
    {
        assertTrue (config.isAllowed(new URL ("http://maven.sateh.com/maven2/org/x")));
    }

    @Test
    public void testIsAllowed1 () throws Exception
    {
        assertTrue (config.isAllowed(new URL ("http://maven.sateh.com/maven2/org/x")));
    }

    @Test
    public void testIsAllowed2 () throws Exception
    {
        assertTrue (config.isAllowed(new URL ("http://people.apache.org/maven-snapshot-repository/org/apache/maven/plugins/maven-deploy-plugin/2.3-SNAPSHOT/")));
    }

    @Test
    public void testIsAllowed3 () throws Exception
    {
        assertFalse (config.isAllowed(new URL ("http://people.apache.org/maven-snapshot-repository/org/apache/maven/plugins/maven-source-plugin/")));
    }
    
    private File oldBaseDir;
    
    @Before
    public void setUp () throws Exception
    {
        config = new Config("src/test/resources");
        System.setProperty("dsmp.conf", "dsmp-test.conf");
        config.reload ();

    }
    @After public void tearDown () throws Exception
    {
        System.setProperty("dsmp.conf", "dsmp.conf");
    }
}
