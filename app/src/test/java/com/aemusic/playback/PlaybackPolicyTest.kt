package com.aemusic.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPolicyTest {
    @Test fun previous_beforeThreshold_movesToPreviousItem() {
        assertFalse(shouldRestartCurrentTrack(3_000))
    }

    @Test fun previous_afterThreshold_restartsCurrentItem() {
        assertTrue(shouldRestartCurrentTrack(3_001))
    }

    @Test fun repeatCyclesOffAllOne() {
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(Player.REPEAT_MODE_ONE))
    }

    @Test fun historyRequiresThirtySecondsForRegularTracks() {
        assertFalse(qualifiesForPlayHistory(29_999, 240_000))
        assertTrue(qualifiesForPlayHistory(30_000, 240_000))
    }

    @Test fun historyUsesHalfDurationForShortTracks() {
        assertFalse(qualifiesForPlayHistory(9_999, 20_000))
        assertTrue(qualifiesForPlayHistory(10_000, 20_000))
    }

    @Test fun seekPositionIsClampedToKnownDuration() {
        assertEquals(0L, safeSeekPosition(-1, 120_000))
        assertEquals(120_000L, safeSeekPosition(150_000, 120_000))
        assertEquals(45_000L, safeSeekPosition(45_000, null))
    }

    @Test fun restoredRepeatModeMustBeAPlayerMode() {
        assertTrue(isValidRepeatMode(Player.REPEAT_MODE_OFF))
        assertTrue(isValidRepeatMode(Player.REPEAT_MODE_ALL))
        assertTrue(isValidRepeatMode(Player.REPEAT_MODE_ONE))
        assertFalse(isValidRepeatMode(99))
    }

    @Test fun queueRemovalKeepsEveryOtherItemInOrder() {
        assertEquals(listOf("first", "third"), removeQueueItem(listOf("first", "second", "third"), 1))
    }

    @Test fun queueRemovalRefusesToCreateAnEmptyQueue() {
        assertEquals(listOf("only"), removeQueueItem(listOf("only"), 0))
    }

    @Test fun sleepTimerModesHaveCorrectDurations() {
        assertEquals(0, SleepTimerMode.Off.minutes)
        assertEquals(15, SleepTimerMode.Minutes15.minutes)
        assertEquals(30, SleepTimerMode.Minutes30.minutes)
        assertEquals(60, SleepTimerMode.Minutes60.minutes)
        assertEquals(-1, SleepTimerMode.EndOfTrack.minutes)
    }
}
