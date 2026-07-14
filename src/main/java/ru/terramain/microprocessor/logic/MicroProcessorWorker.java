package ru.terramain.microprocessor.logic;

import net.minecraft.core.Direction;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.js.JsRequestException;
import ru.terramain.microprocessor.js.JsWorker;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;
import ru.terramain.microprocessor.plate.Plate;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MicroProcessorWorker {
    public DataPool dataPool;
    public final ConcurrentLinkedQueue<LogMessage> logMessages = new ConcurrentLinkedQueue<>();

    public JsWorker jsWorker;
    public Thread workerThread;
    public AtomicLong nextId;

    public MicroProcessorContext microProcessorContext;
    public MicroProcessorJso microProcessorJsObject;

    public IsRunningStorage isRunningStorage;
    public static class IsRunningStorage {
        public boolean isRunning = false;
    }

    public MicroProcessorWorker() {
        dataPool = new DataPool(); // unused init for skip nullptr error
        jsWorker = null;
        isRunningStorage = new IsRunningStorage();
        nextId = new AtomicLong(); // unused init for skip nullptr error
    }


    public boolean run(String code) {
        if (workerThread != null) return false;
        isRunningStorage = new IsRunningStorage();
        isRunningStorage.isRunning = true;
        workerThread = new Thread(() -> {
            try {
                runLoop(code);
            } catch (Exception e) {
                reportFatalError(e, LogMessage.ErrorSource.WORKER_LOOP);
            }
            finally {
                jsWorker.close();
            }
        });
        workerThread.start();
        return true;
    }
    public boolean stop() {
        isRunningStorage.isRunning = false;
        if (workerThread == null) return false;
        workerThread.interrupt();
        workerThread = null;
        return true;
    }

    protected void runLoop(String code) throws InterruptedException {
        this.dataPool = new DataPool();
        this.jsWorker = new JsWorker();
        this.nextId = new AtomicLong();
        this.microProcessorJsObject = new MicroProcessorJso(microProcessorContext, this);

        IsRunningStorage isRunningStorage = this.isRunningStorage;

        HashMap<String, Object> globalObjects = new HashMap<>();
        globalObjects.put("microprocessor", this.microProcessorJsObject);
        globalObjects.put("mp", this.microProcessorJsObject);
        try {
            jsWorker.start(code, globalObjects);
        }
        catch (Exception e) {
            reportFatalError(e, LogMessage.ErrorSource.START);
        }

        System.out.println("worker::runLoop start loop!!!");
        while (isRunningStorage.isRunning) {
            S2WMessage message = null;
            try {
                message = this.dataPool.s2wMessages.take();
            }
            catch (InterruptedException e) { /* ok, its stop() method */ }

            if (message instanceof EventS2WMessage event) {
                this.microProcessorJsObject.runEvent(event.key, event.args);
            }
            if (message instanceof PlateEventS2WMessage event) {
                this.microProcessorJsObject.runPlateEvent(event.direction, event.key, event.args);
            }
            else if (message instanceof AnswerS2WMessage answer) {
                CompletableFuture<Object> future = dataPool.waitAnswerMap.remove(answer.id);
                if (future != null) {
                    if (answer.isError) {
                        future.completeExceptionally(new JsRequestException(answer.data));
                    } else {
                        future.complete(answer.data);
                    }
                }
            }
        }

        jsWorker.close();
        System.out.println("WORKER: jsWorker.close();");
    }

    public void reportFatalError(Throwable e, LogMessage.ErrorSource source) {
        stop();
        this.logMessages.add(LogMessage.error(e, source));
    }


    public static class LogMessage {
        public enum Level { LOG, WARN, ERROR }
        public enum ErrorSource { START, WORKER_LOOP, MP_EVENT, PLATE_EVENT }

        public final Level level;
        public final String content;

        public LogMessage(Level level, String content) {
            this.level = level;
            this.content = content;
        }

        public static LogMessage error(Throwable e, ErrorSource source) {
            return new LogMessage(Level.ERROR, formatErrorContent(unwrap(e), source));
        }

        private static Throwable unwrap(Throwable e) {
            Throwable current = e;
            while (current instanceof CompletionException || current instanceof ExecutionException) {
                Throwable cause = current.getCause();
                if (cause == null) {
                    break;
                }
                current = cause;
            }
            return current;
        }

        private static String formatErrorContent(Throwable e, ErrorSource source) {
            return "[" + source + "] " + formatErrorDetails(e);
        }

        private static String formatErrorDetails(Throwable e) {
            if (e instanceof PolyglotException pe) {
                return formatPolyglotError(pe);
            }
            if (e instanceof JsRequestException) {
                return nonBlankOrElse(e.getMessage(), "Request failed");
            }
            String message = nonBlankOrElse(e.getMessage(), e.getClass().getSimpleName());
            return e.getClass().getSimpleName() + ": " + message;
        }

        private static String formatPolyglotError(PolyglotException pe) {
            String message = firstLine(nonBlankOrElse(pe.getMessage(), "JavaScript error"));
            SourceSection location = pe.getSourceLocation();
            if (location == null || !location.isAvailable()) {
                location = firstGuestSourceLocation(pe);
            }
            if (location != null && location.isAvailable()) {
                return message + " at line " + location.getStartLine() + ", column " + location.getStartColumn();
            }
            return message;
        }

        private static SourceSection firstGuestSourceLocation(PolyglotException pe) {
            for (PolyglotException.StackFrame frame : pe.getPolyglotStackTrace()) {
                if (!frame.isGuestFrame()) {
                    continue;
                }
                SourceSection location = frame.getSourceLocation();
                if (location != null && location.isAvailable()) {
                    return location;
                }
            }
            return null;
        }

        private static String firstLine(String value) {
            int newline = value.indexOf('\n');
            if (newline < 0) {
                return value.trim();
            }
            return value.substring(0, newline).trim();
        }

        private static String nonBlankOrElse(String value, String fallback) {
            if (value != null && !value.isBlank()) {
                return value;
            }
            return fallback;
        }

        @Override
        public String toString() {
            return switch (this.level) {
                case LOG -> "[LOG]: " + content;
                case WARN -> "[WARN]: " + content;
                case ERROR -> "[ERROR]: " + content;
            };
        }
    }
    public static class DataPool {
        BlockingQueue<S2WMessage> s2wMessages;
        ConcurrentLinkedQueue<W2SMessage> w2sRequests;
        ConcurrentHashMap<Long, CompletableFuture<Object>> waitAnswerMap;

        public DataPool() {
            clear();
        }

        public void clear() {
            s2wMessages = new LinkedBlockingQueue<>();
            w2sRequests = new ConcurrentLinkedQueue<>();
            waitAnswerMap = new ConcurrentHashMap<>();
        }

        public S2WMessage waitS2WMessage() throws InterruptedException {
            return s2wMessages.take();
        }
        public void pushS2WMessage(S2WMessage s2wMessage) {
            s2wMessages.add(s2wMessage);
        }
        public W2SMessage getW2SRequest() {
            return w2sRequests.poll();
        }
        public void pushW2S(W2SMessage w2sRequest) {
            w2sRequests.add(w2sRequest);
        }
        public void registerW2SRequest(RequestW2SMessage w2sRequest, CompletableFuture<Object> resolve) {
            w2sRequests.add(w2sRequest);
            waitAnswerMap.put(w2sRequest.id, resolve);
        }
        public <T> JsFuture<T> waitAnswerForW2SRequest(MicroProcessorWorker worker, RequestW2SMessage message) {
            CompletableFuture<T> future = new CompletableFuture<>();
            worker.dataPool.registerW2SRequest(message, (CompletableFuture<Object>) future);
            return new JsFuture<>(future);
        }
    }
    public <T> JsFuture<T> waitAnswerForW2SRequest(RequestW2SMessage message) {
        return this.dataPool.waitAnswerForW2SRequest(this, message);
    }

    public void pushLog(String content, LogMessage.Level level) {
        this.logMessages.add(new LogMessage(level, content));
    }

    public LogMessage pollLog() {
        return this.logMessages.poll();
    }

    public static abstract class S2WMessage {}
    public static class EventS2WMessage extends S2WMessage {
        String key;
        Object[] args;

        public EventS2WMessage(String key, Object[] args) {
            this.key = key;
            this.args = args;
        }
    }
    public static class PlateEventS2WMessage extends S2WMessage {
        Direction direction;
        String key;
        Object[] args;

        public PlateEventS2WMessage(Direction direction, String key, Object[] args) {
            this.direction = direction;
            this.key = key;
            this.args = args;
        }
    }

    public static class AnswerS2WMessage extends S2WMessage {
        long id;
        boolean isError;
        Object data;
        public AnswerS2WMessage(long id, boolean isError, Object data) {
            this.id = id;
            this.isError = isError;
            this.data = data;
        }
    }

    public static abstract class W2SMessage {}
    public static abstract class RequestW2SMessage extends W2SMessage {
        public long id;
        public RequestW2SMessage(long id) {
            this.id = id;
        }
    }
    public static abstract class RequestMicroProcessorW2SMessage extends RequestW2SMessage {
        public RequestMicroProcessorW2SMessage(long id) {
            super(id);
        }
    }
    public static abstract class RequestPlateW2SMessage extends RequestW2SMessage {
        Direction direction;
        Plate<?> plate;
        public RequestPlateW2SMessage(long id, Direction direction, Plate<?> plate) {
            super(id);
            this.direction = direction;
            this.plate = plate;
        }
    }
}
