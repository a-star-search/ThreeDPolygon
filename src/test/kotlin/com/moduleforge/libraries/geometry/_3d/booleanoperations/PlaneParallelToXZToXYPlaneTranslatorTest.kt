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

class PlaneParallelToXZToXYPlaneTranslatorTest {
   @Test
   fun translatePointFromXZPlane_YTakesZValue_XUnchanged() {
      val planeInXZ= planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(1, 0, 0), Point(1, 0, 1)))
      val translator = PlaneParallelToXZToXYPlaneTranslator(planeInXZ)
      val randomPointOnPlane = Point(-4, 0, 5)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.x(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.z(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointFromPlaneParallelToXZPlane_YTakesZValue_XUnchanged() {
      val y = 1
      val planeInXZ= planeFromOrderedPoints(listOf(Point(0, y, 0), Point(1, y, 0), Point(1, y, 1)))
      val translator = PlaneParallelToXZToXYPlaneTranslator(planeInXZ)
      val randomPointOnPlane = Point(46, y, 1)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.x(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.z(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointBackFromXYPlaneToOriginalParallelPlane_SameX_YBecomesZ_CorrectY() {
      val y = 1.0
      val planeParallelToXZ = planeFromOrderedPoints(listOf(Point(0, y, 0), Point(1, y, 0), Point(1, y, 1)))
      val translator = PlaneParallelToXZToXYPlaneTranslator(planeParallelToXZ)
      val pointInXY = Point2d(23.2, -4.0)
      val translated = translator.translateFromXYPlane(pointInXY)
      assertEquals(translated.x(), pointInXY.x, Double.MIN_VALUE)
      assertEquals(translated.y(), y, Double.MIN_VALUE)
      assertEquals(translated.z(), pointInXY.y, Double.MIN_VALUE)
      //another example with a negative z
      val planeParallelToXZ_NegY =
              planeFromOrderedPoints(listOf(Point(0, -y, 0), Point(1, -y, 0), Point(1, -y, 1)))
      val translator2 = PlaneParallelToXZToXYPlaneTranslator(planeParallelToXZ_NegY)
      val translated2 = translator2.translateFromXYPlane(pointInXY)
      assertEquals(translated2.x(), pointInXY.x, Double.MIN_VALUE)
      assertEquals(translated2.y(), -y, Double.MIN_VALUE)
      assertEquals(translated2.z(), pointInXY.y, Double.MIN_VALUE)
   }
}