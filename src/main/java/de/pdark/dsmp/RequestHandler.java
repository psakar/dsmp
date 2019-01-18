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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Handle a connection from a maven.
 * 
 * @author digulla
 *
 */
public class RequestHandler extends Thread
{
    public static final Logger log = LogManager.getLogger(RequestHandler.class);

    public static final Logger downloadLog = LogManager.getLogger("downloadLog");
    public static final String KEEP_ALIVE_HEADER = "Proxy-Connection: Keep-Alive".toLowerCase();

    private final Socket clientSocket;
    private final Config config;

    public RequestHandler(Socket clientSocket, Config config)
    {
        if (clientSocket == null)
            throw new RuntimeException ("Connection is already closed");
        this.clientSocket = clientSocket;
        this.config = config;
    }

    @Override
    public void run ()
    {
        try
        {
            log.debug ("Got connection from "+clientSocket.getInetAddress());
            
            String line;
            boolean keepAlive = false;
            boolean headOnly = false;
            do
            {
                String downloadURL = null;
                StringBuffer fullRequest = new StringBuffer (1024);
                while ((line = readLine ()) != null)
                {
                    if (line.length() == 0)
                        break;
                    
                    log.debug ("Got: "+line);
                    fullRequest.append (line);
                    fullRequest.append ('\n');
                    
                    if (KEEP_ALIVE_HEADER.equals (line.toLowerCase())) {
                        keepAlive = true;
                    }
                    
                    if (line.startsWith("GET "))
                    {
                        int pos = line.lastIndexOf(' ');
                        line = line.substring(4, pos);
                        downloadURL = line;
                    }

                    if (line.startsWith("HEAD "))
                    {
                        int pos = line.lastIndexOf(' ');
                        line = line.substring(4, pos);
                        downloadURL = line;
                        headOnly = true;
                    }
                }
                
                if (downloadURL == null)
                {
                    if (line == null)
                        break;
                    
                    log.error ("Found no URL to download in request:\n"+fullRequest.toString());
                }
                else
                {
                    log.info ("Got request for "+downloadURL);
                    serveURL (downloadURL, headOnly);
                }
            }
            while (line != null && keepAlive);
            
            log.debug ("Terminating connection with "+clientSocket.getInetAddress());
        }
        catch (Exception e)
        {
            log.error ("Conversation with client aborted", e);
        }
        finally
        {
            close();
        }
    }

    public void close ()
    {
        try
        {
            if (out != null)
                out.close();
        }
        catch (Exception e)
        {
            log.error ("Exception while closing the outputstream", e);
        }
        out = null;
        
        try
        {
            if (in != null)
                in.close();
        }
        catch (Exception e)
        {
            log.error ("Exception while closing the inputstream", e);
        }
        in = null;

        try
        {
            if (clientSocket != null)
                clientSocket.close();
        }
        catch (Exception e)
        {
            log.error ("Exception while closing the socket", e);
        }
    }

    private void serveURL (String downloadURL, boolean headOnly) throws IOException
    {
        URL url = new URL (downloadURL);
        url = config.getMirror (url);
        
        if (!"http".equals(url.getProtocol()))
            throw new IOException ("Can only handle HTTP requests, got "+downloadURL);
        
        File f = getPatchFile(url);
        if (!f.exists())
            f = getCacheFile(url, config.getCacheDirectory());
        
        if (!f.exists())
        {
            ProxyDownload d = new ProxyDownload (url, f, config);
            try
            {
                d.download();
            }
            catch (DownloadFailed e)
            {
                log.error(e.getMessage());
                
                println (e.getStatusLine());
                println ();
                getOut().flush();
                return;
            }
        }
        else
        {
            log.debug ("Serving from local cache "+f.getAbsolutePath());
        }
        
        println ("HTTP/1.1 200 OK");
        print ("Date: ");
        Date d = new Date (f.lastModified());
        println (INTERNET_FORMAT.format(d));
        print ("Content-length: ");
        println (String.valueOf(f.length()));
        print ("Content-type: ");
        String ext = StringUtils.substringAfterLast(downloadURL, ".").toLowerCase();
        String type = CONTENT_TYPES.get (ext);
        if (type == null)
        {
            log.warn("Unknown extension "+ext+". Using content type text/plain.");
            type = "text/plain";
        }
        println (type);
        println ();
        if (headOnly) {
            log.info("HEAD for : " + url.toExternalForm());
            downloadLog.info("Downloaded: " + url.toExternalForm());
            return;
        }
        InputStream data = new BufferedInputStream (new FileInputStream (f));
        IOUtils.copy (data, out);
        data.close();
        downloadLog.info("Downloaded: " + url.toExternalForm());
    }

    private String formatLastModified(File file, long lastModified) {
        if (lastModified < 0) {
            log.error("Last modified of file " + file.getAbsolutePath() + " is less than zero " + lastModified);
            lastModified = System.currentTimeMillis();
        }
        Date d = new Date (lastModified);
        String format = null;
        try {
            format = INTERNET_FORMAT.format(d);
        } catch (Exception e) {
            log.error("Last modified of file " + file.getAbsolutePath() + " is " + lastModified + " - " + e.getMessage(), e);
            format = INTERNET_FORMAT.format(new Date());
        }
        return format;
    }

    public File getPatchFile (URL url)
    {
        File dir = config.getPatchesDirectory();
        File f = getCacheFile(url, dir);
        
        if (!f.exists())
        {
            String ext = StringUtils.substringAfterLast(url.getPath(), ".").toLowerCase();
            if ("md5".equals (ext) || "sha1".equals (ext))
            {
                File source = new File (StringUtils.substringBeforeLast(f.getAbsolutePath(), "."));
                if (source.exists())
                {
                    generateChecksum (source, f, ext);
                }
            }
        }
        
        return f;
    }
    
    public static void generateChecksum (File source, File f, String ext)
    {
        try
        {
            String checksum = null;
            if ("md5".equals (ext))
            {
                Md5Digester digester = new Md5Digester ();
                checksum = digester.calc(source);
            }
            else if ("sha1".equals (ext))
            {
                Sha1Digester digester = new Sha1Digester ();
                checksum = digester.calc(source);
            }
            
            if (checksum != null)
            {
                FileWriter w = new FileWriter (f);
                w.write(checksum);
                w.write(System.lineSeparator());
                w.close ();
            }
        }
        catch (DigesterException e)
        {
            log.warn ("Error creating "+ext.toUpperCase()+" checksum for "+source.getAbsolutePath(), e);
        }
        catch (IOException e)
        {
            log.warn ("Error writing "+ext.toUpperCase()+" checksum for "+source.getAbsolutePath()+" to "+f.getAbsolutePath(), e);
        }
        
    }

    public static File getCacheFile (URL url, File root)
    {
        root = new File (root, url.getHost());
        if (url.getPort() != -1 && url.getPort() != 80)
            root = new File (root, String.valueOf(url.getPort()));
        File f = new File (root, url.getPath());
        return f;
    }

    public final static HashMap<String,String> CONTENT_TYPES = new HashMap<String,String> ();
    static {
        CONTENT_TYPES.put ("xml", "application/xml");
        CONTENT_TYPES.put ("pom", "application/xml");
        
        CONTENT_TYPES.put ("jar", "application/java-archive");
        
        CONTENT_TYPES.put ("md5", "text/plain");
        CONTENT_TYPES.put ("sha1", "text/plain");
        CONTENT_TYPES.put ("asc", "text/plain");

        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
        CONTENT_TYPES.put ("", "");
    }
    
    private final static SimpleDateFormat INTERNET_FORMAT = new SimpleDateFormat ("EEE, d MMM yyyy HH:mm:ss zzz");
    private byte[] NEW_LINE = new byte[] { '\r', '\n' };
    
    private void println (String string) throws IOException
    {
        print (string);
        println ();
    }

    private void println () throws IOException
    {
        getOut().write(NEW_LINE);
    }
    
    private void print (String string) throws IOException
    {
        getOut().write (string.getBytes("ISO-8859-1"));
    }

    private OutputStream out;
    
    protected OutputStream getOut () throws IOException
    {
        if (out == null)
            out = new BufferedOutputStream (clientSocket.getOutputStream());
        
        return out;
    }

    private BufferedInputStream in;
    
    private String readLine () throws IOException
    {
        if (in == null)
            in = new BufferedInputStream (clientSocket.getInputStream());
        
        StringBuffer buffer = new StringBuffer (256);
        int c;
        
        try
        {
            while ((c = in.read()) != -1)
            {
                if (c == '\r')
                    continue;
                
                if (c == '\n')
                    break;
                
                buffer.append((char)c);
            }
        }
        catch (SocketException e)
        {
            if ("Connection reset".equals (e.getMessage()))
                return null;
            
            throw e;
        }

        if (c == -1)
            return null;
        
        if (buffer.length() == 0)
            return "";
        
        return buffer.toString();
    }
}
