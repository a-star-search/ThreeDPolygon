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

import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.vecmath.Point2d

class PlaneParallelToXYToXYPlaneTranslatorTest {
   @Test
   fun translatePointFromXYPlane_ShouldRemainSamePoint() {
      val polygonPoints = listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 1, 0))
      val translator = PlaneParallelToXYToXYPlaneTranslator(planeFromOrderedPoints(polygonPoints))
      val randomPointOnPlane = Point(-4, 5, 0)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.x(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.y(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointFromPlaneParallelToXYPlane_SameXAndYValue() {
      val polygonPoints = listOf(Point(0, 0, 1), Point(1, 0, 1), Point(1, 1, 1))
      val translator = PlaneParallelToXYToXYPlaneTranslator(planeFromOrderedPoints(polygonPoints))
      val randomPointOnPlane = Point(46, 0, 1)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.x(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.y(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointBackFromXYPlaneToOriginalParallelPlane_SameXAndYAndCorrectZ() {
      val z = 1.0;
      val planeParallelToXY = planeFromOrderedPoints(listOf(Point(0, 0, z), Point(1, 0, z), Point(1, 1, z)))
      val translator = PlaneParallelToXYToXYPlaneTranslator(planeParallelToXY)
      val pointInXY = Point2d(23.2, -4.0)
      val translated = translator.translateFromXYPlane(pointInXY)
      assertEquals(translated.x(), pointInXY.x, Double.MIN_VALUE)
      assertEquals(translated.y(), pointInXY.y, Double.MIN_VALUE)
      assertEquals(translated.z(), z, Double.MIN_VALUE)
      //another example with a negative z
      val planeParallelToXY_NegZ =
              planeFromOrderedPoints(listOf(Point(0, 0, -z), Point(1, 0, -z), Point(1, 1, -z)))
      val translator2 = PlaneParallelToXYToXYPlaneTranslator(planeParallelToXY_NegZ)
      val translated2 = translator2.translateFromXYPlane(pointInXY)
      assertEquals(translated2.x(), pointInXY.x, Double.MIN_VALUE)
      assertEquals(translated2.y(), pointInXY.y, Double.MIN_VALUE)
      assertEquals(translated2.z(), -z, Double.MIN_VALUE)
   }
}