package com.navercorp.pinpoint.profiler.logging;

import com.navercorp.pinpoint.bootstrap.BootLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerBinder;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.util.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.util.ReflectionUtil;

import java.io.File;
import java.net.URI;
import java.security.CodeSource;

public class Log4j2LoggingSystem implements LoggingSystem {
    public static final String CONTEXT_NAME = "pinpoint-agent-logging-context";

    public static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    private static final String[] LOOKUP = {"log4j2-test.xml", "log4j2.xml", "log4j2.properties"};

    private LoggerContext loggerContext;
    private final String profilePath;

    private PLoggerBinder binder;


    public Log4j2LoggingSystem(String profilePath) {
        this.profilePath = Assert.requireNonNull(profilePath, "profilePath");
    }

    public void start() {
        // log4j init
        String configLocation = getConfigPath(profilePath);
        URI uri = newURI(configLocation);

        BootLogger bootLogger = BootLogger.getLogger(this.getClass());
        bootLogger.info("logPath:" + uri);
        patchReflectionUtilForJava9(bootLogger);
        
        this.loggerContext = getLoggerContext(uri);
//        this.loggerContext = getLoggerContext2(uri);

        Logger logger = getLoggerContextLogger();
        logger.info("{} start", this.getClass().getSimpleName());

        logger.info("LoggerContextFactory:{} LoggerContext:{}", LogManager.getFactory().getClass().getName(), loggerContext.getClass().getName());

        this.binder = new Log4j2Binder(loggerContext);
        bindPLoggerFactory(this.binder);
    }

    private void patchReflectionUtilForJava9(BootLogger bootLogger) {
        Class<ReflectionUtil> reflectionUtilClass = ReflectionUtil.class;
        CodeSource codeSource = reflectionUtilClass.getProtectionDomain().getCodeSource();
        bootLogger.info("patch ReflectionUtil codeSource:" + codeSource);
    }

    private Logger getLoggerContextLogger() {
        return loggerContext.getLogger(getClass().getName());
    }

    private URI newURI(String configLocation) {
        File file = new File(configLocation);
        return file.toURI();
    }

    private String getConfigPath(String profilePath) {
        for (String configFile : LOOKUP) {
            String configLocation = String.format("%s%s", profilePath, configFile);
            final File file = new File(configLocation);
            if (file.exists()) {
                return configLocation;
            }
        }
        throw new IllegalStateException("log4j2.xml not found");
    }

    private LoggerContext getLoggerContext(URI uri) {
        String factory = prepare(FACTORY_PROPERTY_NAME);
        try {
            return (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(), false, null, uri, CONTEXT_NAME);
        } finally {
            rollback(FACTORY_PROPERTY_NAME, factory);
        }
    }

//    private LoggerContext getLoggerContext2(URI uri) {
//        ContextSelector selector = new ClassLoaderContextSelector();
//        ShutdownCallbackRegistry shutdownCallbackRegistry = new DefaultShutdownCallbackRegistry();
//        Log4jContextFactory factory = new Log4jContextFactory(selector, shutdownCallbackRegistry);
//
//        LoggerContext old = (LoggerContext)LogManager.getContext();
//        old.stop();
//
//        LogManager.setFactory(factory);
//
//        return (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(), false, null, uri, CONTEXT_NAME);
//    }


    public void stop() {
        if (loggerContext != null) {
            Logger logger = getLoggerContextLogger();
            logger.info("{} stop", this.getClass().getSimpleName());

            this.binder.shutdown();
            loggerContext.stop();
        }

    }

    private String prepare(String key) {
        final String backup = System.getProperty(key);
        System.setProperty(key, Log4j2ContextFactory.class.getName());
        return backup;
    }

    private void rollback(String key, String backup) {
        if (backup != null) {
            System.setProperty(key, backup);
        } else {
            System.clearProperty(key);
        }
    }


    private void bindPLoggerFactory(PLoggerBinder binder) {
        final String binderClassName = binder.getClass().getName();
        PLogger pLogger = binder.getLogger(binder.getClass().getName());
        pLogger.info("PLoggerFactory.initialize() bind:{} cl:{}", binderClassName, binder.getClass().getClassLoader());
        // Set binder to static LoggerFactory
        // Should we unset binder at shutdown hook or stop()?
        PLoggerFactory.initialize(binder);
    }
}