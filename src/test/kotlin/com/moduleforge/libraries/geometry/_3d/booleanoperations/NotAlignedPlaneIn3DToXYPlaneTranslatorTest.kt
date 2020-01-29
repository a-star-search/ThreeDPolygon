/*
 *    This file is part of "Origami".
 *
 *     Origami is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Origami is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Origami.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.moduleforge.libraries.geometry._3d.booleanoperations

import com.moduleforge.libraries.geometry.Geometry.almostZero
import com.moduleforge.libraries.geometry.GeometryConstants.TOLERANCE_EPSILON
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import com.moduleforge.libraries.geometry._3d.booleanoperations.NotAlignedPlaneIn3DToXYPlaneTranslator.Companion.maxConstantCoordinateSelector
import com.moduleforge.libraries.geometry._3d.booleanoperations.NotAlignedPlaneIn3DToXYPlaneTranslator.Companion.randomVectorPerpendicularTo
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom
import javax.vecmath.Point2d

/**
 * Some of these tests reference parallel planes to coordinate axis
 * in variables used to test functions,
 * even though the class under test doesn't work this parallel planes.
 * That's because it makes testing easier than with random planes.
 */
class NotAlignedPlaneIn3DToXYPlaneTranslatorTest {
   @Test
   fun testPlaneNormalAndLineByReferencePoint_ShouldHaveSameDirection() {
      val randomPlane = planeFromOrderedPoints(listOf(Point(1, 2, 3), Point(-6, 0, 1), Point(3, -9, 12)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(randomPlane)
      val lineDirection = translator.lineByReferencePoint.direction
      val planeNormal = translator.plane.normal
      val angle = lineDirection.angle(planeNormal)
      assertTrue(almostZero(angle))
   }
   @Test
   fun testCoordinateSelector_WhenXYPlane_ShouldSelectZCoordinate(){
      val plane = planeFromOrderedPoints(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      val coordSelector = maxConstantCoordinateSelector(plane)
      val v = Vector(1, 2, 3)
      assertThat(coordSelector(v), `is`(v.z()))
   }
   @Test
   fun whenParallelToXZPlane_ShouldSelectYCoordinate(){
      val plane = planeFromOrderedPoints(Point(0, 2, 0), Point(1, 2, 0), Point(1, 2, 1))
      val coordSelector = maxConstantCoordinateSelector(plane)
      val v = Vector(1, 2, 3)
      assertThat(coordSelector(v), `is`(v.y()))
   }
   @Test
   fun randomPerpendicularVector_TestVectorsAlongAxes_ShouldBeANormalAndDotProductEqualZero(){
      //vectors alongs axes, all 6 directions, different lengths
      val alongAxes = listOf( Vector(1, 0, 0), Vector(-2, 0, 0), Vector(0, 3, 0),
              Vector(0, -4, 0), Vector(0, 0, 5), Vector(0, 0, -6) )
      for(alongAxis in alongAxes) {
         val perpendicular = randomVectorPerpendicularTo(alongAxis)
         val dotProduct = alongAxis.dot(perpendicular)
         assertEquals(1.0, perpendicular.length(), TOLERANCE_EPSILON)
         assertTrue(almostZero(dotProduct))
      }
   }
   @Test
   fun randomPerpendicularVector_TestRandomVectors_ShouldBeANormalAndDotProductEqualZero(){
      val iterations = 100
      for(i in 0..iterations){
         val v = randomVector()
         val perpendicular = randomVectorPerpendicularTo(v)
         val dotProduct = v.dot(perpendicular)
         assertEquals(1.0, perpendicular.length(), TOLERANCE_EPSILON)
         assertTrue(almostZero(dotProduct))
      }
   }
   @Test
   fun testReferencePointAndReferenceVectorAreOnPlane(){
      val iterations = 100
      for(i in 0..iterations){
         val plane = makeRandomPlane()
         val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
         assertTrue(plane.contains(translator.referencePoint))
         assertTrue(plane.contains(translator.referencePoint.translate(translator.referenceVector)))
      }
   }
   @Test
   fun testReferenceVectorIsNormal(){
      val iterations = 100
      for(i in 0..iterations){
         val plane = makeRandomPlane()
         val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
         assertEquals(1.0, translator.referenceVector.length(), TOLERANCE_EPSILON)
      }
   }
   @Test
   fun translatePointsFromXYPlane_PointsShouldEndUpInCreationalPlane(){
      val iterations = 100
      for(i in 0..iterations){
         val plane = makeRandomPlane()
         val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
         //these points are in different 2d quarters and on axes and have random lengths.
         //they are representative of any random point and edge cases
         val testPoints = listOf(
                 Point2d(0.0, 0.0), Point2d(10.0, 0.0), Point2d(0.0, 10.0), Point2d(-10.0, 0.0), Point2d(0.0, -10.0),
                 Point2d(1.3, 2.8), Point2d(5.1, -1.3), Point2d(-9.4, -7.0), Point2d(-20.0, 7.9) )
         testPoints.forEach {
            val translated = translator.translateFromXYPlane(it)
            assertTrue(plane.contains(translated))
         }
      }
   }
   /** see the plane of this test, it's easy to visualize, but difficult to put in words
    *  let's say it's the XY plane rotated 45 around Y
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case1(){
      val plane = planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(1, 0, 1), Point(1, 1, 1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane = listOf(Point(2, 3, 2), Point(2, -3, 2), Point(-2, 3, -2), Point(-2, -3, -2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   /** same as before but XY plane rotated in the opposite direction
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case2(){
      val plane = planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(1, 0, -1), Point(1, 1, -1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane = listOf(Point(2, 3, -2), Point(2, -3, -2), Point(-2, 3, 2), Point(-2, -3, 2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   /** same as case #2, but with plane looking at the opposite direction
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case3(){
      val plane = planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(1, 1, -1), Point(1, 0, -1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane = listOf(Point(2, 3, -2), Point(2, -3, -2), Point(-2, 3, 2), Point(-2, -3, 2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   /** same as case #1, but with different, although theoretically equivalent, plane creation point
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case4(){
      val plane = planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(1, -3, 1), Point(1, 0, 1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane = listOf(Point(2, 3, 2), Point(2, -3, 2), Point(-2, 3, -2), Point(-2, -3, -2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   /** As case #1 but shifted along x axis
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case1_ShiftedAlongX(){
      val plane = planeFromOrderedPoints(listOf(Point(1, 0, 0), Point(2, 0, 1), Point(2, 1, 1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane =
              listOf(Point(3, 3, 2), Point(3, -3, 2), Point(-1, 3, -2), Point(-1, -3, -2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   /** As case #1 but shifted along x axis and y axis
    * */
   @Test
   fun pointsOnPlane_TranslatedAndBack_ShouldResultInSamePoints__Case1_ShiftedAlongXAndAlongY(){
      val xShift = 2
      val yShift = -7
      val plane = planeFromOrderedPoints(
              listOf(Point(xShift, yShift, 0), Point(1 + xShift, yShift, 1), Point(1 + xShift, 1 + yShift, 1)))
      val translator = NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
      val somePointsOnPlane =
              listOf(Point(2 + xShift, 3 + yShift, 2), Point(2 + xShift, -3 + yShift, 2),
                      Point(-2 + xShift, 3 + yShift, -2), Point(-2 + xShift, -3 + yShift, -2))
      for(point in somePointsOnPlane){
         val translated = translator.translateToXYPlane(point)
         val andBack = translator.translateFromXYPlane(translated)
         assertTrue(andBack.epsilonEquals(point))
      }
   }
   private fun randomVector(): Vector{
      val min = 0.0
      val max = 100.0
      val thingy = ThreadLocalRandom.current()
      return Vector(thingy.nextDouble(min, max), thingy.nextDouble(min, max), thingy.nextDouble(min, max))
   }
   private fun makeRandomPlane(): Plane {
      val trianglePointSet = mutableSetOf<List<Int>>()
      val min = 0
      val max = 100
      val thingy = ThreadLocalRandom.current()
      while (trianglePointSet.size < 3)
         trianglePointSet.add(listOf(thingy.nextInt(min, max), thingy.nextInt(min, max), thingy.nextInt(min, max)))
      val trianglePointList = trianglePointSet.toList()
      val v1 = trianglePointList[0]
      val v2 = trianglePointList[1]
      val v3 = trianglePointList[2]
      return planeFromOrderedPoints(Point(v1[0], v1[1], v1[2]), Point(v2[0], v2[1], v2[2]), Point(v3[0], v3[1], v3[2]))
   }
}