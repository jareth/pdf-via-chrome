package com.fostermoore.pdfviachrome.chrome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ChromeOptions and its builder pattern.
 */
class ChromeOptionsTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuilderWithDefaults() {
        ChromeOptions options = ChromeOptions.builder().build();

        assertThat(options.getChromePath()).isNull();
        assertThat(options.isHeadless()).isTrue();
        assertThat(options.isDisableGpu()).isTrue();
        assertThat(options.getRemoteDebuggingPort()).isEqualTo(0);
        assertThat(options.getUserDataDir()).isNull();
        assertThat(options.isNoSandbox()).isFalse();
        assertThat(options.isDisableDevShmUsage()).isFalse();
        assertThat(options.getWindowSize()).isNull();
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(45);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(5);
        assertThat(options.getAdditionalFlags()).isEmpty();
    }

    @Test
    void testBuilderWithCustomChromePath() throws IOException {
        Path chromePath = createExecutableFile("chrome");
        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath)
            .build();

        assertThat(options.getChromePath()).isEqualTo(chromePath);
    }

    @Test
    void testBuilderWithHeadlessMode() {
        ChromeOptions headlessOptions = ChromeOptions.builder()
            .headless(true)
            .build();
        assertThat(headlessOptions.isHeadless()).isTrue();

        ChromeOptions nonHeadlessOptions = ChromeOptions.builder()
            .headless(false)
            .build();
        assertThat(nonHeadlessOptions.isHeadless()).isFalse();
    }

    @Test
    void testBuilderWithDisableGpu() {
        ChromeOptions disabledOptions = ChromeOptions.builder()
            .disableGpu(true)
            .build();
        assertThat(disabledOptions.isDisableGpu()).isTrue();

        ChromeOptions enabledOptions = ChromeOptions.builder()
            .disableGpu(false)
            .build();
        assertThat(enabledOptions.isDisableGpu()).isFalse();
    }

    @Test
    void testBuilderWithRemoteDebuggingPort() {
        ChromeOptions randomPortOptions = ChromeOptions.builder()
            .remoteDebuggingPort(0)
            .build();
        assertThat(randomPortOptions.getRemoteDebuggingPort()).isEqualTo(0);

        ChromeOptions customPortOptions = ChromeOptions.builder()
            .remoteDebuggingPort(9222)
            .build();
        assertThat(customPortOptions.getRemoteDebuggingPort()).isEqualTo(9222);
    }

    @Test
    void testBuilderWithUserDataDir() {
        Path userDataDir = tempDir.resolve("user-data");
        ChromeOptions options = ChromeOptions.builder()
            .userDataDir(userDataDir)
            .build();

        assertThat(options.getUserDataDir()).isEqualTo(userDataDir);
    }

    @Test
    void testBuilderWithNoSandbox() {
        ChromeOptions sandboxedOptions = ChromeOptions.builder()
            .noSandbox(false)
            .build();
        assertThat(sandboxedOptions.isNoSandbox()).isFalse();

        ChromeOptions noSandboxOptions = ChromeOptions.builder()
            .noSandbox(true)
            .build();
        assertThat(noSandboxOptions.isNoSandbox()).isTrue();
    }

    @Test
    void testBuilderWithDisableDevShmUsage() {
        ChromeOptions enabledOptions = ChromeOptions.builder()
            .disableDevShmUsage(false)
            .build();
        assertThat(enabledOptions.isDisableDevShmUsage()).isFalse();

        ChromeOptions disabledOptions = ChromeOptions.builder()
            .disableDevShmUsage(true)
            .build();
        assertThat(disabledOptions.isDisableDevShmUsage()).isTrue();
    }

    @Test
    void testBuilderWithWindowSize() {
        ChromeOptions options = ChromeOptions.builder()
            .windowSize("1920,1080")
            .build();

        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
    }

    @Test
    void testBuilderWithWindowSizeConvenience() {
        ChromeOptions options = ChromeOptions.builder()
            .withWindowSize(1920, 1080)
            .build();

        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
    }

    @Test
    void testBuilderWithMultipleWindowSizeCalls() {
        // Last call should win
        ChromeOptions options = ChromeOptions.builder()
            .withWindowSize(800, 600)
            .withWindowSize(1920, 1080)
            .build();

        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
    }

    @Test
    void testBuilderWithStartupTimeout() {
        ChromeOptions options = ChromeOptions.builder()
            .startupTimeout(60)
            .build();

        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void testBuilderWithShutdownTimeout() {
        ChromeOptions options = ChromeOptions.builder()
            .shutdownTimeout(10)
            .build();

        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    void testBuilderAddFlag() {
        ChromeOptions options = ChromeOptions.builder()
            .addFlag("--custom-flag-1")
            .addFlag("--custom-flag-2")
            .build();

        assertThat(options.getAdditionalFlags())
            .hasSize(2)
            .containsExactly("--custom-flag-1", "--custom-flag-2");
    }

    @Test
    void testBuilderAddFlags() {
        List<String> flags = List.of("--flag-1", "--flag-2", "--flag-3");
        ChromeOptions options = ChromeOptions.builder()
            .addFlags(flags)
            .build();

        assertThat(options.getAdditionalFlags())
            .hasSize(3)
            .containsExactlyElementsOf(flags);
    }

    @Test
    void testBuilderAddFlagAndAddFlags() {
        ChromeOptions options = ChromeOptions.builder()
            .addFlag("--flag-1")
            .addFlags(List.of("--flag-2", "--flag-3"))
            .addFlag("--flag-4")
            .build();

        assertThat(options.getAdditionalFlags())
            .hasSize(4)
            .containsExactly("--flag-1", "--flag-2", "--flag-3", "--flag-4");
    }

    @Test
    void testAdditionalFlagsAreImmutable() {
        ChromeOptions options = ChromeOptions.builder()
            .addFlag("--flag-1")
            .build();

        assertThatThrownBy(() -> options.getAdditionalFlags().add("--flag-2"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testDockerDefaults() {
        ChromeOptions options = ChromeOptions.builder()
            .dockerDefaults()
            .build();

        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options.isDisableDevShmUsage()).isTrue();
    }

    @Test
    void testDockerDefaultsCanBeOverridden() {
        ChromeOptions options = ChromeOptions.builder()
            .dockerDefaults()
            .noSandbox(false)
            .disableDevShmUsage(false)
            .build();

        assertThat(options.isNoSandbox()).isFalse();
        assertThat(options.isDisableDevShmUsage()).isFalse();
    }

    @Test
    void testBuilderChaining() {
        Path userDataDir = tempDir.resolve("user-data");
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .disableGpu(true)
            .remoteDebuggingPort(9222)
            .userDataDir(userDataDir)
            .noSandbox(true)
            .disableDevShmUsage(true)
            .withWindowSize(1920, 1080)
            .startupTimeout(60)
            .shutdownTimeout(10)
            .addFlag("--custom-flag")
            .build();

        assertThat(options.isHeadless()).isTrue();
        assertThat(options.isDisableGpu()).isTrue();
        assertThat(options.getRemoteDebuggingPort()).isEqualTo(9222);
        assertThat(options.getUserDataDir()).isEqualTo(userDataDir);
        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options.isDisableDevShmUsage()).isTrue();
        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(60);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(10);
        assertThat(options.getAdditionalFlags()).contains("--custom-flag");
    }

    @Test
    void testBuildWithNonExistentChromePath() {
        Path nonExistentPath = tempDir.resolve("non-existent-chrome");

        assertThatThrownBy(() -> {
            ChromeOptions.builder()
                .chromePath(nonExistentPath)
                .build();
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Chrome path does not exist");
    }

    @Test
    void testBuildWithNonExecutableChromePath() throws IOException {
        // Create a non-executable file
        Path nonExecutableFile = tempDir.resolve("chrome");
        Files.createFile(nonExecutableFile);

        // On Windows, all files are technically executable, so we need to handle this differently
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            // On Unix-like systems, remove execute permission
            Files.setPosixFilePermissions(nonExecutableFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ));

            assertThatThrownBy(() -> {
                ChromeOptions.builder()
                    .chromePath(nonExecutableFile)
                    .build();
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chrome path is not executable");
        }
    }

    @Test
    void testBuildWithValidChromePath() throws IOException {
        Path executableFile = createExecutableFile("chrome");

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(executableFile)
            .build();

        assertThat(options.getChromePath()).isEqualTo(executableFile);
    }

    @Test
    void testImmutability() throws IOException {
        // Build options and verify that the underlying builder state
        // doesn't affect the built instance
        ChromeOptions.Builder builder = ChromeOptions.builder()
            .addFlag("--flag-1");

        ChromeOptions options1 = builder.build();

        // Modify builder after first build
        builder.addFlag("--flag-2");
        ChromeOptions options2 = builder.build();

        // First instance should only have the first flag
        assertThat(options1.getAdditionalFlags())
            .hasSize(1)
            .containsExactly("--flag-1");

        // Second instance should have both flags
        assertThat(options2.getAdditionalFlags())
            .hasSize(2)
            .containsExactly("--flag-1", "--flag-2");
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
