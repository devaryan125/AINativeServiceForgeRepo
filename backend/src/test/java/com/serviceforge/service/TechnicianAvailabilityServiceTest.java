package com.serviceforge.service;

import com.serviceforge.data.MockDataStore;
import com.serviceforge.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicianAvailabilityServiceTest {

    private MockDataStore dataStore;
    private TechnicianAvailabilityService service;

    @BeforeEach
    void setUp() {
        dataStore = new MockDataStore();
        dataStore.seed();
        service = new TechnicianAvailabilityService(dataStore);
    }

    @Test
    void booksAJobForAnAvailableTechnician() {
        LocalDateTime start = LocalDateTime.now().withHour(16).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        Job job = service.bookJob(1L, "New Customer", start, end);

        assertEquals("New Customer", job.getCustomerName());
        assertEquals(1L, job.getTechnicianId());
    }

    @Test
    void rejectsABookingAtAnIdenticalStartTimeAsAnExistingJob() {
        LocalDateTime start = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        // Technician 1 already has a seeded job starting at 09:00 (see MockDataStore.seed()).

        assertThrows(IllegalStateException.class, () -> service.bookJob(1L, "Conflicting Customer", start, end));
    }

    @Test
    void rejectsABookingForTheExactSameInterval() {
        LocalDateTime start = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);

        assertThrows(IllegalStateException.class,
                () -> service.bookJob(1L, "Identical Interval Customer", start, start.plusHours(2)));
    }

    @Test
    void rejectsABookingWithADifferentStartTimeThatOverlapsAnExistingJob() {
        LocalDateTime start = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        // Technician 1 has a seeded 09:00-11:00 job.

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.bookJob(1L, "Overlapping Customer", start, end));

        assertTrue(exception.getMessage().contains("overlapping"));
    }

    @Test
    void rejectsABookingContainedWithinAnExistingJob() {
        LocalDateTime start = LocalDateTime.now().withHour(9).withMinute(30).withSecond(0).withNano(0);

        assertThrows(IllegalStateException.class,
                () -> service.bookJob(1L, "Contained Customer", start, start.plusMinutes(30)));
    }

    @Test
    void rejectsABookingThatContainsAnExistingJob() {
        LocalDateTime start = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);

        assertThrows(IllegalStateException.class,
                () -> service.bookJob(1L, "Enclosing Customer", start, start.plusHours(4)));
    }

    @Test
    void acceptsABookingThatStartsWhenAnExistingJobEnds() {
        LocalDateTime start = LocalDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        Job job = service.bookJob(1L, "Adjacent Customer", start, end);

        assertEquals(start, job.getStartTime());
    }

    @Test
    void acceptsABookingThatEndsWhenAnExistingJobStarts() {
        LocalDateTime start = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);

        Job job = service.bookJob(1L, "Previous Customer", start, end);

        assertEquals(end, job.getEndTime());
    }

    @Test
    void allowsTheSameTimeWindowForADifferentTechnician() {
        LocalDateTime start = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        Job job = service.bookJob(3L, "Different Technician Customer", start, start.plusHours(1));

        assertEquals(3L, job.getTechnicianId());
    }

    @Test
    void rejectsAnEndTimeThatIsNotAfterTheStartTime() {
        LocalDateTime start = LocalDateTime.now().withHour(16).withMinute(0).withSecond(0).withNano(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.bookJob(1L, "Invalid Customer", start, start));
        assertThrows(IllegalArgumentException.class,
                () -> service.bookJob(1L, "Invalid Customer", start, start.minusMinutes(1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.bookJob(1L, "Invalid Customer", null, start));
        assertThrows(IllegalArgumentException.class,
                () -> service.bookJob(1L, "Invalid Customer", start, null));
    }

    @Test
    void concurrentOverlappingBookingsAllowOnlyOneToBeSaved() throws Exception {
        int requestCount = 8;
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                final int requestNumber = i;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!startTogether.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent bookings");
                    }
                    try {
                        service.bookJob(1L, "Concurrent Customer " + requestNumber, start, start.plusHours(1));
                        return true;
                    } catch (IllegalStateException conflict) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Concurrent booking tasks did not become ready");
            startTogether.countDown();

            long successfulBookings = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successfulBookings++;
                }
            }

            assertEquals(1, successfulBookings);
            assertEquals(1, dataStore.getJobsForTechnician(1L).stream()
                    .filter(job -> job.getStartTime().equals(start))
                    .count());
        } finally {
            startTogether.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void throwsForAnUnknownTechnician() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);

        assertThrows(IllegalArgumentException.class, () -> service.bookJob(999L, "Nobody", start, end));
    }
}
