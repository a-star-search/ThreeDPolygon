Concave polygon in 3D space with boolean operations such as intersection, difference, etc for co-planar
polygons.

The reason it needs to be concave (and keep in mind that the calculation of boolean operations on pairs of convex faces
would be much simpler) is that although, in 'Origami', the application for which this library was created, there are no
concave faces, the part of a face that is visible can be concave.

However this library is designed to be generic and can be used in other projects (although I would imagine that boolean
operations of coplanar polygons in 3D space are an uncommon need).

The boolean operations are a facade over JTS boolean operations that definitely do not work on an arbitrary plane
- despite the interface of the library suggesting that it does-. However they do work in the XY space.

Internally, this library rotates the polygons to the XY plane, delegates on JTS for the operation and rotates back the
result or simply returns the result if it's numeric (ie, any kind of area).

The user of this module can not expect anywhere near double precision (around 15 decimal places)
in the boolean operations' results for a number of reasons - mostly JTS being a careless, sloppy piece of code - and for
 a high number of calculations involved in the aforementioned boolean polygon operations.

The JTS library is so inaccurate that the operations of this library that relies on it can barely guarantee six decimal
digits of precision (float precision).

Accuracy aside, and while the public interface of JTS is also horribly designed, it does seem to otherwise do a
decent job of performing these boolean operations, implementing the right algorithms which can
be quite complex.

I didn't have another choice of library upon which to rely. JTS is the most widely used and most commonly recommended
of any geometric java libraries.