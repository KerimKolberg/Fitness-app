package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksAndAnatomyTest {
    @Test
    fun linksAreStoredOnePerLineWithOptionalTitles() {
        val links = listOf(
            ExerciseLink("https://www.youtube.com/watch?v=abc123", "Nordic curl progression"),
            ExerciseLink("https://example.com/nordic-curls/"),
        )
        val text = ExerciseLinks.format(links)
        assertEquals("Nordic curl progression | https://www.youtube.com/watch?v=abc123\nhttps://example.com/nordic-curls/", text)
        assertEquals(links, ExerciseLinks.parse(text))
        assertEquals(emptyList<ExerciseLink>(), ExerciseLinks.parse("\n  \n"))
        // A title may contain the separator; the address never has spaces.
        assertEquals(ExerciseLink("https://a.io", "Push | pull"), ExerciseLinks.parse("Push | pull | https://a.io").single())
    }

    @Test
    fun linkLabels() {
        assertEquals("youtube.com/watch?v=abc", ExerciseLink("https://www.youtube.com/watch?v=abc").label)
        assertEquals("My video", ExerciseLink("https://youtu.be/abc", "My video").label)
        assertTrue(ExerciseLink("https://youtu.be/abc").isVideo)
        assertTrue(ExerciseLink("https://m.youtube.com/watch?v=abc").isVideo)
        assertFalse(ExerciseLink("https://example.com/video").isVideo)
    }

    @Test
    fun typedAddressesAreCleanedUp() {
        assertEquals("https://youtu.be/abc", ExerciseLinks.normalizeUrl("  youtu.be/abc "))
        assertEquals("http://example.com", ExerciseLinks.normalizeUrl("http://example.com"))
        assertNull(ExerciseLinks.normalizeUrl(""))
        assertNull(ExerciseLinks.normalizeUrl("not a link"))
        assertNull(ExerciseLinks.normalizeUrl("nodot"))
        assertNull(ExerciseLinks.normalizeUrl("ftp://example.com/file"))
        assertNull(ExerciseLinks.normalizeUrl("javascript://alert(1)"))
    }

    @Test
    fun youTubeSearch() {
        assertEquals(
            "https://www.youtube.com/results?search_query=Nordic+Hamstring+Curl+exercise+form",
            ExerciseLinks.youTubeSearchUrl("Nordic Hamstring Curl"),
        )
    }

    @Test
    fun musclesBelongToTheirSection() {
        assertEquals(listOf(Muscle.QUADS, Muscle.HAMSTRINGS), Muscle.forRegion(Regions.LEGS).take(2))
        assertEquals(Muscle.OTHER, Muscle.forRegion(Regions.LEGS).last())
        // A section with one muscle uses it by default; with several, "Other".
        assertEquals(Muscle.CHEST, Muscle.defaultFor(Regions.CHEST))
        assertEquals(Muscle.OTHER, Muscle.defaultFor(Regions.LEGS))
        assertEquals(Muscle.OTHER, Muscle.defaultFor(null))
        assertEquals(Muscle.HAMSTRINGS, Muscle.resolve("HAMSTRINGS", Regions.LEGS))
        assertEquals(Muscle.CHEST, Muscle.resolve("", Regions.CHEST))
    }

    @Test
    fun stylesDefaultFromSectionAndType() {
        assertEquals(TrainingStyle.ECCENTRIC, TrainingStyle.resolve("ECCENTRIC", Regions.LEGS, ExerciseType.REPS))
        assertEquals(TrainingStyle.STRENGTH, TrainingStyle.resolve("", Regions.LEGS, ExerciseType.WEIGHT_REPS))
        assertEquals(TrainingStyle.CARDIO, TrainingStyle.resolve("", Regions.CARDIO, ExerciseType.DISTANCE_TIME))
        assertEquals(TrainingStyle.SPORT, TrainingStyle.resolve("", Regions.SPORTS, ExerciseType.SESSION))
        assertEquals(TrainingStyle.HIIT, TrainingStyle.resolve("", Regions.SPORTS, ExerciseType.INTERVALS))
        assertEquals(TrainingStyle.STRENGTH, TrainingStyle.resolve("SOMETHING_NEW", null, ExerciseType.WEIGHT_REPS))
    }
}
