package io.github.wesleyosantos91.infrastructure.logging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging.adaptive")
public class LoggingProperties {

    private long slowRequestThresholdMs = 700;
    private boolean includeHeadersOnError = true;
    private boolean includeQuerystringOnWarn = true;
    private int maxPayloadLogBytes = 2048;
    private List<String> maskHeaders = new ArrayList<>();
    private List<String> maskJsonFields = new ArrayList<>();

    public long getSlowRequestThresholdMs() {
        return slowRequestThresholdMs;
    }

    public void setSlowRequestThresholdMs(long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    public boolean isIncludeHeadersOnError() {
        return includeHeadersOnError;
    }

    public void setIncludeHeadersOnError(boolean includeHeadersOnError) {
        this.includeHeadersOnError = includeHeadersOnError;
    }

    public boolean isIncludeQuerystringOnWarn() {
        return includeQuerystringOnWarn;
    }

    public void setIncludeQuerystringOnWarn(boolean includeQuerystringOnWarn) {
        this.includeQuerystringOnWarn = includeQuerystringOnWarn;
    }

    public int getMaxPayloadLogBytes() {
        return maxPayloadLogBytes;
    }

    public void setMaxPayloadLogBytes(int maxPayloadLogBytes) {
        this.maxPayloadLogBytes = maxPayloadLogBytes;
    }

    public List<String> getMaskHeaders() {
        return maskHeaders;
    }

    public void setMaskHeaders(List<String> maskHeaders) {
        this.maskHeaders = maskHeaders;
    }

    public List<String> getMaskJsonFields() {
        return maskJsonFields;
    }

    public void setMaskJsonFields(List<String> maskJsonFields) {
        this.maskJsonFields = maskJsonFields;
    }
}