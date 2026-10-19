package com.datingsocial.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserActivityTracker {

    private final Map<Long, Instant> lastActivityMap = new ConcurrentHashMap<>();

    public void recordActivity(Long userId) {
        if (userId != null && userId > 0) {
            lastActivityMap.put(userId, Instant.now());
        }
    }

    public boolean isOnline(Long userId) {
        if (userId == null) return false;
        Instant last = lastActivityMap.get(userId);
        if (last == null) return false;
        return Duration.between(last, Instant.now()).toMinutes() < 3;
    }

    public String getStatusDescription(Long userId, boolean hideOnlineStatus) {
        if (hideOnlineStatus || userId == null) {
            return "Offline";
        }
        Instant last = lastActivityMap.get(userId);
        if (last == null) {
            return "Offline";
        }
        long minutes = Duration.between(last, Instant.now()).toMinutes();
        if (minutes < 3) {
            return "Active now";
        } else if (minutes < 60) {
            return "Active " + Math.max(1, minutes) + "m ago";
        } else {
            long hours = minutes / 60;
            if (hours < 24) {
                return "Active " + hours + "h ago";
            }
            return "Offline";
        }
    }
}
