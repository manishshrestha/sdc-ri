package test.org.ieee11073.common;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTestWatcher implements TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingTestWatcher.class);

    @Override
    public void testAborted(ExtensionContext extensionContext, Throwable throwable) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has been aborted",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testDisabled(ExtensionContext extensionContext, Optional<String> optional) {
        LOG.info("");
        LOG.info(
                "Test {}.{} was disabled",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testFailed(ExtensionContext extensionContext, Throwable throwable) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has failed",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testSuccessful(ExtensionContext extensionContext) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has been successful",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }
}