package com.fostermoore.pdfviachrome.chrome;

import com.fostermoore.pdfviachrome.exception.ChromeNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChromeManager.
 *
 * Note: These are unit tests that test the logic without actually launching Chrome.
 * Integration tests that launch Chrome should be in a separate *IT.java file.
 */
class ChromeManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testChromeOptionsBuilder() {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .disableGpu(true)
            .remoteDebuggingPort(9222)
            .noSandbox(true)
            .disableDevShmUsage(true)
            .startupTimeout(60)
            .shutdownTimeout(10)
            .addFlag("--window-size=1920,1080")
            .build();

        assertThat(options.isHeadless()).isTrue();
        assertThat(options.isDisableGpu()).isTrue();
        assertThat(options.getRemoteDebuggingPort()).isEqualTo(9222);
        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options.isDisableDevShmUsage()).isTrue();
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(60);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(10);
        assertThat(options.getAdditionalFlags()).contains("--window-size=1920,1080");
    }

    @Test
    void testChromeOptionsDefaults() {
        ChromeOptions options = ChromeOptions.builder().build();

        assertThat(options.isHeadless()).isTrue(); // Default is headless
        assertThat(options.isDisableGpu()).isTrue(); // Default is true
        assertThat(options.getRemoteDebuggingPort()).isEqualTo(0); // Default is random port
        assertThat(options.isNoSandbox()).isFalse(); // Default is false for security
        assertThat(options.isDisableDevShmUsage()).isFalse();
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(45);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(5);
        assertThat(options.getAdditionalFlags()).isEmpty();
    }

    @Test
    void testChromeOptionsWithCustomPath() throws Exception {
        Path chromePath = createExecutableFile("chrome");
        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath)
            .build();

        assertThat(options.getChromePath()).isEqualTo(chromePath);
    }

    @Test
    void testChromeOptionsWithUserDataDir() {
        Path userDataDir = tempDir.resolve("user-data");
        ChromeOptions options = ChromeOptions.builder()
            .userDataDir(userDataDir)
            .build();

        assertThat(options.getUserDataDir()).isEqualTo(userDataDir);
    }

    @Test
    void testChromeOptionsImmutability() {
        ChromeOptions.Builder builder = ChromeOptions.builder();
        builder.addFlag("--flag1");
        ChromeOptions options = builder.build();

        // Trying to modify the returned list should not affect the options
        assertThatThrownBy(() -> options.getAdditionalFlags().add("--flag2"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testChromeProcessCreation() {
        Process mockProcess = createMockProcess();
        String wsUrl = "ws://127.0.0.1:9222/devtools/browser/12345";
        Path userDataDir = tempDir.resolve("user-data");

        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, wsUrl, userDataDir, true);

        assertThat(chromeProcess.getProcess()).isEqualTo(mockProcess);
        assertThat(chromeProcess.getWebSocketDebuggerUrl()).isEqualTo(wsUrl);
        assertThat(chromeProcess.getUserDataDir()).isEqualTo(userDataDir);
        assertThat(chromeProcess.isTemporaryUserDataDir()).isTrue();
    }

    @Test
    void testChromeProcessNullValidation() {
        assertThatThrownBy(() -> new ChromeProcess(null, "ws://test", tempDir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Process cannot be null");
    }

    @Test
    void testChromeProcessNullWebSocketUrl() {
        Process mockProcess = createMockProcess();

        assertThatThrownBy(() -> new ChromeProcess(mockProcess, null, tempDir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket debugger URL cannot be null or empty");
    }

    @Test
    void testChromeProcessEmptyWebSocketUrl() {
        Process mockProcess = createMockProcess();

        assertThatThrownBy(() -> new ChromeProcess(mockProcess, "", tempDir, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket debugger URL cannot be null or empty");
    }

    @Test
    void testChromeManagerRequiresOptions() {
        assertThatThrownBy(() -> new ChromeManager(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ChromeOptions cannot be null");
    }

    @Test
    void testChromeManagerInitialState() {
        ChromeOptions options = ChromeOptions.builder().build();
        ChromeManager manager = new ChromeManager(options);

        assertThat(manager.isRunning()).isFalse();
        assertThat(manager.getChromeProcess()).isNull();
    }

    @Test
    void testChromeManagerCloseIsIdempotent() {
        ChromeOptions options = ChromeOptions.builder().build();
        ChromeManager manager = new ChromeManager(options);

        // Should not throw when closing without starting
        assertThatCode(manager::close).doesNotThrowAnyException();

        // Should not throw when closing multiple times
        assertThatCode(manager::close).doesNotThrowAnyException();
        assertThatCode(manager::close).doesNotThrowAnyException();
    }

    @Test
    void testChromeOptionsBuilderFailsWithNonExistentChrome() {
        Path nonExistentPath = Path.of("/non/existent/chrome");

        assertThatThrownBy(() -> {
            ChromeOptions.builder()
                .chromePath(nonExistentPath)
                .build();
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Chrome path does not exist");
    }

    @Test
    void testChromeManagerCannotStartAfterClose() throws Exception {
        ChromeOptions options = ChromeOptions.builder().build();
        ChromeManager manager = new ChromeManager(options);
        manager.close();

        assertThatThrownBy(manager::start)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ChromeManager has been closed");
    }

    @Test
    void testTryWithResourcesPattern() {
        ChromeOptions options = ChromeOptions.builder().build();

        // This tests that ChromeManager can be used with try-with-resources
        assertThatCode(() -> {
            try (ChromeManager manager = new ChromeManager(options)) {
                // ChromeManager is AutoCloseable
                assertThat(manager).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testChromeProcessToString() {
        Process mockProcess = createMockProcess();
        String wsUrl = "ws://127.0.0.1:9222/devtools/browser/12345";

        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, wsUrl, tempDir, false);
        String toString = chromeProcess.toString();

        assertThat(toString).contains("ChromeProcess");
        assertThat(toString).contains(wsUrl);
        assertThat(toString).contains("pid=");
    }

    @Test
    void testChromeOptionsMultipleFlags() {
        ChromeOptions options = ChromeOptions.builder()
            .addFlag("--flag1")
            .addFlag("--flag2")
            .addFlags(java.util.Arrays.asList("--flag3", "--flag4"))
            .build();

        assertThat(options.getAdditionalFlags())
            .hasSize(4)
            .containsExactly("--flag1", "--flag2", "--flag3", "--flag4");
    }

    /**
     * Creates a mock Process for testing purposes.
     * This process doesn't actually run Chrome.
     */
    private Process createMockProcess() {
        return new Process() {
            private boolean alive = true;
            private final int exitValue = 0;

            @Override
            public OutputStream getOutputStream() {
                return new java.io.ByteArrayOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public InputStream getErrorStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public int waitFor() throws InterruptedException {
                while (alive) {
                    Thread.sleep(10);
                }
                return exitValue;
            }

            @Override
            public int exitValue() {
                if (alive) {
                    throw new IllegalThreadStateException("process has not exited");
                }
                return exitValue;
            }

            @Override
            public void destroy() {
                alive = false;
            }

            @Override
            public boolean isAlive() {
                return alive;
            }

            @Override
            public long pid() {
                return 12345L;
            }
        };
    }

    /**
     * Helper method to create an executable file for testing.
     * On Unix-like systems, sets the executable permission.
     * On Windows, files are executable by default.
     */
    private Path createExecutableFile(String filename) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.createFile(file);

        // On Unix-like systems, set executable permission
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            Files.setPosixFilePermissions(file, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            ));
        }

        return file;
    }
}
