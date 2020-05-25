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

import java.awt.Font;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.util.Random;
import vast.line2d;
import vast.SFVoronoi;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;

public class Mabravo extends Canvas implements KeyListener {
	static private int sizex = 500, sizey = 500; // size of the canva
	static int ballSize = 6;
	int width = 1000, height = 1000;
	Point2D[] source = null;
	Point2D[] dest = null;
    private int number_of_sites_defining_the_AoI;
	AreaOfInterest aoi=null;
	VoronoiNetwork vn=null;
    Vector<Integer> all_sites_touched=null;
	Random r = new Random();

    public void initExperiment(Random generator, int aoi, int sites) {
        this.r = generator;
        this.aoi = createAoI(aoi);
        this.vn = createVN(sites);
    }

	public static void main(String[] args) {
		if (args.length == 5) {
			System.out.println("nodes vertices_aoi packets networks seed");
			System.out.println(args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]+"\n");
			Random randomgenerator = new Random(Integer.parseInt(args[4]));
			int numnetworks = Integer.parseInt(args[3]);
			for (int i = 0 ; i < numnetworks ; i++) {
				Mabravo canvas = new Mabravo();
                canvas.number_of_sites_defining_the_AoI = Integer.parseInt(args[1]);
                canvas.initExperiment(randomgenerator, canvas.number_of_sites_defining_the_AoI, Integer.parseInt(args[0]));

				int num_experiments = Integer.parseInt(args[2]);
				canvas.processVoronoiNetwork(num_experiments);
			}
            System.out.println("End -------------------------");
		} else if (args.length == 3) {
			JFrame frame = new JFrame("My Drawing");
			Random randomgenerator = new Random(Integer.parseInt(args[2]));
            Mabravo canvas = new Mabravo();
            canvas.number_of_sites_defining_the_AoI = Integer.parseInt(args[1]);
            canvas.initExperiment(randomgenerator, canvas.number_of_sites_defining_the_AoI, Integer.parseInt(args[0]));
            canvas.all_sites_touched = new Vector<Integer>();
			int num_experiments = 1;
			canvas.processVoronoiNetwork(num_experiments);
			canvas.setSize(sizex, sizey);
			frame.add(canvas);
			frame.pack();
			frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            canvas.addKeyListener(canvas);
            System.out.println("press a number for new experiment. ESC to quit");
		} else {
			System.out.println("Mabravo over Voronoi networks\n");
			System.out.println("Execute me either as:\n");
			System.out.println("\tgraphical mode: pass me 3 parameters\n");
			System.out.println("\t\tnodes vertices_aoi random_seed\n");
			System.out.println("\tbatch mode: pass me 5 parameters\n");
			System.out.println("nodes vertices_aoi number_of_packets number_of_networks random_seed\n");
			System.out.println("Chef's suggestion:\n");
			System.out.println("\t\t100 10 1000\n");
        }
	}

    public void keyPressed(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (27==c) System.exit(0);
        if (c==10) {
            long newSeed = r.nextLong();
            System.out.println(Long.toString(newSeed) + " new seed");
            r.setSeed(newSeed);
            initExperiment(r, number_of_sites_defining_the_AoI, vn.globalVoronoiArea.graph.size());
            all_sites_touched = new Vector<Integer>();
			int num_experiments = 1;
			processVoronoiNetwork(num_experiments);
        }
        repaint();
    }

	boolean fileDumper(String filename) {
/*		File toSave = new File(filename);
		try {
			WritableImage writableImage = new WritableImage(this.getWidth(), this.getHeight());
			canvas.snapshot(null, writableImage);
			RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
			ImageIO.write(renderedImage, "png", file);
		} catch (IOException ex) {
			Logger.getLogger(JavaFX_DrawOnCanvas.class.getName()).log(Level.SEVERE, null, ex);
		}*/
		return true;
	}

	public void processVoronoiNetwork(int num_experiments) {
        System.out.println("\nStart of experiment ---------");

		System.out.println(vn.SFVoronoi_to_String(vn.globalVoronoiArea.graph));
		vn.tagAoI(aoi);
		source = new Point2D[num_experiments];
		dest = new Point2D[num_experiments];
		for (int i = 0 ; i < num_experiments ; i++) {
			source[i] = randomPointInAoI();
			dest[i] = randomPointInAoI();
		}
		System.out.println("src, dst, total nodes, nodes in AoI, unicast route length (oracle), avg AoIcast route (oracle), avg AoIcast route (mabravo), unicasts route (mabravo):");
		for (int i = 0 ; i < source.length ; i++) {
			Vector<Integer> rotta = vn.computeRouting(source[i], dest[i], aoi);
			StringBuilder sb = new StringBuilder();
			StringBuilder sbroute = new StringBuilder();
			sbroute.append("(");
			int num_node_route = 0;
			Iterator<Integer> it8 = rotta.iterator();
			while (it8.hasNext ()) {
				Integer l = it8.next();
				sbroute.append(l + ",");
                if (null!=all_sites_touched) all_sites_touched.add(l);
				num_node_route++;
			}
			sbroute.append(")");
			int source_id = vn.point_to_site(source[i]);
			int dest_id = vn.point_to_site(dest[i]);
			sb.append(source_id + ", " + dest_id + ", ");

			vn.bfsVisit(source_id, false);
			int visited_nodes = 0;
			for (int j=0;j<vn.visit_in_bfs.length;j++) {
				if (vn.visit_in_bfs[j] != -1) visited_nodes++;
				else System.out.println("rogue node "+j);
			}
			sb.append(visited_nodes+", ");
			vn.bfsVisit(source_id, true);
			visited_nodes = 0;
			double total_steps = 0;
			for (int j=0;j<vn.visit_in_bfs.length;j++) if (vn.visit_in_bfs[j] != -1) {
				visited_nodes++;
				total_steps += vn.visit_in_bfs[j];
			}
			int[] visit_traditional_bfs = vn.visit_in_bfs.clone();
			sb.append(visited_nodes+", "+ vn.visit_in_bfs[dest_id] + ", " + (total_steps / visited_nodes));
			vn.mabravo_visit(source_id, true, aoi, source[i]);
			for (int j=0;j<vn.visit_in_bfs.length;j++) {
				if (visit_traditional_bfs[j]*vn.visit_in_bfs[j]<0)
					System.err.println("\n\n!!!mabravo and the breadth first visit do not agree regarding which nodes should receive packets: node "+j+" visit "+visit_traditional_bfs[j]+" mabravo "+vn.visit_in_bfs[j]+"!!!\n\n");
			}
			visited_nodes = 0;
			total_steps = 0;
			for (int j=0;j<vn.visit_in_bfs.length;j++) if (vn.visit_in_bfs[j] != -1) {
				visited_nodes++;
				total_steps += vn.visit_in_bfs[j];
			}
			sb.append(", " + (total_steps / visited_nodes));
			sb.append(", "+(num_node_route-1)+": "+sbroute.toString());

			System.out.println(sb.toString());
		}
	}


	private AreaOfInterest createAoI(int points_of_aoi) {
		long timeold = java.lang.System.currentTimeMillis();

		Point2D[] points = new Point2D[points_of_aoi];
		for (int i = 0 ; i < points.length ; i++) {
			points[i] = new Point2D.Double(r.nextDouble()*width, r.nextDouble()*height);
		}
		AreaOfInterest aoi = new AreaOfInterest(width, height, points);

		long timenow = java.lang.System.currentTimeMillis();
//		System.out.println("time " + (timenow-timeold));
		return aoi;
	}

	private VoronoiNetwork createVN(int number_of_points) {
		long timeold = java.lang.System.currentTimeMillis();

		VoronoiNetwork myNetwork = new VoronoiNetwork(width, height);
		Point2D[] points = new Point2D[number_of_points];
		for (int i = 0 ; i < number_of_points ; i++) {
			points[i] = new Point2D.Double(r.nextDouble()*width, r.nextDouble()*height);
		}
		myNetwork.createVoronoiNetwork(points);

		long timenow = java.lang.System.currentTimeMillis();
//		System.out.println("time elapsed " + (timenow-timeold));
		timeold = timenow;
		return myNetwork;
	}


	public void paint(Graphics g) {
		if (null!=aoi) {
			g.setColor(Color.red);
			for (int i=0 ; i<aoi.aoiPoints.length ; i++) {
				Point2D p1 = aoi.aoiPoints[i];
				Point2D p2 = aoi.aoiPoints[(i+1)%aoi.aoiPoints.length];
				g.drawLine(
					(int)(sizex * (p1.getX() / vn.diagramma.width)),
					(int)(sizey * (p1.getY() / vn.diagramma.height)),
					(int)(sizex * (p2.getX() / vn.diagramma.width)),
					(int)(sizey * (p2.getY() / vn.diagramma.height))
				);
			}
		}
		if (source != null) {
			for (int i = 0 ; i < source.length ; i++) {
				int x1 = (int)(sizex * (source[i].getX() / vn.diagramma.width));
				int y1 = (int)(sizey * (source[i].getY() / vn.diagramma.height));
				int x2 = (int)(sizex * (dest[i].getX() / vn.diagramma.width));
				int y2 = (int)(sizey * (dest[i].getY() / vn.diagramma.height));
				g.setColor(Color.magenta);
				g.drawLine(x1, y1, x2, y2);
			}
		}
		if (vn != null) {
			g.setColor(Color.blue);
			Font oldFont = g.getFont();
			Font newFont = oldFont.deriveFont((float)(20));
			g.setFont(newFont);
			SFVoronoi graph = vn.globalVoronoiArea.graph;
			graph.get_en(0);
			Set<Integer> keys = graph.get_sites().keySet();
			for(Integer key: keys) {
				int x = (int)(sizex * (graph.get(key).getX() / vn.diagramma.width));
				int y = (int)(sizey * (graph.get(key).getY() / vn.diagramma.height));
				g.fillOval(x-ballSize/2, y-ballSize/2, ballSize, ballSize);
				String siteId = ""+key;
//				g.drawString(siteId, x, y);
			}


            if (null!=all_sites_touched) {
//                g.setColor(Color.cyan orange pink yellow);
                g.setColor(Color.cyan);
                for(Integer touched: all_sites_touched) {
                    int x = (int)(sizex * (graph.get(touched).getX() / vn.diagramma.width));
                    int y = (int)(sizey * (graph.get(touched).getY() / vn.diagramma.height));
                    for (int i=ballSize/2;i<ballSize;i++)
                        g.drawOval(x-i, y-i, i*2, i*2);
                }
            }

			g.setFont(oldFont);
			Iterator<line2d> it3 = graph.mEdges.iterator();
			while (it3.hasNext ()) {
				line2d l = it3.next();

				if (vn.siteInAoI!=null) {
					if (vn.siteInAoI[l.bisectingID[0]] || vn.siteInAoI[l.bisectingID[1]])
						g.setColor(Color.green);
					else
						g.setColor(Color.blue);
				}
				int x1 = (int)(sizex * (l.getX1() / vn.diagramma.width));
				int y1 = (int)(sizey * (l.getY1() / vn.diagramma.height));
				int x2 = (int)(sizex * (l.getX2() / vn.diagramma.width));
				int y2 = (int)(sizey * (l.getY2() / vn.diagramma.height));

				g.drawLine(x1, y1, x2, y2);
			}
			g.setColor(Color.blue);
		}
	}

	public Point2D randomPointInAoI() {
		boolean needed = true;
		Point2D ret = null;
		while (needed) {
			ret = new Point2D.Double(r.nextDouble()*width, r.nextDouble()*height);
			if (aoi.isInAoI(ret)) needed = false;
		}
		return ret;
	}
}
