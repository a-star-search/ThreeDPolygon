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

package com.moduleforge.libraries.geometry._3d

import com.google.common.annotations.VisibleForTesting
import com.moduleforge.libraries.geometry.Geometry.areaIsAlmostZero
import com.moduleforge.libraries.geometry.Geometry.epsilonEquals
import com.moduleforge.libraries.geometry._3d.Constants.TOLERANCE
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator.convexHull
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator.difference
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator.differenceArea
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator.intersectionArea
import java.awt.Color
import java.awt.Color.WHITE
import org.locationtech.jts.geom.Coordinate as JTSCoordinate
import org.locationtech.jts.geom.Geometry as JTSGeometry
import org.locationtech.jts.geom.GeometryFactory as JTSFactory
import org.locationtech.jts.geom.MultiPolygon as JTSMultiPolygon
import org.locationtech.jts.geom.Polygon as JTSPolygon

/**
 * This class represents a CONCAVE polygon. All operations works for concave polygons, and naturally that requires
 * a special algorithm to calculate the orientation of the plane from the points (can't just take any consecutive
 * three points).
 *
 * A concave polygon in origami is useful for example, when determining the part of a face that is visible, that is, not
 * covered by other faces.
 *
 * Conversely a face in origami is always a convex polygon, but that is a consideration for a different class that
 * deals with faces.
 *
 * BOOLEAN OPERATIONS
 * In this class, in all operations of visibility such as is hidden, is covered, etc, it is strongly discouraged to pass
 * anything as parameters but polygons that share the same plane as this object. Otherwise it could lead to confusion.
 */
open class Polygon {
   open val vertices: List<Point>
   open val edges: Set<LineSegment>
   /** Plane where the normal is given by the order of the vertices of the polygon */
   open val polygonPlane: Plane
   val colors: ColorCombination
   val frontColor: Color
      get() = colors.front
   val backColor: Color
      get() = colors.back
   /** constructors with three vertices */
   constructor(v1: Point, v2: Point, v3: Point, vararg vRest: Point, frontColor: Color=DEFAULT_COLOR, backColor: Color=DEFAULT_COLOR):
           this(listOf(v1, v2, v3, *vRest), frontColor, backColor)
   constructor(v1: Point, v2: Point, v3: Point, vararg vRest: Point, colors: ColorCombination):
           this(listOf(v1, v2, v3, *vRest), colors)
   constructor(vertices: List<Point>, frontColor: Color=DEFAULT_COLOR, backColor: Color=DEFAULT_COLOR):
           this(vertices, ColorCombination(frontColor, backColor))
   constructor(vertices: List<Point>, colors: ColorCombination) {
      //I don't do a lot of checks in my code, is my project after all, shouldn't clog the code with the necessary
      // validation that comes with public libraries, and I have unit tests anyway
      if (vertices.size < 3)
         throw RuntimeException("A polygon needs at least three vertices.")
      this.colors = colors
      //apparently I shouldn't worry about intellij warnings for the following assignments (https://stackoverflow.com/questions/49162835/kotlin-how-to-override-class-variable-that-is-a-collection-as-a-collection-of-a)
      this.vertices = vertices.toList()
      this.polygonPlane = planeOfConcavePolygon(vertices)
      this.edges = makeSegments(vertices)
   }
   fun nextVertex(vertex: Point): Point = vertices[(vertices.indexOf(vertex) + 1) % vertices.size]
   fun previousVertex(vertex: Point): Point =
           vertices[(vertices.indexOf(vertex) + vertices.size - 1) % vertices.size]
   /**
    * Returns a list of the vertices in the same order but starting with the vertex passed as parameter.
    */
   fun verticesStartingWith(vertex: Point): List<Point> {
      if(!vertices.contains(vertex))
         throw IllegalArgumentException("Wrong argument.")
      val v2 = vertices + vertices
      val firstIndex = v2.indexOf(vertex)
      val lastIndex = v2.lastIndexOf(vertex)
      return v2.subList(firstIndex, lastIndex)
   }
   fun isTriangle(): Boolean = vertices.size == 3
   fun isQuadrilateral(): Boolean = vertices.size == 4
   fun isAVertex(v: Point): Boolean = vertices.contains(v)
   fun areAllVertices(vertices: List<Point>): Boolean = vertices.fold(true){ result, pos -> isAVertex(pos) && result }
   fun isShowingFront(observerLookingDirection: Vector): Boolean {
      val dotProduct = polygonPlane.normal.dot(observerLookingDirection)
      return dotProduct < 0.0 //this is a property of the dot product
   }
   fun isShowingBack(observerLookingDirection: Vector): Boolean {
      val dotProduct = polygonPlane.normal.dot(observerLookingDirection)
      return dotProduct > 0.0 //this is a property of the dot product
   }
   /**
    * True if the polygons match one another (it doesn't mean that their area is the same but
    * that it is the same and also the same as the intersection)
    *
    * This method does not use double precision, it uses the precision of this module.
    */
   fun areaMatches(other: Polygon): Boolean {
      val intersectionArea =  intersectionArea(other)
      val polygonArea = area()
      val otherPolyonArea = other.area()
      //gotta use the tolerance constant of this library
      // actually this method is not very exact, because I'm testing areas
      //and the formula is different than for linear units, but at the same time the tolerance is much higher, I think
      //it should be ok.
      return epsilonEquals(polygonArea, intersectionArea, TOLERANCE) &&
              epsilonEquals(otherPolyonArea, intersectionArea, TOLERANCE)
   }
   /**
    * Determine if this polygon intersects another in the same plane.
    *
    * If the polygon crosses in a different plane or if they just share a border (either on the same plane
    * or in a different plane) the method returns false.
    *
    * But it is strongly discouraged to pass anything but other polygons that same the same plane as this one.
    */
   //TODO decide which epsilon to use here
   fun overlaps(other: Polygon): Boolean = !areaIsAlmostZero(intersectionArea(other))
   /** Just another name for "covered by" function. */
   fun isHiddenBy(covering: Set<Polygon>): Boolean = isCoveredBy(covering)
   fun isCompletelyVisible(covering: Set<Polygon>): Boolean {
      val intersectionArea = intersectionArea(covering)
      return areaIsAlmostZero(intersectionArea)
   }
   /**
    * Same order, but the points might be shifted around the list.
    *
    * Note that the points are positions, not vertices objects, we are checking for positions
    * not to match the vertices.
    */
   fun hasSamePointStructure(otherPoints: List<Point>): Boolean = isSamePointStructure(vertices, otherPoints)
   fun area(): Double = PolygonBooleanOperator.area(this)
   /**
    * The reason there is a function for the intersection and another for the intersection area is that we rely on a library
    * that only performs this operation in 2d, requiring us to transform to 2d and back if we need the resulting
    * polygon or polygons, but if we only need the area then we save an operation and reduce rounding errors
    */
   private fun intersectionArea(other: Polygon): Double = intersectionArea(this, other)
   /**
    * This method can be confusing, it means the intersection between this object and the union of the parameters
    * Not the intersection of all the polygons.
    */
   private fun intersectionArea(others: Set<Polygon>): Double = intersectionArea(this, others)
   fun isCoveredBy(others: Set<Polygon>): Boolean = areaIsAlmostZero(differenceArea(others))
   /**
    * The reason there is a function for the difference and another for the difference area is that we rely on a library
    * that only performs this operation in 2d, requiring us to transform to 2d and back if we need the resulting
    * polygon or polygons, but if we only need the area then we save an operation and reduce rounding errors
    */
   private fun differenceArea(others: Set<Polygon>): Double = differenceArea(this, others)
   /** What remains of this polygon after removing what is covered by the polygon parameters.*/
   fun difference(coveringPolygons: Set<Polygon>): Set<Polygon> = difference(this, coveringPolygons)
   fun partiallyOrTotallyCoveredBy(coveringPolygons: Set<Polygon>): Boolean = !isCompletelyVisible(coveringPolygons)
   fun intersections(segment: LineSegment): Set<Point>{
      val intersectionVertices = vertices.filter { segment.contains(it) }
      val intersectedEdges = edges.filter { it.intersectionPoint(segment) != null }
      val intersectedEdgesButNotAtVertex = intersectedEdges.filter { edge ->
         val intersection = edge.intersectionPoint(segment)
         intersectionVertices.none { vertex -> vertex.epsilonEquals(intersection) }}
      val intersectionsAtEdges = intersectedEdgesButNotAtVertex.map { it.intersectionPoint(segment) }
      return (intersectionVertices + intersectionsAtEdges).toSet()
   }
   companion object {
      @JvmStatic val DEFAULT_COLOR: Color = WHITE
      @JvmStatic val DEFAULT_COLOR_COMBINATION: ColorCombination = ColorCombination(WHITE, WHITE)
      private fun <P: Point> makeSegments(points: List<P>): Set<LineSegment> =
              (0 until points.size).map { LineSegment(points[it], points[(it + 1) % points.size]) }.toSet()
      /*
       * Selecting the points of a concave polygon to determine its order it's not as easy as taking any three
       * consecutive points
       *
       * A simple working algorithm is to take those points that lay on the sides of a rectangular bounding box
       * (any bounding box) of the polygon in the same order as they appear on the polygon list
       */
      private fun planeOfConcavePolygon(polygonPoints: List<Point>): Plane {
         assert(polygonPoints.size > 2)
         if(polygonPoints.size == 3)
            return planeFromOrderedPoints(polygonPoints)
         val planeCreationPoints = calculatePlaneCreationPoints(polygonPoints)
         return planeFromOrderedPoints(planeCreationPoints)
      }
      @VisibleForTesting
      internal fun calculatePlaneCreationPoints(pointsOfConcavePolygon: List<Point>): List<Point> {
         val convexHull = convexHull(pointsOfConcavePolygon)
         val threePointsOfAConvexHull = convexHull.take(3).toSet()
         val orderedPoints = orderPointsFromWhichToComputePlane(threePointsOfAConvexHull, pointsOfConcavePolygon)
         if(orderedPoints.size < 3)
            throw RuntimeException()
         return orderedPoints
      }
      private fun orderPointsFromWhichToComputePlane(points: Set<Point>, polygonPoints: List<Point>): List<Point> =
         polygonPoints.filter { polygonPoint -> points.any { it.epsilonEquals(polygonPoint, Constants.TOLERANCE) } }
      fun calculateUnionArea(polygons: Set<Polygon>): Double = PolygonBooleanOperator.unionArea(polygons)
      fun isSamePointStructure(points1: List<Point>, points2: List<Point>): Boolean {
         val pointCount = points1.size
         if(pointCount != points2.size) return false
         val indexOfFirst = points1.indexOfFirst { it.epsilonEquals(points2[0]) }
         if(indexOfFirst < 0)
            return false
         for((index, point) in points2.withIndex()){
            val thisIndex = (indexOfFirst + index) % pointCount
            if(!points1[thisIndex].epsilonEquals(point))
               return false
         }
         return true
      }
      /**
       * The set of line segments that remains after removing the parts covered by the polygons.
       */
      fun lineSegmentDifferenceWithPolygons(lineSegment: LineSegment, polygons: Set<Polygon>) = difference(lineSegment, polygons)
   }
}