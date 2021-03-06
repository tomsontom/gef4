/*******************************************************************************
 * Copyright (c) 2011 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *     
 *******************************************************************************/
package org.eclipse.gef4.geometry.examples.intersection;

import org.eclipse.gef4.geometry.convert.Geometry2SWT;
import org.eclipse.gef4.geometry.planar.IGeometry;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.geometry.planar.Polygon;
import org.eclipse.gef4.geometry.planar.Polyline;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;

public class PolygonPolylineIntersection extends
		AbstractPolygonIntersectionExample {

	public static void main(String[] args) {
		new PolygonPolylineIntersection();
	}

	public PolygonPolylineIntersection() {
		super("Polygon/Polyline Intersection");
	}

	@Override
	protected Point[] computeIntersections(IGeometry g1, IGeometry g2) {
		return ((Polygon) g1).getOutline().getIntersections((Polyline) g2);
	}

	@Override
	protected AbstractControllableShape createControllableShape2(Canvas canvas) {
		return new AbstractControllableShape(canvas) {
			@Override
			public void createControlPoints() {
				addControlPoint(new Point(100, 100));
				addControlPoint(new Point(100, 300));
				addControlPoint(new Point(300, 300));
				addControlPoint(new Point(300, 100));
			}

			@Override
			public Polyline createGeometry() {
				Point[] points = getControlPoints();
				Polyline polyline = new Polyline(points);
				return polyline;
			}

			@Override
			public void drawShape(GC gc) {
				Polyline polyline = createGeometry();
				gc.drawPolyline(Geometry2SWT.toSWTPointArray(polyline));
			}
		};
	}

}
