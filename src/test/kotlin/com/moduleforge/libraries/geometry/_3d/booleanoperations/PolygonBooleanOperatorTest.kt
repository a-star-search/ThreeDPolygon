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
import com.moduleforge.libraries.geometry.Geometry.epsilonEqualsFloatPrecision
import com.moduleforge.libraries.geometry._3d.ColorCombination
import com.moduleforge.libraries.geometry._3d.Line.linePassingBy
import com.moduleforge.libraries.geometry._3d.LineSegment
import com.moduleforge.libraries.geometry._3d.Point
import com.moduleforge.libraries.geometry._3d.Point.midPoint
import com.moduleforge.libraries.geometry._3d.Polygon
import com.moduleforge.libraries.geometry._3d.booleanoperations.PolygonBooleanOperator.difference
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color.MAGENTA
import java.awt.Color.ORANGE
import java.lang.Math.sqrt
import java.util.concurrent.ThreadLocalRandom

class PolygonBooleanOperatorTest {
   @Test(expected=RuntimeException::class)
   fun differenceOfPolygonsThatResultsInHole_ShouldThrowRunTimeException(){
      val triangle = Polygon(Point(0, 0, 0), Point(2, 0, -2), Point(2, 2, -2))
      val hole = Polygon(Point(0.9, 0.1, -0.9), Point(1, 0.1, -1), Point(1, 0.5, -1))
      difference(triangle, setOf(hole))
   }
   @Test(expected=RuntimeException::class)
   fun differenceOfPolygonsThatResultsInHole_ForRandomlyGeneratedTriangle_ShouldThrowRunTimeException(){
      val randomPointsTriangle = makeTriangleOfRandomPoints()
      val hole = makeInnerPolygon(randomPointsTriangle)
      difference(randomPointsTriangle, setOf(hole))
   }
   @Test
   fun differenceWithTwoPolygons_WhenDifferenceWithOneResultsInHoleButDifferenceOfUnionDoesNot_ShouldNotThrowException(){
      val triangle = Polygon(Point(0, 0, 0), Point(2, 0, -2), Point(2, 2, -2))
      val hole = makeInnerPolygon(triangle)
      val triangleHalf = makeHalfTriangle(triangle)
      //the union of both does not produce holes
      difference(triangle, setOf(hole, triangleHalf))
   }
   /**
    * Same as the other test, different polygon.
    *
    * I don't see much of a point in doing a drawing, put it in the documentation and refer to it here,
    * but just realize that all this cases where we cut out the center of the triangle plus half of the triangle
    * result in a difference polygon that has eight vertices.
    */
   @Test
   fun differenceWithTwoPolygons_WhenDifferenceWithOneResultsInHoleButDifferenceOfUnionDoesNot_ShouldNotThrowException_2(){
      val points =
              listOf(Point(0.0, 8.0, 2.0), Point(1.0, 6.0, 1.0), Point(2.0, 2.0, 1.0))
      val triangle = Polygon(points)
      val hole = Polygon(listOf(
              Point(0.375, 7.0, 1.75), Point(0.875, 6.0, 1.25), Point(1.375, 4.0, 1.25)))
      val triangleHalf = Polygon(listOf(
              Point(0.0, 8.0, 2.0), Point(0.5, 7.0, 1.5), Point(1.0, 5.0, 1.5)))
      val difference = difference(triangle, setOf(hole, triangleHalf))
      assertThat(difference.size, `is`(1)) //the difference polygon is a single polygon
      val differencePolygon = difference.first()
      assertThat(differencePolygon.vertices.size, `is`(8)) //the difference polygon has 8 vertices
   }
   @Test
   fun differenceWithTwoPolygons_WhenDifferenceWithOneResultsInHoleButDifferenceOfUnionDoesNot_ShouldNotThrowException_3(){
      val points =
              listOf(Point(3.0, 5.0, 8.0), Point(5.0, 6.0, 2.0), Point(1.0, 8.0, 9.0))
      val triangle = Polygon(points)
      val hole = makeInnerPolygon(triangle)
      val triangleHalf = makeHalfTriangle(triangle)
      val difference = difference(triangle, setOf(hole, triangleHalf))
      assertThat(difference.size, `is`(1)) //the difference polygon is a single polygon
      val differencePolygon = difference.first()
      assertThat(differencePolygon.vertices.size, `is`(8)) //the difference polygon has 8 vertices
   }

   /**
    * In this example, the method name (long enough already) doesn't include what we intend to do:
    * take a "hole" in a random triangle.
    *
    * Each vertex of the hole is the mid point between a vertex of the triangle and the midpoint
    * of the other two vertices.
    *
    * Make the union with a half of the triangle.
    *
    * This guarantees, in theory that the difference is a single polygon of 8 vertices.
    *
    * In practice, because the half of the triangle matches part of the borders of the bigger triangle,
    * it makes an interesting corner case more prone to errors
    */
   @Test
   fun differenceWithTwoPolygons_WhenDifferenceWithOneResultsInHoleButDifferenceOfUnionDoesNot_ForRandomlyGeneratedTriangle_ShouldNotThrowException_AndResultShouldBeASinglePolygon(){
      for(i in 0..5000){
         val randomPointsTriangle = makeTriangleOfRandomPoints()
         val hole = makeInnerPolygon(randomPointsTriangle)
         val triangleHalf = makeHalfTriangle(randomPointsTriangle)
         val diff = difference(randomPointsTriangle, setOf(hole, triangleHalf))
         assertThat(diff.size, `is`(1))
         val diffPolygon = diff.first()
         assertThat(diffPolygon.vertices.size, `is`(8))
      }
   }
   /**
      By "half" it means, same proportions and half sides
      (think that is a 1/2 version of the triangle with same proportions, so area is reduce by the square of that)
    */
   @Test
   fun areaOfHalfTriangle_ShouldBeAQuarter(){
      val triangle = makeTriangleOfRandomPoints()
      val halfTriangle = makeHalfTriangle(triangle)
      val triangleArea = PolygonBooleanOperator.area(triangle)
      val halfTriangleArea = PolygonBooleanOperator.area(halfTriangle)
      assertTrue(epsilonEquals(triangleArea, halfTriangleArea * 4))
   }
   @Test
   fun differenceAreaBetweenATriangleAndItsHalf_ShouldBeThreeQuartersTheAreaOfTheTriangle(){
      //bit of a sloppy function name, but if we reduce sides by half, area should be 1/4
      // (think that is a 1/2 version of the triangle with same proportions, so area is reduce by the square of that)
      val triangle = makeTriangleOfRandomPoints()
      val halfTriangle = makeHalfTriangle(triangle)
      val triangleArea = PolygonBooleanOperator.area(triangle)
      val diffArea = PolygonBooleanOperator.differenceArea(triangle, setOf(halfTriangle))
      assertTrue(epsilonEquals(diffArea, triangleArea * 3 / 4))
   }
   @Test
   fun differenceOfTriangleWithItself_ShouldReturnEmptySetOfPolygons(){
      val triangle = makeTriangleOfRandomPoints()
      val diff = difference(triangle, setOf(triangle))
      assertTrue(diff.isEmpty())
   }
   @Test
   fun testDifference_WhenClipOneCornersOfRectangle_DifferencePolygonHasSixVertices(){
      //this rectangles can almost be visualized (or at least easily drawn)
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0))
      val clippingRectangle = Polygon(Point(1, -1, 1), Point(3, -1, 3), Point(3, 1, 3), Point(1, 1, 1))
      val diff = difference(rectangle, setOf(clippingRectangle))
      assertThat(diff.size, `is`(1))
      val vertexCountOfDifferencePolygon = diff.first().vertices.size
      assertThat(vertexCountOfDifferencePolygon, `is`(6))
   }
   @Test
   fun testDifference_WhenClipTwoCornersOfRectangle_DifferencePolygonHasEightVertices(){
      //this rectangles can almost be visualized (or at least easily drawn)
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0))
      val clippingRectangle = Polygon(Point(1, -1, 1), Point(3, -1, 3), Point(3, 1, 3), Point(1, 1, 1))
      val clippingRectangle2 = Polygon(Point(1, 2, 1), Point(3, 2, 3), Point(3, 4, 3), Point(1, 4, 1))
      val diff = difference(rectangle, setOf(clippingRectangle, clippingRectangle2))
      assertThat(diff.size, `is`(1))
      val vertexCountOfDifferencePolygon = diff.first().vertices.size
      assertThat(vertexCountOfDifferencePolygon, `is`(8))
   }
   @Test
   fun testDifference_WhenClipOneCornersOfRectangle_DifferencePolygonHasPointsInCorrectOrder(){
      //this rectangles can almost be visualized (or at least easily drawn)
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0))
      val clippingRectangle = Polygon(Point(1, -1, 1), Point(3, -1, 3), Point(3, 1, 3), Point(1, 1, 1))
      val diff = difference(rectangle, setOf(clippingRectangle)).first()

      val expectedClippedPolygonPoints =
              listOf(Point(0, 0, 0), Point(1, 0, 1), Point(1, 1, 1), Point(2, 1, 2),
                      Point(2, 3, 2), Point(0, 3, 0))

      val indexOf000InDiff = diff.vertices.indexOfFirst { it.epsilonEquals(Point(0, 0, 0))}
      assertTrue(indexOf000InDiff > 0)
      var next = diff.vertices[indexOf000InDiff]
      for(point in expectedClippedPolygonPoints) {
         assertTrue(point.epsilonEquals(next))
         next = diff.nextVertex(next)
      }
   }
   @Test
   fun testDifference_WhenClipOneCornersOfRectangle_DifferencePolygonHasSameColorsAsOriginalPolygon(){
      val colors = ColorCombination(MAGENTA, ORANGE)
      //this rectangles can almost be visualized (or at least easily drawn)
      val rectangle =
              Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0),
                      colors = colors)
      val clippingRectangle = Polygon(Point(1, -1, 1), Point(3, -1, 3), Point(3, 1, 3), Point(1, 1, 1))
      val diff = difference(rectangle, setOf(clippingRectangle)).first()
      assertEquals(diff.frontColor, MAGENTA)
      assertEquals(diff.backColor, ORANGE)
   }
   @Test
   fun differenceOfSegmentWithPolygonThatEmbedsIt_ShouldReturnEmptySetOfSegments() {
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0) )
      val segment = LineSegment(Point(0.5, 1, 0.5), Point(1, 1, 1))
      val difference = difference(segment, rectangle)
      assertTrue(difference.isEmpty())
   }
   @Test
   fun differenceOfSegmentWithPolygon_WhenSegmentStandsOutOnOneSide_Calculated() {
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0) )
      val segment = LineSegment(Point(0.5, 1, 0.5), Point(3, 1, 3))
      val difference = difference(segment, rectangle)
      assertTrue(difference.size == 1)
      val differenceSegment = difference.first()
      assertTrue( epsilonEqualsFloatPrecision( differenceSegment.length(), sqrt(2.0) ))
   }
   @Test
   fun differenceOfSegmentWithPolygon_WhenSegmentStandsOutOnBothSides_Calculated() {
      val rectangle = Polygon(Point(0, 0, 0), Point(2, 0, 2), Point(2, 3, 2), Point(0, 3, 0) )
      val segment = LineSegment(Point(-1, 1, -1), Point(3, 1, 3))
      val difference = difference(segment, rectangle)
      assertTrue(difference.size == 2)
      //both segments have the same length
      difference.forEach {
         assertTrue( epsilonEqualsFloatPrecision( it.length(), sqrt(2.0) ))
      }
   }
   /** Takes a triangle and makes a new triangle from one of the verttices and the midpoints of the adjacent sides
    */
   private fun makeHalfTriangle(pol: Polygon): Polygon {
      val v1 = pol.vertices[0]
      val v2 = pol.vertices[1]
      val v3 = pol.vertices[2]
      return Polygon(v1, midPoint(v1, v2), midPoint(v1, v3))
   }
   private fun makeInnerPolygon(pol: Polygon): Polygon {
      val vertices = pol.vertices
      val centroid = midPoint(vertices[0], midPoint(vertices[1], vertices[2]))
      return Polygon(vertices.map { midPoint(centroid, it) })
   }

   companion object {
      fun makeTriangleOfRandomPoints(): Polygon {
         val min = 0
         val max = 10
         val thingy = ThreadLocalRandom.current()
         while(true){
            val trianglePointSet = mutableSetOf<List<Int>>()
            while (trianglePointSet.size < 3)
               trianglePointSet.add(listOf(thingy.nextInt(min, max), thingy.nextInt(min, max), thingy.nextInt(min, max)))
            val trianglePointList = trianglePointSet.toList()
            val v1 = trianglePointList[0]
            val v2 = trianglePointList[1]
            val v3 = trianglePointList[2]
            val p1 = Point(v1[0], v1[1], v1[2])
            val p2 = Point(v2[0], v2[1], v2[2])
            val p3 = Point(v3[0], v3[1], v3[2])
            if(!linePassingBy(p1, p2).contains(p3))
               return Polygon(p1, p2, p3)
         }
      }
   }
}