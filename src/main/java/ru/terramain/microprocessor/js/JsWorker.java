package ru.terramain.microprocessor.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

import java.util.Map;

public class JsWorker {
    public static final Engine engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    public Context jsContext;

    public JsWorker() {
        jsContext = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.EXPLICIT)
                .build();
    }

    public void start(String code, Map<String, Object> globalObjects) {
        globalObjects.forEach((string, object) -> {
            jsContext.getBindings("js").putMember(string, object);
        });
        jsContext.eval("js", code);
    }

    public void close() {
        jsContext.close();
    }
}
