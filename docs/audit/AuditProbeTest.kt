package com.scribe.caligrafia.audit
import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.ink.capture.*
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.notebook.export.PageExporter
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import javax.imageio.ImageIO

/** Diagnostic probes: assertions document BUGGY behavior at b31538c, not acceptance criteria. */
class AuditProbeTest {
 private fun stroke(points: List<StrokePoint>) = Stroke("probe",ToolType.STYLUS,points,100,200,color=0xff123456.toInt(),baseWidthPx=8f)
 @Test fun replayDropsStyle() {
  val s=stroke(listOf(StrokePoint(0f,0f,100),StrokePoint(10f,10f,200)))
  val active=StrokeReplayEngine(listOf(s)).computeFrameAt(50).activeStroke!!
  println("REPLAY color=${active.color} width=${active.baseWidthPx}")
  assertNull(active.color); assertNull(active.baseWidthPx)
 }
 @Test fun zeroSensorValuesAreLost() {
  val m=StrokeCapturePipeline::class.java.getDeclaredMethod("createPoint",Float::class.javaPrimitiveType,Float::class.javaPrimitiveType,Long::class.javaPrimitiveType,Float::class.javaPrimitiveType,Float::class.javaPrimitiveType,Float::class.javaPrimitiveType)
  m.isAccessible=true
  val p=m.invoke(StrokeCapturePipeline(),1f,2f,100L,0f,0f,0f) as StrokePoint
  println("ZERO pressure=${p.pressure} tilt=${p.tiltRad} orientation=${p.orientationRad}")
  assertNull(p.tiltRad); assertNull(p.orientationRad)
 }
 @Test fun eraserMissesBridgeBetweenEvents() {
  val repo=InMemoryStrokeRepository()
  repo.addStroke(stroke(listOf(StrokePoint(50f,-50f,100),StrokePoint(50f,50f,200))))
  val pipeline=StrokeCapturePipeline(onEraserPointsAdded={repo.eraseStrokesIntersecting(it,5f)})
  pipeline.onPointerDown(0,ToolType.ERASER,StrokePoint(0f,0f,100))
  pipeline.onPointerMove(0,listOf(StrokePoint(100f,0f,200)))
  println("ERASER remaining=${repo.count}")
  assertEquals(1,repo.count)
 }
 @Test fun failedSaveDestroysPreviousFile() {
  val dir=Files.createTempDirectory("scribe-audit-").toFile()
  try {
   val strategy=DedicatedFileStrategy(dir); val file=dir.resolve("page.scribe")
   val s=stroke(listOf(StrokePoint(0f,0f,100)))
   strategy.save(file,listOf(s)); val original=file.readBytes()
   val failure=runCatching{strategy.save(file,listOf(s.copy(id="x".repeat(70000))))}.exceptionOrNull()
   assertNotNull(failure); assertFalse(original.contentEquals(file.readBytes()))
   assertTrue(runCatching{strategy.load(file)}.isFailure)
   println("SAVE failed=${failure!!::class.java.simpleName}; old page no longer readable")
  } finally {dir.deleteRecursively()}
 }
 @Test fun tinyIncompleteAttemptPassesAndReferenceSlantDisagrees() {
  val g=ReferenceGlyphCatalog.BASIC_SLANT
  val band=GuidelineBand(0,0f,100f,200f,300f)
  val pts=g.strokes.first().points.mapIndexed { i,p -> val q=GeometricFeedbackEvaluator.mapToScreen(p,band,0f,80f); StrokePoint(q.x,q.y,100L+i) }
  val full=GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts)),g,band,0f,80f,SlantConfig(52f))
  val tiny=GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts.take(2))),g,band,0f,80f,SlantConfig(52f))
  println("EVALUATOR full=${full.scorePercent}, referenceAngle=${full.slant.measuredAngleDegrees}; tiny=${tiny.scorePercent}, passed=${tiny.isPassed}")
  assertTrue(tiny.isPassed); assertTrue(full.slant.measuredAngleDegrees!! > 70f)
 }
 @Test fun exportReturnsInvalidPngUnderStubbedGraphics() {
  val dir=Files.createTempDirectory("scribe-png-").toFile()
  try {
   val page=NotebookPage(id="p",notebookId="n")
   val file=PageExporter.exportToPng(page,emptyList(),dir.resolve("p.png"))
   val decoded=runCatching{ImageIO.read(file)}.getOrNull()
   println("PNG bytes=${file.length()}, decodable=${decoded!=null}")
   assertEquals(8L,file.length()); assertNull(decoded)
  } finally {dir.deleteRecursively()}
 }
}
