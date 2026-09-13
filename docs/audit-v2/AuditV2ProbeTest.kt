package com.scribe.caligrafia.audit
import com.scribe.caligrafia.learning.session.*
import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.history.*
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.engine.*
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.alphabet.engine.PersonalStyleCompiler
import com.scribe.caligrafia.alphabet.model.*
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.util.Calendar
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/** Diagnostics assert observed defects; invert expectations when fixing. */
class AuditV2ProbeTest {
 private fun record(id:String,seconds:Int=60)=CompletedSessionRecord(id,"lesson","Lesson",System.currentTimeMillis(),5,seconds,0,null)
 private fun stroke(points:List<StrokePoint>)=Stroke("s",ToolType.STYLUS,points,points.first().tMs,points.last().tMs)
 @Test fun defaultTimerNeverTicks() {
  val timer=SessionTimer(); timer.startSession(CurriculumCatalog.defaultFirstLesson(),SessionDuration.MIN_5)
  Thread.sleep(1200)
  println("DEFAULT_TIMER elapsed=${timer.sessionState.value!!.totalElapsedSeconds}")
  assertEquals(0,timer.sessionState.value!!.totalElapsedSeconds);timer.cancelSession()
 }
 @Test fun finalTimerTickIsLost() {
  val timer=SessionTimer();timer.startSession(CurriculumCatalog.defaultFirstLesson(),SessionDuration.MIN_5)
  repeat(300){timer.tickOneSecond()}
  val s=timer.sessionState.value!!
  println("FINAL_TIMER elapsed=${s.totalElapsedSeconds} finished=${s.isFinished}")
  assertTrue(s.isFinished);assertEquals(299,s.totalElapsedSeconds)
 }
 @Test fun personalStyleIsRejectedEvenByItsOwnRegistry() {
  val style=PersonalStyleCompiler().compile(PersonalAlphabet())
  val e=StyleEngine();e.registerCustomStyle(style)
  assertTrue(e.getAvailableStyles().any{it.id==style.id})
  val resolved=e.getStyle(style.id)
  println("STYLE requested=${style.id} resolved=${resolved.id}")
  assertNotEquals(style.id,resolved.id)
 }
 @Test fun newUserAlreadyHasScoredEvolutionAndAlphabet()=runBlocking {
  val dir=Files.createTempDirectory("scribe-v2-seeds").toFile()
  try {
   val attempts=LocalPracticeAttemptRepository(dir).getAllAttempts()
   val a=LocalPersonalAlphabetRepository(dir.resolve("alphabet")).getAlphabet().first()
   println("SEEDS attempts=${attempts.size}, scores=${attempts.map{it.scorePercent}}, completedGlyphs=${a.completedGlyphsCount}")
   assertEquals(4,attempts.size);assertEquals(4,a.completedGlyphsCount)
  }finally{dir.deleteRecursively()}
 }
 @Test fun historyInstancesGoStaleAndCanOverwrite() {
  val dir=Files.createTempDirectory("scribe-v2-history").toFile()
  try {
   val a=LocalLearningHistoryRepository(dir);val b=LocalLearningHistoryRepository(dir)
   a.recordSession(record("a"));assertEquals(0,b.getProgressSummary().totalSessionsCompleted)
   b.recordSession(record("b"));val restored=LocalLearningHistoryRepository(dir).getProgressSummary()
   println("HISTORY after two instances write=${restored.recentSessions.map{it.sessionId}}")
   assertEquals(listOf("b"),restored.recentSessions.map{it.sessionId})
  }finally{dir.deleteRecursively()}
 }
 @Test fun evolutionSummaryUsesOnlyTwentyAndCalendarInflatesZero() {
  val dir=Files.createTempDirectory("scribe-v2-cal").toFile()
  try {
   val r=LocalLearningHistoryRepository(dir);repeat(21){r.recordSession(record("$it"))}
   val s=r.getProgressSummary();val e=CalendarConsistencyHelper.computeSummary(s.recentSessions,0,0)
   val days=CalendarConsistencyHelper.buildMonthDays(listOf(record("zero",0)))
   println("TOTALS learning=${s.totalMinutesPracticed} evolution=${e.totalMinutesPracticed}; zeroSecondCalendar=${days.sumOf{it.totalMinutesPracticed}}")
   assertEquals(21,s.totalMinutesPracticed);assertEquals(20,e.totalMinutesPracticed);assertEquals(1,days.sumOf{it.totalMinutesPracticed})
  }finally{dir.deleteRecursively()}
 }
 @Test fun compilerDiscardsCorrectDescendingSlant() {
  val dx=(-100.0/kotlin.math.tan(Math.toRadians(52.0))).toFloat()
  val st=stroke(listOf(StrokePoint(100f,0f,0),StrokePoint(100f+dx,100f,100)))
  val v=GlyphVariant("v","g",1,"v1",80f,0f,strokes=listOf(st))
  val angle=PersonalStyleCompiler().calculateAverageSlant(listOf(v))
  println("COMPILER actual52 returned=$angle");assertEquals(60f,angle,0.01f)
 }
 @Test fun replayNormalizesAwaySpeedDifference() {
  val a=stroke(listOf(StrokePoint(0f,0f,0),StrokePoint(5f,5f,500),StrokePoint(10f,10f,1000)))
  val b=stroke(listOf(StrokePoint(0f,0f,0),StrokePoint(5f,5f,1000),StrokePoint(10f,10f,2000)))
  val f=DualReplayEngine(listOf(a),listOf(b)).computeDualFrameAt(0.5f)
  println("DUAL atHalf A_last=${f.visibleStrokesA.last().points.last().tMs} B_last=${f.visibleStrokesB.last().points.last().tMs}")
  assertEquals(500L,f.visibleStrokesA.last().points.last().tMs)
 }
 @Test fun sameStraightLineFailsWithSparseSampling() {
  val g=ReferenceGlyphCatalog.BASIC_SLANT;val band=GuidelineBand(0,0f,100f,200f,300f)
  val pts=g.strokes.first().points.mapIndexed{i,p->val q=GeometricFeedbackEvaluator.mapToScreen(p,band,0f,80f);StrokePoint(q.x,q.y,i.toLong())}
  val full=GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts)),g,band,0f,80f,SlantConfig(52f))
  val sparse=GeometricFeedbackEvaluator.evaluate(listOf(stroke(listOf(pts.first(),pts.last()))),g,band,0f,80f,SlantConfig(52f))
  println("SAMPLING full=${full.scorePercent} sparseSameLine=${sparse.scorePercent}")
  assertTrue(full.isPassed);assertFalse(sparse.isPassed)
 }
 @Test fun syncAfterWriterUseHasClosedDescriptor() {
  val f=Files.createTempFile("scribe-sync",".txt").toFile()
  try {FileOutputStream(f).use{fos->OutputStreamWriter(fos).use{it.write("test");it.flush()};
   println("SYNC descriptorValid=${fos.fd.valid()} failure=${runCatching{fos.fd.sync()}.isFailure}")
   assertFalse(fos.fd.valid())
  }}finally{f.delete()}
 }
}
