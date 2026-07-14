package ru.terramain.microprocessor.js;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.concurrent.CompletableFuture;

public record JsFuture<T>(CompletableFuture<T> future) {

    @HostAccess.Export
    public void then(Value resolve, Value reject) {
        future.whenComplete((result, e) -> {
            if (e != null) {
                reject.execute(e);
            }
            else {
                resolve.execute(result);
            }
        });
    }
}