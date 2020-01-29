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

import com.google.common.cache.CacheBuilder.newBuilder
import com.google.common.cache.CacheLoader
import com.moduleforge.libraries.geometry.Geometry.almostZero
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import java.util.concurrent.TimeUnit.MINUTES
import javax.vecmath.Point2d

/**
 * Creation of translator through static factory methods with caching.
 * Translates points to the XY plane (no guarantees on the final position other than consistency).
 * Translate points from 2d back to the original plane, using, of course, the exact inverse of the original transformation.
 */
internal abstract class PlaneIn3DToXYPlaneTranslator(val plane: Plane){
   /** Method exists because a pair of lists of points is a common scenario */
   fun translateToXYPlane(polygonPoints1: List<Point>, polygonPoints2: List<Point>): List<List<Point2d>> =
           if(polygonPoints1 == polygonPoints2) {
              val translated = translateToXYPlane(polygonPoints1)
              listOf(translated, translated)
           } else {
              translateToXYPlane(setOf(polygonPoints1, polygonPoints2))
           }
   fun translateToXYPlane(setOfPolygonPoints: Set<List<Point>>): List<List<Point2d>> =
           setOfPolygonPoints.map { pol -> translateToXYPlane(pol) }
   fun translateToXYPlane(points: List<Point>): List<Point2d> = points.map { translateToXYPlane(it) }
   internal abstract fun translateToXYPlane(point: Point): Point2d
   fun translateFromXYPlane(polygonPoints: List<Point2d>): List<Point> = polygonPoints.map { translateFromXYPlane(it)}
   fun translateFromXYPlane(setOfPolygonPoints: Set<List<Point2d>>): Set<List<Point>> =
           setOfPolygonPoints.map { polygonPoints -> polygonPoints.map { point -> translateFromXYPlane(point)} }.toSet()
   /**
    Translate 2d points in the XY plane, which have, presumably, come an operation such as union or difference, for
     which points had had to be translated to XY, back to the original plane.
    */
   internal abstract fun translateFromXYPlane(point: Point2d): Point
   companion object {
      //cache because typically there's a very small number of planes in an origami figure and this class is immutable
      private val cache = newBuilder().maximumSize(100).expireAfterWrite(10, MINUTES).build(
              object : CacheLoader<Plane, PlaneIn3DToXYPlaneTranslator>() {
                 override fun load(plane: Plane): PlaneIn3DToXYPlaneTranslator =
                      when {
                         isPlaneParallelToXY(plane) -> PlaneParallelToXYToXYPlaneTranslator(plane)
                         isPlaneParallelToXZ(plane) -> PlaneParallelToXZToXYPlaneTranslator(plane)
                         isPlaneParallelToYZ(plane) -> PlaneParallelToYZToXYPlaneTranslator(plane)
                         else -> NotAlignedPlaneIn3DToXYPlaneTranslator(plane)
                      }
                }
              )
      fun makeTranslator(plane: Plane): PlaneIn3DToXYPlaneTranslator = cache[plane]
      private fun isPlaneParallelToXY(plane: Plane): Boolean =
              almostZero(plane.equation[0]) && almostZero(plane.equation[1]) && !almostZero(plane.equation[2])
      private fun isPlaneParallelToXZ(plane: Plane): Boolean =
              almostZero(plane.equation[0]) && !almostZero(plane.equation[1]) && almostZero(plane.equation[2])
      private fun isPlaneParallelToYZ(plane: Plane): Boolean =
              !almostZero(plane.equation[0]) && almostZero(plane.equation[1]) && almostZero(plane.equation[2])
   }
}