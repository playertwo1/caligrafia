package com.scribe.caligrafia.learning.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do currículo caligráfico canônico (M4 — SCR-401).
 */
class CurriculumCatalogTest {

    @Test
    fun allLessons_contains18Lessons() {
        val lessons = CurriculumCatalog.ALL_LESSONS
        assertEquals(18, lessons.size)
    }

    @Test
    fun allLessons_haveUniqueNonEmptyIds() {
        val lessons = CurriculumCatalog.ALL_LESSONS
        val ids = lessons.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(lessons.all { it.id.isNotBlank() })
    }

    @Test
    fun allStages_areRepresentedInCurriculum() {
        val stages = CurriculumStage.entries
        stages.forEach { stage ->
            val stageLessons = CurriculumCatalog.getLessonsByStage(stage)
            assertTrue("O estágio ${stage.name} deve conter ao menos 2 lições", stageLessons.size >= 2)
        }
    }

    @Test
    fun lessonDurations_arePositive() {
        CurriculumCatalog.ALL_LESSONS.forEach { lesson ->
            assertTrue(lesson.recommendedMinutes in 5..30)
            assertTrue(lesson.title.isNotBlank())
            assertTrue(lesson.description.isNotBlank())
        }
    }

    @Test
    fun getLessonById_returnsCorrectLesson() {
        val lesson = CurriculumCatalog.getLessonById("lesson_s1_01_slant")
        assertNotNull(lesson)
        assertEquals("Controle de Inclinação e Pressão", lesson?.title)
        assertEquals(CurriculumStage.STAGE_1_STROKES, lesson?.stage)
    }

    @Test
    fun defaultFirstLesson_isFirstInStage1() {
        val first = CurriculumCatalog.defaultFirstLesson()
        assertEquals(CurriculumStage.STAGE_1_STROKES, first.stage)
        assertEquals("lesson_s1_01_slant", first.id)
    }
}
