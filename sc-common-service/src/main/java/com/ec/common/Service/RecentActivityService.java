package com.ec.common.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * In-memory, bounded buffer of recent user activity — powers the admin "Live Activity"
 * (tail -f style) view. No DB, no disk: recording is an O(1) lock-free append. The buffer
 * is capped by {@code activity.tracking.maxEntries} and entries older than
 * {@code activity.tracking.retentionHours} are evicted lazily, so memory is bounded forever.
 * <p>
 * State is per-instance and resets on restart — this is a live view, not an audit trail.
 */
@Service
public class RecentActivityService {

    @Value("${activity.tracking.enabled:true}")
    private boolean enabled;

    @Value("${activity.tracking.maxEntries:20000}")
    private int maxEntries;

    @Value("${activity.tracking.retentionHours:24}")
    private long retentionHours;

    private final ConcurrentLinkedDeque<Activity> buffer = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong seq = new AtomicLong(0);

    public boolean isEnabled() {
        return enabled;
    }

    /** O(1) append. Skips blank/anonymous users. */
    public void record(String username, String method, String url, String tenant) {
        if (!enabled) return;
        if (username == null || username.trim().isEmpty() || "anonymousUser".equals(username)) return;

        Activity a = new Activity(seq.incrementAndGet(), username, method, url, tenant, LocalDateTime.now());
        buffer.addLast(a);
        // Cap total size — drop oldest beyond the ceiling.
        if (size.incrementAndGet() > maxEntries) {
            if (buffer.pollFirst() != null) size.decrementAndGet();
        }
    }

    /** Remove entries older than the retention window (called lazily on read). */
    private void evictExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        Activity head;
        while ((head = buffer.peekFirst()) != null && head.timestamp.isBefore(cutoff)) {
            if (buffer.pollFirst() != null) size.decrementAndGet();
        }
    }

    /** Entries with seq greater than afterSeq (i.e. new since the caller last polled). */
    public List<Activity> getSince(long afterSeq) {
        evictExpired();
        List<Activity> out = new ArrayList<>();
        for (Activity a : buffer) {
            if (a.seq > afterSeq) out.add(a);
        }
        return out;
    }

    public long getLastSeq() {
        Activity last = buffer.peekLast();
        return last != null ? last.seq : seq.get();
    }

    /** Distinct users seen within the last {@code minutes}, with their most recent activity. */
    public List<ActiveUser> getActiveUsers(int minutes) {
        evictExpired();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        Map<String, LocalDateTime> latest = new LinkedHashMap<>();
        for (Activity a : buffer) {
            if (a.timestamp.isAfter(cutoff)) {
                LocalDateTime prev = latest.get(a.username);
                if (prev == null || a.timestamp.isAfter(prev)) latest.put(a.username, a.timestamp);
            }
        }
        List<ActiveUser> out = new ArrayList<>();
        latest.forEach((u, t) -> out.add(new ActiveUser(u, t)));
        out.sort((x, y) -> y.lastActivity.compareTo(x.lastActivity));
        return out;
    }

    // ---- value types (public getters for JSON serialization) ----

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Activity {
        public final long seq;
        public final String username;
        public final String method;
        public final String url;
        public final String tenant;
        final LocalDateTime timestamp;

        Activity(long seq, String username, String method, String url, String tenant, LocalDateTime timestamp) {
            this.seq = seq;
            this.username = username;
            this.method = method;
            this.url = url;
            this.tenant = tenant;
            this.timestamp = timestamp;
        }

        public long getSeq() { return seq; }
        public String getUsername() { return username; }
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public String getTenant() { return tenant; }
        public String getTimestamp() { return timestamp.format(TS); }
    }

    public static class ActiveUser {
        public final String username;
        final LocalDateTime lastActivity;

        ActiveUser(String username, LocalDateTime lastActivity) {
            this.username = username;
            this.lastActivity = lastActivity;
        }

        public String getUsername() { return username; }
        public String getLastActivity() { return lastActivity.format(TS); }
        public long getMinutesAgo() { return ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now()); }
        public long getSecondsAgo() { return ChronoUnit.SECONDS.between(lastActivity, LocalDateTime.now()); }
    }
}
