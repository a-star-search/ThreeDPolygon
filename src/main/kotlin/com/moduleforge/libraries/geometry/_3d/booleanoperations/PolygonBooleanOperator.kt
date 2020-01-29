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

import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Polygon
import com.moduleforge.libraries.geometry._3d.booleanoperations.PlaneIn3DToXYPlaneTranslator.Companion.makeTranslator
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import javax.vecmath.Point2d
import org.locationtech.jts.geom.Coordinate as JTSCoordinate
import org.locationtech.jts.geom.Geometry as JTSGeometry
import org.locationtech.jts.geom.GeometryFactory as JTSFactory
import org.locationtech.jts.geom.MultiPolygon as JTSMultiPolygon
import org.locationtech.jts.geom.Polygon as JTSPolygon

/**
 * JTS is the library we use to calculate union, intersection, difference of polygons,
 * unfortunately it only works in a 2d space (although it claims to work in 3d, it doesn't),
 * so we encapsulate those operations here, which require translating the points to the xy plane,
 * using JTS and, sometimes, translating them back.
 *
 * With regards to our application, only two types of operation are needed, one is calculating the area
 * of an intersection to determine if there is some overlap, to determine if the face if completely covered
 * partially covered or not covered at all.
 *
 * In the case of a partially covered face, for display purposes of the flat representation of the figure,
 * we will need to determine the difference polygon. In this case it is necessary to translate the figure back
 * to the original plane.
 *
 * Notice that this is the reason that we have pairs of methods, one with the operation and another with the
 * operation and the "area" suffix. It's because the latter does not need a translation operation, thus
 * saving us computation and loss of precision.
 *
 * Apparently it's possible to use Java2D class "Area" for this too. Maybe I should have had...
 *
 *============================================
 * About JTS quality and precision (Important):
 *
 * JTS appears to be a buggy piece of shit, but there are no other libraries that I can trust for boolean
 * operations of convex polygons in 2D.
 *
 * In this code I have to amend the library and "cut off" the holes that it creates sometimes (and to be fair, only
 * sometimes) when removing a part of a polygon from another part (difference operation) and that part to remove happens
 * to have segments that coincide and go along the segments of the polygon from which we remove.
 *
 * Sometimes it's not only holes, in point of fact, it's probably the case that holes can't happen in origami
 * (although I can create them in tests) but is actually a chain of segments of no width that needs to be cut off.
 *
 * If we compound the problems of 1) having to rotate polygons to the XY plane 2) JTS being a piece of shit
 * 3) the precision expected of the area which is a different formula than than for linear segment
 *
 * All that tells us that we need to treat errors rigorously and consider there is unavoidable inaccuracy of several
 * orders of magnitude that of the double precision in simple arithmetic operations.
 *
 * Before I had a polygon validation method like:
 *
 * polygon.numInteriorRing <= 0
 *
 * But not only would be useless since there can be holes for valid input but also insufficient, even when there
 * are no holes we need to take care of what I call "sticks" that need to be broken off.
 *
 */
internal object PolygonBooleanOperator {
   private val jtsFactory = JTSFactory()

   fun area(pol: Polygon): Double = area(pol.polygonPlane, pol.vertices)
   /* passing the plane too (and not just the points) for increased precision*/
   private fun area(plane: Plane, polygonPoints: List<Point>): Double {
      val translator = makeTranslator(plane)
      val translated = translator.translateToXYPlane(polygonPoints)
      val translatedJTSPolygon = makeJTSPolygon(translated)
      return translatedJTSPolygon.area
   }
   fun intersectionArea(pol1: Polygon, pol2: Polygon): Double = intersectionArea(pol1.polygonPlane, pol1.vertices, pol2.vertices)
   private fun intersectionArea(plane: Plane, points1: List<Point>, points2: List<Point>): Double {
      val translator = makeTranslator(plane)
      val translated = translator.translateToXYPlane(points1, points2)
      val translatedJTSPolygon1 = makeJTSPolygon(translated[0])
      val translatedJTSPolygon2 = makeJTSPolygon(translated[1])
      return translatedJTSPolygon1.intersection(translatedJTSPolygon2).area
   }
   /**
    * All polygons should be in the same plane
    */
   fun unionArea(polygons: Set<Polygon>): Double {
      val randomPolygon = polygons.first()
      val plane = randomPolygon.polygonPlane
      val setOfPolygonPoints = polygons.map { it.vertices }.toSet()
      val unionGeometry = unionAsJTSGeometryInXYPlane(plane, setOfPolygonPoints)
      return unionGeometry.area
   }
   /**
    * This method can be confusing, it means the intersection between the union of the parameters and this object
    * Not the intersection of all the polygons.
    */
   fun intersectionArea(polygon: Polygon, others: Set<Polygon>): Double =
           intersectionArea(polygon.polygonPlane, polygon.vertices, others.map {pol -> pol.vertices}.toSet())
   private fun intersectionArea(plane: Plane, points: List<Point>, others: Set<List<Point>>): Double {
      val unionGeometry = unionAsJTSGeometryInXYPlane(plane, others)
      val jtsPolygon = polygonAsJTSPolygonInXYPlane(plane, points)
      return jtsPolygon.intersection(unionGeometry).area
   }
   /**
    * This is the difference of the first parameter with the union of the set of list of points that is the second
    * parameter.
    *
    * The difference can be no polygon or an unlimited number of polygons. The order of the points of each of the
    * resulting polygons in the different set is the same order as the polygon they came from
    */
   fun difference(polygon: Polygon, coveringPolygons: Set<Polygon>): Set<Polygon> {
      val difference =
              difference(polygon.polygonPlane, polygon.vertices, coveringPolygons.map { pol -> pol.vertices}.toSet() )
      val noPoints = difference.size == 1 && difference.first().isEmpty()
      if(noPoints) return emptySet()
      val differencePolygons = difference.map { polygonPoints -> Polygon(polygonPoints, polygon.colors) }.toSet()
      return differencePolygons.map { differencePolygon -> ensurePointsDirection(polygon, differencePolygon) }.toSet()
   }
   private fun ensurePointsDirection(originPolygon: Polygon, differenceFragment: Polygon): Polygon {
      val polygonNormal = originPolygon.polygonPlane.normal
      val differenceFragmentNormal = differenceFragment.polygonPlane.normal
      val facingOppositeWays = polygonNormal.dot(differenceFragmentNormal) < 0
      return if(facingOppositeWays)
         Polygon(differenceFragment.vertices.asReversed(), differenceFragment.colors)
      else differenceFragment
   }
   private fun difference(plane: Plane, polygonPoints: List<Point>, coveringPolygonPoints: Set<List<Point>>): Set<List<Point>> {
      val unionGeometry = unionAsJTSGeometryInXYPlane(plane, coveringPolygonPoints)
      val jtsPolygon = polygonAsJTSPolygonInXYPlane(plane, polygonPoints)
      val diff = jtsPolygon.difference(unionGeometry)
      val diffAsPoints2d = toPoints(diff)
      val diffAsPoints2d_StickArmsBrokenOff = diffAsPoints2d
              .map { StickRemover.breakOffArms(it) }
              .filterNot { it.isEmpty() }.toSet()
      val translator = makeTranslator(plane)
      return translator.translateFromXYPlane(diffAsPoints2d_StickArmsBrokenOff)
   }
   fun difference(segment: LineSegment, coveringPolygon: Polygon): Set<LineSegment> =
           difference(segment, setOf(coveringPolygon))
   fun difference(segment: LineSegment, coveringPolygons: Set<Polygon>): Set<LineSegment> {
      val randomPolygon = coveringPolygons.first()
      val plane = randomPolygon.polygonPlane
      val difference =
              difference(plane, segment.points, coveringPolygons.map { pol -> pol.vertices}.toSet() )
      if(difference.isEmpty()) return emptySet()
      return difference.map {
         segmentPoints ->
         assert(segmentPoints.size == 2)
         LineSegment(segmentPoints[0], segmentPoints[1]) }.toSet()
   }
   private fun difference(plane: Plane, linePoints: org.javatuples.Pair<Point, Point>, coveringPolygonPoints: Set<List<Point>>):
           Set<List<Point>> {
      val unionGeometry = unionAsJTSGeometryInXYPlane(plane, coveringPolygonPoints)
      val line = lineAsJTSLineInXYPlane(plane, linePoints)
      val diff = line.difference(unionGeometry)
      if(diff.isEmpty)
         return emptySet()
      val diffAsPoints2d = toPointsOfLineSegments(diff)
      val translator = makeTranslator(plane)
      return translator.translateFromXYPlane(diffAsPoints2d)
   }
   /**
    * I ignore if the points of the convex hull maintain the same order as the points passed as parameter because
    * (as expected) jts javadoc says nothing on that matter. Mathematical libraries always have appalling documentation.
    */
   fun convexHull(polygonPoints: List<Point>): List<Point>{
      val planeAnyDirection = planeFromOrderedPoints(polygonPoints.take(3))
      val jtsPolygon = polygonAsJTSPolygonInXYPlane(planeAnyDirection, polygonPoints)
      val convexHull = ConvexHull(jtsPolygon).convexHull
      val convexHullPoints = toPoints(convexHull).first()
      val translator = makeTranslator(planeAnyDirection)
      return translator.translateFromXYPlane(convexHullPoints)
   }
   /**
    * This method can be confusing, it means the intersection between the union of the parameters and this object
    * Not the intersection of all the polygons.
    */
   fun differenceArea(polygon: Polygon, others: Set<Polygon>): Double =
           differenceArea(polygon.polygonPlane, polygon.vertices, others.map {pol -> pol.vertices}.toSet())
   private fun differenceArea(plane: Plane, points: List<Point>, others: Set<List<Point>>): Double {
      val unionGeometry = unionAsJTSGeometryInXYPlane(plane, others)
      val jtsPolygon = polygonAsJTSPolygonInXYPlane(plane, points)
      return jtsPolygon.difference(unionGeometry).area
   }
   private fun lineAsJTSLineInXYPlane(plane: Plane, points: org.javatuples.Pair<Point, Point>): LineString {
      val translator = makeTranslator(plane)
      val translatedPolygonPoints = translator.translateToXYPlane(listOf(points.value0, points.value1))
      val coords = translatedPolygonPoints.map { jtsCoordinate(it) }
      val cas = CoordinateArraySequence(coords.toTypedArray())
      return LineString(cas, jtsFactory)
   }
   private fun polygonAsJTSPolygonInXYPlane(plane: Plane, points: List<Point>): JTSPolygon {
      val translator = makeTranslator(plane)
      val translatedPolygonPoints = translator.translateToXYPlane(points)
      return makeJTSPolygon(translatedPolygonPoints)
   }
   //translate and make the union in the 2d space
   private fun unionAsJTSGeometryInXYPlane(plane: Plane, setOfPolygonPoints: Set<List<Point>>): JTSGeometry {
      val translator = makeTranslator(plane)
      val translated = translator.translateToXYPlane(setOfPolygonPoints)
      return unionAsJTSGeometry(translated.toSet())
   }
   private fun unionAsJTSGeometry(points: Set<List<Point2d>>): JTSGeometry =
           unionOfJTSPolygons(points.map { makeJTSPolygon(it) }.toSet() )
   private fun unionOfJTSPolygons(pols: Set<JTSPolygon>): JTSGeometry =
           (pols as Set<JTSGeometry>).reduce { acc, pol -> acc.union(pol) }
   private fun jtsCoordinate(p: Point2d) = JTSCoordinate(p.x, p.y)
   private fun makeJTSPolygon(points: List<Point2d>): JTSPolygon {
      val first = points[0]
      val coords = points.map { jtsCoordinate(it) } + jtsCoordinate(first) //append the first point to close the polygon
      return jtsFactory.createPolygon(coords.toTypedArray())
   }
   /**
    * Makes lists of pairs of vertices that are line segments (what else can two points be?)
    */
   private fun toPointsOfLineSegments(multiLine: JTSGeometry): Set<List<Point2d>> =
      when (multiLine) {
         is MultiLineString -> (0..(multiLine.numGeometries - 1)).map {
            toPoints(multiLine.getGeometryN(it) as LineString) }.toSet()
         is LineString -> setOf(toPoints(multiLine))
         else -> throw RuntimeException("Unknown geometry.")
      }
   private fun toPoints(line: LineString): List<Point2d> =
           line.coordinates.asList().map { Point2d(it.x, it.y) }
  /**
   * Makes lists of vertices that are polygons
   */
   private fun toPoints(multiPolygonGeometry: JTSGeometry): Set<List<Point2d>> =
           when (multiPolygonGeometry) {
              is JTSMultiPolygon -> (0..(multiPolygonGeometry.numGeometries - 1))
                      .map { toPoints(multiPolygonGeometry.getGeometryN(it) as JTSPolygon) }.toSet()
              is JTSPolygon -> setOf(toPoints(multiPolygonGeometry))
              else -> throw RuntimeException("Unknown geometry.")
           }
   private fun toPoints(jtsPolygon: JTSPolygon): List<Point2d> {
      //the last coordinate is the first in a jts polygon, so don't take it
      val coordinates = jtsPolygon.coordinates.dropLast(1)
      return (0..coordinates.lastIndex).map { Point2d(coordinates[it].x, coordinates[it].y) }
   }
}
