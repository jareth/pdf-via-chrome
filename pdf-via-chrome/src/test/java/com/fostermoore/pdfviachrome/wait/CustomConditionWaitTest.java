package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.ExceptionDetails;
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
 * Unit tests for {@link CustomConditionWait}.
 */
class CustomConditionWaitTest {

    @Mock
    private ChromeDevToolsService mockCdp;

    @Mock
    private Runtime mockRuntime;

    @Mock
    private Evaluate mockEvaluateResult;

    @Mock
    private RemoteObject mockRemoteObject;

    @Mock
    private ExceptionDetails mockExceptionDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockCdp.getRuntime()).thenReturn(mockRuntime);
        // Setup default chain: evaluate() -> Evaluate -> getResult() -> RemoteObject -> getValue()
        when(mockEvaluateResult.getResult()).thenReturn(mockRemoteObject);
    }

    /**
     * Helper method to set up mock for condition met (returns true)
     */
    private void mockConditionMet() {
        when(mockRemoteObject.getValue()).thenReturn(true);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);
    }

    /**
     * Helper method to set up mock for condition not met (returns false)
     */
    private void mockConditionNotMet() {
        when(mockRemoteObject.getValue()).thenReturn(false);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);
    }

    // ========== Builder Tests ==========

    @Test
    void builder_withValidExpression_shouldSucceed() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.myApp !== undefined")
                .build();

        assertThat(wait.getJavascriptExpression()).isEqualTo("window.myApp !== undefined");
        assertThat(wait.getPollInterval()).isEqualTo(Duration.ofMillis(100)); // default
    }

    @Test
    void builder_withAllParameters_shouldSucceed() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("document.readyState === 'complete'")
                .pollInterval(Duration.ofMillis(200))
                .build();

        assertThat(wait.getJavascriptExpression()).isEqualTo("document.readyState === 'complete'");
        assertThat(wait.getPollInterval()).isEqualTo(Duration.ofMillis(200));
    }

    @Test
    void builder_withNullExpression_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder().expression(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript expression cannot be null or empty");
    }

    @Test
    void builder_withEmptyExpression_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder().expression(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript expression cannot be null or empty");
    }

    @Test
    void builder_withWhitespaceExpression_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder().expression("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript expression cannot be null or empty");
    }

    @Test
    void builder_withNullPollInterval_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder()
                .expression("true")
                .pollInterval(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval cannot be null");
    }

    @Test
    void builder_withNegativePollInterval_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder()
                .expression("true")
                .pollInterval(Duration.ofMillis(-100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval must be positive");
    }

    @Test
    void builder_withZeroPollInterval_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder()
                .expression("true")
                .pollInterval(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll interval must be positive");
    }

    @Test
    void builder_withoutExpression_shouldThrowException() {
        assertThatThrownBy(() -> CustomConditionWait.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JavaScript expression must be set before building");
    }

    @Test
    void builder_trimsExpression_shouldSucceed() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("  window.ready === true  ")
                .build();

        assertThat(wait.getJavascriptExpression()).isEqualTo("window.ready === true");
    }

    // ========== await() Parameter Validation Tests ==========

    @Test
    void await_withNullCdp_shouldThrowException() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThatThrownBy(() -> wait.await(null, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChromeDevToolsService cannot be null");
    }

    @Test
    void await_withNullTimeout_shouldThrowException() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThatThrownBy(() -> wait.await(mockCdp, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void await_withNegativeTimeout_shouldThrowException() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void await_withZeroTimeout_shouldThrowException() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    // ========== Condition Met Tests ==========

    @Test
    void await_whenConditionMetImmediately_shouldReturnQuickly() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.ready === true")
                .build();

        // Mock condition met
        mockConditionMet();

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
    void await_whenConditionBecomesMetAfterDelay_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.appReady === true")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // Simulate condition becoming true after 3 polls
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
    void await_shouldEvaluateCorrectExpression() throws Exception {
        String expression = "window.myApp && window.myApp.ready === true";
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression(expression)
                .build();

        mockConditionMet();

        wait.await(mockCdp, Duration.ofSeconds(10));

        // Verify the exact expression was evaluated
        verify(mockRuntime, atLeastOnce()).evaluate(expression);
    }

    // ========== Timeout Tests ==========

    @Test
    void await_whenConditionNeverBecomesTrue_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.neverReady === true")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // Condition never becomes true
        mockConditionNotMet();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("Custom condition not met");

        verify(mockRuntime).enable();
        verify(mockRuntime).disable();
    }

    @Test
    void await_whenTimeout_shouldIncludeExpressionInMessage() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.testCondition === true")
                .pollInterval(Duration.ofMillis(50))
                .build();

        mockConditionNotMet();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("window.testCondition === true");
    }

    @Test
    void await_withLongExpression_shouldTruncateInTimeoutMessage() {
        String longExpression = "a".repeat(150);
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression(longExpression)
                .pollInterval(Duration.ofMillis(50))
                .build();

        mockConditionNotMet();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("...");
    }

    // ========== Truthy/Falsy Value Tests ==========

    @Test
    void await_withTrueValue_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("true")
                .build();

        when(mockRemoteObject.getValue()).thenReturn(true);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    @Test
    void await_withFalseValue_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("false")
                .pollInterval(Duration.ofMillis(50))
                .build();

        when(mockRemoteObject.getValue()).thenReturn(false);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_withNullValue_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("null")
                .pollInterval(Duration.ofMillis(50))
                .build();

        when(mockRemoteObject.getValue()).thenReturn(null);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_withZeroNumber_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("0")
                .pollInterval(Duration.ofMillis(50))
                .build();

        when(mockRemoteObject.getValue()).thenReturn(0);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_withNonZeroNumber_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("1")
                .build();

        when(mockRemoteObject.getValue()).thenReturn(1);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    @Test
    void await_withNegativeNumber_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("-1")
                .build();

        when(mockRemoteObject.getValue()).thenReturn(-1);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    @Test
    void await_withEmptyString_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("''")
                .pollInterval(Duration.ofMillis(50))
                .build();

        when(mockRemoteObject.getValue()).thenReturn("");
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_withNonEmptyString_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("'hello'")
                .build();

        when(mockRemoteObject.getValue()).thenReturn("hello");
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    @Test
    void await_withObject_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("{}")
                .build();

        when(mockRemoteObject.getValue()).thenReturn(new Object());
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    @Test
    void await_withDoubleZero_shouldTimeout() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("0.0")
                .pollInterval(Duration.ofMillis(50))
                .build();

        when(mockRemoteObject.getValue()).thenReturn(0.0);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofMillis(200)))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_withNonZeroDouble_shouldSucceed() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("3.14")
                .build();

        when(mockRemoteObject.getValue()).thenReturn(3.14);
        when(mockRuntime.evaluate(anyString())).thenReturn(mockEvaluateResult);

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );
    }

    // ========== Exception Handling Tests ==========

    @Test
    void await_whenEvaluateReturnsNull_shouldContinuePolling() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.app.ready")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls return null, then condition becomes true
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
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.app.ready")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls throw exception, then condition becomes true
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Simulated JavaScript error");
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
    void await_whenJavaScriptHasException_shouldContinuePolling() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.app.ready")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls have exception details, then condition becomes true
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            if (count < 3) {
                when(mockEvaluateResult.getExceptionDetails()).thenReturn(mockExceptionDetails);
                when(mockExceptionDetails.getText()).thenReturn("ReferenceError: app is not defined");
                return mockEvaluateResult;
            }
            when(mockEvaluateResult.getExceptionDetails()).thenReturn(null);
            when(mockRemoteObject.getValue()).thenReturn(true);
            return mockEvaluateResult;
        });

        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );

        assertThat(pollCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void await_whenResultHasNoValue_shouldContinuePolling() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.test")
                .pollInterval(Duration.ofMillis(50))
                .build();

        // First few polls have null RemoteObject, then condition becomes true
        AtomicInteger pollCount = new AtomicInteger(0);
        when(mockRuntime.evaluate(anyString())).thenAnswer(invocation -> {
            int count = pollCount.incrementAndGet();
            if (count < 3) {
                when(mockEvaluateResult.getResult()).thenReturn(null);
                return mockEvaluateResult;
            }
            when(mockEvaluateResult.getResult()).thenReturn(mockRemoteObject);
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
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.neverReady")
                .pollInterval(Duration.ofMillis(100))
                .build();

        // Condition never becomes true
        mockConditionNotMet();

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

    // ========== Static Factory Method Tests ==========

    @Test
    void staticFactoryMethod_customCondition_withDefaultPollInterval_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.customCondition("window.ready === true");

        assertThat(wait).isInstanceOf(CustomConditionWait.class);
        CustomConditionWait customWait = (CustomConditionWait) wait;
        assertThat(customWait.getJavascriptExpression()).isEqualTo("window.ready === true");
        assertThat(customWait.getPollInterval()).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    void staticFactoryMethod_customCondition_withCustomPollInterval_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.customCondition(
            "document.readyState === 'complete'",
            Duration.ofMillis(200)
        );

        assertThat(wait).isInstanceOf(CustomConditionWait.class);
        CustomConditionWait customWait = (CustomConditionWait) wait;
        assertThat(customWait.getJavascriptExpression()).isEqualTo("document.readyState === 'complete'");
        assertThat(customWait.getPollInterval()).isEqualTo(Duration.ofMillis(200));
    }

    // ========== Object Methods Tests ==========

    @Test
    void toString_shouldReturnFormattedString() {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.myApp.ready === true")
                .pollInterval(Duration.ofMillis(150))
                .build();

        String result = wait.toString();

        assertThat(result).contains("CustomConditionWait");
        assertThat(result).contains("javascriptExpression=");
        assertThat(result).contains("pollInterval=");
    }

    @Test
    void toString_withLongExpression_shouldTruncate() {
        String longExpression = "a".repeat(150);
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression(longExpression)
                .build();

        String result = wait.toString();

        assertThat(result).contains("...");
        assertThat(result.length()).isLessThan(longExpression.length() + 100);
    }

    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThat(wait).isEqualTo(wait);
    }

    @Test
    void equals_withEqualConfiguration_shouldReturnTrue() {
        CustomConditionWait wait1 = CustomConditionWait.builder()
                .expression("window.ready")
                .pollInterval(Duration.ofMillis(100))
                .build();

        CustomConditionWait wait2 = CustomConditionWait.builder()
                .expression("window.ready")
                .pollInterval(Duration.ofMillis(100))
                .build();

        assertThat(wait1).isEqualTo(wait2);
    }

    @Test
    void equals_withDifferentExpression_shouldReturnFalse() {
        CustomConditionWait wait1 = CustomConditionWait.builder().expression("expr1").build();
        CustomConditionWait wait2 = CustomConditionWait.builder().expression("expr2").build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withDifferentPollInterval_shouldReturnFalse() {
        CustomConditionWait wait1 = CustomConditionWait.builder()
                .expression("true")
                .pollInterval(Duration.ofMillis(100))
                .build();

        CustomConditionWait wait2 = CustomConditionWait.builder()
                .expression("true")
                .pollInterval(Duration.ofMillis(200))
                .build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        assertThat(wait).isNotEqualTo(null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();
        String other = "not a CustomConditionWait";

        assertThat(wait).isNotEqualTo(other);
    }

    @Test
    void hashCode_withEqualObjects_shouldReturnSameHashCode() {
        CustomConditionWait wait1 = CustomConditionWait.builder()
                .expression("window.ready")
                .pollInterval(Duration.ofMillis(100))
                .build();

        CustomConditionWait wait2 = CustomConditionWait.builder()
                .expression("window.ready")
                .pollInterval(Duration.ofMillis(100))
                .build();

        assertThat(wait1.hashCode()).isEqualTo(wait2.hashCode());
    }

    // ========== Multiple Invocations Test ==========

    @Test
    void await_multipleInvocations_shouldWorkCorrectly() throws Exception {
        CustomConditionWait wait = CustomConditionWait.builder()
                .expression("window.ready")
                .pollInterval(Duration.ofMillis(50))
                .build();

        mockConditionMet();

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
        CustomConditionWait wait = CustomConditionWait.builder().expression("true").build();

        mockConditionMet();
        doThrow(new RuntimeException("Disable failed")).when(mockRuntime).disable();

        // Should not throw exception even if disable fails
        assertThatNoException().isThrownBy(() ->
            wait.await(mockCdp, Duration.ofSeconds(10))
        );

        verify(mockRuntime).disable();
    }

    @Test
    void await_withComplexExpression_shouldEvaluateCorrectly() throws Exception {
        String complexExpression = "(function() { " +
            "var imgs = Array.from(document.images); " +
            "return imgs.length > 0 && imgs.every(function(img) { return img.complete; }); " +
            "})()";

        CustomConditionWait wait = CustomConditionWait.builder()
                .expression(complexExpression)
                .build();

        mockConditionMet();

        wait.await(mockCdp, Duration.ofSeconds(10));

        // Verify the complex expression was evaluated
        verify(mockRuntime, atLeastOnce()).evaluate(complexExpression);
    }
}
