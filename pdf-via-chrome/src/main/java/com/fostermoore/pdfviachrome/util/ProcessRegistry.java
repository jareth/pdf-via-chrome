package com.fostermoore.pdfviachrome.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for tracking all active Chrome processes.
 *
 * This singleton class maintains a registry of all spawned Chrome processes with their metadata,
 * providing monitoring capabilities and emergency cleanup functionality. It helps prevent zombie
 * processes in long-running applications that generate many PDFs.
 *
 * Thread-safe for concurrent access.
 */
public class ProcessRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRegistry.class);

    /**
     * Singleton instance of the registry.
     */
    private static final ProcessRegistry INSTANCE = new ProcessRegistry();

    /**
     * Registry of active processes with their metadata.
     * Key: Process instance
     * Value: ProcessMetadata containing tracking information
     */
    private final Map<Process, ProcessMetadata> activeProcesses = new ConcurrentHashMap<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ProcessRegistry() {
        logger.debug("ProcessRegistry initialized");
    }

    /**
     * Gets the singleton instance of the ProcessRegistry.
     *
     * @return the ProcessRegistry instance
     */
    public static ProcessRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new Chrome process in the registry.
     *
     * @param process the process to register
     * @throws IllegalArgumentException if process is null
     */
    public void register(Process process) {
        if (process == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }

        ProcessMetadata metadata = new ProcessMetadata(process);
        activeProcesses.put(process, metadata);

        logger.info("Registered Chrome process (PID: {}, Total active: {})",
                   metadata.getPid(), activeProcesses.size());
        logger.debug("Process details: {}", metadata);
    }

    /**
     * Unregisters a Chrome process from the registry.
     * This should be called when the process is properly closed.
     *
     * @param process the process to unregister
     * @return true if the process was found and removed, false otherwise
     */
    public boolean unregister(Process process) {
        if (process == null) {
            return false;
        }

        ProcessMetadata metadata = activeProcesses.remove(process);

        if (metadata == null) {
            logger.debug("Attempted to unregister process that was not in registry (PID: {})",
                        getPidSafe(process));
            return false;
        }

        logger.info("Unregistered Chrome process (PID: {}, Total active: {})",
                   metadata.getPid(), activeProcesses.size());

        return true;
    }

    /**
     * Gets the count of currently active (registered) processes.
     *
     * @return the number of active processes
     */
    public int getActiveProcessCount() {
        return activeProcesses.size();
    }

    /**
     * Gets metadata for all currently active processes.
     * Returns a snapshot copy to avoid concurrent modification issues.
     *
     * @return unmodifiable list of process metadata
     */
    public List<ProcessMetadata> getActiveProcesses() {
        return Collections.unmodifiableList(new ArrayList<>(activeProcesses.values()));
    }

    /**
     * Gets metadata for a specific process.
     *
     * @param process the process to get metadata for
     * @return the metadata, or null if process is not registered
     */
    public ProcessMetadata getMetadata(Process process) {
        return activeProcesses.get(process);
    }

    /**
     * Checks if a process is currently registered.
     *
     * @param process the process to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(Process process) {
        return process != null && activeProcesses.containsKey(process);
    }

    /**
     * Performs a health check on all registered processes and removes any that are no longer alive.
     * This helps detect orphaned process entries.
     *
     * @return the number of dead processes that were cleaned up
     */
    public int performHealthCheck() {
        logger.debug("Performing health check on {} registered processes", activeProcesses.size());

        int cleaned = 0;
        Iterator<Map.Entry<Process, ProcessMetadata>> iterator = activeProcesses.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Process, ProcessMetadata> entry = iterator.next();
            Process process = entry.getKey();
            ProcessMetadata metadata = entry.getValue();

            if (!process.isAlive()) {
                logger.info("Health check detected dead process (PID: {}), removing from registry",
                           metadata.getPid());
                iterator.remove();
                cleaned++;
            }
        }

        if (cleaned > 0) {
            logger.info("Health check cleaned up {} dead process(es). Remaining active: {}",
                       cleaned, activeProcesses.size());
        } else {
            logger.debug("Health check completed. All {} processes are alive", activeProcesses.size());
        }

        return cleaned;
    }

    /**
     * Emergency cleanup: forcefully terminates all registered Chrome processes.
     * This should only be used in emergency situations or application shutdown.
     *
     * WARNING: This will forcibly kill all Chrome processes tracked by this registry.
     *
     * @return the number of processes that were terminated
     */
    public int cleanupAll() {
        logger.warn("Emergency cleanup initiated for {} active processes", activeProcesses.size());

        int terminated = 0;

        for (Map.Entry<Process, ProcessMetadata> entry : activeProcesses.entrySet()) {
            Process process = entry.getKey();
            ProcessMetadata metadata = entry.getValue();

            if (process.isAlive()) {
                logger.warn("Forcibly terminating Chrome process (PID: {})", metadata.getPid());
                process.destroyForcibly();
                terminated++;
            }
        }

        // Clear the registry
        activeProcesses.clear();

        logger.warn("Emergency cleanup completed. Terminated {} process(es)", terminated);

        return terminated;
    }

    /**
     * Clears the registry without terminating processes.
     * This is primarily useful for testing.
     */
    void clearForTesting() {
        logger.debug("Clearing registry for testing (was {} processes)", activeProcesses.size());
        activeProcesses.clear();
    }

    /**
     * Safely gets the PID of a process, returning "unknown" if not available.
     *
     * @param process the process
     * @return the PID as a string, or "unknown"
     */
    private String getPidSafe(Process process) {
        try {
            return String.valueOf(process.pid());
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Metadata about a tracked Chrome process.
     */
    public static class ProcessMetadata {
        private final long pid;
        private final Instant startTime;
        private final boolean isAliveAtRegistration;

        /**
         * Creates metadata for a process.
         *
         * @param process the process to track
         */
        public ProcessMetadata(Process process) {
            this.pid = getPidOrZero(process);
            this.startTime = Instant.now();
            this.isAliveAtRegistration = process.isAlive();
        }

        /**
         * Gets the process ID.
         *
         * @return the PID, or 0 if unavailable
         */
        public long getPid() {
            return pid;
        }

        /**
         * Gets the time when this process was registered.
         *
         * @return the start time
         */
        public Instant getStartTime() {
            return startTime;
        }

        /**
         * Checks if the process was alive when registered.
         *
         * @return true if alive at registration time
         */
        public boolean wasAliveAtRegistration() {
            return isAliveAtRegistration;
        }

        /**
         * Gets the age of this process since registration.
         *
         * @return age in milliseconds
         */
        public long getAgeMillis() {
            return System.currentTimeMillis() - startTime.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("ProcessMetadata{pid=%d, startTime=%s, age=%dms, wasAlive=%b}",
                               pid, startTime, getAgeMillis(), isAliveAtRegistration);
        }

        /**
         * Safely gets the PID, returning 0 if not available.
         */
        private static long getPidOrZero(Process process) {
            try {
                return process.pid();
            } catch (UnsupportedOperationException e) {
                return 0L;
            }
        }
    }
}
