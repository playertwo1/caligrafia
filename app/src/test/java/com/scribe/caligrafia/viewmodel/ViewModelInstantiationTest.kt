package com.scribe.caligrafia.viewmodel

import android.app.Application
import com.scribe.caligrafia.inspector.viewmodel.StylusLabViewModel
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViewModelInstantiationTest {

    @Test
    fun verifyStylusLabViewModelHasSingleApplicationConstructor() {
        val constructor = StylusLabViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyNotebookPracticeViewModelHasSingleApplicationConstructor() {
        val constructor = NotebookPracticeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyGuidedPracticeViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyLearningViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.learning.ui.LearningViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyEvolutionViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.evolution.ui.EvolutionViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyAlphabetViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.alphabet.ui.AlphabetViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyTeacherViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.teacher.ui.TeacherViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun verifyExpansionsViewModelHasSingleApplicationConstructor() {
        val constructor = com.scribe.caligrafia.expansions.ui.ExpansionsViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }
}
