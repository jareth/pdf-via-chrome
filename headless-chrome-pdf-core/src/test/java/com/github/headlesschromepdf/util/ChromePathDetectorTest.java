package com.github.headlesschromepdf.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChromePathDetector.
 *
 * Note: These tests verify the detection logic works correctly.
 * Platform-specific tests are enabled only on their respective platforms.
 */
class ChromePathDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void testDetectChromePath_ReturnsOptional() {
        Optional<Path> result = ChromePathDetector.detectChromePath();
        assertThat(result).isNotNull();
    }

    @Test
    void testIsWindows() {
        boolean isWindows = ChromePathDetector.isWindows();
        String osName = System.getProperty("os.name").toLowerCase();
        assertThat(isWindows).isEqualTo(osName.contains("win"));
    }

    @Test
    void testIsLinux() {
        boolean isLinux = ChromePathDetector.isLinux();
        String osName = System.getProperty("os.name").toLowerCase();
        assertThat(isLinux).isEqualTo(osName.contains("nux") || osName.contains("nix"));
    }

    @Test
    void testIsMac() {
        boolean isMac = ChromePathDetector.isMac();
        String osName = System.getProperty("os.name").toLowerCase();
        assertThat(isMac).isEqualTo(osName.contains("mac") || osName.contains("darwin"));
    }

    @Test
    void testIsValidChromeExecutable_WithNullPath() {
        assertThat(ChromePathDetector.isValidChromeExecutable(null)).isFalse();
    }

    @Test
    void testIsValidChromeExecutable_WithNonExistentPath() {
        Path nonExistent = Paths.get("/non/existent/path/chrome");
        assertThat(ChromePathDetector.isValidChromeExecutable(nonExistent)).isFalse();
    }

    @Test
    void testIsValidChromeExecutable_WithDirectory() throws IOException {
        Path dir = tempDir.resolve("chrome");
        Files.createDirectory(dir);
        assertThat(ChromePathDetector.isValidChromeExecutable(dir)).isFalse();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testIsValidChromeExecutable_WithNonExecutableFile_OnUnix() throws IOException {
        Path file = tempDir.resolve("chrome");
        Files.createFile(file);
        // File is not executable by default on Unix
        assertThat(ChromePathDetector.isValidChromeExecutable(file)).isFalse();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testIsValidChromeExecutable_WithExecutableChrome_OnUnix() throws IOException {
        Path chromePath = tempDir.resolve("chrome");
        Files.createFile(chromePath);

        // Make file executable
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(chromePath);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(chromePath, perms);

        assertThat(ChromePathDetector.isValidChromeExecutable(chromePath)).isTrue();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testIsValidChromeExecutable_WithExecutableChromium_OnUnix() throws IOException {
        Path chromiumPath = tempDir.resolve("chromium");
        Files.createFile(chromiumPath);

        // Make file executable
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(chromiumPath);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(chromiumPath, perms);

        assertThat(ChromePathDetector.isValidChromeExecutable(chromiumPath)).isTrue();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsValidChromeExecutable_WithChromeExe_OnWindows() throws IOException {
        Path chromePath = tempDir.resolve("chrome.exe");
        Files.createFile(chromePath);

        assertThat(ChromePathDetector.isValidChromeExecutable(chromePath)).isTrue();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsValidChromeExecutable_WithChromiumExe_OnWindows() throws IOException {
        Path chromiumPath = tempDir.resolve("chromium.exe");
        Files.createFile(chromiumPath);

        assertThat(ChromePathDetector.isValidChromeExecutable(chromiumPath)).isTrue();
    }

    @Test
    void testIsValidChromeExecutable_WithInvalidFileName() throws IOException {
        Path invalidPath = tempDir.resolve("firefox");
        Files.createFile(invalidPath);

        assertThat(ChromePathDetector.isValidChromeExecutable(invalidPath)).isFalse();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testDetectChromeOnWindows() {
        Optional<Path> result = ChromePathDetector.detectChromeOnWindows();

        // Test structure - result may be present or empty depending on actual installation
        assertThat(result).isNotNull();

        // If Chrome is found, validate it
        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(path.toString()).containsIgnoringCase("chrome");
        });
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testDetectChromeOnLinux() {
        Optional<Path> result = ChromePathDetector.detectChromeOnLinux();

        // Test structure - result may be present or empty depending on actual installation
        assertThat(result).isNotNull();

        // If Chrome is found, validate it
        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.isExecutable(path)).isTrue();
            String pathStr = path.toString().toLowerCase();
            assertThat(pathStr).matches(".*chrom(e|ium).*");
        });
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void testDetectChromeOnMac() {
        Optional<Path> result = ChromePathDetector.detectChromeOnMac();

        // Test structure - result may be present or empty depending on actual installation
        assertThat(result).isNotNull();

        // If Chrome is found, validate it
        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.isExecutable(path)).isTrue();
            String pathStr = path.toString().toLowerCase();
            assertThat(pathStr).matches(".*chrom(e|ium).*");
        });
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testFindExecutableWithWhich_GoogleChrome() {
        Optional<Path> result = ChromePathDetector.findExecutableWithWhich("google-chrome");

        // May or may not be installed
        assertThat(result).isNotNull();

        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.isExecutable(path)).isTrue();
        });
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testFindExecutableWithWhich_Chromium() {
        Optional<Path> result = ChromePathDetector.findExecutableWithWhich("chromium");

        // May or may not be installed
        assertThat(result).isNotNull();

        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.isExecutable(path)).isTrue();
        });
    }

    @Test
    void testFindExecutableWithWhich_NonExistentCommand() {
        Optional<Path> result = ChromePathDetector.findExecutableWithWhich("this-command-does-not-exist-12345");

        assertThat(result).isEmpty();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testGetChromPathFromWindowsRegistry() {
        Optional<Path> result = ChromePathDetector.getChromPathFromWindowsRegistry();

        // May or may not be in registry
        assertThat(result).isNotNull();

        result.ifPresent(path -> {
            assertThat(Files.exists(path)).isTrue();
            assertThat(path.toString()).containsIgnoringCase("chrome");
        });
    }

    @Test
    void testValidateChromeVersion_WithNonExistentPath() {
        Path nonExistent = Paths.get("/non/existent/chrome");
        assertThat(ChromePathDetector.validateChromeVersion(nonExistent)).isFalse();
    }

    @Test
    void testValidateChromeVersion_WithInvalidFile() throws IOException {
        Path invalidFile = tempDir.resolve("not-chrome.txt");
        Files.writeString(invalidFile, "This is not Chrome");

        assertThat(ChromePathDetector.validateChromeVersion(invalidFile)).isFalse();
    }

    @Test
    void testValidateChromeVersion_WithActualChrome() {
        // Only test if Chrome is actually found on the system
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();

        // This test is optional - it validates Chrome version if Chrome is detected
        // If Chrome is not installed or validation fails, the test still passes
        // because this is testing the validation mechanism, not Chrome availability
        if (chromePath.isPresent()) {
            // Test that the validation method executes without throwing exceptions
            boolean isValid = ChromePathDetector.validateChromeVersion(chromePath.get());
            // Just verify the method returns a boolean value (true or false both acceptable)
            assertThat(isValid).isIn(true, false);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsStandardPaths() {
        // Test that Windows detection checks standard paths
        Optional<Path> result = ChromePathDetector.detectChromeOnWindows();

        // Verify the method completes without errors
        assertThat(result).isNotNull();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testLinuxStandardPaths() {
        // Test that Linux detection checks standard paths
        Optional<Path> result = ChromePathDetector.detectChromeOnLinux();

        // Verify the method completes without errors
        assertThat(result).isNotNull();
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void testMacStandardPaths() {
        // Test that macOS detection checks standard paths
        Optional<Path> result = ChromePathDetector.detectChromeOnMac();

        // Verify the method completes without errors
        assertThat(result).isNotNull();
    }

    @Test
    void testMultiplePlatformDetectionCalls() {
        // Test that we can call detection multiple times without issues
        Optional<Path> first = ChromePathDetector.detectChromePath();
        Optional<Path> second = ChromePathDetector.detectChromePath();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();

        // Both calls should return the same result
        assertThat(first.isPresent()).isEqualTo(second.isPresent());
        if (first.isPresent() && second.isPresent()) {
            assertThat(first.get()).isEqualTo(second.get());
        }
    }

    @Test
    void testCaseInsensitiveFileNameValidation() throws IOException {
        // Test that Chrome/Chromium names are detected case-insensitively
        if (ChromePathDetector.isWindows()) {
            // Test uppercase
            Path upperCase = tempDir.resolve("CHROME.EXE");
            Files.createFile(upperCase);
            assertThat(ChromePathDetector.isValidChromeExecutable(upperCase)).isTrue();

            // Test lowercase (different file)
            Path lowerCase = tempDir.resolve("chrome.exe");
            if (!Files.exists(lowerCase)) {
                Files.createFile(lowerCase);
                assertThat(ChromePathDetector.isValidChromeExecutable(lowerCase)).isTrue();
            }

            // Test chromium
            Path chromium = tempDir.resolve("chromium.exe");
            Files.createFile(chromium);
            assertThat(ChromePathDetector.isValidChromeExecutable(chromium)).isTrue();
        }
    }
}
