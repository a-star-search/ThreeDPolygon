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

import com.moduleforge.libraries.geometry._3d.Plane
import com.moduleforge.libraries.geometry._3d.Point
import javax.vecmath.Point2d

/**
 * While the general case translator doesn't make promises on how the points are mapped, in this case there is a Z
 * coordinate to Y coordinate mapping. This is useful for test cases. (And it is also the most obvious thing to do)
 */
internal class PlaneParallelToXZToXYPlaneTranslator (plane: Plane): PlaneIn3DToXYPlaneTranslator(plane) {
   /** y = -d/b */
   val y: Double = -plane.equation[3]/plane.equation[1]
   //take z coordinate and make it the y
   override fun translateToXYPlane(point: Point): Point2d = Point2d(point.x(), point.z())
   override fun translateFromXYPlane(point: Point2d): Point = Point(point.x, y,point.y)
}