/*******************************************************************************
 * Copyright (c) 2011, 2012 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     Matthias Wienand (itemis AG) - contribution for Bugzilla #355997
 *     
 *******************************************************************************/
package org.eclipse.gef4.geometry.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AffineTransformTests.class, AngleTests.class, ArcTests.class,
		BezierCurveTests.class, CubicCurveTests.class,
		CurvedPolygonTests.class, CurveUtilsTests.class, DimensionTests.class,
		EllipseTests.class, IGeometryTests.class, LineTests.class,
		PathTests.class, PointListUtilsTests.class, PointTests.class,
		PolygonTests.class, PolylineTests.class, PrecisionUtilsTests.class,
		QuadraticCurveTests.class, RectangleTests.class, RegionTests.class,
		RingTests.class, RoundedRectangleTests.class, StraightTests.class,
		SWTConversionTests.class, VectorTests.class, Vector3DTests.class })
public class AllTests {

}
