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

import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import javax.vecmath.Point2d

/**
 * While the general case translator doesn't make promises on how the points are mapped, in this case there is a Z
 * coordinate to X coordinate mapping. This is useful for test cases. (And it is also the most obvious thing to do)
 */
internal class PlaneParallelToYZToXYPlaneTranslator(plane: Plane): PlaneIn3DToXYPlaneTranslator(plane) {
   /** x = -d/b */
   val x: Double = -plane.equation[3]/plane.equation[0]
   //take z coordinate and make it the x
   override fun translateToXYPlane(point: Point): Point2d = Point2d(point.z(), point.y())
   override fun translateFromXYPlane(point: Point2d): Point = Point(x, point.y, point.x)
}