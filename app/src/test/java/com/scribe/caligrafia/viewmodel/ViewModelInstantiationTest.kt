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
}
