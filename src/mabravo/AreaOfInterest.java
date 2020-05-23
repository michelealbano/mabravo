/*
 * MABRAVO, AoI-based Multicast Routing over Voronoi Overlays with minimal overhead
 * Copyright (C) 2020 Michele Albano (mialb@cs.aau.dk)
 *
 * This file is part of MABRAVO.
 * 
 * MABRAVO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MABRAVO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MABRAVO.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package mabravo;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;
import vast.SFVoronoi;
import vast.line2d;

/**
 * Class for the convex AoI
 *
 * @author Michele Albano
 */
public class AreaOfInterest
{
	public Point2D[] aoiPoints=null;

	public static int global_debug = 0;
	private double crossProduct(Point2D[] p) {
		return crossProduct(p[0], p[1], p[2]);
	}

	private static double crossProduct(Point2D p0, Point2D p1, Point2D p2) {
		double x1 = p0.getX()-p1.getX();
		double x2 = p2.getX()-p1.getX();
		double y1 = p0.getY()-p1.getY();
		double y2 = p2.getY()-p1.getY();
		return x1*y2-x2*y1;
	}

	private static Point2D[] extractPointsForConvex(Point2D[] allPoints) {
		if (allPoints.length < 3) return null;
		Vector<Integer> ret = new Vector<Integer>();
		int currentPoint = 0;
		for (int i=0 ; i<allPoints.length ; i++) {
			if (allPoints[i].getX() < allPoints[currentPoint].getX())
				currentPoint = i;
		}
		boolean go_on = true;
		int nextPoint;
		while (go_on) {
			ret.add(currentPoint);
			nextPoint = (currentPoint+1)%allPoints.length;
			for (int i = 0 ; i < allPoints.length ; i++)
				if (i!=currentPoint && i!=nextPoint)
					if (crossProduct(allPoints[currentPoint], allPoints[nextPoint], allPoints[i]) < 0)
						nextPoint = i;
			if (nextPoint == ret.get(0)) go_on = false;
			else currentPoint = nextPoint;
		}
		Point2D[] result = new Point2D[ret.size()];
		for (int i = 0 ; i < ret.size() ; i++) {
			result[i] = allPoints[ret.get(i)];
		}
		return result;
	}

	private boolean isConvex(Point2D[] p) {
		int plength = p.length;

		if (plength<3) return false;
		double last_product = crossProduct(p[0], p[1], p[2]);
		for (int i=1;i<plength;i++) {
			double new_product = crossProduct(p[i%plength], p[(i+1)%plength], p[(i+2)%plength]);
			if (last_product * new_product < 0) return false;
		}
		
		for (int i=0;i<plength;i++)
			for (int j=i+1;j<plength;j++)
				if (Line2D.linesIntersect(p[i].getX(), p[i].getY(),
					p[(i+1)%plength].getX(), p[(i+1)%plength].getY(),
					p[j].getX(), p[j].getY(),
					p[(j+1)%plength].getX(), p[(j+1)%plength].getY())
					) return false;
		return true;
	}

	public boolean intersect(line2d l1, boolean ignore_ends) {
		int plength = aoiPoints.length;
		for (int i=0;i<plength;i++)
			if (Line2D.linesIntersect(l1.getX1(), l1.getY1(),
				l1.getX2(), l1.getY2(),
				aoiPoints[i].getX(), aoiPoints[i].getY(),
				aoiPoints[(i+1)%plength].getX(), aoiPoints[(i+1)%plength].getY())
				) return true;
		return false;
	}

	/** Constructor */
	public AreaOfInterest(int x,int y, Point2D[] points)
	{
		aoiPoints = extractPointsForConvex(points);
	}

	public boolean isInAoI(Point2D punto)
	{
		int count_intersect=0;
		for (int i=0;i<aoiPoints.length;i++)
			if (Line2D.linesIntersect(punto.getX(), punto.getY(),
				0, 0,
				aoiPoints[i].getX(), aoiPoints[i].getY(),
				aoiPoints[(i+1)%aoiPoints.length].getX(), aoiPoints[(i+1)%aoiPoints.length].getY())
				) count_intersect++;
		if (global_debug>99) System.err.println("count_intersect = "+count_intersect);
		if (count_intersect%2==1) return true;
		return false;
	}

	public boolean isInAoI(Vector<line2d> allEdges)
	{
		int itero = 0;
		Iterator<line2d> it4 = allEdges.iterator();
		while (it4.hasNext ()) {
			if (global_debug>99) System.err.println("isInAoI iteration " + itero); itero++;
			line2d l = it4.next();
			for (int i=0;i<aoiPoints.length;i++) {
				// verify if an edge intersects the borders of the AoI
				if (Line2D.linesIntersect(l.getX1(), l.getY1(),
					l.getX2(), l.getY2(),
					aoiPoints[i].getX(), aoiPoints[i].getY(),
					aoiPoints[(i+1)%aoiPoints.length].getX(), aoiPoints[(i+1)%aoiPoints.length].getY())
					) return true;
			}
		}
		if (global_debug>99) System.err.println("does NOT cross the borders");
		Point2D v1 = allEdges.get(0).getP1();
		return isInAoI(v1);
	}

	public static void main(String[] args) {
		java.util.Random r = new java.util.Random();

        int width = 1000000;
        int height = 1000000;
		long timeold = java.lang.System.currentTimeMillis();
        for (int round = 950 ; round <= 1000 ; round++) {
            SFVoronoi graph = new SFVoronoi();

            for (int i = 0 ; i < 1000*round ; i++) {
                graph.insert(i, new Point2D.Double(r.nextInt(width), r.nextInt(height)));
            }

            long timenow = java.lang.System.currentTimeMillis();
            System.out.println("round " + round + " time " + (timenow-timeold));
            timeold = timenow;
        }
	}
}
