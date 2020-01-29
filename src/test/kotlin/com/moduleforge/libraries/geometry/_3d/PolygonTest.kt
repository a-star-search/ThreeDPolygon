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

import com.moduleforge.libraries.geometry._3d.Plane.planeFromOrderedPoints
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.moduleforge.libraries.geometry._3d.Polygon.Companion.calculatePlaneCreationPoints
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Polygon is an abstract class, I use a derived implementation for the tests. */
class PolygonTest {
   @Before
   fun setUp() {
   }
   @Test
   fun nextVertex_Calculated(){
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val pol = Polygon(firstPoint, secondPoint, thirdPoint)
      assertThat(secondPoint, `is`(pol.nextVertex(firstPoint)))
      assertThat(thirdPoint, `is`(pol.nextVertex(secondPoint)))
      assertThat(firstPoint, `is`(pol.nextVertex(thirdPoint)))
   }
   @Test
   fun triangle_IsTriangle(){
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val pol = Polygon(firstPoint, secondPoint, thirdPoint)
      assertTrue(pol.isTriangle())
   }
   @Test
   fun vertex_IsAVertex(){
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val pol = Polygon(firstPoint, secondPoint, thirdPoint)
      assertTrue(pol.isAVertex(firstPoint))
      assertTrue(pol.isAVertex(secondPoint))
      assertTrue(pol.isAVertex(thirdPoint))
      assertTrue(pol.areAllVertices(listOf(firstPoint, secondPoint, thirdPoint)))
   }
   @Test
   fun vertexIsInSamePosition_NotAVertex(){
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val pol = Polygon(firstPoint, secondPoint, thirdPoint)
      assertFalse(pol.isAVertex(Point(0, 0, 0)))
      assertFalse(pol.isAVertex(Point(1, 0, 0)))
      assertFalse(pol.isAVertex(Point(1, 1, 0)))
      assertFalse(pol.areAllVertices(listOf(Point(0, 0, 0))))
   }
   @Test fun vectorLookingAtBackOfPolygon_Calculated(){
      //from origin towards the z positive part of 3d space, but very slanted
      val slantedVectorLookingTowardsZPositive = Vector(1000, 1000, 1)

      //anticlockwise points as viewed from z positive space
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val polygonFacingZPos = Polygon(firstPoint, secondPoint, thirdPoint)

      assertTrue(polygonFacingZPos.isShowingBack(slantedVectorLookingTowardsZPositive))
   }
   @Test fun vectorLookingAtFrontOfPolygon_Calculated(){
      //from origin towards the z negative part of 3d space
      val lookingTowardsZNeg = Vector(0, 0, -1)

      //anticlockwise points as viewed from z positive space
      val firstPoint = Point(0, 0, 0)
      val secondPoint = Point(1, 0, 0)
      val thirdPoint = Point(1, 1, 0)
      val polygonFacingZPos = Polygon(firstPoint, secondPoint, thirdPoint)

      assertTrue(polygonFacingZPos.isShowingFront(lookingTowardsZNeg))
   }
   @Test fun areaExactlyMatches_ReturnsTrue(){
      val firstPointFirstPolygon = Point(0, 0, 0)
      val secondPointFirstPolygon = Point(1, 0, 0)
      val thirdPointFirstPolygon = Point(1, 1, 0)
      val firstPol = Polygon(firstPointFirstPolygon, secondPointFirstPolygon, thirdPointFirstPolygon)

      val firstPointSecondPolygon = Point(0, 0, 0)
      val secondPointSecondPolygon = Point(1, 0, 0)
      val thirdPointSecondPolygon = Point(1, 1, 0)
      val secondPol = Polygon(thirdPointSecondPolygon, firstPointSecondPolygon, secondPointSecondPolygon)

      assertTrue(firstPol.areaMatches(secondPol))
   }
   @Test fun areaDoesNotMatch_PolygonsPerpendicularToXYPlane_ReturnsTrue(){
      val firstPointFirstPolygon = Point(1, 0, 0)
      val secondPointFirstPolygon = Point(1, 1, 0)
      val thirdPointFirstPolygon = Point(1, 1, -1)
      val fourthPointFirstPolygon = Point(1, 0, -1)
      val firstPol = Polygon(firstPointFirstPolygon, secondPointFirstPolygon, thirdPointFirstPolygon, fourthPointFirstPolygon)

      val firstPointSecondPolygon = Point(1, 0, 0)
      val secondPointSecondPolygon = Point(1, 1, 0)
      val thirdPointSecondPolygon = Point(1, 1, -0.5)
      val fourthPointSecondPolygon = Point(1, 0, -0.5)
      val secondPol = Polygon(firstPointSecondPolygon, secondPointSecondPolygon, thirdPointSecondPolygon, fourthPointSecondPolygon)

      assertFalse(firstPol.areaMatches(secondPol))
   }
   @Test fun areaDoesNotMatch_ReturnsFalse(){
      val firstPointFirstPolygon = Point(0, 0, 0)
      val secondPointFirstPolygon = Point(1, 0, 0)
      val thirdPointFirstPolygon = Point(1, 1, 0)
      val firstPol = Polygon(firstPointFirstPolygon, secondPointFirstPolygon, thirdPointFirstPolygon)

      val firstPointSecondPolygon = Point(0, 0, 0)
      val secondPointSecondPolygon = Point(1, 0, 0)
      val differentThirdPointSecondPolygon = Point(1, 1, 1)
      val secondPol = Polygon(differentThirdPointSecondPolygon, firstPointSecondPolygon, secondPointSecondPolygon)

      assertFalse(firstPol.areaMatches(secondPol))
   }
   @Test fun testIsCompletelyVisible_When_InXZPlane_And_ParameterPolygonDontOverlapOrTouch_True(){
      val pol = Polygon(Point(0,0,0), Point(1,0,0), Point(1,0 ,1))
      val coveringPolThatDoesNotOverlap = Polygon(Point(10,0,10), Point(11,0,10), Point(11,0 ,11))
      assertTrue(pol.isCompletelyVisible(setOf(coveringPolThatDoesNotOverlap)))
   }
   @Test fun testIsCompletelyVisible_When_InXZPlane_And_ParameterPolygonTouchesButNotOverlap_True(){
      val pol = Polygon(Point(0,0,0), Point(1,0,0), Point(1,0 ,1))
      val coveringPolThatTouches = Polygon(Point(1,0,1), Point(1,0,10), Point(10,0 ,10))
      assertTrue(pol.isCompletelyVisible(setOf(coveringPolThatTouches)))
   }
   @Test fun testIsCompletelyVisible_When_InXZPlane_And_ParameterPolygonOverlaps_False(){
      val pol = Polygon(Point(0,0,0), Point(1,0,0), Point(1,0 ,1))
      val coveringPolThatOverlaps = Polygon(Point(0.5,0,0.5), Point(0.5,0,10), Point(10,0 ,0.5))
      assertFalse(pol.isCompletelyVisible(setOf(coveringPolThatOverlaps)))
   }
   /**
    * same test as similarly named above, but matching the points to some random plane
    */
   @Test fun testIsCompletelyVisible_When_InRandomPlane_And_ParameterPolygonTouchesButNotOverlap_True(){
      val plane = planeFromOrderedPoints(Point(0,3,0), Point(-10,4,-5), Point(10,8,-5))
      val pol = Polygon( plane.closestPoint(Point(0,0,0)),
              plane.closestPoint(Point(1,0,0)),
              plane.closestPoint(Point(1,0 ,1)))
      val coveringPolThatTouches = Polygon(plane.closestPoint(Point(1,0,1)),
              plane.closestPoint(Point(1,0,10)),
              plane.closestPoint(Point(10,0 ,10)))
      assertTrue(pol.isCompletelyVisible(setOf(coveringPolThatTouches)))
   }
   /**
    * same test as similarly named above, but matching the points to some random plane
    */
   @Test fun testIsCompletelyVisible_When_InRandomPlane_And_ParameterPolygonOverlaps_False(){
      val plane = planeFromOrderedPoints(Point(0,3,0), Point(-10,4,-5), Point(10,8,-5))
      val pol = Polygon( plane.closestPoint(Point(0,0,0)),
              plane.closestPoint(Point(1,0,0)),
              plane.closestPoint(Point(1,0 ,1)))
      val coveringPolThatOverlaps = Polygon(plane.closestPoint(Point(0.5,0,0.5)),
              plane.closestPoint(Point(0.5,0,10)),
              plane.closestPoint(Point(10,0 ,0.5)))
      assertFalse(pol.isCompletelyVisible(setOf(coveringPolThatOverlaps)))
   }
   @Test fun testIsCompletelyVisible_When_InXZPlane_And_OneParameterPolygonDontOverlapOrTouch_TheOtherParameterPolygonDoOverlap_False(){
      val pol = Polygon(Point(0,0,0), Point(1,0,0), Point(1,0 ,1))
      val coveringPolThatDoesNotOverlap = Polygon(Point(10,0,10), Point(11,0,10), Point(11,0 ,11))
      val coveringPolThatOverlaps = Polygon(Point(0.5,0,0.5), Point(0.5,0,10), Point(10,0 ,0.5))
      assertFalse(pol.isCompletelyVisible(setOf(coveringPolThatDoesNotOverlap, coveringPolThatOverlaps)))
   }
   @Test fun testFindPlanePointsOfConcavePolygon_WhenConvexQuadrilateral_ShouldReturnThreeOfThePointsOfTheQuadrilateral(){
      val planePoints= listOf(Point(-3, 2, 6), Point(4, 3, 7), Point(0, 1, -2))
      val randomPlane= planeFromOrderedPoints(planePoints)
      val randomPointInPlane= randomPlane.closestPoint(Point(-5, 0, -2))
      val pointsOfQuadrilateral= listOf(randomPointInPlane) + planePoints
      val calculatedPlanePoints= calculatePlaneCreationPoints(pointsOfQuadrilateral)
      assertThat(calculatedPlanePoints.size, `is`(3))
      assertTrue(pointsOfQuadrilateral.containsAll(calculatedPlanePoints))
   }
   /**
    * An indentation meaning like boomerang-shaped quadrilateral
    * If you have trouble visualizing, draw an XZ coordinate system on a piece of paper
    */
   @Test fun testFindPlanePointsOfConcavePolygon_WhenOneOfTheFourPointsOfAQuadrilateralMakeAnIndentation_ShouldNeverReturnThatPoint(){
      val pA = Point(-3, 2, 6)
      val pB = Point(4, 3, 7)
      val pC = Point(0, 1, -2)
      val pABC = listOf(pA, pB, pC)
      val randomPlane= planeFromOrderedPoints(pABC)
      val indentationPointBetween_pA_pC= randomPlane.closestPoint( midPoint(midPoint(pA, pC), pB))
      var pointsOfQuadrilateral= listOf(indentationPointBetween_pA_pC) + pABC
      var calculatedPlanePoints= calculatePlaneCreationPoints(pointsOfQuadrilateral)
      assertFalse(calculatedPlanePoints.contains(indentationPointBetween_pA_pC))
      assertTrue(calculatedPlanePoints.containsAll(pABC))

      //same as before but shifting the points one position, shouldn't make any difference
      pointsOfQuadrilateral = listOf(pB, pC, indentationPointBetween_pA_pC, pA)
      calculatedPlanePoints = calculatePlaneCreationPoints(pointsOfQuadrilateral)
      assertFalse(calculatedPlanePoints.contains(indentationPointBetween_pA_pC))
      assertTrue(calculatedPlanePoints.containsAll(pABC))

      //then with a differentt indentation point
      val indentationPointBetween_pB_pC= randomPlane.closestPoint( midPoint(midPoint(pB, pC), pA))
      pointsOfQuadrilateral= listOf(pC, indentationPointBetween_pB_pC, pB, pA)
      calculatedPlanePoints= calculatePlaneCreationPoints(pointsOfQuadrilateral)
      assertFalse(calculatedPlanePoints.contains(indentationPointBetween_pB_pC))
      assertTrue(calculatedPlanePoints.containsAll(pABC))
   }
   /**
    * Same example as previously, but check that the points selected foor the plane creation
    * maintain the same order as in the original polygon
    */
   @Test fun testFindPlanePointsOfConcavePolygon_WhenQuadrilateralWithIndentation_ShouldHavePointInRightOrder(){
      val pA = Point(-3, 2, 6)
      val pB = Point(4, 3, 7)
      val pC = Point(0, 1, -2)
      val pABC = listOf(pA, pB, pC)
      val randomPlane= planeFromOrderedPoints(pABC)
      val indentationPointBetween_pA_pC= randomPlane.closestPoint( midPoint(midPoint(pA, pC), pB))
      val pointsOfQuadrilateral= listOf(indentationPointBetween_pA_pC) + pABC
      val calculatedPlanePoints= calculatePlaneCreationPoints(pointsOfQuadrilateral)
      assertTrue(
              //valid configurations maintaining order
              calculatedPlanePoints == listOf(pA, pB, pC) ||
              calculatedPlanePoints == listOf(pB, pC, pA) ||
              calculatedPlanePoints == listOf(pC, pA, pB)
      )
   }
   @Test fun testVerticesStartingWith_Calculated(){
      val v0 = Point(0, 0, 0)
      val v1 = Point(1, 0, 0)
      val v2 = Point(1, 1, 0)
      val pol = Polygon(v0, v1, v2)
      assertThat(pol.verticesStartingWith(v0), `is`(listOf(v0, v1, v2)))
      assertThat(pol.verticesStartingWith(v1), `is`(listOf(v1, v2, v0)))
      assertThat(pol.verticesStartingWith(v2), `is`(listOf(v2, v0, v1)))
      val v3 = Point(0, 1, 0)
      val quad = Polygon(v0, v1, v2, v3)
      assertThat(quad.verticesStartingWith(v2), `is`(listOf(v2, v3, v0, v1)))
   }
}