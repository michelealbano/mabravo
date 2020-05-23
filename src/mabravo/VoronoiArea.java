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

public class VoronoiArea
{
	public SFVoronoi graph;
	private int nextID;

	private boolean modified;
	private VoronoiNetwork voronoiNetwork;

	// Hash table to get point id from the point
	private Hashtable<Point2D, Integer> PointtoIdmap;

	/** Constructor */
	public VoronoiArea(VoronoiNetwork vn)
	{
		voronoiNetwork = vn;
		PointtoIdmap = new Hashtable<Point2D, Integer>();
		graph = new SFVoronoi();
		nextID = 0;
		modified=true;
	}

 	/**
 	 * Verify if point me is a VN of side lato
 	 * @param me id of a point
     * @param lato a side
 	 * @return true if me is actually a VN w.r.t. side lato
 	 */
	public boolean vn_control(int me, line2d lato)
	{
		if((lato.vertexIndex[0]==-1)&&(lato.vertexIndex[1]==-1))
			return true;

		if((lato.vertexIndex[0]!=-1)&&(lato.vertexIndex[1]!=-1))
		{
			Point2D p1 = graph.mVertices.get(lato.vertexIndex[0]);
			Point2D p2 = graph.mVertices.get(lato.vertexIndex[1]);

			Line2D.Double l = new Line2D.Double(p1,p2);
			if(l.intersects(voronoiNetwork.diagramma))
				return true;
			else
				return false;
		}

		int id = lato.vertexIndex[0]==-1?lato.vertexIndex[1]:lato.vertexIndex[0];

		if((graph.mVertices.get(id)).getX()<0)
			return lato.vertexIndex[0]==-1?false:true;

		if((graph.mVertices.get(id)).getX()>voronoiNetwork.diagramma.getMaxX())
			return lato.vertexIndex[0]==-1?true:false;

		if((graph.mVertices.get(id)).getY()<0)
		{
			if(lato.b==0) 
				return lato.vertexIndex[0]==-1?true:false;
			
			double q = (lato.c / lato.b) - graph.mVertices.get(id).getY();

			if(lato.vertexIndex[0]==-1)
				return q>0?true:false;
			else 
				return q <0?true:false;
		}
		
		if (graph.mVertices.get(id).getY()>voronoiNetwork.diagramma.getMaxY())
		{
			if(lato.b==0) 
				return lato.vertexIndex[0]==-1?false:true;
			
			double q = (lato.c / lato.b) - graph.mVertices.get(id).getY();
			
			if(lato.vertexIndex[0]==-1)
				return q<0?true:false;
			else 
				return q>0?true:false;
		}
		
		return true;
	}

	/**
	 * Returns the VAST Id of point p
	 * @param p point that is part of a VAST Voronoi graph
	 * @return id of p
	 */
	public int getID(Point2D p)
	{
		Integer ris = PointtoIdmap.get(p);
		if(ris==null)
			return -1;
		else
			return ris.intValue();
	}	

	/**
	 * Inserts a point into a Voronoi diagram
	 * 
	 * @param p a point
	 * @return true if successful
	 */
	public boolean insertPoint(Point2D p)
	{
        // if a point is already in the graph, it returns false
		if (PointtoIdmap.get(p)!=null)
		{
            return false;
		}

		// inserts a point in the graph
		graph.insert(nextID, p);
		nextID++;
		modified=true;
		return true;
	}

	public static void main(String[] args) {
        int debug = 1;
		java.util.Random r = new java.util.Random();
        int w = 1000000;
        int h = 1000000;
		long timeold = java.lang.System.currentTimeMillis();
		try {
            for (int round = 950 ; round <= 1000 ; round++) {
                SFVoronoi graph = new SFVoronoi();

                for (int i = 0 ; i < 1000*round ; i++) {
                    graph.insert(i, new Point2D.Double(r.nextInt(w), r.nextInt(h)));
                }
                // force the update of the Voronoi graph
                graph.get_en(0);
                for (int i=0;i<graph.size();i++) {
                    if (debug > 2)
                        System.out.println("site: (" + i + ") point: [" + graph.get(i).getX() + ", " + graph.get(i).getY() + ")\n");
                }
                for (int i = 0 ; i < graph.mVertices.size() ; i++) {
                    Point2D p = graph.mVertices.elementAt(i);
                    if (debug > 2)
                        System.out.println("vertex: " + i + " (" + p.getX() + ", " + p.getY() + ")\n");
                }
                Iterator<line2d> it3 = graph.mEdges.iterator();
                while (it3.hasNext ()) {
                    line2d l = it3.next();
                    if (debug > 2)
                        System.out.println("edge: ((" + l.getX1() + ", " + l.getY1() + "), (" + l.getX2() + ", " + l.getY2() +
                    ")) vertices (" + l.vertexIndex[0] + ", " + l.vertexIndex[1] + ") bisecting "+ l.bisectingID[0] +", " + l.bisectingID[1] + "\n");
                }
                long timenow = java.lang.System.currentTimeMillis();
                System.out.println("round " + round + " time " + (timenow-timeold));
                timeold = timenow;
                /*return graph.contains(0, p);*/
            }
		} catch (Exception e) {
			System.out.println("exception " + e.toString()+"\n\n");
		}
	}
}
