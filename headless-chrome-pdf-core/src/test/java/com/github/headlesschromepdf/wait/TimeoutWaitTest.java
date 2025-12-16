package com.github.headlesschromepdf.wait;

import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link TimeoutWait}.
 */
class TimeoutWaitTest {

    @Mock
    private ChromeDevToolsService mockCdp;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void constructor_withValidDuration_shouldSucceed() {
        Duration duration = Duration.ofSeconds(1);
        TimeoutWait wait = new TimeoutWait(duration);

        assertThat(wait.getDuration()).isEqualTo(duration);
    }

    @Test
    void constructor_withNullDuration_shouldThrowException() {
        assertThatThrownBy(() -> new TimeoutWait(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration cannot be null");
    }

    @Test
    void constructor_withNegativeDuration_shouldThrowException() {
        assertThatThrownBy(() -> new TimeoutWait(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be positive");
    }

    @Test
    void constructor_withZeroDuration_shouldThrowException() {
        assertThatThrownBy(() -> new TimeoutWait(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be positive");
    }

    @Test
    void await_withValidParameters_shouldWaitForSpecifiedDuration() throws Exception {
        Duration duration = Duration.ofMillis(100);
        TimeoutWait wait = new TimeoutWait(duration);

        long startTime = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Allow 50ms tolerance for timing variations
        assertThat(elapsedTime).isGreaterThanOrEqualTo(100);
        assertThat(elapsedTime).isLessThan(200);
    }

    @Test
    void await_withNullCdp_shouldThrowException() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(1));

        assertThatThrownBy(() -> wait.await(null, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChromeDevToolsService cannot be null");
    }

    @Test
    void await_withNullTimeout_shouldThrowException() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(1));

        assertThatThrownBy(() -> wait.await(mockCdp, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void await_whenInterrupted_shouldThrowInterruptedException() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(5));

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
            Thread.sleep(100);
            testThread.interrupt();
            testThread.join(1000);
        } catch (InterruptedException e) {
            fail("Test thread interrupted");
        }

        assertThat(testThread.isAlive()).isFalse();
    }

    @Test
    void staticFactoryMethod_timeout_withDuration_shouldCreateInstance() {
        Duration duration = Duration.ofSeconds(3);
        WaitStrategy wait = WaitStrategy.timeout(duration);

        assertThat(wait).isInstanceOf(TimeoutWait.class);
        assertThat(((TimeoutWait) wait).getDuration()).isEqualTo(duration);
    }

    @Test
    void staticFactoryMethod_timeout_withoutDuration_shouldCreateDefaultInstance() {
        WaitStrategy wait = WaitStrategy.timeout();

        assertThat(wait).isInstanceOf(TimeoutWait.class);
        assertThat(((TimeoutWait) wait).getDuration()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void toString_shouldReturnFormattedString() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(5));
        String result = wait.toString();

        assertThat(result).contains("TimeoutWait");
        assertThat(result).contains("duration=");
        assertThat(result).contains("PT5S");
    }

    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(1));

        assertThat(wait).isEqualTo(wait);
    }

    @Test
    void equals_withEqualDuration_shouldReturnTrue() {
        TimeoutWait wait1 = new TimeoutWait(Duration.ofSeconds(1));
        TimeoutWait wait2 = new TimeoutWait(Duration.ofSeconds(1));

        assertThat(wait1).isEqualTo(wait2);
    }

    @Test
    void equals_withDifferentDuration_shouldReturnFalse() {
        TimeoutWait wait1 = new TimeoutWait(Duration.ofSeconds(1));
        TimeoutWait wait2 = new TimeoutWait(Duration.ofSeconds(2));

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(1));

        assertThat(wait).isNotEqualTo(null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        TimeoutWait wait = new TimeoutWait(Duration.ofSeconds(1));
        String other = "not a TimeoutWait";

        assertThat(wait).isNotEqualTo(other);
    }

    @Test
    void hashCode_withEqualObjects_shouldReturnSameHashCode() {
        TimeoutWait wait1 = new TimeoutWait(Duration.ofSeconds(1));
        TimeoutWait wait2 = new TimeoutWait(Duration.ofSeconds(1));

        assertThat(wait1.hashCode()).isEqualTo(wait2.hashCode());
    }

    @Test
    void await_multipleInvocations_shouldWorkCorrectly() throws Exception {
        TimeoutWait wait = new TimeoutWait(Duration.ofMillis(50));

        // First invocation
        long start1 = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsed1 = System.currentTimeMillis() - start1;

        // Second invocation
        long start2 = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsed2 = System.currentTimeMillis() - start2;

        // Both should wait approximately the same amount of time
        assertThat(elapsed1).isGreaterThanOrEqualTo(50);
        assertThat(elapsed2).isGreaterThanOrEqualTo(50);
    }
}
