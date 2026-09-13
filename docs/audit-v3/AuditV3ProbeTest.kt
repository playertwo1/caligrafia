package com.scribe.caligrafia.audit
import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.teacher.engine.*
import com.scribe.caligrafia.teacher.model.*
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.expansions.backup.*
import com.scribe.caligrafia.expansions.signature.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Assert.*
import java.io.*
import java.util.zip.*

/** Diagnostic probes: passing means the documented defect was reproduced, NOT acceptance. */
class AuditV3ProbeTest {
 @get:Rule val temp = TemporaryFolder()
 private fun stroke(points:List<StrokePoint>)=Stroke("s",ToolType.STYLUS,points,0L,100L)
 private fun p(x:Float,y:Float,t:Long)=StrokePoint(x,y,t,null,null,null)
 private fun zip(vararg entries:Pair<String,String>):ByteArray {
  val out=ByteArrayOutputStream(); ZipOutputStream(out).use { z -> entries.forEach{(name,value)->z.putNextEntry(ZipEntry(name));z.write(value.toByteArray());z.closeEntry()} };return out.toByteArray()
 }
 @Test fun noWritingProducesMaturityAndMasteryPrescription() {
  val d=MotorDiagnosticEngine().diagnoseAttempts(emptyList())
  val prescription=CoachingCurriculumGenerator().generatePrescription(d)
  assertEquals(70f,d.overallScore,0f);assertTrue(prescription.rationale.contains("superior"))
  println("EMPTY score=${d.overallScore} maturity=${d.maturityLevel} rationale=${prescription.rationale}")
 }
 @Test fun everyPrescribedFocusIsMissingFromRealCatalog() {
  val base=MotorDiagnosticEngine().diagnoseAttempts(emptyList())
  val prescriptions=(listOf<BiomechanicalDimension?>(null)+BiomechanicalDimension.values().toList()).map { CoachingCurriculumGenerator().generatePrescription(base.copy(primaryWeakness=it)) }
  prescriptions.forEach{assertNull("Unexpected catalog match: ${it.focusExerciseId}",ReferenceGlyphCatalog.findById(it.focusExerciseId))}
  println("MISSING_FOCUS ${prescriptions.map{it.focusExerciseId}}")
 }
 @Test fun absentSensorIsReportedAsMeasuredGoodPressure() {
  val d=MotorDiagnosticEngine().diagnoseStrokes(listOf(stroke(listOf(p(0f,0f,0),p(20f,30f,100)))))
  val pressure=d.dimensions.getValue(BiomechanicalDimension.PRESSURE_CONTROL)
  assertEquals(0.5f,pressure.observedValue,0f);assertEquals(EvaluationStatus.GOOD,pressure.status)
 }
 @Test fun backupOmitsActualProductionPaths() {
  val root=temp.newFolder("source")
  val paths=listOf("personal_alphabet/strokes/a.scribe","attempts/a.scribe","learning_history.json","custom_fonts/a.ttf","teacher/diagnostic.json")
  paths.forEach{File(root,it).apply{parentFile.mkdirs();writeText("fixture")}}
  val out=ByteArrayOutputStream();val summary=ScribeBackupManager(root).exportBackup(out)
  val names=mutableListOf<String>();ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z -> var e=z.nextEntry;while(e!=null){names.add(e.name);e=z.nextEntry} }
  assertFalse(names.contains(paths[0]));assertFalse(names.contains(paths[1]));assertFalse(names.contains(paths[2]));assertFalse(names.contains(paths[3]));assertTrue(names.contains(paths[4]));assertFalse(summary.manifest.hasTeacherDiagnostic)
  println("BACKUP_ENTRIES $names teacherFlag=${summary.manifest.hasTeacherDiagnostic}")
 }
 @Test fun exportedZipHasNoCentralDirectory() {
  val root=temp.newFolder("zipSource");File(root,"notebooks").mkdirs();File(root,"notebooks/a").writeText("ink")
  val f=temp.newFile("export.scribepack");FileOutputStream(f).use { ScribeBackupManager(root).exportBackup(it) }
  val failure=runCatching{ZipFile(f).use{it.size()}}.exceptionOrNull()
  println("ZIPFILE_ERROR $failure");assertTrue(failure is ZipException)
 }
 @Test fun invalidManifestOverwritesExistingDataSuccessfully() {
  val root=temp.newFolder("restore");File(root,"notebooks").mkdirs();val data=File(root,"notebooks/page.scribe");data.writeText("original")
  val result=ScribeBackupManager(root).importBackup(ByteArrayInputStream(zip("manifest.json" to "NOT JSON", "notebooks/page.scribe" to "invalid raw data")))
  assertTrue(result.isSuccess);assertEquals("invalid raw data",data.readText())
 }
 @Test fun failedRestoreLeavesEarlierDirectoryChanged() {
  val root=temp.newFolder("partial");File(root,"notebooks").mkdirs();val data=File(root,"notebooks/page.scribe");data.writeText("original")
  File(root,"alphabet").writeText("blocking file")
  val result=ScribeBackupManager(root).importBackup(ByteArrayInputStream(zip("manifest.json" to "{}", "notebooks/page.scribe" to "changed", "alphabet/g.scribe" to "new")))
  assertFalse(result.isSuccess);assertEquals("changed",data.readText());println("PARTIAL_RESTORE success=${result.isSuccess} oldPage=${data.readText()}")
 }
 @Test fun zipSlipContainmentPredicateAllowsSiblingPrefix() {
  val root=temp.newFolder("containment");val staging=File(root,"temp_restore_123")
  val escaped=File(staging,"../temp_restore_123_other/payload").canonicalFile
  assertTrue(escaped.path.startsWith(staging.canonicalPath));assertFalse(escaped.toPath().startsWith(staging.canonicalFile.toPath()))
 }
 @Test fun baselineListAliasingAllowsClearToEraseReference() {
  val mutable=mutableListOf(stroke(listOf(p(0f,0f,0),p(10f,10f,100))))
  val baseline=SignatureAttempt(id="baseline",strokes=mutable,durationMs=100,minX=0f,minY=0f,maxX=10f,maxY=10f)
  mutable.clear();assertTrue(baseline.strokes.isEmpty())
 }
 @Test fun oppositeSignatureDiagonalsReceivePerfectConsistency() {
  val a=stroke(listOf(p(0f,0f,0),p(100f,100f,100)))
  val b=stroke(listOf(p(100f,0f,0),p(0f,100f,100)))
  val r=SignatureConsistencyEngine.evaluateConsistency(SignatureConsistencyEngine.computeMetrics(listOf(a)),SignatureConsistencyEngine.computeMetrics(listOf(b)))
  assertEquals(100f,r.repeatabilityScore,0.001f)
 }
}
