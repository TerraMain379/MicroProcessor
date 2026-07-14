package ru.terramain.microprocessor.js;

import org.graalvm.polyglot.HostAccess;

public class JsRequestException extends RuntimeException {
    private final Object data;

    public JsRequestException(Object data) {
        super(data instanceof String message ? message : "Request failed");
        this.data = data;
    }

    @HostAccess.Export
    public Object data() {
        return data;
    }
}
