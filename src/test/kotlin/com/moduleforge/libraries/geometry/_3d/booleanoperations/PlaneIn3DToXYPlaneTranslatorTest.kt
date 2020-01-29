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

/*
 * Copyright (c) 2018.  This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package com.moduleforge.libraries.geometry._3d.booleanoperations

import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaneIn3DToXYPlaneTranslatorTest {
   /** Normally we shouldn't test implementation details, however in this case
    * it is very clear-cut that translating XY, YZ or XZ parallel planes are a
    * special, much simpler case
    */
   @Test
   fun fromXYPlane_TheCorrectTranslatorTypeIsCreated(){
      val p1 = Point(0, 0, 0)
      val p2 = Point(1, 0, 0)
      val p3 = Point(1, 1, 0)
      val plane = planeFromOrderedPoints(p1, p2, p3)
      val translator = PlaneIn3DToXYPlaneTranslator.makeTranslator(plane)
      assertTrue(translator is PlaneParallelToXYToXYPlaneTranslator)
   }
   @Test
   fun fromParallelToXYPlane_TheCorrectTranslatorTypeIsCreated(){
      val p1 = Point(0, 0, 3)
      val p2 = Point(1, 0, 3)
      val p3 = Point(1, 1, 3)
      val plane = planeFromOrderedPoints(p1, p2, p3)
      val translator = PlaneIn3DToXYPlaneTranslator.makeTranslator(plane)
      assertTrue(translator is PlaneParallelToXYToXYPlaneTranslator)
   }
   @Test
   fun fromParallelToXZPlane_TheCorrectTranslatorTypeIsCreated(){
      val p1 = Point(0, 3, 0)
      val p2 = Point(1, 3, 0)
      val p3 = Point(1, 3, 1)
      val plane = planeFromOrderedPoints(p1, p2, p3)
      val translator = PlaneIn3DToXYPlaneTranslator.makeTranslator(plane)
      assertTrue(translator is PlaneParallelToXZToXYPlaneTranslator)
   }
   @Test
   fun fromParallelToYZPlane_TheCorrectTranslatorTypeIsCreated(){
      val p1 = Point(-1, 0, 0)
      val p2 = Point(-1, 1,0)
      val p3 = Point(-1, 1, 1)
      val plane = planeFromOrderedPoints(p1, p2, p3)
      val translator = PlaneIn3DToXYPlaneTranslator.makeTranslator(plane)
      assertTrue(translator is PlaneParallelToYZToXYPlaneTranslator)
   }
}