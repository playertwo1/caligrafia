package com.scribe.caligrafia.guided.ui

import android.app.Application
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GuidedPracticeViewModelTest {

    private val application = Application()

    @Test
    fun testViewModelConstructor() {
        val constructor = GuidedPracticeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)

        val vm = GuidedPracticeViewModel(application)
        assertNotNull(vm)
        assertEquals(ReferenceGlyphCatalog.BASIC_SLANT.id, vm.state.value.selectedGlyph.id)
        assertEquals(PracticeStage.TRACE, vm.state.value.currentStage)
    }

    @Test
    fun testSelectGlyphResetsEvaluation() {
        val vm = GuidedPracticeViewModel(application)
        vm.selectGlyph(ReferenceGlyphCatalog.LETTER_A)

        assertEquals(ReferenceGlyphCatalog.LETTER_A.id, vm.state.value.selectedGlyph.id)
        assertNull(vm.state.value.evaluation)
        assertEquals(0, vm.state.value.strokeCount)
    }

    @Test
    fun testSelectStageUpdatesDefaultGhostLevel() {
        val vm = GuidedPracticeViewModel(application)
        assertEquals(GhostModeLevel.CLEAR, vm.state.value.ghostModeLevel)

        vm.selectStage(PracticeStage.COPY)
        assertEquals(PracticeStage.COPY, vm.state.value.currentStage)
        assertEquals(GhostModeLevel.OFF, vm.state.value.ghostModeLevel)
    }

    @Test
    fun testAdvanceProgressTransitionsThroughStages() {
        val vm = GuidedPracticeViewModel(application)
        assertEquals(PracticeStage.TRACE, vm.state.value.currentStage)

        vm.advanceProgress()
        assertEquals(PracticeStage.COPY, vm.state.value.currentStage)

        vm.advanceProgress()
        assertEquals(PracticeStage.SOLO, vm.state.value.currentStage)

        vm.advanceProgress()
        // Após SOLO, avança para o próximo glyph e volta para TRACE
        assertEquals(PracticeStage.TRACE, vm.state.value.currentStage)
        assertEquals(ReferenceGlyphCatalog.BASIC_UNDERTURN.id, vm.state.value.selectedGlyph.id)
    }
}
