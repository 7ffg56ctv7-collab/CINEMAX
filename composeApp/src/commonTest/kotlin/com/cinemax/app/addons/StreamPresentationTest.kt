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


    @Test fun additionalSourceMetadataMatrix() {
        val cases = listOf(
            Triple("Movie MKV", "MKV", "container"),
            Triple("Movie Matroska", "MKV", "container"),
            Triple("Movie MP4", "MP4", "container"),
            Triple("Movie WebM", "WebM", "container"),
            Triple("Movie AVI", "AVI", "container"),
            Triple("Movie Blu-Ray", "Blu-ray", "release"),
            Triple("Movie BDRip", "Blu-ray", "release"),
            Triple("Movie WEB-DL", "WEB-DL", "release"),
            Triple("Movie WEBRip", "WEBRip", "release"),
            Triple("Movie HDTV", "HDTV", "release"),
            Triple("Movie DVDRip", "DVDRip", "release"),
            Triple("Movie 60fps", "60 fps", "fps"),
            Triple("Movie 50fps", "50 fps", "fps"),
            Triple("Movie 30fps", "30 fps", "fps"),
            Triple("Movie 24fps", "24 fps", "fps"),
            Triple("Movie 7.1", "7.1", "channels"),
            Triple("Movie 5.1", "5.1", "channels"),
            Triple("Movie stereo", "Stereo", "channels"),
            Triple("Movie mono", "Mono", "channels"),
            Triple("Movie Arabic subtitles", "Arabic subtitles", "subtitles"),
            Triple("Movie English subs", "English subtitles", "subtitles"),
            Triple("Movie Arabic dubbed", "Arabic dub", "dub"),
            Triple("Movie English dub", "English dub", "dub")
        )
        cases.forEach { (title, expected, category) ->
            val stream = StremioStream(title = title)
            val actual = when (category) {
                "container" -> StreamPresentation.containerFormat(stream)
                "release" -> StreamPresentation.releaseSource(stream)
                "fps" -> StreamPresentation.frameRate(stream)
                "channels" -> StreamPresentation.audioChannels(stream)
                "subtitles" -> StreamPresentation.subtitleLanguage(stream)
                else -> StreamPresentation.audioLanguage(stream)
            }
            assertEquals(expected, actual, title)
        }
    }

    @Test fun metadataMustNotMatchInsideUnrelatedWords() {
        assertNull(StreamPresentation.containerFormat(StremioStream(name = "xmp4z")))
        assertNull(StreamPresentation.releaseSource(StremioStream(name = "notwebdlhere")))
        assertNull(StreamPresentation.frameRate(StremioStream(name = "x60fpsz")))
        assertNull(StreamPresentation.subtitleLanguage(StremioStream(name = "englishsubmarine")))
        assertEquals("Unknown", StreamPresentation.sourceType(StremioStream(url = "http://remote.example/video.mp4")))
        assertEquals("Unknown", StreamPresentation.sourceType(StremioStream(url = "https://user:pass@cdn.example/video.mp4")))
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
