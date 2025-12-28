package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ElementWait}.
 */
class ElementWaitTest {

    @Mock
    private ChromeDevToolsService mockCdp;

    @Mock
    private Runtime mockRuntime;

    @Mock
    private Evaluate mockEvaluateResult;

    @Mock
    private RemoteObject mockRemoteObject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockCdp.getRuntime()).thenReturn(mockRuntime);
        // Setup default chain: evaluate() -> Evaluate -> getResult() -> RemoteObject -> getValue()
        when(mockEvaluateResult.getResult()).thenReturn(mockRemoteObject);
    }

    /**
     * Helper method to set up mock for element found (returns true)
     */
    private void mockElementFound() {
        when(mockRemoteObject.getValue()).thenReturn(true);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);
    }

    /**
     * Helper method to set up mock for element not found (returns false)
     */
    private void mockElementNotFound() {
        when(mockRemoteObject.getValue()).thenReturn(false);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);
    }

    // ========== Builder Tests ==========

    @Test
    void builder_withValidSelector_shouldSucceed() {
        ElementWait wait = ElementWait.builder()
                .selector("#my-element")
                .build();

        assertThat(wait.getCssSelector()).isEqualTo("#my-element");
        assertThat(wait.getPollInterval()).isEqualTo(Duration.ofMillis(100)); // default
        assertThat(wait.isWaitForVisible()).isFalse(); // default
    }

    @Test
    void builder_withAllParameters_shouldSucceed() {
        ElementWait wait = ElementWait.builder()
                .selector(".my-class")
                .pollInterval(Duration.ofMillis(200))
                .waitForVisible(true)
                .build();

        assertThat(wait.getCssSelector()).isEqualTo(".my-class");
        assertThat(wait.getPollInterval()).isEqualTo(Duration.ofMillis(200));
        assertThat(wait.isWaitForVisible()).isTrue();
    }

    @Test
    void builder_withNullSelector_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder().selector(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSS selector cannot be null or empty");
    }

    @Test
    void builder_withEmptySelector_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder().selector(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSS selector cannot be null or empty");
    }

    @Test
    void builder_withWhitespaceSelector_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder().selector("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSS selector cannot be null or empty");
    }

    @Test
    void builder_withNullPollInterval_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder()
                .selector("#test")
                .pollInterval(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval cannot be null");
    }

    @Test
    void builder_withNegativePollInterval_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(-100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval must be positive");
    }

    @Test
    void builder_withZeroPollInterval_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval must be positive");
    }

    @Test
    void builder_withoutSelector_shouldThrowException() {
        assertThatThrownBy(() -> ElementWait.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CSS selector must be set before building");
    }

    @Test
    void builder_trimsSelector_shouldSucceed() {
        ElementWait wait = ElementWait.builder()
                .selector("  #my-element  ")
                .build();

        assertThat(wait.getCssSelector()).isEqualTo("#my-element");
    }

    // ========== await() Parameter Validation Tests ==========

    @Test
    void await_withNullCdp_shouldThrowException() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThatThrownBy(() -> wait.await(null, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChromeDevToolsService cannot be null");
    }

    @Test
    void await_withNullTimeout_shouldThrowException() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThatThrownBy(() -> wait.await(mockCdp, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void await_withNegativeTimeout_shouldThrowException() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void await_withZeroTimeout_shouldThrowException() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    // ========== Element Found Tests ==========

    @Test
    void await_whenElementPresentImmediately_shouldReturnQuickly() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("#my-element")
                .waitForVisible(false)
                .build();

        // Mock element found
        mockElementFound();

        long startTime = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Should return immediately (within 100ms)
        assertThat(elapsedTime).isLessThan(200);

        verify(mockRuntime).enable();
        verify(mockRuntime).disable();
        verify(mockRuntime, atLeastOnce()).evaluate(anyString());
    }

    @Test
    void await_whenElementAppearsAfterDelay_shouldSucceed() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("#delayed-element")
                .pollInterval(Duration.ofMillis(50))
                .waitForVisible(false)
                .build();

        // Simulate element appearing after 3 polls
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            when(mockRemoteObject.getValue()).thenReturn(count >= 3);
            return mockEvaluateResult;
        });

        long startTime = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Should take at least 2 poll intervals (100ms)
        assertThat(elapsedTime).isGreaterThanOrEqualTo(100);
        assertThat(pollCount.get()).isGreaterThanOrEqualTo(3);

        verify(mockRuntime).enable();
        verify(mockRuntime).disable();
    }

    @Test
    void await_withWaitForVisible_shouldUseVisibilityCheck() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("#my-element")
                .waitForVisible(true)
                .build();

        mockElementFound();

        wait.await(mockCdp, Duration.ofSeconds(10));

        // Verify that the visibility check script was used
        verify(mockRuntime, atLeastOnce()).evaluate(argThat(script ->
            script != null && script.contains("getBoundingClientRect") && script.contains("getComputedStyle")
        ));
    }

    @Test
    void await_withWaitForPresent_shouldUsePresenceCheck() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("#my-element")
                .waitForVisible(false)
                .build();

        mockElementFound();

        wait.await(mockCdp, Duration.ofSeconds(10));

        // Verify that the simple presence check was used
        verify(mockRuntime, atLeastOnce()).evaluate(argThat(script ->
            script != null && script.contains("querySelector") && !script.contains("getBoundingClientRect")
        ));
    }

    // ========== Timeout Tests ==========

    @Test
    void await_whenElementNeverAppears_shouldTimeout() {
        ElementWait wait = ElementWait.builder()
                .selector("#nonexistent-element")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // Element never appears
        mockElementNotFound();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("#nonexistent-element")
                .hasMessageContaining("not present");

        verify(mockRuntime).enable();
        verify(mockRuntime).disable();
    }

    @Test
    void await_whenElementNeverBecomesVisible_shouldTimeoutWithVisibleMessage() {
        ElementWait wait = ElementWait.builder()
                .selector("#hidden-element")
                .waitForVisible(true)
                .pollInterval(Duration.ofMillis(50))
                .build();

        // Element exists but is not visible
        mockElementNotFound();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("#hidden-element")
                .hasMessageContaining("not visible");

        verify(mockRuntime).enable();
        verify(mockRuntime).disable();
    }

    @Test
    void await_whenEvaluateReturnsNull_shouldContinuePolling() {
        ElementWait wait = ElementWait.builder()
                .selector("#test-element")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls return null, then element appears
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            if (count < 3) {
                return null; // Simulate null result
            }
            when(mockRemoteObject.getValue()).thenReturn(true);
            return mockEvaluateResult;
        });

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );

        assertThat(pollCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void await_whenEvaluateThrowsException_shouldContinuePolling() {
        ElementWait wait = ElementWait.builder()
                .selector("#test-element")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls throw exception, then element appears
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Simulated error");
            }
            when(mockRemoteObject.getValue()).thenReturn(true);
            return mockEvaluateResult;
        });

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );

        assertThat(pollCount.get()).isGreaterThanOrEqualTo(3);
    }

    // ========== Interruption Tests ==========

    @Test
    void await_whenInterrupted_shouldThrowInterruptedException() {
        ElementWait wait = ElementWait.builder()
                .selector("#test-element")
                .pollInterval(Duration.ofMillis(100))
                .build();

        // Element never appears
        mockElementNotFound();

        Thread testThread = new Thread(() -> {
            try {
                wait.await(mockCdp, Duration.ofSeconds(10));
                fail("Expected InterruptedException to be thrown");
            } catch (InterruptedException e) {
                // Expected - verify interrupt status is restored
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } catch (TimeoutException e) {
                fail("Expected InterruptedException, got TimeoutException");
            }
        });

        testThread.start();

        // Wait a bit then interrupt the thread
        try {
            Thread.sleep(150);
            testThread.interrupt();
            testThread.join(2000);
        } catch (InterruptedException e) {
            fail("Test thread interrupted");
        }

        assertThat(testThread.isAlive()).isFalse();
    }

    // ========== Selector Escaping Tests ==========

    @Test
    void await_withSelectorContainingSingleQuotes_shouldEscape() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("input[value='test']")
                .build();

        mockElementFound();

        wait.await(mockCdp, Duration.ofSeconds(10));

        // Verify that single quotes were escaped
        verify(mockRuntime, atLeastOnce()).evaluate(argThat(script ->
            script != null && script.contains("\\'")
        ));
    }

    // ========== Static Factory Method Tests ==========

    @Test
    void staticFactoryMethod_elementPresent_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.elementPresent("#my-element");

        assertThat(wait).isInstanceOf(ElementWait.class);
        ElementWait elementWait = (ElementWait) wait;
        assertThat(elementWait.getCssSelector()).isEqualTo("#my-element");
        assertThat(elementWait.isWaitForVisible()).isFalse();
    }

    @Test
    void staticFactoryMethod_elementVisible_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.elementVisible(".my-class");

        assertThat(wait).isInstanceOf(ElementWait.class);
        ElementWait elementWait = (ElementWait) wait;
        assertThat(elementWait.getCssSelector()).isEqualTo(".my-class");
        assertThat(elementWait.isWaitForVisible()).isTrue();
    }

    @Test
    void staticFactoryMethod_element_withCustomConfiguration_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.element(
            "#custom-element",
            Duration.ofMillis(200),
            true
        );

        assertThat(wait).isInstanceOf(ElementWait.class);
        ElementWait elementWait = (ElementWait) wait;
        assertThat(elementWait.getCssSelector()).isEqualTo("#custom-element");
        assertThat(elementWait.getPollInterval()).isEqualTo(Duration.ofMillis(200));
        assertThat(elementWait.isWaitForVisible()).isTrue();
    }

    // ========== Object Methods Tests ==========

    @Test
    void toString_shouldReturnFormattedString() {
        ElementWait wait = ElementWait.builder()
                .selector("#my-element")
                .pollInterval(Duration.ofMillis(150))
                .waitForVisible(true)
                .build();

        String result = wait.toString();

        assertThat(result).contains("ElementWait");
        assertThat(result).contains("cssSelector='#my-element'");
        assertThat(result).contains("pollInterval=");
        assertThat(result).contains("waitForVisible=true");
    }

    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThat(wait).isEqualTo(wait);
    }

    @Test
    void equals_withEqualConfiguration_shouldReturnTrue() {
        ElementWait wait1 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(100))
                .waitForVisible(false)
                .build();

        ElementWait wait2 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(100))
                .waitForVisible(false)
                .build();

        assertThat(wait1).isEqualTo(wait2);
    }

    @Test
    void equals_withDifferentSelector_shouldReturnFalse() {
        ElementWait wait1 = ElementWait.builder().selector("#test1").build();
        ElementWait wait2 = ElementWait.builder().selector("#test2").build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withDifferentPollInterval_shouldReturnFalse() {
        ElementWait wait1 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(100))
                .build();

        ElementWait wait2 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(200))
                .build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withDifferentWaitForVisible_shouldReturnFalse() {
        ElementWait wait1 = ElementWait.builder()
                .selector("#test")
                .waitForVisible(false)
                .build();

        ElementWait wait2 = ElementWait.builder()
                .selector("#test")
                .waitForVisible(true)
                .build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        assertThat(wait).isNotEqualTo(null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        ElementWait wait = ElementWait.builder().selector("#test").build();
        String other = "not an ElementWait";

        assertThat(wait).isNotEqualTo(other);
    }

    @Test
    void hashCode_withEqualObjects_shouldReturnSameHashCode() {
        ElementWait wait1 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(100))
                .waitForVisible(true)
                .build();

        ElementWait wait2 = ElementWait.builder()
                .selector("#test")
                .pollInterval(Duration.ofMillis(100))
                .waitForVisible(true)
                .build();

        assertThat(wait1.hashCode()).isEqualTo(wait2.hashCode());
    }

    // ========== Multiple Invocations Test ==========

    @Test
    void await_multipleInvocations_shouldWorkCorrectly() throws Exception {
        ElementWait wait = ElementWait.builder()
                .selector("#test-element")
                .pollInterval(Duration.ofMillis(50))
                .build();

        mockElementFound();

        // First invocation
        wait.await(mockCdp, Duration.ofSeconds(10));

        // Second invocation
        wait.await(mockCdp, Duration.ofSeconds(10));

        // Both should succeed
        verify(mockRuntime, times(2)).enable();
        verify(mockRuntime, times(2)).disable();
    }

    // ========== Edge Cases ==========

    @Test
    void await_withRuntimeDisableFailure_shouldNotThrowException() throws Exception {
        ElementWait wait = ElementWait.builder().selector("#test").build();

        mockElementFound();
        doThrow(new RuntimeException("Disable failed")).when(mockRuntime).disable();

        // Should not throw exception even if disable fails
        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );

        verify(mockRuntime).disable();
    }
}
