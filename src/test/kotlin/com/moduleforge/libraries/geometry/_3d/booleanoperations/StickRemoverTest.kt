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

import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.vecmath.Point2d

class StickRemoverTest {
   @Test
   fun removeSticks_WhenTriangle_ReturnsPolygonUnchanged() {
      val points = listOf(Point2d(0.0, 0.0), Point2d(1.0, 0.0), Point2d(1.0, 1.0))
      val armsRemoved = StickRemover.breakOffArms(points)
      assertEquals(points, armsRemoved)
   }
   @Test
   fun removeSticks_WhenQuadrilateral_ReturnsPolygonUnchanged() {
      val points = listOf(Point2d(0.0, 0.0), Point2d(1.0, 0.0), Point2d(1.0, 1.0), Point2d(0.0, 1.0))
      val armsRemoved = StickRemover.breakOffArms(points)
      assertEquals(points, armsRemoved)
   }
   /**
    * Hard to describe in words, just make a drawing to visualize this example
    */
   @Test
   fun removeSticks_Complex1_Calculated() {
      val points = listOf(
              Point2d(0.0, 0.0),
              Point2d(1.0, 0.0),
              Point2d(1.0, 1.0),
              Point2d(2.0, 2.0),
              Point2d(1.0, 1.0))
      val armsRemoved = StickRemover.breakOffArms(points)
      val expectedPointsAfterArmsRemoved = points.take(3)
      assertTrue(hasSamePointStructure(armsRemoved, expectedPointsAfterArmsRemoved))
   }
   @Test
   fun removeSticks_Complex3_Calculated() {
      val points = listOf(
              Point2d(0.0, 0.0),
              Point2d(1.0, 0.0),
              Point2d(1.0, 1.0),
              Point2d(2.0, 2.0),
              Point2d(2.0, 3.0),//one more link in the chain of stick arm
              Point2d(2.0, 2.0),
              Point2d(1.0, 1.0))
      val armsRemoved = StickRemover.breakOffArms(points)
      val expectedPointsAfterArmsRemoved = points.take(3)
      assertTrue(hasSamePointStructure(armsRemoved, expectedPointsAfterArmsRemoved))
   }
   @Test
   fun removeSticks_Complex4_Calculated() {
      val points = listOf(
              Point2d(0.0, 0.0),
              Point2d(1.0, 0.0),
              Point2d(1.0, 1.0),
              Point2d(-1.0, 1.0),
              Point2d(0.0, 1.0) )
      val armsRemoved = StickRemover.breakOffArms(points)
      val expectedPointsAfterArmsRemoved = points.take(3) + points.last()
      assertTrue(hasSamePointStructure(armsRemoved, expectedPointsAfterArmsRemoved))
   }
   @Test
   fun removeSticks_Complex5_Calculated() {
      val points = listOf(
              Point2d(0.0, 0.0),
              Point2d(1.0, 0.0),
              Point2d(1.0, 1.0),
              Point2d(0.5, 0.0) ) //goes to the middle of the first segment, so the first point will be discarded
      val armsRemoved = StickRemover.breakOffArms(points)
      val expectedPointsAfterArmsRemoved = points.takeLast(3)
      assertTrue(hasSamePointStructure(armsRemoved, expectedPointsAfterArmsRemoved))
   }
   @Test
   fun removeSticks_Complex6_Calculated() {
      val points = listOf(
              Point2d(0.0, 0.0),
              Point2d(1.0, 0.0),
              Point2d(1.0, 1.0),
              Point2d(0.0, 1.0),
              Point2d(1.0, 0.5) ) //goes to the middle of the second segment, so the first two points will be discarded
      val armsRemoved = StickRemover.breakOffArms(points)
      val expectedPointsAfterArmsRemoved = points.takeLast(3)
      assertTrue(hasSamePointStructure(armsRemoved, expectedPointsAfterArmsRemoved))
   }
   companion object {
      //do this for point 2d...
      fun hasSamePointStructure(points1: List<Point2d>, points2: List<Point2d>): Boolean {
         if(points1.size != points2.size) return false
         val indexOfFirst = points1.indexOfFirst { epsilonEquals(it, points2[0]) }
         if(indexOfFirst < 0)
            return false
         for((point2Index, point2) in points2.withIndex()){
            val indexOfPoints1 = (indexOfFirst + point2Index) % points1.size
            if(!epsilonEquals(points1[indexOfPoints1], point2))
               return false
         }
         return true
      }
   }
}