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

import com.moduleforge.libraries.geometry._3d.Constants.TOLERANCE
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import javax.vecmath.Point2d

/**
 * What I call "sticks" for lack of a better name, is when we remove a polygon from another polygon (difference operation)
 * and the polygon have one or more partially or fully coincidental segments.
 *
 * Sometimes that result in a difference polygon with "sticks" or "arms" that need to be broken off.
 *
 * Based on tests with JTS, we make the assumption that, as along as the original polygons have matching
 * segments with maximum double precision, then the resulting difference polygon will have "arms" with points
 * that are considered contained in segments of the polygon or close enough to a vertex to be considered in
 * the same position according to the precision used by the geometry library (which is about two or three
 * significant decimals fewer than the double precision (which is, in its turn, about 15 positions).
 *
 * It will break off both arms or remove rings that create holes.
 *
 * PRECISION:
 *
 * My geometry library uses a tolerance designed for double precision on any well implemented geometric operation.
 *
 * JTS uses double precision in its algorithms. Whatever dumb stuff it does, I don't expect it to be more than
 * one order of magnitude higher than the tolerance I use for my own code.
 * And if that's the case... bad luck, I tried.
 *
 * In any case, as I said before, I can confirm that for a few tests that I have run, that resulted in polygons with
 * holes, to JTS's credit, all the points belong on segments or vertices of the figure within Geometry's tolerance.
 *
 * At the same time, because of the fact I am using a higher tolerance here, I accept the fact that it is
 * theoretically possible to be deleting correct points in this class. Although it is very improbable.
 *
 * This class is essentially inaccurate for that reason.
 *
 * Consider if as a user of this library, you should be concerned with such small differences or not at all.
 *
 */
internal object StickRemover {
   /**
    * If the first is the same as the last, then the last will be removed (not any one of either the first or the last,
    * but specifically the last).
    *
    * In other words, whether the list inputted is a closed path or not, it will return an open path list points of
    * the polygon, where it is implicit that the first and the last are connected.
    *
    * The first point of the list is not always "respected" though, if the last point ends in the middle
    * of a segment or in the position of another point, then all the previous points from the first are removed.
    *
    * As we know, if a point P' "goes back" to another point P, that is, to its same position, where there is another
    * point between P and P', then the point after P and P' are deleted from the result.
    *
    * We could either delete P or P', but by convention we choose to delete always P', for a simple reason, it makes it
    * easier to unit test.
    *
    * Other than that, the first point in the list returned shouldn't be assumed, while the order of the points is,
    * obviously, unchanged, there could be any shift in the list.
    *
    */
   fun breakOffArms(points: List<Point2d>): List<Point2d> {
      if(points.isEmpty()) return points
      assert(points.size >= 3) //no reason to pass 1 or 2 points, that is a bug
      val minusDups = removeDuplicates(points)
      /*
      less than 3 points after removing dups
      it happens sometimes with the JTS piece of shit, it means it's not a real polygon, actually another
      form of area-less "stick arm"
       */
      if(minusDups.size < 3)
         return emptyList()
      val armsBroken = breakOffArmsAtTheEnd(minusDups)
      val result = breakOffInitialArm(armsBroken)
      assert(result.size >= 3)
      return result
   }
   private fun removeDuplicates(points: List<Point2d>): List<Point2d> {
      val minusDups = points.filterIndexed { i, p -> i == 0 || inDifferentPosition(p, points[i - 1]) }
      val shouldRemoveLast = minusDups.size > 1 && inSamePosition(minusDups.first(), minusDups.last())
      return if(shouldRemoveLast) minusDups.dropLast(1) else minusDups
   }
   /**
    * Search for arms starting at the beginning of the list, but maybe some part of the beginning of the list must be cut off
    * too (that depends on the last point of the list). This function doesn't take care of that case.
    */
   private fun breakOffArmsAtTheEnd(points: List<Point2d>): List<Point2d> {
      val returningPointsRemoved = removePointThatReturnsToPath(points)
      val noPointsRemoved = returningPointsRemoved.size == points.size
      return if(noPointsRemoved) points else breakOffArmsAtTheEnd(returningPointsRemoved)
   }
   /**
    * Removes the first occurrence of a point that "returns" or goes back in the path.
    *
    * We assume no repeated points in the in put list (meaning points at the same position) and an open path
    * (ie first and last not repeated).
    *
    * This function does not attempt to break off all arms at once, it should be called
    * iteratively until no points are removed.
    *
    */
   private fun removePointThatReturnsToPath(points: List<Point2d>): List<Point2d> {
      assert(points.size >= 3)
      val indexOfSecondToLast= points.lastIndex - 1
      //check that if for any point, the next to the next point is at the same position as this point
      val upToPointWhereNextToNextIsInSamePosition = points.withIndex()
              .takeWhile { (index, point) ->
                 index >= indexOfSecondToLast || inDifferentPosition(point, points[index + 2]) }
              .map { it.value }
      var foundSuchPoint = upToPointWhereNextToNextIsInSamePosition.size < points.size
      if(foundSuchPoint) {
         val lastTaken = upToPointWhereNextToNextIsInSamePosition.last()
         val restOfList = points.dropWhile { it != lastTaken }.drop(3)
         return upToPointWhereNextToNextIsInSamePosition + restOfList
      }
      //check that if for any point, the next to the next point is between this point and the next
      val countOfPointsUpToWhereNextOfNextIsInBetween = points.withIndex()
              .takeWhile { (index, point) ->
                 index >= indexOfSecondToLast || ! isPointBetweenPoints(point, points[index + 1], points[index + 2]) }
              .count() + 1 // need to add one more
      foundSuchPoint = countOfPointsUpToWhereNextOfNextIsInBetween < points.size
      if(!foundSuchPoint)
         return points
      //need to add one more
      val upToPointWhereNextToNextIsInBetween = points.take(countOfPointsUpToWhereNextOfNextIsInBetween)
      val restOfList = points.drop(countOfPointsUpToWhereNextOfNextIsInBetween + 1) //drops the last taken and the next one
      return upToPointWhereNextToNextIsInBetween + restOfList
   }
   private fun breakOffInitialArm(points: List<Point2d>): List<Point2d> {
      assert(points.size >= 3)
      val last = points.last()
      for((index, point) in points.dropLast(2).withIndex()) {
         val otherEndOfSegment = points[index + 1]
         if(isPointBetweenPoints(point, otherEndOfSegment, last))
            return points.drop(index + 1)
      }
      return points
   }
   private fun inDifferentPosition(p1: Point2d, p2: Point2d): Boolean = !inSamePosition(p1, p2)
   private fun inSamePosition(p1: Point2d, p2: Point2d): Boolean {
      //to 3d point so I can use my own precise algorithms
      val p1As3D  = Point(p1.x, p1.y, 0.0)
      val p2As3D  = Point(p2.x, p2.y, 0.0)
      return p1As3D.epsilonEquals(p2As3D, TOLERANCE)
   }
   private fun isPointBetweenPoints(segmentEnd1: Point2d, segmentEnd2: Point2d, point: Point2d): Boolean {
      val segmentEnd1As3D = Point(segmentEnd1.x, segmentEnd1.y, 0.0)
      val segmentEnd2As3D = Point(segmentEnd2.x, segmentEnd2.y, 0.0)
      val pointAs3D = Point(point.x, point.y, 0.0)
      val segment = LineSegment(segmentEnd1As3D, segmentEnd2As3D)
      val closest = segment.getClosestPointInSegment(pointAs3D)
      return closest.epsilonEquals(pointAs3D, TOLERANCE)
   }
}