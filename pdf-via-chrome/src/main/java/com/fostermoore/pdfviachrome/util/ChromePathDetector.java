package com.fostermoore.pdfviachrome.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for automatically detecting Chrome/Chromium installation paths
 * across different operating systems (Windows, Linux, macOS).
 *
 * This class provides platform-specific detection logic to locate Chrome/Chromium
 * executables without requiring manual configuration.
 */
public class ChromePathDetector {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * Detects and returns the path to Chrome/Chromium executable.
     *
     * @return Optional containing the path to Chrome/Chromium if found, empty otherwise
     */
    public static Optional<Path> detectChromePath() {
        if (isWindows()) {
            return detectChromeOnWindows();
        } else if (isLinux()) {
            return detectChromeOnLinux();
        } else if (isMac()) {
            return detectChromeOnMac();
        }
        return Optional.empty();
    }

    /**
     * Detects Chrome on Windows by checking standard installation paths and registry.
     *
     * @return Optional containing the Chrome path if found
     */
    static Optional<Path> detectChromeOnWindows() {
        List<String> windowsPaths = Arrays.asList(
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("PROGRAMFILES") + "\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe"
        );

        // Check standard paths first
        Optional<Path> foundPath = windowsPaths.stream()
            .filter(pathStr -> pathStr != null && !pathStr.contains("null"))
            .map(Paths::get)
            .filter(ChromePathDetector::isValidChromeExecutable)
            .findFirst();

        if (foundPath.isPresent()) {
            return foundPath;
        }

        // Try to get path from registry
        return getChromPathFromWindowsRegistry();
    }

    /**
     * Attempts to retrieve Chrome path from Windows Registry.
     *
     * @return Optional containing the Chrome path from registry if found
     */
    static Optional<Path> getChromPathFromWindowsRegistry() {
        try {
            String command = "reg query \"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe\" /ve";
            Process process = Runtime.getRuntime().exec(command);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                    .filter(line -> line.trim().startsWith("(Default)") || line.trim().startsWith("(default)"))
                    .map(line -> {
                        String[] parts = line.split("REG_SZ");
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                        return null;
                    })
                    .filter(pathStr -> pathStr != null && !pathStr.isEmpty())
                    .map(Paths::get)
                    .filter(ChromePathDetector::isValidChromeExecutable)
                    .findFirst();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Detects Chrome/Chromium on Linux by checking standard installation paths
     * and using 'which' command.
     *
     * @return Optional containing the Chrome path if found
     */
    static Optional<Path> detectChromeOnLinux() {
        List<String> linuxPaths = Arrays.asList(
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/snap/bin/chromium",
            "/usr/local/bin/google-chrome",
            "/usr/local/bin/chromium",
            "/opt/google/chrome/chrome"
        );

        // Check standard paths first
        Optional<Path> foundPath = linuxPaths.stream()
            .map(Paths::get)
            .filter(ChromePathDetector::isValidChromeExecutable)
            .findFirst();

        if (foundPath.isPresent()) {
            return foundPath;
        }

        // Try using 'which' command for google-chrome
        Optional<Path> whichChrome = findExecutableWithWhich("google-chrome");
        if (whichChrome.isPresent()) {
            return whichChrome;
        }

        // Try using 'which' command for chromium
        return findExecutableWithWhich("chromium");
    }

    /**
     * Uses the 'which' command to find an executable in PATH.
     *
     * @param executableName the name of the executable to find
     * @return Optional containing the path if found
     */
    static Optional<Path> findExecutableWithWhich(String executableName) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", executableName});

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                    .filter(line -> !line.isEmpty())
                    .map(String::trim)
                    .map(Paths::get)
                    .filter(ChromePathDetector::isValidChromeExecutable)
                    .findFirst();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Detects Chrome on macOS by checking standard application paths.
     *
     * @return Optional containing the Chrome path if found
     */
    static Optional<Path> detectChromeOnMac() {
        List<String> macPaths = Arrays.asList(
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
            System.getProperty("user.home") + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            System.getProperty("user.home") + "/Applications/Chromium.app/Contents/MacOS/Chromium"
        );

        return macPaths.stream()
            .map(Paths::get)
            .filter(ChromePathDetector::isValidChromeExecutable)
            .findFirst();
    }

    /**
     * Validates that the given path points to a valid Chrome/Chromium executable.
     *
     * @param path the path to validate
     * @return true if the path is a valid Chrome executable, false otherwise
     */
    static boolean isValidChromeExecutable(Path path) {
        if (path == null || !Files.exists(path)) {
            return false;
        }

        if (!Files.isRegularFile(path)) {
            return false;
        }

        // On Unix systems, check if file is executable
        if (!isWindows() && !Files.isExecutable(path)) {
            return false;
        }

        // Validate by checking the filename contains chrome or chromium
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.contains("chrome") || fileName.contains("chromium");
    }

    /**
     * Attempts to validate Chrome executable by running --version command.
     * This is a more thorough validation but requires executing the binary.
     *
     * @param chromePath the path to Chrome executable
     * @return true if Chrome version can be successfully queried, false otherwise
     */
    public static boolean validateChromeVersion(Path chromePath) {
        if (!isValidChromeExecutable(chromePath)) {
            return false;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(chromePath.toString(), "--version")
                .redirectErrorStream(true)
                .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines()
                    .findFirst()
                    .orElse("");

                // Wait for process to complete
                process.waitFor(10, TimeUnit.SECONDS);

                // Check if output contains "Chrome" or "Chromium"
                String lowerOutput = output.toLowerCase();
                return lowerOutput.contains("chrome") || lowerOutput.contains("chromium");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // Ensure the process is destroyed to prevent resource leaks
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return true if running on Windows
     */
    static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /**
     * Checks if the current operating system is Linux.
     *
     * @return true if running on Linux
     */
    static boolean isLinux() {
        return OS_NAME.contains("nux") || OS_NAME.contains("nix");
    }

    /**
     * Checks if the current operating system is macOS.
     *
     * @return true if running on macOS
     */
    static boolean isMac() {
        return OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    }
}
