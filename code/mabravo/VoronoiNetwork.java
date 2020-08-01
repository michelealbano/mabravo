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
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;
import vast.SFVoronoi;
import vast.line2d;

public class VoronoiNetwork
{
	public VoronoiArea globalVoronoiArea;
	private int number_of_points;
	public boolean[] siteInAoI;
	public int[] visit_in_bfs;
	public Rectangle diagramma;

	public Vector<Integer> neighbors(int ID) {
		Vector<Integer> neighs = new Vector<Integer>();
		// Get through all sides to identify the neighbors of point Id
		Vector<line2d> mEdges = globalVoronoiArea.graph.mEdges;
		for (int i = 0; i < mEdges.size(); i++) {
			line2d l = mEdges.get(i);
			if (l.bisectingID[0] == ID|| l.bisectingID[1] == ID)
				if(globalVoronoiArea.vn_control(ID,l)) {
					Integer adding = l.bisectingID[0] == ID?l.bisectingID[1]:l.bisectingID[0];
					if (!neighs.contains(adding))
						neighs.add(adding);
				}
		}
		return neighs;
	}

	private Integer find_neighbor(Integer si, Integer sj, Integer vertex_index) {
		if (global_debug > 6)
			System.out.println("si, sj, vertex_index: "+ si + ", " + sj + ", " + vertex_index);
		int count_find = 0;
		Integer sk = null;
		SFVoronoi graph = globalVoronoiArea.graph;
		for (int i = 0; i < graph.mEdges.size(); i++) {
			line2d l = graph.mEdges.get(i);
			if (
			(l.bisectingID[0] == si || l.bisectingID[1] == si) &&
			(l.vertexIndex[0] == vertex_index || l.vertexIndex[1] == vertex_index)
			) {
				Integer candidate = l.bisectingID[0] == si?l.bisectingID[1]:l.bisectingID[0];
				if (!candidate.equals(sj)) {
					if (sk != null) System.err.println("\n\n!!!incongruence regarding the borders of type 1: candidate = "+ candidate+ " sk = "+sk+", sj = "+sj+", si ="+si+"!!!\n\n");
					sk = candidate;
				}
			}
		}
		for (int i = 0; i < graph.mEdges.size(); i++) {
			line2d l = graph.mEdges.get(i);
			if (
			(l.bisectingID[0] == sj || l.bisectingID[1] == sj) &&
			(l.vertexIndex[0] == vertex_index || l.vertexIndex[1] == vertex_index)
			) {
				Integer candidate = l.bisectingID[0] == sj?l.bisectingID[1]:l.bisectingID[0];
				if (!candidate.equals(si) && !candidate.equals(sk)) System.err.println("\n\n!!!incongruence regarding the borders of type 2!!!:"+ candidate + " is not "+ sk+ "\n\n");
			}
		}
		if (sk == null) System.err.println("\n\n\n!!!I cannot a border!!!\n\n");
		if (global_debug > 6)
			System.out.println("I will return sk: "+ sk);

		return sk;
	}

	public boolean mabravo_decision(Point2D root, Integer si, Integer sj, boolean aoi_only, AreaOfInterest aoi)
	{
		if (global_debug > 5)
			System.out.println("I am node " + si + " considering node "+ sj);
		SFVoronoi graph = globalVoronoiArea.graph;
		Point2D pi = graph.get_sites().get(si);
		Point2D pj = graph.get_sites().get(sj);
		if (root.distanceSq(pi) > root.distanceSq(pj)) {

			if (global_debug > 5)
				System.out.println("node rejected for the distance");
			return false;
		}

		line2d border = null;
		for (int i = 0; i < graph.mEdges.size(); i++) {
			line2d l = graph.mEdges.get(i);
			if (
			(l.bisectingID[0] == si && l.bisectingID[1] == sj) ||
			(l.bisectingID[0] == sj && l.bisectingID[1] == si)
			) {
				if (border != null) System.err.println("\n\n!!!incongruence regarding the borders of type 3!!!\n\n");
				border = l;
			}
		}
		if (border == null) System.err.println("\n\n\n!!!I cannot find a border!!!\n\n");
		Point2D v1 = null;
		Point2D v2 = null;
		boolean v1inAoI = false;
		boolean v2inAoI = false;

		if (-1 != border.vertexIndex[0]) {
			v1 = graph.mVertices.elementAt(border.vertexIndex[0]);
			v1inAoI = aoi.isInAoI(v1);
		}
		if (-1 != border.vertexIndex[1]) {
			v2 = graph.mVertices.elementAt(border.vertexIndex[1]);
			v2inAoI = aoi.isInAoI(v2);
		}
		if (!v1inAoI && !v2inAoI) {
			if (aoi.intersect(border, false)) {
				if (global_debug > 5)
					System.out.println("sending the packet, the node has a border crossing the AoI");
				return true;
			}
			if (global_debug > 5)
				System.out.println("rejected, node totally out");
			return false;
		}
		if (v1inAoI && border.vertexIndex[0] != -1) {
			Integer sk = find_neighbor(si, sj, border.vertexIndex[0]);
			Point2D pk = graph.get_sites().get(sk);
			if (
				(root.distanceSq(pk) < root.distanceSq(pj))&&
                    ( // large cos2(angle) means that the abs(angle) is close to 0
                        (cos2(root, pj, pk) > cos2(root, pj, pi)) ||
                        (cos2(root, pj, pk) == cos2(root, pj, pi) && sk < si)
                    )
				) {
					if (global_debug > 5)
						System.out.println("rejected, node "+sk+" would be better");
					return false;
				}
		}
		if (v2inAoI && border.vertexIndex[1] != -1) {
			Integer sk = find_neighbor(si, sj, border.vertexIndex[1]);
			Point2D pk = graph.get_sites().get(sk);
			if (
				(root.distanceSq(pk) < root.distanceSq(pj))&&
                    ( // large cos2(angle) means that the abs(angle) is close to 0
                        (cos2(root, pj, pk) > cos2(root, pj, pi)) ||
                        (cos2(root, pj, pk) == cos2(root, pj, pi) && sk < si)
                    )
//				cos2(root, pj, pk) > cos2(root, pj, pi)
				) {
					if (global_debug > 5)
						System.out.println("rejected, node "+sk+" would be better");
					return false;
				}
		}
		if (global_debug > 5)
			System.out.println("sending the packet, node is good");
		return true;
	}

	public boolean mabravo_visit(int first_node, boolean aoi_only, AreaOfInterest aoi, Point2D root) {
		if (aoi_only && siteInAoI == null) return false;
		visit_in_bfs = new int[number_of_points];
		for (int i = 0;i<number_of_points;i++) visit_in_bfs[i] = -1;
		visit_in_bfs[first_node] = 0;
		int round = 0;
		boolean go_on = true;
		while (go_on) {
			go_on = false;
			for (int i = 0;i<number_of_points;i++)
				if (round == visit_in_bfs[i]) {
					Iterator<Integer> it2 = neighbors(i).iterator();
					while (it2.hasNext ()) {
						Integer site_id = it2.next();
						boolean relay = mabravo_decision(root, i, site_id, aoi_only, aoi);
						if (relay) {
							if (visit_in_bfs[site_id] != -1)
								System.err.println("\n\n\n!!!node received a packet from 2 nodes!!!\n\n");
							visit_in_bfs[site_id] = round+1;
							go_on=true;
						}
					}
				}
			round++;
		}
		return true;
	}

	public boolean bfsVisit(int first_node, boolean aoi_only) {
		if (aoi_only && siteInAoI == null) return false;
		visit_in_bfs = new int[number_of_points];
		for (int i = 0;i<number_of_points;i++) visit_in_bfs[i] = -1;
		visit_in_bfs[first_node] = 0;
		int round = 0;
		boolean go_on = true;
		while (go_on) {
			go_on = false;
			for (int i = 0;i<number_of_points;i++)
				if (round == visit_in_bfs[i]) {
					Iterator<Integer> it2 = neighbors(i).iterator();
					while (it2.hasNext ()) {
						Integer site_id = it2.next();
						if (-1 == visit_in_bfs[site_id])
							if (!aoi_only || siteInAoI[site_id]) {
								visit_in_bfs[site_id] = round+1;
								go_on = true;
							}
					}
				}
			round++;
		}
		return true;
	}

	/** Constructor */
	public VoronoiNetwork(int x, int y)
	{
		visit_in_bfs = null;
		siteInAoI = null;
		number_of_points = 0;
		diagramma = new Rectangle(x,y);
		globalVoronoiArea = null;
	}

	/**
	 * Resets the network
	 */
	public void reset()
	{
		visit_in_bfs = null;
		siteInAoI = null;
		number_of_points = 0;
		diagramma = new Rectangle(diagramma.width, diagramma.height);
		globalVoronoiArea = null;
	}

	void tagAoI(AreaOfInterest aoi) {
		siteInAoI = new boolean[number_of_points];
		for (int k=0;k<number_of_points;k++) {
			Vector<line2d> Area = new Vector<line2d>();
			for (int i = 0; i < globalVoronoiArea.graph.mEdges.size(); i++) {
				line2d l = globalVoronoiArea.graph.mEdges.get(i);
				if (l.bisectingID[0] == k || l.bisectingID[1] == k)
					Area.add(l);
			}
			if (aoi.isInAoI(Area))
				siteInAoI[k]=true;
			else
				siteInAoI[k] = false;
		}
	}

	private Integer makeRoutingStep(Point2D destination, Integer ID, AreaOfInterest aoi)
	{
		Vector<Integer> ris = new Vector<Integer>();
		SFVoronoi graph = globalVoronoiArea.graph;
		Point2D current_site = graph.get_sites().get(ID);
		double dist_old = destination.distanceSq(current_site);
		boolean at_least_one_closer = false;

		// Get through all sides to identify neighboring points
		for (int i = 0; i < graph.mEdges.size(); i++) {
			line2d l = graph.mEdges.get(i);
			if (l.bisectingID[0] == ID || l.bisectingID[1] == ID)
				if(globalVoronoiArea.vn_control(ID,l)) {
					Integer adding = l.bisectingID[0] == ID?l.bisectingID[1]:l.bisectingID[0];
					Point2D next_site = graph.get_sites().get(adding);

					double dist = destination.distanceSq(next_site);
					if (global_debug > 2)
						System.out.println("distance^2 of "+adding+" is "+dist+ " vs " + dist_old);
					if (dist < dist_old) at_least_one_closer = true;
					boolean v1ok = true;
					Point2D v1 = null;
					if (l.vertexIndex[0] != -1) {
						v1 = graph.mVertices.elementAt(l.vertexIndex[0]);
						v1ok = aoi.isInAoI(v1);
					}
					boolean v2ok = true;
					Point2D v2 = null;
					if (l.vertexIndex[1] != -1) {
            v2 = graph.mVertices.elementAt(l.vertexIndex[1]);
			v2ok = aoi.isInAoI(v2);
					}

					if (global_debug > 2)
						System.out.println("vertici di merda: ("+l.vertexIndex[0]+", "+v1.getX()+", "+v1.getY()+ ", " + v1ok+ ") ("+l.vertexIndex[1]+", "+v2.getX()+", "+v2.getY()+", "+ v2ok + ")");
					if (dist <= dist_old &&(v1ok||v2ok))
						ris.add(adding);
				}
		}
		if (!at_least_one_closer) return null;
		if (ris.size()==0) {
			for (int i = 0; i < graph.mEdges.size(); i++) {
				line2d l = graph.mEdges.get(i);
				if (l.bisectingID[0] == ID || l.bisectingID[1] == ID)
					if(globalVoronoiArea.vn_control(ID,l)) {
						Integer adding = l.bisectingID[0] == ID?l.bisectingID[1]:l.bisectingID[0];
						Point2D next_site = graph.get_sites().get(adding);

						double dist = destination.distanceSq(next_site);
						if (dist < dist_old) {
							if (aoi.intersect(l, false))
								return adding;
						}
					}
			}
			// if I get here, there is a very bad bug
			System.err.println("\n\n!!!super bug!!!\n\n");
			return null;
		} else {
			Integer best_site = -1;
			double best_cos = -1;
			Iterator<Integer> it2 = ris.iterator();
			// looking for the max cosin
			while (it2.hasNext ()) {
				Integer site_id = it2.next();
				Point2D site_coord = graph.get_sites().get(site_id);
				double cos = cos2(destination, current_site, site_coord);
				if (cos > best_cos || -1 == best_site || (cos == best_cos && site_id < best_site)) {
					best_cos = cos;
					best_site = site_id;
				}
			}
			return best_site;
		}
	}

	private double cos2(Point2D destination, Point2D current_site, Point2D site_coord) {
		double v1x = destination.getX()-current_site.getX();
		double v1y = destination.getY()-current_site.getY();
		double v2x = site_coord.getX()-current_site.getX();
		double v2y = site_coord.getY()-current_site.getY();
		double cos = v1x * v2x + v1y * v2y;
		if (cos < 0) cos = - cos * cos;
		else cos = cos * cos;
		cos = cos / destination.distanceSq(current_site);
		cos = cos / site_coord.distanceSq(current_site);
		return cos;
	}

	public Vector<Integer> computeRouting(Point2D src, Point2D dst, AreaOfInterest aoi) {
		Vector<Integer> rotta = new Vector<Integer>();
		int current = point_to_site(src);
		boolean go_on = true;
		while (go_on) {
			rotta.add(current);
			Integer next_site = makeRoutingStep(dst, current, aoi);
			if (next_site == null)
				go_on = false;
			else {
				current = next_site;
			}
		}
		return rotta;
	}

	public int point_to_site(Point2D coord) {
		return globalVoronoiArea.graph.closest_to(coord);
	}

	public String SFVoronoi_to_String(SFVoronoi graph) {
		// Force the update of the Voronoi graph
        graph.get_en(0);

		StringBuilder sb = new StringBuilder();
		sb.append("graph with sites, vertices, edges, " + graph.size() + ", " + graph.mVertices.size() + ", " + graph.mEdges.size() + "\n");
		if (global_debug > 2) {
			Set<Integer> keys = graph.get_sites().keySet();
			for(Integer key: keys)
				sb.append("\t\tsite: (" + key + ") point: [" + graph.get(key).getX() + ", " + graph.get(key).getY() + ")\n");
		}
		for (int i = 0 ; i < graph.mVertices.size() ; i++) {
            Point2D p = graph.mVertices.elementAt(i);
			if (global_debug > 2)
				sb.append("\t\tvertex: " + i + " (" + p.getX() + ", " + p.getY() + ")\n");
		}
        Iterator<line2d> it3 = graph.mEdges.iterator();
        while (it3.hasNext ()) {
            line2d l = it3.next();
			if (global_debug > 2)
				sb.append("\t\tedge: ((" + l.getX1() + ", " + l.getY1() + "), (" + l.getX2() + ", " + l.getY2() +
			")) vertices (" + l.vertexIndex[0] + ", " + l.vertexIndex[1] + ") bisecting "+ l.bisectingID[0] +", " + l.bisectingID[1] + "\n");
		}
		return sb.toString();
	}

	public void createVoronoiNetwork(Point2D[] points) {
		number_of_points = points.length;
		globalVoronoiArea = new VoronoiArea(this);
		for (int i = 0 ; i < number_of_points ; i++) {
            globalVoronoiArea.insertPoint(points[i]);
		}
	}

	private int global_debug = 2;
	public static void main(String[] args) {
		java.util.Random r = new java.util.Random(123456);
		int w = 1000000; int h = 1000000;
		long timeold = java.lang.System.currentTimeMillis();
			int number_of_points = 1000;
			VoronoiNetwork myNetwork = new VoronoiNetwork(w, h);
			SFVoronoi graph = new SFVoronoi();
			Point2D[] points = new Point2D[number_of_points];
			for (int i = 0 ; i < number_of_points ; i++) {
				points[i] = new Point2D.Double(r.nextInt(w), r.nextInt(h));
				graph.insert(i, points[i]);
			}
			// force update of the Voronoi graph
            graph.get_en(0);

			myNetwork.createVoronoiNetwork(points);
			System.out.println("\ngraph:\n"+myNetwork.SFVoronoi_to_String(graph));
			System.out.println("\nglobal network:\n"+myNetwork.SFVoronoi_to_String(myNetwork.globalVoronoiArea.graph));


			long timenow = java.lang.System.currentTimeMillis();
			System.out.println("time elapsed " + (timenow-timeold));
			timeold = timenow;
	}
}
