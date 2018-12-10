package com.mrcrayfish.furniture.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class GifDownloadThread extends Thread
{
    private static final Set<String> LOADING_URLS = new HashSet<>();

    // Prevents GIFs larger than 2MB from loading
    private static final long MAX_FILE_SIZE = 2097152;

    // User Agent - Set to Chrome version 60 running on windows 10
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Safari/537.36";

    private String url;
    private ResponseProcessor processor;
    private int tryCount;

    public GifDownloadThread(String url, ResponseProcessor processor)
    {
        super("Image Download Thread");
        this.url = url;
        this.processor = processor;
    }

    @Override
    public void run()
    {
        if(GifCache.INSTANCE.loadCached(url))
        {
            processor.process(ImageDownloadResult.SUCCESS, "Successfully processed GIF");
            return;
        }

        if(isLoading(url))
        {
            while(true)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch(InterruptedException e)
                {
                    e.printStackTrace();
                }

                if(GifCache.INSTANCE.isCached(url))
                {
                    processor.process(ImageDownloadResult.SUCCESS, "Successfully processed GIF");
                    return;
                }

                if(tryCount++ == 10)
                {
                    processor.process(ImageDownloadResult.FAILED, "Unable to process GIF");
                    return;
                }
            }
        }

        System.out.println("Downloading: "+url);

        // Use a custom RequestConfig because the default one cannot correctly parse the latest RFC for cookies
        RequestConfig clientConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

        // Testing URL's
        // SUCCESS: https://github.com/bvierra/MrCrayfishFurnitureMod/raw/master/test/files/tv/SUCCESS.gif
        // TOO_LARGE: https://github.com/bvierra/MrCrayfishFurnitureMod/raw/master/test/files/tv/TOO_LARGE.gif
        // UNKNOWN_FILE: https://github.com/bvierra/MrCrayfishFurnitureMod/raw/master/test/files/tv/UNKNOWN_FILE.gif

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(clientConfig).build()){
            HttpGet request = new HttpGet(url);
            request.addHeader("User-Agent",USER_AGENT);

            setLoading(url, true);
            // Download the headers
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() > 200) {
                setLoading(url, false);
                processor.process(ImageDownloadResult.FAILED, "Could not retrieve GIF");
                System.out.println("Could not download, server responded with status code: "+response.getStatusLine().getStatusCode());
                return;
            }
            else if (Integer.parseInt(response.getLastHeader("Content-Length").getValue() ) > MAX_FILE_SIZE) {
                setLoading(url, false);
                processor.process(ImageDownloadResult.TOO_LARGE, "The GIF is greater than " + MAX_FILE_SIZE / 1024.0 + "MB");
                System.out.println("File too large: " + Integer.parseInt(response.getLastHeader("Content-Length").getValue() ));
                System.out.println("Max size allowed: " + MAX_FILE_SIZE);
                return;
            }

            // Download the full file
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] data = EntityUtils.toByteArray(entity);

                // Check the contentType by looking at the actual file stream to avoid a server misconfiguration or an attempt to deceive and download a malicious file as a gif.
                String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
                if (!"image/gif".equals(contentType))
                {
                    setLoading(url, false);
                    processor.process(ImageDownloadResult.UNKNOWN_FILE, "The file is not a GIF");
                    System.out.println("File downloaded was not a gif it's content type was: : "+contentType);
                    return;
                }

                if(GifCache.INSTANCE.add(url, data))
                {
                    setLoading(url, false);
                    processor.process(ImageDownloadResult.SUCCESS, "Successfully processed GIF");
                    return;
                }

            }
            else {
                setLoading(url, false);
                processor.process(ImageDownloadResult.FAILED, "Could not retrieve GIF");
                return;
            }
        }
        catch(IOException e)
        {
            setLoading(url, false);
            processor.process(ImageDownloadResult.FAILED, "Could not retrieve GIF");
            e.printStackTrace();
        }
    }


    public interface ResponseProcessor
    {
        void process(ImageDownloadResult result, String message);
    }

    public enum ImageDownloadResult
    {
        SUCCESS("cfm.tv.success"),
        FAILED("cfm.tv.failed"),
        UNKNOWN_FILE("cfm.tv.unknown_file"),
        TOO_LARGE("cfm.tv.too_large");

        private String key;

        ImageDownloadResult(String key)
        {
            this.key = key;
        }

        public String getKey()
        {
            return key;
        }
    }

    public static void setLoading(String url, boolean loading)
    {
        synchronized(LOADING_URLS)
        {
            if(loading)
            {
                LOADING_URLS.add(url);
            }
            else
            {
                LOADING_URLS.remove(url);
            }
        }
    }

    public static boolean isLoading(String url)
    {
        synchronized(LOADING_URLS)
        {
           return LOADING_URLS.contains(url);
        }
    }
}
