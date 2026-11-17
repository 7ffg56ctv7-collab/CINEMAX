package com.cinemax.app.addons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamPresentationTest {
    @Test fun codecRecognitionMatrix() {
        val cases = listOf(
            "Movie AV1" to "AV1",
            "Movie AV01" to "AV1",
            "Movie HEVC" to "HEVC",
            "Movie H265" to "HEVC",
            "Movie H.265" to "HEVC",
            "Movie X265" to "HEVC",
            "Movie AVC" to "H.264",
            "Movie H264" to "H.264",
            "Movie H.264" to "H.264",
            "Movie X264" to "H.264"
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, StreamPresentation.videoCodec(StremioStream(name = input)), input)
        }
        assertNull(StreamPresentation.videoCodec(StremioStream(name = "previewav1test")))
    }

    @Test fun hdrRecognitionMatrix() {
        val cases = listOf(
            "Dolby Vision" to "Dolby Vision",
            "DoVi" to "Dolby Vision",
            "DV" to "Dolby Vision",
            "HDR10+" to "HDR10+",
            "HDR10plus" to "HDR10+",
            "HDR10" to "HDR10",
            "HDR" to "HDR"
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, StreamPresentation.hdrFormat(StremioStream(title = input)), input)
        }
        assertNull(StreamPresentation.hdrFormat(StremioStream(name = "notHDR10inside")))
    }

    @Test fun audioRecognitionMatrix() {
        val cases = listOf(
            "TrueHD" to "TrueHD",
            "Atmos" to "Atmos",
            "EAC3" to "E-AC3",
            "E-AC3" to "E-AC3",
            "DD+" to "E-AC3",
            "AC3" to "AC3",
            "DD5.1" to "AC3",
            "DTS" to "DTS",
            "DTS-HD" to "DTS",
            "AAC" to "AAC"
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, StreamPresentation.audioFormat(StremioStream(name = input)), input)
        }
        assertNull(StreamPresentation.audioFormat(StremioStream(name = "No audio metadata")))
    }

    @Test fun transportAndBadges() {
        assertEquals("Torrent", StreamPresentation.sourceType(StremioStream(infoHash = "ABC")))
        assertEquals("Direct", StreamPresentation.sourceType(StremioStream(url = "https://cdn.example/video")))
        assertEquals("Direct", StreamPresentation.sourceType(StremioStream(url = "HTTP://localhost/video")))
        assertEquals("Unknown", StreamPresentation.sourceType(StremioStream()))
        assertEquals(
            listOf("4K", "Dolby Vision", "HEVC", "Atmos"),
            StreamPresentation.badges(StremioStream(name = "2160p DoVi HEVC Atmos"))
        )
        assertEquals(emptyList(), StreamPresentation.badges(StremioStream()))
    }
}
