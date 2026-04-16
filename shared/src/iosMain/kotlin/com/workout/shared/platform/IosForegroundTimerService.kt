package com.workout.shared.platform

import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.create
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosForegroundTimerService : ForegroundTimerService {

    private var onDispatch: ((TimerIntent) -> Unit)? = null
    private var silencePlayer: AVAudioPlayer? = null

    override fun start(workoutName: String, onDispatch: (TimerIntent) -> Unit) {
        this.onDispatch = onDispatch
        setupAudioSession()
        startSilenceLoop()
        setupRemoteCommands()
    }

    override fun update(state: TimerState, workoutName: String) {
        updateNowPlayingInfo(state, workoutName)
    }

    override fun stop() {
        onDispatch = null
        silencePlayer?.stop()
        silencePlayer = null
        clearNowPlayingInfo()
        clearRemoteCommands()
        deactivateAudioSession()
    }

    // ── Audio session ────────────────────────────────────────────────

    private fun setupAudioSession() {
        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null,
            )
            session.setActive(true, error = null)
        } catch (_: Exception) {
            // best-effort
        }
    }

    private fun deactivateAudioSession() {
        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (_: Exception) {
            // best-effort
        }
    }

    // ── Silent audio loop (keeps the app alive in background) ────────

    private fun startSilenceLoop() {
        val silenceData = generateSilentWav(durationSeconds = 10)
        val player = AVAudioPlayer(data = silenceData, error = null) ?: return
        player.numberOfLoops = -1   // loop forever
        player.volume = 0.01f       // nearly silent
        player.prepareToPlay()
        player.play()
        silencePlayer = player
    }

    // ── Remote Command Center (lock screen / Control Center) ─────────

    private fun setupRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()

        center.playCommand.enabled = true
        center.playCommand.addTargetWithHandler { _ ->
            onDispatch?.invoke(TimerIntent.TogglePause)
            MPRemoteCommandHandlerStatusSuccess
        }

        center.pauseCommand.enabled = true
        center.pauseCommand.addTargetWithHandler { _ ->
            onDispatch?.invoke(TimerIntent.TogglePause)
            MPRemoteCommandHandlerStatusSuccess
        }

        center.togglePlayPauseCommand.enabled = true
        center.togglePlayPauseCommand.addTargetWithHandler { _ ->
            onDispatch?.invoke(TimerIntent.TogglePause)
            MPRemoteCommandHandlerStatusSuccess
        }

        center.nextTrackCommand.enabled = true
        center.nextTrackCommand.addTargetWithHandler { _ ->
            onDispatch?.invoke(TimerIntent.SkipPhase)
            MPRemoteCommandHandlerStatusSuccess
        }

        center.previousTrackCommand.enabled = true
        center.previousTrackCommand.addTargetWithHandler { _ ->
            onDispatch?.invoke(TimerIntent.PreviousPhase)
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun clearRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.removeTarget(null)
        center.pauseCommand.removeTarget(null)
        center.togglePlayPauseCommand.removeTarget(null)
        center.nextTrackCommand.removeTarget(null)
        center.previousTrackCommand.removeTarget(null)
    }

    // ── Now Playing info ─────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun updateNowPlayingInfo(state: TimerState, workoutName: String) {
        val info = NSMutableDictionary()

        fun nsKey(key: String): NSString = NSString.create(string = key)

        // Title: workout name
        info.setObject(workoutName.ifBlank { "Workout" }, forKey = nsKey(MPMediaItemPropertyTitle))

        // Artist: current phase info
        val phaseText = when {
            state.isPrepBeforeWork -> "Prep"
            state.currentPhase?.type == PhaseType.Work ->
                state.currentPhase?.name ?: "Work"
            else -> "Rest"
        }
        info.setObject(phaseText, forKey = nsKey(MPMediaItemPropertyArtist))

        // Album: repeat label if present
        state.currentPhase?.repeatLabel?.let { label ->
            info.setObject("Set $label", forKey = nsKey(MPMediaItemPropertyAlbumTitle))
        }

        // Duration and elapsed time for progress bar on lock screen
        val duration = when {
            state.isPrepBeforeWork -> state.blockPrepDurationSeconds.toDouble()
            else -> (state.currentPhase?.durationSeconds ?: 0).toDouble()
        }
        val elapsed = (duration - state.secondsRemaining.toDouble()).coerceAtLeast(0.0)

        info.setObject(NSNumber(double = duration), forKey = nsKey(MPMediaItemPropertyPlaybackDuration))
        info.setObject(NSNumber(double = elapsed), forKey = nsKey(MPNowPlayingInfoPropertyElapsedPlaybackTime))
        info.setObject(
            NSNumber(double = if (state.isPaused) 0.0 else 1.0),
            forKey = nsKey(MPNowPlayingInfoPropertyPlaybackRate),
        )

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info as Map<Any?, *>
    }

    private fun clearNowPlayingInfo() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    // ── WAV generation (silent audio data) ───────────────────────────

    private fun generateSilentWav(durationSeconds: Int): NSData {
        val sampleRate = 8000
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples
        val fileSize = 44 + dataSize

        val bytes = ByteArray(fileSize)

        // RIFF header
        "RIFF".encodeToByteArray().copyInto(bytes, destinationOffset = 0)
        putLittleEndianInt(bytes, 4, fileSize - 8)
        "WAVE".encodeToByteArray().copyInto(bytes, destinationOffset = 8)

        // fmt chunk
        "fmt ".encodeToByteArray().copyInto(bytes, destinationOffset = 12)
        putLittleEndianInt(bytes, 16, 16)          // chunk size
        putLittleEndianShort(bytes, 20, 1)         // PCM format
        putLittleEndianShort(bytes, 22, 1)         // mono
        putLittleEndianInt(bytes, 24, sampleRate)  // sample rate
        putLittleEndianInt(bytes, 28, sampleRate)  // byte rate (sampleRate * 1 channel * 1 byte)
        putLittleEndianShort(bytes, 32, 1)         // block align
        putLittleEndianShort(bytes, 34, 8)         // bits per sample

        // data chunk
        "data".encodeToByteArray().copyInto(bytes, destinationOffset = 36)
        putLittleEndianInt(bytes, 40, dataSize)

        // Fill with silence (0x80 for unsigned 8-bit PCM)
        for (i in 44 until fileSize) {
            bytes[i] = 0x80.toByte()
        }

        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }

    private fun putLittleEndianInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun putLittleEndianShort(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
