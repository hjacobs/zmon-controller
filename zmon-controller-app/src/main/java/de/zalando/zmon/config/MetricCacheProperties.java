package de.zalando.zmon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URL;

/**
 * Created by hjacobs on 2/5/16.
 */
@ConfigurationProperties(prefix = "zmon.metric-cache")
public class MetricCacheProperties {

    private URL url;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}