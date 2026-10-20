package com.datingsocial.service;

import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.dto.VideoCallDto;
import com.datingsocial.model.Gender;
import com.datingsocial.model.GenderPreference;
import com.datingsocial.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoCallServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserActivityTracker userActivityTracker;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VideoCallService videoCallService;

    private UserProfileDto mockProfile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(videoCallService, "livekitUrl", "wss://vibly-dukzgaut.livekit.cloud");
        ReflectionTestUtils.setField(videoCallService, "livekitApiKey", "APIDEJNCYET5MCu");
        ReflectionTestUtils.setField(videoCallService, "livekitApiSecret", "1znSRh2ffVpP71Wfs5Zm1f9OgKEQy9FMuTzBoLdGG4cB");

        mockProfile = new UserProfileDto(
            1L, 2L, "Sarah", 24, Gender.FEMALE, GenderPreference.MALE,
            "Lover of travel", "Engineer", "New York", true, false, false,
            Collections.emptyList(), Collections.emptyList(), 5.0, 95, "Active now"
        );
    }

    @Test
    void testInitiateCall_Success() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(userActivityTracker.isOnline(2L)).thenReturn(true);
        when(profileService.getProfileByUserId(anyLong())).thenReturn(mockProfile);

        VideoCallDto result = videoCallService.initiateCall(1L, 2L, 100L);

        assertNotNull(result);
        assertEquals("RINGING", result.status());
        assertEquals(1L, result.callerId());
        assertEquals(2L, result.targetId());
        assertEquals(100L, result.conversationId());
        assertTrue(result.isTargetOnline());
        assertNotNull(result.token(), "LiveKit token should be generated");
        assertTrue(result.roomId().startsWith("vid_100_"));
    }

    @Test
    void testInitiateCall_TargetBusy() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(userActivityTracker.isOnline(anyLong())).thenReturn(true);
        when(profileService.getProfileByUserId(anyLong())).thenReturn(mockProfile);

        // First call between 1 and 2
        videoCallService.initiateCall(1L, 2L, 100L);

        // Second caller 3 tries to call 2
        VideoCallDto busyResult = videoCallService.initiateCall(3L, 2L, 101L);

        assertEquals("BUSY", busyResult.status());
    }

    @Test
    void testAcceptCall_Success() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(profileService.getProfileByUserId(anyLong())).thenReturn(mockProfile);

        VideoCallDto initiated = videoCallService.initiateCall(1L, 2L, 100L);
        assertNotNull(initiated);

        VideoCallDto accepted = videoCallService.acceptCall(2L, initiated.callId());
        assertNotNull(accepted);
        assertEquals("ACCEPTED", accepted.status());
        assertNotNull(accepted.token());
    }

    @Test
    void testRejectCall_Success() {
        when(userRepository.existsById(2L)).thenReturn(true);

        VideoCallDto initiated = videoCallService.initiateCall(1L, 2L, 100L);
        assertNotNull(initiated);

        videoCallService.rejectCall(2L, initiated.callId(), "Busy right now");

        VideoCallDto active = videoCallService.getActiveCall(1L);
        assertNull(active, "Active call should be cleaned up after rejection");
    }

    @Test
    void testEndCall_Success() {
        when(userRepository.existsById(2L)).thenReturn(true);

        VideoCallDto initiated = videoCallService.initiateCall(1L, 2L, 100L);
        assertNotNull(initiated);

        videoCallService.endCall(1L, initiated.callId());

        VideoCallDto active = videoCallService.getActiveCall(1L);
        assertNull(active, "Active call should be cleared after endCall");
    }
}
