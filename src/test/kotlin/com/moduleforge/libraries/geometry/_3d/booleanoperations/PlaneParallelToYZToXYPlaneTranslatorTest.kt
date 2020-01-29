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

class PlaneParallelToYZToXYPlaneTranslatorTest {
   @Test
   fun translatePointFromYZPlane_XTakesZValue_YUnchanged() {
      val planeInYZ= planeFromOrderedPoints(listOf(Point(0, 0, 0), Point(0, 1, 0), Point(0, 1, 1)))
      val translator = PlaneParallelToYZToXYPlaneTranslator(planeInYZ)
      val randomPointOnPlane = Point(0, -4, 5)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.z(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.y(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointFromPlaneParallelToYZPlane_XTakesZValue_YUnchanged() {
      val x = 1
      val planeParallelToYZ= planeFromOrderedPoints(listOf(Point(x, 0, 0), Point(x, 1, 0), Point(x, 1, 1)))
      val translator = PlaneParallelToYZToXYPlaneTranslator(planeParallelToYZ)
      val randomPointOnPlane = Point(x, 46, 1)
      val translated = translator.translateToXYPlane(randomPointOnPlane)
      assertEquals(translated.x, randomPointOnPlane.z(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
      assertEquals(translated.y, randomPointOnPlane.y(), Double.MIN_VALUE) //should be the same value, compare within the minimum error epsilon
   }
   @Test
   fun translatePointBackFromXYPlaneToOriginalParallelPlane_SameY_XBecomesZ_CorrectX() {
      val x = 1.0
      val planeParallelToYZ= planeFromOrderedPoints(listOf(Point(x, 0, 0), Point(x, 1, 0), Point(x, 1, 1)))
      val translator = PlaneParallelToYZToXYPlaneTranslator(planeParallelToYZ)
      val pointInXY = Point2d(23.2, -4.0)
      val translated = translator.translateFromXYPlane(pointInXY)
      assertEquals(translated.x(), x, Double.MIN_VALUE)
      assertEquals(translated.y(), pointInXY.y, Double.MIN_VALUE)
      assertEquals(translated.z(), pointInXY.x, Double.MIN_VALUE)
      //another example with a negative z
      val planeParallelToYZ_NegX =
              planeFromOrderedPoints(listOf(Point(-x, 0, 0), Point(-x, 1, 0), Point(-x, 1, 1)))
      val translator2 = PlaneParallelToYZToXYPlaneTranslator(planeParallelToYZ_NegX)
      val translated2 = translator2.translateFromXYPlane(pointInXY)
      assertEquals(translated2.x(), -x, Double.MIN_VALUE)
      assertEquals(translated2.y(), pointInXY.y, Double.MIN_VALUE)
      assertEquals(translated2.z(), pointInXY.x, Double.MIN_VALUE)
   }
}