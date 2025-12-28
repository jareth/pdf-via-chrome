package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NetworkIdleWait}.
 */
class NetworkIdleWaitTest {

    @Mock
    private ChromeDevToolsService mockCdp;

    @Mock
    private Network mockNetwork;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockCdp.getNetwork()).thenReturn(mockNetwork);
    }

    // ========== Builder Tests ==========

    @Test
    void builder_withDefaultValues_shouldSucceed() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThat(wait.getQuietPeriod()).isEqualTo(Duration.ofMillis(500));
        assertThat(wait.getMaxInflightRequests()).isEqualTo(0);
    }

    @Test
    void builder_withCustomQuietPeriod_shouldSucceed() {
        Duration customPeriod = Duration.ofSeconds(2);
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .quietPeriod(customPeriod)
                .build();

        assertThat(wait.getQuietPeriod()).isEqualTo(customPeriod);
    }

    @Test
    void builder_withNullQuietPeriod_shouldThrowException() {
        assertThatThrownBy(() -> NetworkIdleWait.builder().quietPeriod(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quiet period cannot be null");
    }

    @Test
    void builder_withNegativeQuietPeriod_shouldThrowException() {
        assertThatThrownBy(() -> NetworkIdleWait.builder().quietPeriod(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quiet period must be positive");
    }

    @Test
    void builder_withZeroQuietPeriod_shouldThrowException() {
        assertThatThrownBy(() -> NetworkIdleWait.builder().quietPeriod(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quiet period must be positive");
    }

    @Test
    void builder_withCustomMaxInflightRequests_shouldSucceed() {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .maxInflightRequests(3)
                .build();

        assertThat(wait.getMaxInflightRequests()).isEqualTo(3);
    }

    @Test
    void builder_withNegativeMaxInflightRequests_shouldThrowException() {
        assertThatThrownBy(() -> NetworkIdleWait.builder().maxInflightRequests(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max inflight requests must be non-negative");
    }

    @Test
    void builder_withZeroMaxInflightRequests_shouldSucceed() {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .maxInflightRequests(0)
                .build();

        assertThat(wait.getMaxInflightRequests()).isEqualTo(0);
    }

    // ========== Await Parameter Validation Tests ==========

    @Test
    void await_withNullCdp_shouldThrowException() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThatThrownBy(() -> wait.await(null, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChromeDevToolsService cannot be null");
    }

    @Test
    void await_withNullTimeout_shouldThrowException() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThatThrownBy(() -> wait.await(mockCdp, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout cannot be null");
    }

    @Test
    void await_withNegativeTimeout_shouldThrowException() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void await_withZeroTimeout_shouldThrowException() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThatThrownBy(() -> wait.await(mockCdp, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be positive");
    }

    // ========== Network Monitoring Tests ==========
    // Note: Detailed network monitoring tests are in NetworkIdleWaitIT.java
    // These unit tests focus on parameter validation and basic behavior

    @Test
    void await_shouldEnableAndDisableNetworkDomain() throws Exception {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofMillis(100))
                .build();

        wait.await(mockCdp, Duration.ofSeconds(5));

        verify(mockNetwork).enable();
        verify(mockNetwork).disable();
    }

    @Test
    void await_withNoNetworkActivity_shouldCompleteAfterQuietPeriod() throws Exception {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofMillis(200))
                .build();

        long startTime = System.currentTimeMillis();
        wait.await(mockCdp, Duration.ofSeconds(10));
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Should complete after quiet period + small overhead
        assertThat(elapsedTime).isGreaterThanOrEqualTo(200);
        assertThat(elapsedTime).isLessThan(500);
    }

    // ========== Interruption Tests ==========

    @Test
    void await_whenInterrupted_shouldThrowInterruptedException() {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(5))
                .build();

        Thread testThread = new Thread(() -> {
            try {
                wait.await(mockCdp, Duration.ofSeconds(30));
                fail("Expected InterruptedException to be thrown");
            } catch (InterruptedException e) {
                // Expected - verify interrupt status is restored
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } catch (TimeoutException e) {
                fail("Expected InterruptedException, got TimeoutException");
            }
        });

        testThread.start();

        try {
            Thread.sleep(100);
            testThread.interrupt();
            testThread.join(2000);
        } catch (InterruptedException e) {
            fail("Test thread interrupted");
        }

        assertThat(testThread.isAlive()).isFalse();
    }

    // ========== equals, hashCode, toString Tests ==========

    @Test
    void toString_shouldReturnFormattedString() {
        NetworkIdleWait wait = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .maxInflightRequests(2)
                .build();

        String result = wait.toString();

        assertThat(result).contains("NetworkIdleWait");
        assertThat(result).contains("quietPeriod=");
        assertThat(result).contains("maxInflightRequests=2");
    }

    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThat(wait).isEqualTo(wait);
    }

    @Test
    void equals_withEqualConfiguration_shouldReturnTrue() {
        NetworkIdleWait wait1 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .maxInflightRequests(2)
                .build();
        NetworkIdleWait wait2 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .maxInflightRequests(2)
                .build();

        assertThat(wait1).isEqualTo(wait2);
    }

    @Test
    void equals_withDifferentQuietPeriod_shouldReturnFalse() {
        NetworkIdleWait wait1 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .build();
        NetworkIdleWait wait2 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(2))
                .build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withDifferentMaxInflightRequests_shouldReturnFalse() {
        NetworkIdleWait wait1 = NetworkIdleWait.builder()
                .maxInflightRequests(1)
                .build();
        NetworkIdleWait wait2 = NetworkIdleWait.builder()
                .maxInflightRequests(2)
                .build();

        assertThat(wait1).isNotEqualTo(wait2);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();

        assertThat(wait).isNotEqualTo(null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        NetworkIdleWait wait = NetworkIdleWait.builder().build();
        String other = "not a NetworkIdleWait";

        assertThat(wait).isNotEqualTo(other);
    }

    @Test
    void hashCode_withEqualObjects_shouldReturnSameHashCode() {
        NetworkIdleWait wait1 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .maxInflightRequests(2)
                .build();
        NetworkIdleWait wait2 = NetworkIdleWait.builder()
                .quietPeriod(Duration.ofSeconds(1))
                .maxInflightRequests(2)
                .build();

        assertThat(wait1.hashCode()).isEqualTo(wait2.hashCode());
    }

    // ========== Static Factory Method Tests ==========

    @Test
    void staticFactoryMethod_networkIdle_withDefaults_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.networkIdle();

        assertThat(wait).isInstanceOf(NetworkIdleWait.class);
        NetworkIdleWait networkWait = (NetworkIdleWait) wait;
        assertThat(networkWait.getQuietPeriod()).isEqualTo(Duration.ofMillis(500));
        assertThat(networkWait.getMaxInflightRequests()).isEqualTo(0);
    }

    @Test
    void staticFactoryMethod_networkIdle_withCustomValues_shouldCreateInstance() {
        WaitStrategy wait = WaitStrategy.networkIdle(Duration.ofSeconds(2), 3);

        assertThat(wait).isInstanceOf(NetworkIdleWait.class);
        NetworkIdleWait networkWait = (NetworkIdleWait) wait;
        assertThat(networkWait.getQuietPeriod()).isEqualTo(Duration.ofSeconds(2));
        assertThat(networkWait.getMaxInflightRequests()).isEqualTo(3);
    }
}
