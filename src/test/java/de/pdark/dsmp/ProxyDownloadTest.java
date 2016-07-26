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

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;


public class ProxyDownloadTest
{

    @Test
    public void testMkdirs () throws Exception
    {
        URL url = new URL ("http://repo1.maven.org/maven2/org/apache/commons/commons-parent/1/commons-parent-1.pom");
        File f = RequestHandler.getCacheFile(url);
        File dir = Config.getCacheDirectory();
        String expected = dir.getAbsolutePath().replace(File.separatorChar, '/')+"/repo1.maven.org/maven2/org/apache/commons/commons-parent/1/commons-parent-1.pom";
        String s = f.getAbsolutePath().replace(File.separatorChar, '/');
        assertEquals(expected, s);
    }

    @Test
    public void testDownload () throws Exception
    {
        URL url = new URL ("http://repo1.maven.org/maven2/org/apache/commons/commons-parent/1/commons-parent-1.pom");
        File f = Files.createTempFile("zz", "commons-parent-1.pom").toFile();
        ProxyDownload d = new ProxyDownload (url, f);
        d.download();
        
        assertEquals (7616, f.length());
    }
    
    @Before
    public void setUp () throws Exception
    {
        Config.setBaseDir("tmp");
        File from = new File ("dsmp-test.conf");
        if (!from.exists())
        {
            File orig = new File ("src/test/resources/dsmp-test.conf");
            throw new RuntimeException ("Please copy "+orig.getAbsolutePath()+" to "+from.getAbsolutePath()+" and adjust the proxy settings for your site to run this test.");
        }
        FileUtils.copyFile(from, new File ("tmp/dsmp.conf"));
        Config.reload ();
    }

}
