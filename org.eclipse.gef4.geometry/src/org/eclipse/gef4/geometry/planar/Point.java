/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Nyßen (itemis AG) - migration do double precision
 *     Matthias Wienand (itemis AG) - contribution for Bugzilla #355997
 *     
 *******************************************************************************/
package org.eclipse.gef4.geometry.planar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gef4.geometry.euclidean.Angle;
import org.eclipse.gef4.geometry.euclidean.Straight;
import org.eclipse.gef4.geometry.euclidean.Vector;
import org.eclipse.gef4.geometry.utils.PrecisionUtils;

/**
 * Represents a point (x, y) in 2-dimensional space. This class provides various
 * methods for manipulating this point or creating new derived geometrical
 * objects.
 * 
 * @author ebordeau
 * @author rhudson
 * @author pshah
 * @author ahunter
 * @author anyssen
 * @author mwienand
 * 
 */
public class Point implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private static Point[] eliminateDuplicates(Point... points) {
		// sort points by x and y
		Arrays.sort(points, 0, points.length, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				if (p1.x < p2.x) {
					return -1;
				}
				if (p1.x == p2.x && p1.y < p2.y) {
					return -1;
				}
				return 1;
			}
		});

		// filter points
		List<Point> uniquePoints = new ArrayList<Point>(points.length);
		for (int i = 0; i < points.length - 1; i++) {
			if (!points[i].equals(points[i + 1])) {
				uniquePoints.add(points[i]);
			}
		}
		uniquePoints.add(points[points.length - 1]);
		return uniquePoints.toArray(new Point[] {});
	}

	/**
	 * Returns the smallest {@link Rectangle} that encloses all {@link Point}s
	 * in the given sequence. Note that the right and bottom borders of a
	 * {@link Rectangle} are regarded as being part of the {@link Rectangle}.
	 * 
	 * @param points
	 *            a sequence of {@link Point}s which should all be contained in
	 *            the to be computed {@link Rectangle}
	 * @return a new {@link Rectangle}, which is the smallest {@link Rectangle}
	 *         that contains all given {@link Point}s
	 */
	public static Rectangle getBounds(Point... points) {
		// compute the top left and bottom right points by means of the maximum
		// and minimum x and y coordinates
		if (points.length == 0) {
			return new Rectangle();
		}
		// calculate bounds
		Point topLeft = points[0];
		Point bottomRight = points[0];
		for (Point p : points) {
			topLeft = Point.min(topLeft, p);
			bottomRight = Point.max(bottomRight, p);
		}
		return new Rectangle(topLeft, bottomRight);
	}

	/**
	 * Computes the centroid of the given {@link Point}s. The centroid is the
	 * "center of gravity", i.e. assuming the {@link Polygon} spanned by the
	 * {@link Point}s is made of a material of constant density, it will be in a
	 * balanced state, if you put it on a pin that is placed exactly on its
	 * centroid.
	 * 
	 * @param points
	 * @return the center {@link Point} (or centroid) of the given {@link Point}
	 *         s
	 */
	public static Point getCentroid(Point... points) {
		if (points.length == 0) {
			return null;
		} else if (points.length == 1) {
			return points[0].getCopy();
		}

		double cx = 0, cy = 0, a, sa = 0;
		for (int i = 0; i < points.length - 1; i++) {
			a = points[i].x * points[i + 1].y - points[i].y * points[i + 1].x;
			sa += a;
			cx += (points[i].x + points[i + 1].x) * a;
			cy += (points[i].y + points[i + 1].y) * a;
		}

		// closing segment
		a = points[points.length - 1].x * points[0].y
				- points[points.length - 1].y * points[0].x;
		sa += a;
		cx += (points[points.length - 1].x + points[0].x) * a;
		cy += (points[points.length - 1].y + points[0].y) * a;

		return new Point(cx / (3 * sa), cy / (3 * sa));
	}

	/**
	 * Computes the convex hull of the given set of {@link Point}s using the
	 * Graham scan algorithm.
	 * 
	 * @param points
	 *            the set of {@link Point}s to calculate the convex hull for
	 * @return the convex hull of the given set of {@link Point}s
	 */
	public static Point[] getConvexHull(Point... points) {
		// if we have up to three points, no calculation has to be performed, as
		// there may be no inner points.
		if (points.length <= 3) {
			return Point.getCopy(points);
		}

		// Remove duplicate points from the given point list in orde to be able
		// to apply the Graham scan.
		points = eliminateDuplicates(points);

		// do a graham scan to find the convex hull of the given set of points

		// move point with lowest y coordinate to first position
		int minIdx = 0;
		Point min = points[minIdx];
		for (int i = 1; i < points.length; i++) {
			if (points[i].y < min.y || points[i].y == min.y
					&& points[i].x < min.x) {
				min = points[i];
				minIdx = i;
			}
		}
		Point tmp = points[0];
		points[0] = points[minIdx];
		points[minIdx] = tmp;

		// sort all but first points by the angle they have compared to the
		// first position
		final Point p0 = points[0];
		Arrays.sort(points, 1, points.length, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				Vector v1 = new Vector(p0, p1);
				Vector v2 = new Vector(p0, p2);
				double angleInDeg = v1.getAngleCCW(v2).deg();
				return angleInDeg > 180 ? -1 : 1;
			}
		});

		// initialize stack with first three points
		ArrayList<Point> convexHull = new ArrayList<Point>();
		convexHull.add(points[2]);
		convexHull.add(points[1]);
		convexHull.add(points[0]);

		// expand initial stack to full convex hull
		for (int i = 3; i < points.length; i++) {
			// do always turn right
			while (convexHull.size() > 2
					&& Straight.getSignedDistanceCCW(convexHull.get(1),
							convexHull.get(0), points[i]) > 0) {
				convexHull.remove(0);
			}
			convexHull.add(0, points[i]);
		}

		return convexHull.toArray(new Point[] {});
	}

	/**
	 * Copies an array of points, by copying each point contained in the array.
	 * 
	 * @param points
	 *            the array of {@link Point}s to copy
	 * @return a new array, which contains copies of the given {@link Point}s at
	 *         the respective index positions
	 */
	public static final Point[] getCopy(Point[] points) {
		Point[] copy = new Point[points.length];
		for (int i = 0; i < points.length; i++) {
			copy[i] = points[i].getCopy();
		}
		return copy;
	}

	/**
	 * Returns a copy of the given array of points, where the points are placed
	 * in reversed order.
	 * 
	 * @param points
	 *            the array of {@link Point}s to reverse
	 * @return a new array, which contains a copy of each {@link Point} of the
	 *         given array of points at the respective reverse index
	 */
	public static final Point[] getReverseCopy(Point[] points) {
		Point[] reversed = new Point[points.length];
		for (int i = 0; i < points.length; i++) {
			reversed[i] = points[points.length - i - 1].getCopy();
		}
		return reversed;
	}

	/**
	 * Creates a new Point representing the MAX of two provided Points.
	 * 
	 * @param p1
	 *            first point
	 * @param p2
	 *            second point
	 * @return A new Point representing the Max()
	 */
	public static Point max(Point p1, Point p2) {
		return new Point(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y));
	}

	/**
	 * Creates a new Point representing the MIN of two provided Points.
	 * 
	 * @param p1
	 *            first point
	 * @param p2
	 *            second point
	 * @return A new Point representing the Min()
	 */
	public static Point min(Point p1, Point p2) {
		return new Point(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));
	}

	/**
	 * Rotates (in-place) the given {@link Point}s counter-clock-wise (CCW) by
	 * the specified {@link Angle} around the given center {@link Point}.
	 * 
	 * @param points
	 * @param angle
	 * @param cx
	 * @param cy
	 */
	public static void rotateCCW(Point[] points, Angle angle, double cx,
			double cy) {
		translate(points, -cx, -cy);
		for (Point p : points) {
			Point np = new Vector(p).rotateCCW(angle).toPoint();
			p.x = np.x;
			p.y = np.y;
		}
		translate(points, cx, cy);
	}

	/**
	 * Rotates (in-place) the given {@link Point}s clock-wise (CW) by the
	 * specified {@link Angle} around the given center {@link Point}.
	 * 
	 * @param points
	 * @param angle
	 * @param cx
	 * @param cy
	 */
	public static void rotateCW(Point[] points, Angle angle, double cx,
			double cy) {
		translate(points, -cx, -cy);
		for (Point p : points) {
			Point np = new Vector(p).rotateCW(angle).toPoint();
			p.x = np.x;
			p.y = np.y;
		}
		translate(points, cx, cy);
	}

	/**
	 * Scales the given array of {@link Point}s by the given x and y scale
	 * factors around the given center {@link Point} (cx, cy).
	 * 
	 * @param points
	 * @param fx
	 * @param fy
	 * @param cx
	 * @param cy
	 */
	public static void scale(Point[] points, double fx, double fy, double cx,
			double cy) {
		translate(points, -cx, -cy);
		for (Point p : points) {
			p.scale(fx, fy);
		}
		translate(points, cx, cy);
	}

	/**
	 * Translates an array of {@link Point}s by translating each individual
	 * point by a given x and y offset.
	 * 
	 * @param points
	 *            an array of points to translate
	 * @param dx
	 *            the x offset to translate each {@link Point} by
	 * @param dy
	 *            the y offset to translate each {@link Point} by
	 */
	public static void translate(Point[] points, double dx, double dy) {
		for (int i = 0; i < points.length; i++) {
			points[i].x += dx;
			points[i].y += dy;
		}
	}

	/**
	 * The x value.
	 */
	public double x;

	/**
	 * The y value.
	 */
	public double y;

	/**
	 * Constructs a Point at location (0,0).
	 * 
	 */
	public Point() {
	}

	/**
	 * Constructs a Point at the specified x and y locations.
	 * 
	 * @param x
	 *            x value
	 * @param y
	 *            y value
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs a Point at the same location as the given SWT Point.
	 * 
	 * @param p
	 *            Point from which the initial values are taken.
	 */
	public Point(org.eclipse.swt.graphics.Point p) {
		x = p.x;
		y = p.y;
	}

	/**
	 * Constructs a Point at the same location as the given Point.
	 * 
	 * @param p
	 *            Point from which the initial values are taken.
	 */
	public Point(Point p) {
		x = p.x;
		y = p.y;
	}

	/**
	 * Overwritten with public visibility as proposed in {@link Cloneable}.
	 */
	@Override
	public Point clone() {
		return getCopy();
	}

	/**
	 * Returns <code>true</code> if this Points x and y are equal to the given x
	 * and y.
	 * 
	 * @param x
	 *            the x value
	 * @param y
	 *            the y value
	 * @return <code>true</code> if this point's x and y are equal to those
	 *         given.
	 */
	public boolean equals(double x, double y) {
		return PrecisionUtils.equal(this.x, x)
				&& PrecisionUtils.equal(this.y, y);
	}

	/**
	 * Test for equality.
	 * 
	 * @param o
	 *            Object being tested for equality
	 * @return true if both x and y values are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Point) {
			Point p = (Point) o;
			return equals(p.x, p.y);
		}
		return false;
	}

	/**
	 * @return a copy of this Point
	 */
	public Point getCopy() {
		return new Point(this);
	}

	/**
	 * Calculates the distance from this Point to the one specified.
	 * 
	 * @param p
	 *            The Point being compared to this
	 * @return The distance
	 */
	public double getDistance(Point p) {
		double i = p.x - x;
		double j = p.y - y;
		return Math.sqrt(i * i + j * j);
	}

	/**
	 * Creates a Point with negated x and y values.
	 * 
	 * @return A new Point
	 */
	public Point getNegated() {
		return getCopy().negate();
	}

	/**
	 * Creates a new Point from this Point by scaling by the specified amount.
	 * 
	 * @param factor
	 *            scale factor
	 * @return A new Point
	 */
	public Point getScaled(double factor) {
		return getCopy().scale(factor);
	}

	/**
	 * Creates a new Point from this Point by scaling by the specified values.
	 * 
	 * @param xFactor
	 *            horizontal scale factor
	 * @param yFactor
	 *            vertical scale factor
	 * @return A new Point
	 */
	public Point getScaled(double xFactor, double yFactor) {
		return getCopy().scale(xFactor, yFactor);
	}

	/**
	 * Returns a new {@link Point} scaled by the given scale-factors. The
	 * scaling is performed relative to the given {@link Point} center.
	 * 
	 * @param factorX
	 *            The horizontal scale-factor
	 * @param factorY
	 *            The vertical scale-factor
	 * @param center
	 *            The relative {@link Point} for the scaling
	 * @return The new, scaled {@link Point}
	 */
	public Point getScaled(double factorX, double factorY, Point center) {
		return getCopy().scale(factorX, factorY, center);
	}

	/**
	 * Creates a new Point which is translated by the values of the input
	 * Dimension.
	 * 
	 * @param d
	 *            Dimension which provides the translation amounts.
	 * @return A new Point
	 */
	public Point getTranslated(Dimension d) {
		return getCopy().translate(d);
	}

	/**
	 * Creates a new Point which is translated by the specified x and y values
	 * 
	 * @param x
	 *            horizontal component
	 * @param y
	 *            vertical component
	 * @return A new Point
	 */
	public Point getTranslated(double x, double y) {
		return getCopy().translate(x, y);
	}

	/**
	 * Creates a new Point which is translated by the values of the provided
	 * Point.
	 * 
	 * @param p
	 *            Point which provides the translation amounts.
	 * @return A new Point
	 */
	public Point getTranslated(Point p) {
		return getCopy().translate(p);
	}

	/**
	 * Creates a new Point with the transposed values of this Point. Can be
	 * useful in orientation change calculations.
	 * 
	 * @return A new Point
	 */
	public Point getTransposed() {
		return getCopy().transpose();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// calculating a better hashCode is not possible, because due to the
		// imprecision, equals() is no longer transitive
		return 0;
	}

	/**
	 * Negates the x and y values of this Point.
	 * 
	 * @return <code>this</code> for convenience
	 */
	public Point negate() {
		scale(-1.0d);
		return this;
	}

	/**
	 * Scales this Point by the specified amount.
	 * 
	 * @return <code>this</code> for convenience
	 * @param factor
	 *            scale factor
	 */
	public Point scale(double factor) {
		return scale(factor, factor);
	}

	/**
	 * Scales this Point by the specified values.
	 * 
	 * @param xFactor
	 *            horizontal scale factor
	 * @param yFactor
	 *            vertical scale factor
	 * @return <code>this</code> for convenience
	 */
	public Point scale(double xFactor, double yFactor) {
		x *= xFactor;
		y *= yFactor;
		return this;
	}

	/**
	 * Scales this {@link Point} by the given scale-factors. The scaling is
	 * performed relative to the given {@link Point} center.
	 * 
	 * @param factorX
	 *            The horizontal scale-factor
	 * @param factorY
	 *            The vertical scale-factor
	 * @param center
	 *            The relative {@link Point} for the scaling
	 * @return <code>this</code> for convenience
	 */
	public Point scale(double factorX, double factorY, Point center) {
		translate(center.getNegated());
		scale(factorX, factorY);
		translate(center);
		return this;
	}

	/**
	 * Sets the location of this Point to the provided x and y locations.
	 * 
	 * @return <code>this</code> for convenience
	 * @param x
	 *            the x location
	 * @param y
	 *            the y location
	 */
	public Point setLocation(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Sets the location of this Point to the specified Point.
	 * 
	 * @return <code>this</code> for convenience
	 * @param p
	 *            the Location
	 */
	public Point setLocation(Point p) {
		x = p.x;
		y = p.y;
		return this;
	}

	/**
	 * Sets the x value of this Point to the given value.
	 * 
	 * @param x
	 *            The new x value
	 * @return this for convenience
	 */
	public Point setX(double x) {
		this.x = x;
		return this;
	}

	/**
	 * Sets the y value of this Point to the given value;
	 * 
	 * @param y
	 *            The new y value
	 * @return this for convenience
	 */
	public Point setY(double y) {
		this.y = y;
		return this;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "Point(" + x + ", " + y + ")";//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
	}

	/**
	 * Shifts this Point by the values of the Dimension along each axis, and
	 * returns this for convenience.
	 * 
	 * @param d
	 *            Dimension by which the origin is being shifted.
	 * @return <code>this</code> for convenience
	 */
	public Point translate(Dimension d) {
		return translate(d.width, d.height);
	}

	/**
	 * Shifts this Point by the values supplied along each axes, and returns
	 * this for convenience.
	 * 
	 * @param dx
	 *            Amount by which point is shifted along X axis.
	 * @param dy
	 *            Amount by which point is shifted along Y axis.
	 * @return <code>this</code> for convenience
	 */
	public Point translate(double dx, double dy) {
		x += dx;
		y += dy;
		return this;
	}

	/**
	 * Shifts the location of this Point by the location of the input Point
	 * along each of the axes, and returns this for convenience.
	 * 
	 * @param p
	 *            Point to which the origin is being shifted.
	 * @return <code>this</code> for convenience
	 */
	public Point translate(Point p) {
		return translate(p.x, p.y);
	}

	/**
	 * Transposes this object. X and Y values are exchanged.
	 * 
	 * @return <code>this</code> for convenience
	 */
	public Point transpose() {
		double temp = x;
		x = y;
		y = temp;
		return this;
	}

	/**
	 * Returns the x value of this Point.
	 * 
	 * @return The current x value
	 */
	public double x() {
		return x;
	}

	/**
	 * Returns the y value of this Point.
	 * 
	 * @return The current y value
	 */
	public double y() {
		return y;
	}

}
