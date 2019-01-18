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

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Download a file via a proxy server and store it somewhere.
 * 
 * @author digulla
 *
 */
public class ProxyDownload
{
    public static final Logger log = LogManager.getLogger(ProxyDownload.class);
    private final URL url;
    private final File dest;
    private final Config config;

    /**
     * Download <code>url</code> to <code>dest</code>.
     * 
     * <p>If the directory to store <code>dest</code> doesn't exist,
     * it will be created.
     * 
     * @param url The resource to download
     * @param dest Where to store it.
     */
    public ProxyDownload (URL url, File dest, Config config)
    {
        this.url = url;
        this.dest = dest;
        this.config = config;
    }
    
    /**
     * Create the neccessary paths to store the destination file.
     * 
     * @throws IOException
     */
    public void mkdirs () throws IOException
    {
        File parent = dest.getParentFile();
        IOUtils.mkdirs (parent);
    }
    
    /**
     * Do the download.
     * 
     * @throws IOException
     * @throws DownloadFailed
     */
    public void download () throws IOException, DownloadFailed
    {
        if (!config.isAllowed(url))
        {
            throw new DownloadFailed (url, HttpStatus.SC_FORBIDDEN,"HTTP/1.1 " + HttpStatus.SC_FORBIDDEN + " Download denied by rule in DSMP config");
        }
        
        // If there is a status file in the cache, return it instead of trying it again
        // As usual with caches, this one is a key area which will always cause
        // trouble.
        // TODO There should be a simple way to get rid of the cached statuses
        // TODO Maybe retry a download after a certain time?
        File statusFile = new File (dest.getAbsolutePath()+".status");
        if (statusFile.exists())
        {
            String status = new String(Files.readAllBytes(statusFile.toPath()));
            throw new DownloadFailed (url, HttpStatus.SC_NOT_FOUND, status);
        }
        
        mkdirs();


        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(5 * 60, TimeUnit.SECONDS);
        connectionManager.setValidateAfterInactivity(60*1000);
        connectionManager.setDefaultMaxPerRoute(1);

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        CredentialsProvider credentialsProvider = new SystemDefaultCredentialsProvider(); // or BasicCredentialsProvider ?
        String msgProxyInfo = "";
        if (config.useProxy(url))
        {
            Credentials credentials = new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword());
            AuthScope scope = new AuthScope(config.getProxyHost(), config.getProxyPort(), AuthScope.ANY_REALM);

            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(scope, credentials);

            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            requestConfigBuilder.setProxy(proxy);

            msgProxyInfo = "via proxy ";

        }

//        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        CloseableHttpClient client = HttpClientBuilder.create()
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setConnectionManager(connectionManager)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .setRedirectStrategy(new LaxRedirectStrategy())
//                .setRoutePlanner(routePlanner)
                .build();

        log.info("Downloading " + msgProxyInfo + "to "+dest.getAbsolutePath());

        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpResponse response = client.execute(httpGet))
        {
            int status = response.getStatusLine().getStatusCode();

            log.info ("Download status: " + status);
            log.info ("Content: " + valueOf(response.getFirstHeader("Content-Length")) + " bytes; "
                    + valueOf(response.getFirstHeader("Content-Type")));
            
            if (status != HttpStatus.SC_OK)
            {
                // Remember "File not found"
                if (status == HttpStatus.SC_NOT_FOUND)
                {
                    Files.write(statusFile.toPath(), response.getStatusLine().toString().getBytes());
                }
                throw new DownloadFailed (url, status, response.getStatusLine());
            }
            
            File dl = new File (dest.getAbsolutePath() + ".new");
            OutputStream out = new BufferedOutputStream (new FileOutputStream (dl));
            IOUtils.copy (response.getEntity().getContent(), out);
            out.close ();
            
            File bak = new File (dest.getAbsolutePath() + ".bak");
            if (bak.exists()) {
                bak.delete();
            }
            if (dest.exists()) {
                dest.renameTo(bak);
            }
            dl.renameTo(dest);
        }
        finally
        {
            client.close();
        }
    }

    private String valueOf (Header responseHeader)
    {
        return responseHeader == null ? "unknown" : responseHeader.getValue();
    }
}
