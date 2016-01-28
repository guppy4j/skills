package com.cgi.skills;

import static io.undertow.Handlers.resource;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static org.guppy4j.log.Log.Level.error;
import static org.guppy4j.log.Log.Level.info;
import static org.guppy4j.log.Log.Level.warn;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.guppy4j.http.Resources;
import org.guppy4j.http.Server;
import org.guppy4j.http.UndertowAdapter;
import org.guppy4j.http.UndertowServer;
import org.guppy4j.log.Log;
import org.guppy4j.log.LogProvider;
import org.guppy4j.log.Slf4jLogProvider;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

/**
 * The 'Skills' application class that creates and injects all components
 * and starts the HTTP server
 */
public final class Application {

    private final Log log;

    private final Server server;
    private final String appName;


    public Application(String appName, int port, String host) {
        this.appName = appName;

        final Class<? extends Application> myClass = getClass();

        final LogProvider logProvider = new Slf4jLogProvider();
        log = logProvider.getLog(myClass);

        final Path homeDir = Paths.get(System.getProperty("user.home"));

        final ResourceHandler fileHandler = resource(new ClassPathResourceManager(
                myClass.getClassLoader(), myClass.getPackage()));

        final RequestHandlerImpl requestHandler = new RequestHandlerImpl(logProvider, new Resources() {});

        final HttpHandler httpHandler = new UndertowAdapter(requestHandler, fileHandler, "file");

        server = new UndertowServer(Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(httpHandler)
                .build());

        setShutdownHandlers();

    }

    private void setShutdownHandlers() {
        setDefaultUncaughtExceptionHandler((t, e) -> {
            log.as(error, e, "Uncaught exception occurred");
            stop();
        });
        getRuntime().addShutdownHook(new Thread(() -> {
            log.as(warn, "Application '{}': shutdown in progress.", appName);
            stop();
        }));
    }

    public void start() {
        server.start();
        logServerAction("started");
    }

    private void stop() {
        log.as(warn, "Stopping HTTP server: {}", server);
        server.stop();
        logServerAction("stopped");
    }

    private void logServerAction(String didSomethingTo) {
        log.as(info, "Successfully " + didSomethingTo + " HTTP server: {}", server);
    }
}