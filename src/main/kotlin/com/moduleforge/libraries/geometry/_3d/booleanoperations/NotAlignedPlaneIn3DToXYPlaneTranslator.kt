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

import com.google.common.annotations.VisibleForTesting
import com.moduleforge.libraries.geometry.Geometry.*
import com.moduleforge.libraries.geometry._2d.PolarPoint
import com.moduleforge.libraries.geometry._2d.PolarPoint.polarFromCartesian
import com.moduleforge.libraries.geometry._3d.Line
import com.moduleforge.libraries.geometry._3d.Line.linePassingBy
import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Vector
import java.lang.Math.*
import javax.vecmath.Point2d

/**
 * The purpose of this class is to transform a set of polygons in any plane in space into polygons in the XY plane,
 * which in turn allows to be used by other algorithms that only operate on the XY plane to calculate unions, differences,
 * etc.
 *
 * It doesn't actually matter where the polygons end up in the XY plane. All it matters is that they
 * are in the XY plane and that they can be moved back and forth between the original plane and XY.
 */
internal class NotAlignedPlaneIn3DToXYPlaneTranslator(plane: Plane): PlaneIn3DToXYPlaneTranslator(plane) {
   /**
    * A reference point along with a reference vector allows to give each point coordinates
    */
   @VisibleForTesting
   internal val referencePoint: Point
   /**
    * A reference vector is a vector used to calculate the angle for each point that we want to map.
    * It is a normal.
    */
   @VisibleForTesting
   internal val referenceVector: Vector
   /**
    * Useful for rotations.
    * The direction vector of this object has the same direction as the normal vector of the plane of this object
    * */
   @VisibleForTesting
   internal val lineByReferencePoint: Line

   init {
      val d = plane.equation.last()
      val planeByCoordinateOrigin = isZero(d)
      val coordinateOrigin = Point(0, 0, 0)
      referencePoint = if(planeByCoordinateOrigin) coordinateOrigin else plane.closestPoint(coordinateOrigin)
      referenceVector = randomVectorPerpendicularTo(plane.normal)
      lineByReferencePoint = linePassingBy(referencePoint, referencePoint.translate(plane.normal))
   }
   /**
    * The coordinate selector is used as a way to decide the direction of the angle the point and reference point
    * make with the reference vector. It uses the signum of just one of the coordinates and, naturally,
    * it's a faster operation than comparing to the plane normal for distance or any other
    */
   override fun translateToXYPlane(point: Point): Point2d = translateToXYPolar(point).asCartesian()
   private fun translateToXYPolar(point: Point): PolarPoint {
      if(point.epsilonEquals(referencePoint))
         return PolarPoint(0.0, 0.0)
      val toPoint = referencePoint.vectorTo(point)
      val angle = calculateAngleClockwiseDirection(toPoint)
      return PolarPoint(referencePoint.distance(point), angle)
   }
   /**
    * Returns an angle such as if we rotate the reference vector
    * in a clockwise direction as referenced by the plane normal or the direction of the line passing
    * by the reference point it would point to the point.
    *
    * At the time of this comment if the angle to rotate clockwise is greater than PI it returns a negative value,
    * that is, a counter clockwise rotation of less than PI. But it shouldn't really matter if it returned a
    * positive value greater than PI. And a future implementation might do so. Although I see no reason to change this
    * function.
    */
   private fun calculateAngleClockwiseDirection(toPoint: Vector): Double {
      val angleMagnitude = abs(referenceVector.angle(toPoint))
      //if pi or zero rotation:direction doesn't matter, simply return magnitude
      if(epsilonEquals(angleMagnitude, PI) || almostZero(angleMagnitude))
         return angleMagnitude
      /* with the cross product, we get a perpendicular to the plane whose direction indicates
      if the reference vector has to be rotated clockwise or counterclockwise
      in order the point at the point
       */
      val crossProduct = referenceVector.cross(toPoint)
      /* the normal of the plane and cross product calculated in the previous step are compared
      with the dot product. if it's positive, they point in the same direction
      */
      val direction = signum(crossProduct.dot(plane.normal))
      /*
      in theory the direction cannot be zero and we could assert it here, in practice,
      sometimes the cross product is so small that the direction value coming of the dot product is zero,
      meaning the angle is just too close to either 0 or pi for the direction to be calculated in practice
      in that case we can just return the angle magnitude
      */
      return if(almostZero(direction))
            angleMagnitude
         else direction * angleMagnitude
   }
   override fun translateFromXYPlane(point: Point2d): Point = translateFromXYPlane(polarFromCartesian(point))
   private fun translateFromXYPlane(point: PolarPoint): Point {
      if(almostZero(point.r))
         return referencePoint
      val referenceEndPoint = referencePoint.translate(referenceVector)
      val rotatedPoint = lineByReferencePoint.rotatePointAroundClockwise(referenceEndPoint, point.theta)
      val vectorToTheNewPoint = referencePoint.vectorTo(rotatedPoint).withLength(point.r)
      return referencePoint.translate(vectorToTheNewPoint)
   }
   companion object {
      /** Produces a normal
       */
      @VisibleForTesting
      internal fun randomVectorPerpendicularTo(v: Vector): Vector {
         /*
         There a simpler implementation (in case I ever want to change this one),
         that's theoretically correct
         https://codereview.stackexchange.com/a/43943/9358
         but for some planes, the vector produced may not  have enough precision
         */
         val coords = v.coordinates()
         val indexOfMin = coords.indexOf(coords.map{ abs(it)}.min())
         val x = v.x()
         val y = v.y()
         val z = v.z()
         //use the two biggest values to avoid causing loss of precision
         return when (indexOfMin) {
            0 -> Vector(0, z, -y)
            1 -> Vector(z, 0, -x)
            else -> Vector(y, -x, 0)
         }.normalize()
      }
      @VisibleForTesting
      internal fun maxConstantCoordinateSelector(plane: Plane): (v: Vector) -> Double {
         val coordConstants = plane.equation.take(3).map {it -> abs(it)}
         return when (coordConstants.indices.maxBy { coordConstants[it] }){ //maxBy returns the first
            0 -> fun(v: Vector) = v.x()
            1 -> fun(v: Vector) = v.y()
            2 -> fun(v: Vector) = v.z()
            else -> throw RuntimeException()
         }
      }
   }
}