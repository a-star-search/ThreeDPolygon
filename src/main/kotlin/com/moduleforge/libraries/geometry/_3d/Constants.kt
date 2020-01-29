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

object Constants {
   /**
    * Because of the piece of shit JTS library is, we are forced to used an epsilon one order of magnitude higher in some
    * of the operations of this module.
    *
    * Using a tolerance and choosing its value requires some explanation, read the StickRemover class javadoc.
    */
   const val TOLERANCE: Double = 1e-6 //yeah, 1e-6, JTS is a real piece of trash
}