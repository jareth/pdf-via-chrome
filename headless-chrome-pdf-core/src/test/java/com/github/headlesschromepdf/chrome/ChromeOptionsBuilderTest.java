package com.github.headlesschromepdf.chrome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ChromeOptions.Builder.
 */
class ChromeOptionsBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuildWithDefaults() {
        ChromeOptions options = ChromeOptions.builder().build();

        assertThat(options).isNotNull();
        assertThat(options.getChromePath()).isNull();
        assertThat(options.isHeadless()).isTrue();
        assertThat(options.getRemoteDebuggingPort()).isZero();
        assertThat(options.getUserDataDir()).isNull();
        assertThat(options.getAdditionalFlags()).isEmpty();
        assertThat(options.isDisableGpu()).isTrue();
        assertThat(options.isDisableDevShmUsage()).isFalse();
        assertThat(options.isNoSandbox()).isFalse();
        assertThat(options.getWindowSize()).isNull();
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(30);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(5);
    }

    @Test
    void testBuildWithAllOptions() throws IOException {
        Path chromePath = createExecutableFile("chrome");
        Path userDataDir = tempDir.resolve("user-data");
        Files.createDirectories(userDataDir);

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath)
            .headless(false)
            .remoteDebuggingPort(9222)
            .userDataDir(userDataDir)
            .addFlag("--test-flag")
            .disableGpu(false)
            .disableDevShmUsage(true)
            .noSandbox(true)
            .windowSize("1920,1080")
            .startupTimeout(60)
            .shutdownTimeout(10)
            .build();

        assertThat(options.getChromePath()).isEqualTo(chromePath);
        assertThat(options.isHeadless()).isFalse();
        assertThat(options.getRemoteDebuggingPort()).isEqualTo(9222);
        assertThat(options.getUserDataDir()).isEqualTo(userDataDir);
        assertThat(options.getAdditionalFlags()).containsExactly("--test-flag");
        assertThat(options.isDisableGpu()).isFalse();
        assertThat(options.isDisableDevShmUsage()).isTrue();
        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(60);
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    void testBuilderMethodChaining() throws IOException {
        Path chromePath = createExecutableFile("chrome");

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath)
            .headless(false)
            .disableGpu(false)
            .build();

        assertThat(options.getChromePath()).isEqualTo(chromePath);
        assertThat(options.isHeadless()).isFalse();
        assertThat(options.isDisableGpu()).isFalse();
    }

    @Test
    void testAddMultipleFlags() {
        ChromeOptions options = ChromeOptions.builder()
            .addFlag("--flag1")
            .addFlag("--flag2")
            .addFlag("--flag3")
            .build();

        assertThat(options.getAdditionalFlags())
            .containsExactly("--flag1", "--flag2", "--flag3");
    }

    @Test
    void testAddFlagsList() {
        List<String> flags = Arrays.asList("--flag1", "--flag2", "--flag3");

        ChromeOptions options = ChromeOptions.builder()
            .addFlags(flags)
            .build();

        assertThat(options.getAdditionalFlags())
            .containsExactlyElementsOf(flags);
    }

    @Test
    void testWindowSizeWithDimensions() {
        ChromeOptions options = ChromeOptions.builder()
            .withWindowSize(1920, 1080)
            .build();

        assertThat(options.getWindowSize()).isEqualTo("1920,1080");
    }

    @Test
    void testDockerDefaults() {
        ChromeOptions options = ChromeOptions.builder()
            .dockerDefaults()
            .build();

        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options.isDisableDevShmUsage()).isTrue();
    }

    // Validation Tests

    @Test
    void testChromePath_Valid() throws IOException {
        Path chromePath = createExecutableFile("chrome");

        assertThatCode(() -> ChromeOptions.builder().chromePath(chromePath).build())
            .doesNotThrowAnyException();
    }

    @Test
    void testChromePath_NotExists() {
        Path nonExistent = tempDir.resolve("non-existent-chrome");

        assertThatThrownBy(() -> ChromeOptions.builder().chromePath(nonExistent).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chrome path does not exist");
    }

    @Test
    void testChromePath_NotExecutable() throws IOException {
        // Skip on Windows where all files are considered executable
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        Path nonExecutable = tempDir.resolve("chrome");
        Files.createFile(nonExecutable);
        // Don't set executable permission - on Unix-like systems, files are not executable by default

        assertThatThrownBy(() -> ChromeOptions.builder().chromePath(nonExecutable).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chrome path is not executable");
    }

    @Test
    void testChromePath_Null() {
        assertThatCode(() -> ChromeOptions.builder().chromePath(null).build())
            .doesNotThrowAnyException();
    }

    @Test
    void testRemoteDebuggingPort_Valid() {
        assertThatCode(() -> ChromeOptions.builder().remoteDebuggingPort(0).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> ChromeOptions.builder().remoteDebuggingPort(9222).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> ChromeOptions.builder().remoteDebuggingPort(65535).build())
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 65536, 70000, 100000})
    void testRemoteDebuggingPort_Invalid(int invalidPort) {
        assertThatThrownBy(() -> ChromeOptions.builder().remoteDebuggingPort(invalidPort).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Remote debugging port must be between 0 and 65535");
    }

    @Test
    void testAddFlag_Null() {
        assertThatThrownBy(() -> ChromeOptions.builder().addFlag(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Flag cannot be null");
    }

    @Test
    void testAddFlags_Null() {
        assertThatThrownBy(() -> ChromeOptions.builder().addFlags(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Flags list cannot be null");
    }

    @Test
    void testAddFlags_ContainsNull() {
        List<String> flagsWithNull = Arrays.asList("--flag1", null, "--flag2");

        assertThatThrownBy(() -> ChromeOptions.builder().addFlags(flagsWithNull).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Flags list cannot contain null elements");
    }

    @Test
    void testStartupTimeout_Valid() {
        assertThatCode(() -> ChromeOptions.builder().startupTimeout(1).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> ChromeOptions.builder().startupTimeout(60).build())
            .doesNotThrowAnyException();

        ChromeOptions options = ChromeOptions.builder().startupTimeout(120).build();
        assertThat(options.getStartupTimeoutSeconds()).isEqualTo(120);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, -100})
    void testStartupTimeout_Invalid(int invalidTimeout) {
        assertThatThrownBy(() -> ChromeOptions.builder().startupTimeout(invalidTimeout).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Startup timeout must be positive");
    }

    @Test
    void testShutdownTimeout_Valid() {
        assertThatCode(() -> ChromeOptions.builder().shutdownTimeout(1).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> ChromeOptions.builder().shutdownTimeout(10).build())
            .doesNotThrowAnyException();

        ChromeOptions options = ChromeOptions.builder().shutdownTimeout(30).build();
        assertThat(options.getShutdownTimeoutSeconds()).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, -100})
    void testShutdownTimeout_Invalid(int invalidTimeout) {
        assertThatThrownBy(() -> ChromeOptions.builder().shutdownTimeout(invalidTimeout).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Shutdown timeout must be positive");
    }

    @Test
    void testImmutability() {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .noSandbox(true)
            .build();

        // Verify getters return correct values
        assertThat(options.isHeadless()).isTrue();
        assertThat(options.isNoSandbox()).isTrue();

        // Create another instance with different values
        ChromeOptions options2 = ChromeOptions.builder()
            .headless(false)
            .noSandbox(false)
            .build();

        // Verify first instance is unchanged (immutable)
        assertThat(options.isHeadless()).isTrue();
        assertThat(options.isNoSandbox()).isTrue();
        assertThat(options2.isHeadless()).isFalse();
        assertThat(options2.isNoSandbox()).isFalse();
    }

    @Test
    void testAdditionalFlagsImmutability() {
        ChromeOptions.Builder builder = ChromeOptions.builder();
        builder.addFlag("--flag1");

        ChromeOptions options = builder.build();
        List<String> flags = options.getAdditionalFlags();

        assertThat(flags).containsExactly("--flag1");

        // Try to modify the returned list (should not affect options)
        assertThatThrownBy(() -> flags.add("--flag2"))
            .isInstanceOf(UnsupportedOperationException.class);

        // Add another flag to builder
        builder.addFlag("--flag2");

        // Original options should remain unchanged
        assertThat(options.getAdditionalFlags()).containsExactly("--flag1");
    }

    @Test
    void testBuilderReuse() throws IOException {
        Path chromePath = createExecutableFile("chrome");

        ChromeOptions.Builder builder = ChromeOptions.builder()
            .chromePath(chromePath)
            .headless(true);

        ChromeOptions options1 = builder.build();
        assertThat(options1.getChromePath()).isEqualTo(chromePath);
        assertThat(options1.isHeadless()).isTrue();

        // Modify builder and create another instance
        builder.headless(false).noSandbox(true);
        ChromeOptions options2 = builder.build();

        assertThat(options2.getChromePath()).isEqualTo(chromePath);
        assertThat(options2.isHeadless()).isFalse();
        assertThat(options2.isNoSandbox()).isTrue();

        // Original instance should remain unchanged
        assertThat(options1.isHeadless()).isTrue();
        assertThat(options1.isNoSandbox()).isFalse();
    }

    // Helper methods

    private Path createExecutableFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.createFile(file);

        // Make file executable (works on Unix-like systems)
        // On Windows, files are executable by default if they have certain extensions
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            file.toFile().setExecutable(true);
        }

        return file;
    }
}
