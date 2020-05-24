/*
 * The author of this software is Steven Fortune. 
 * Copyright (c) 1994 by AT&T Bell Laboratories.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 */

/*
 * VAST, a scalable peer-to-peer network for virtual environments
 * Copyright (C) 2004 Guan-Ming Liao (gm.liao@msa.hinet.net)    adpated from C   to C++
 * Copyright (C) 2006 Shun-Yun Hu    (syhu@yahoo.com)           adapted from C++ to Java
 * Copyright (C) 2019 Michele Albano (mialb@cs.aau.dk)          structural changes to implement the MABRAVO use case
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package vast;

import java.awt.geom.Point2D;
import java.util.Vector;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Enumeration;

class Site
{
    public Point2D  coord;
    public int      num;            // originally 'sitenbr', now either the site id or vertex num

    public Vector<Integer>   edge_idxlist = new Vector<Integer>();
    
    public Site(double x, double y) {
        coord = new Point2D.Double();
        coord.setLocation(x, y);
    }

    public double dist(Site s) {
        return coord.distance (s.coord);
    }
};

class Edge  
{
    public double   a,b,c;
    public Site[]   ep  = new Site[2];
    public Site[]   reg = new Site[2];
    public int      num;
};

class Halfedge 
{
    public Halfedge    ELleft, ELright;
    public Edge        ELedge;
    public int         ELpm;
    public Site        vertex;
    public double      ystar;
    public Halfedge    PQnext;
};

public class SFVoronoi {

    public SFVoronoi () {
        DELETED = new Edge ();
        DELETED.a = DELETED.b = DELETED.c = (-2);
        
        le = 0;
        re = 1;
    }

    private boolean     invalidated = false;

    // NOTE: we use TreeMap for mSites as it is both sorted and also a map (hashtable functions)
    private Hashtable<Integer, Point2D>   sites       = new Hashtable<Integer, Point2D>();   // internal persistent record
    private TreeMap<Point2D, Site>      mSites      = new TreeMap<Point2D, Site>(new Comparator<Point2D>() {
            @Override
            public int compare(Point2D p1, Point2D p2) {
                double x1 = p1.getX();
                double y1 = p1.getY();
                double x2 = p2.getX();
                double y2 = p2.getY();
                if (y1 < y2)
                    return (-1);
                if (y1 > y2)
                    return (1);
                if (x1 < x2)
                    return (-1);
                if (x1 > x2)
                    return (1);
                return (0);
            }
        });
    public Vector<line2d>       mEdges      = new Vector<line2d>();
    public Vector<Point2D>       mVertices   = new Vector<Point2D>();

    // insert a new site, the first inserted is myself
    public void insert(int id, Point2D coord) {
        // avoid duplicate insert
        if (sites.containsKey(id) == false) {
            invalidated = true;
            sites.put(id, coord);
        }
    }

    // get the point of a site
    public Point2D get(int id) {
        Point2D p = sites.get(id);
        return p;
    }

    // get a list of enclosing neighbors
    public Vector<Integer> get_en(int id) {
        //if (sites.containsKey (new Integer(id)) == false)
        //    return null;
        
        Point2D coord = sites.get(id);
        if (coord == null)
            return null;
        
        recompute();
        Vector<Integer> en_list = new Vector<Integer>();

        Enumeration<Integer> e = mSites.get(coord).edge_idxlist.elements ();
            
        while (e.hasMoreElements ()) {
            
            int    edge_idx = (e.nextElement ()).intValue ();
            line2d line     = mEdges.elementAt (edge_idx);
            
            // NOTE: bisecting has changed from storing node index to node id
            int en_id = (line.bisectingID[0] == id ? line.bisectingID[1] : line.bisectingID[0]);
            en_list.add(en_id);
        }
        
        return en_list; 
    }

    //
    // non Voronoi-specific methods
    //

    // returns the closest node to a point
    public int closest_to(Point2D coord) {
        Integer[] keys    = sites.keySet ().toArray (new Integer[0]);
        Point2D[] points  = sites.values ().toArray (new Point2D[0]);

        // assume the first node is the closest                        
        int     closest  = (keys[0]).intValue ();
        double  min_dist2 = coord.distanceSq(points[0]);
        
        Point2D pt;
        double  d;
        
        for (int i=1; i<sites.size (); i++) {
            pt = points[i];            

            if ((d = coord.distanceSq(pt)) < min_dist2) {       
                min_dist2 = d;
                closest = (keys[i]).intValue();
            }
        }
                                
        return closest;
    }

    // get all the neighbors
    public Hashtable<Integer, Point2D> get_sites() {        
        return sites;        
    }

    // get the number of sites currently maintained
    public int size() {        
        return sites.size ();
    }
    
    //
    // private functions
    //

    // recompute the Voronoi graph
    private void recompute() {
        if(invalidated == false)
            return;

        //clearAll();
        mSites.clear();
        mEdges.clear();    
        mVertices.clear();        

        // originally in calsvf()
        sorted = false; triangulate = false; plot = true; debug = true;

        readsites();        
        curr_site = mSites.values().iterator();
        
        geominit();
        
        if (plot) 
            plotinit();
            
        voronoi(triangulate);      
        invalidated = false;                
    }

    //
    // original SFVoronoi protected functions, but now turn them into private ones       
    //    

    private Site nextone() {
        if (curr_site.hasNext() == false)
            return null;

        Site s = (Site)curr_site.next ();

        return s;
    }   

    private void readsites() {

        // find out the x & y ranges for all sites
        nsites = sites.size ();        
        
        Point2D[] points = sites.values ().toArray (new Point2D[0]);
        Integer[] keys   = sites.keySet ().toArray (new Integer[0]);
        
                
        xmin = xmax = points[0].getX();
        ymin = ymax = points[0].getY();
        Point2D pt;
        Site s;

        // converting sites to mSites
        // NOTE: this is an important step as mSites needs to be sorted for Voronoi construction
        for (int i=0; i<nsites; i++) {   
            pt = points[i];
            s  = new Site (pt.getX(), pt.getY());
            s.num = (keys[i]).intValue ();
            
            mSites.put(pt, s);
                                    
            if(pt.getX() < xmin) 
                xmin = pt.getX();
            else if(pt.getX() > xmax) 
                xmax = pt.getX();
            if(pt.getY() < ymin)
                ymin = pt.getY();
            else if(pt.getY() > ymax)
                ymax = pt.getY();
                            
            //System.err.println ("storing num: [" + s.num + "] (" + pt.x + ", " + pt.y + ")");
        }
        
        //ymin = ((Site)((Map.Entry)entries[0]).getValue()).coord.y;
        //ymax = ((Site)((Map.Entry)entries[nsites-1]).getValue()).coord.y;
        
        Iterator<Site> it = mSites.values ().iterator ();
        while (it.hasNext ()) {
            s = it.next ();
            //System.err.println ("sorted num: [" + s.num + "] (" + s.coord.x + ", " + s.coord.y + ")");
        }
                
        //System.err.println ("xmax=" + xmax + " xmin=" + xmin + " ymax=" + ymax + " ymin=" + ymin);

    }

    //////////////////////////////////////////////////////////////////////////  
    // defs.h
    //
    
    // command-line flags
    
    private boolean     triangulate, sorted, plot, debug;   
    private double      xmin, xmax, ymin, ymax, deltax, deltay;
    private int         nsites;
    private Iterator    curr_site;
    private int         sqrt_nsites;
    private int         nvertices;
    private Site        bottomsite;             // Site *bottomsite;
    private int         nedges;
    private Halfedge[]  PQhash;                 // Halfedge *PQhash;
    private int         PQhashsize;
    private int         PQcount;
    private int         PQmin;
    private Halfedge    ELleftend, ELrightend;  // Halfedge *ELleftend, *ELrightend;
    private int         ELhashsize;
    private Halfedge[]  ELhash;                 // Halfedge **ELhash;
    private Edge        DELETED;                // special marker
    
    private int     le;
    private int     re; 


    //////////////////////////////////////////////////////////////////////////
    // geometry.c
    //    
    private void geominit() {
                
        nvertices = 0;
        nedges = 0;
        double sn = nsites+4;
        sqrt_nsites = (int)Math.sqrt(sn);
        deltay = ymax - ymin;
        deltax = xmax - xmin;           
    }

    // find the bisecting edge for two sites (creating a new edge)
    private Edge bisect (Site s1, Site s2) {
    
        //System.out.println ("bisecting (" + s1.coord.x + ", " + s1.coord.y + ") (" + s2.coord.x + ", " + s2.coord.y + ")");
        double dx, dy, adx, ady;        // deltas in coords and their absolute values
        Edge newedge = new Edge ();
                
        newedge.reg[0] = s1;
        newedge.reg[1] = s2;

        newedge.ep[0] = null;
        newedge.ep[1] = null;
        
        dx = s2.coord.getX() - s1.coord.getX();
        dy = s2.coord.getY() - s1.coord.getY();
        adx = (dx > 0 ? dx : -dx);
        ady = (dy > 0 ? dy : -dy);
        
        newedge.c = s1.coord.getX() * dx + s1.coord.getY() * dy + (dx*dx + dy*dy) * 0.5;
        
        if (adx > ady) {
            newedge.a =  1.0; 
            newedge.b =  dy/dx; 
            newedge.c /= dx;
        }
        else {
            newedge.b =  1.0; 
            newedge.a =  dx/dy; 
            newedge.c /= dy;
        }        
        
        newedge.num = nedges++;        
        out_bisector (newedge);
        
        return newedge; 
    }

    public int pointCompare(Point2D q, Point2D p) {
        if (q.getY() < p.getY())
            return (-1);
        if (q.getY() > p.getY())
            return (1);
        if (q.getX() < p.getX())
            return (-1);
        if (q.getX() > p.getX())
            return (1);
        return (0);
    }

    private Site intersect (Halfedge el1, Halfedge el2) {
        
        Edge e1, e2, e;
        Halfedge el;
        
        double d, xint, yint;
        
        e1 = el1.ELedge;
        e2 = el2.ELedge;

        if (e1 == null || e2 == null || (e1.reg[1] == e2.reg[1]))            
            return null;
        
        d = e1.a * e2.b - e1.b * e2.a;
        
        if (-1.0e-10 < d && d < 1.0e-10) 
            return null;
        
        xint = (e1.c * e2.b - e2.c * e1.b) / d;
        yint = (e2.c * e1.a - e1.c * e2.a) / d;
        
        if (pointCompare(e1.reg[1].coord, e2.reg[1].coord) < 0) {
            el = el1; 
            e = e1;
        }
        else {
            el = el2; 
            e = e2;
        }

        boolean right_of_site = xint >= e.reg[1].coord.getX();
        
        if ((right_of_site && el.ELpm == le) || (!right_of_site && el.ELpm == re)) 
            return null;

        Site v = new Site (xint, yint);

        return v;
    }
    
    private boolean right_of (Halfedge el, Point2D p) {

        boolean right_of_site, above, fast;
        double  dxp, dyp, dxs, t1, t2, t3, yl;

        Edge e       = el.ELedge;
        Site topsite = e.reg[1];
        
        right_of_site = p.getX() > topsite.coord.getX();
        
        if (right_of_site && el.ELpm == le) 
            return true;
            
        if (!right_of_site && el.ELpm == re) 
            return false;
        
        if (e.a == 1.0) {
        
            dyp = p.getY() - topsite.coord.getY();
            dxp = p.getX() - topsite.coord.getX();
            fast = false;
            
            if ((!right_of_site & (e.b<0.0)) | (right_of_site & (e.b>=0.0)))
                fast = above = (dyp >= e.b*dxp);
            else {  
                above = p.getX() + p.getY() * e.b > e.c;
                if (e.b < 0.0)
                    above = !above;
                if (!above) 
                    fast = true;
            }
            if (!fast) {

                dxs = topsite.coord.getX() - (e.reg[0]).coord.getX();
            
                if (dxs != 0)
                    above = e.b * (dxp*dxp - dyp*dyp) < dxs*dyp*(1.0+2.0*dxp/dxs + e.b*e.b);
                else
                    above = false;
        
                if (e.b < 0.0) 
                    above = !above;
            };
        }
        // e.b==1.0
        else {  
            yl = e.c - e.a*p.getX();
            t1 = p.getY() - yl;
            t2 = p.getX() - topsite.coord.getX();
            t3 = yl - topsite.coord.getY();
            above = t1*t1 > (t2*t2 + t3*t3);
        }
        
        return (el.ELpm == le ? above : !above);        
    }
    
    private void endpoint (Edge e, int lr, Site s) {
        e.ep[lr] = s;

        if(e.ep[re-lr] == null) 
            return;

        out_ep(e);
    }
        
    // return int change to void
    private void makevertex (Site v) {
        v.num = nvertices++;
        out_vertex (v);
    }


    //////////////////////////////////////////////////////////////////////////
    // output.c
    //

    private double pxmin, pxmax, pymin, pymax, cradius;
    
    private void out_bisector (Edge e) {
                 
        //System.out.println ("out_bisector [" + (float)e.a + ", " + (float)e.b + ", " + (float)e.c + "]");                        
        
        line2d line = new line2d(e.a, e.b, e.c);
        
        line.bisectingID[0] = e.reg[le].num;
        line.bisectingID[1] = e.reg[re].num;

        Point2D pt1 = sites.get(line.bisectingID[0]);
        Point2D pt2 = sites.get(line.bisectingID[1]);

        mSites.get(pt1).edge_idxlist.add(e.num);
        mSites.get(pt2).edge_idxlist.add(e.num);
                
        mEdges.add (line);
    }
    
    private void out_ep (Edge e) {
        mEdges.elementAt(e.num).vertexIndex[0] = (e.ep[le] != null) ? (e.ep[le].num) : (-1);
        mEdges.elementAt(e.num).vertexIndex[1] = (e.ep[re] != null) ? (e.ep[re].num) : (-1);
        
        if (!triangulate & plot) 
            clip_line (e);     
    }
    
    private void out_vertex (Site v) {
        mVertices.add(v.coord);
    }
    
    
    // store output of a site
    //private void out_site (Site s) {}    
    //private void out_triple (Site s1, Site s2, Site s3) {}

    private void plotinit() {
        double dy = ymax - ymin;;
        double dx = xmax - xmin;
        double d = (dx > dy ? dx : dy) * 1.1;
        
        pxmin = xmin - (d-dx)/2.0;
        pxmax = xmax + (d-dx)/2.0;
        pymin = ymin - (d-dy)/2.0;
        pymax = ymax + (d-dy)/2.0;
        
        cradius = (pxmax - pxmin)/350.0;        
    }
    
    // cut edges so that they are displayable
    private void clip_line(Edge e) {
        Site s1, s2;
        double x1, x2, y1, y2;

        if(e.a == 1.0 && e.b >= 0.0) {
            s1 = e.ep[1];
            s2 = e.ep[0];
        }
        else {
            s1 = e.ep[0];
            s2 = e.ep[1];
        }
     
        if(e.a == 1.0) {
            
            y1 = pymin;
            if (s1 != null && s1.coord.getY() > pymin)
                y1 = s1.coord.getY();
                
            if (y1 > pymax)
                return;
                
            x1 = e.c - e.b * y1;
            y2 = pymax;
            
            if (s2 != null && s2.coord.getY() < pymax) 
                y2 = s2.coord.getY();
                
            if (y2 < pymin) 
                return;
                
            x2 = e.c - e.b * y2;
            
            if (((x1> pxmax) & (x2>pxmax)) | ((x1 < pxmin) & (x2<pxmin))) 
                return;
                
            if (x1 > pxmax) {
                x1 = pxmax; 
                y1 = (e.c - x1)/e.b;
            }
            
            if (x1 < pxmin) {
                x1 = pxmin; 
                y1 = (e.c - x1)/e.b;
            }                        
            
            if (x2 > pxmax) {
                x2 = pxmax; 
                y2 = (e.c - x2)/e.b;
            }
            
            if (x2 < pxmin) {
                x2 = pxmin; 
                y2 = (e.c - x2)/e.b;
            }
        }
        else {

            x1 = pxmin;
            if (s1 != null && s1.coord.getX() > pxmin) 
                x1 = s1.coord.getX();
                
            if (x1 > pxmax) 
                return;
                
            y1 = e . c - e . a * x1;
            x2 = pxmax;
            if (s2 != null && s2.coord.getX() < pxmax) 
                x2 = s2.coord.getX();
                
            if (x2 < pxmin) 
                return;
                
            y2 = e.c - e.a * x2;
            
            if (((y1> pymax) & (y2>pymax)) | ((y1<pymin) & (y2<pymin))) 
                return;
                
            if (y1> pymax) {
                y1 = pymax; 
                x1 = (e.c - y1)/e.a;
            }
            
            if (y1 < pymin) {
                y1 = pymin; x1 = (e.c - y1)/e.a;
            }
            
            if (y2 > pymax) {
                y2 = pymax; 
                x2 = (e.c - y2)/e.a;
            }
            
            if (y2<pymin) {
                y2 = pymin; 
                x2 = (e.c - y2)/e.a;
            }
        }

		mEdges.elementAt(e.num).setLine(x1,y1,x2,y2);
    }

    //////////////////////////////////////////////////////////////////////////
    // heap.c
    //

    private void PQinsert (Halfedge he, Site v, double offset) {
        Halfedge last, next;

        he.vertex = v;
        he.ystar = v.coord.getY() + offset;

        last = PQhash[PQbucket (he)];
        
        while ((next = last.PQnext) != null && 
               (he.ystar > next.ystar || (he.ystar == next.ystar && v.coord.getX() > next.vertex.coord.getX())))
            last = next;
                        
        he.PQnext = last.PQnext; 
        last.PQnext = he;
        PQcount++;    
    }
    
    private void PQdelete (Halfedge he) {
        Halfedge last;
        
        if(he.vertex != null) {
            last = PQhash[PQbucket (he)];
            while (last.PQnext != he) 
                last = last.PQnext;
                
            last.PQnext = he.PQnext;

            PQcount--;
            he.vertex = null;
        }
    }

    private int PQbucket (Halfedge he) {
        int bucket = (int)((he.ystar - ymin)/deltay * PQhashsize);
        if (bucket < 0) 
            bucket = 0;
        if (bucket >= PQhashsize) 
            bucket = PQhashsize-1;
        if (bucket < PQmin) 
            PQmin = bucket;
        return bucket;
    }
    
    private boolean PQempty () {
        return (PQcount == 0);
    }
    
    private Point2D PQ_min () {
        Point2D answer = new Point2D.Double();
        
        while (PQhash[PQmin].PQnext == null) 
            PQmin++;
        
        answer.setLocation(
            PQhash[PQmin].PQnext.vertex.coord.getX(),
            PQhash[PQmin].PQnext.ystar
        );
        
        return answer;          
    }
    
    private Halfedge PQextractmin () {
        Halfedge curr;
        
        curr = PQhash[PQmin].PQnext;
        PQhash[PQmin].PQnext = curr.PQnext;
        PQcount--;
        return curr;
    }
    
    private void PQinitialize () {
        PQcount = 0;
        PQmin = 0;
        PQhashsize = 4 * sqrt_nsites;
        PQhash = new Halfedge[PQhashsize];
        
        for (int i=0; i < PQhashsize; i++) {
            PQhash[i] = new Halfedge ();                        
            PQhash[i].PQnext = null;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    // edgelist.c
    //

    // initialize edgelist
    private void ELinitialize() {

        //freeinit (&hfl, sizeof (Halfedge));
        ELhashsize = 2 * sqrt_nsites;
        ELhash = new Halfedge[ELhashsize];
        
        for(int i=0; i < ELhashsize; i++)
            ELhash[i] = null;
        
        ELleftend  = HEcreate (null, 0);
        ELrightend = HEcreate (null, 0);
        
        ELleftend.ELleft = null;
        ELleftend.ELright = ELrightend;
        
        ELrightend.ELleft = ELleftend;
        ELrightend.ELright = null;
        
        ELhash[0]            = ELleftend;
        ELhash[ELhashsize-1] = ELrightend;      
    }
    
    private Halfedge HEcreate (Edge e, int pm) {
        Halfedge he = new Halfedge ();

        he.ELedge = e;
        he.ELpm = pm;
        he.PQnext = null;
        he.vertex = null;
        he.ystar = 0;

        return he;
    }
    
    //change arg2 to newH
    private void ELinsert (Halfedge lb, Halfedge newH) {
        newH.ELleft         = lb;
        newH.ELright        = lb.ELright;
        lb.ELright.ELleft   = newH;
        lb.ELright          = newH;        
    }
    
    private Halfedge ELgethash (int b) {

        Halfedge he;
        
        if (b<0 || b>=ELhashsize) 
            return null;
            
        he = ELhash[b]; 
        if (he == null || he.ELedge != DELETED) 
            return he;
        
        /* Hash table points to deleted half edge.  Patch as necessary. */
        ELhash[b] = null;

        return null;
    }
    
    private Halfedge ELleftbnd (Point2D p) {
        int i, bucket;
        Halfedge he;
        
        /* Use hash table to get close to desired halfedge */
        bucket = (int)((p.getX() - xmin)/deltax * ELhashsize);
        if (bucket < 0)
            bucket = 0;
        if (bucket >= ELhashsize)
            bucket = ELhashsize - 1;
        he = ELgethash (bucket);
        
        if (he == null) {   
            //System.err.println ("ELleftbnd: first he is null");
            for (i=1; true; i++) {   
                if ((he=ELgethash(bucket-i)) != null) 
                    break;
                if ((he=ELgethash(bucket+i)) != null) 
                    break;
            }
        }

        /* Now search linear list of halfedges for the correct one */
        if (he == ELleftend || (he != ELrightend && right_of (he,p))) {
            //System.err.println ("ELleftbnd: loop1");
            do {
                he = he.ELright; 
            }
            while (he != ELrightend && right_of (he,p));
            
            he = he.ELleft;
        }
        else {
            do {
                he = he.ELleft;
            }
            while (he != ELleftend && !right_of(he,p));
        }
        
        // Update hash table and reference counts 
        if (bucket > 0 && bucket < ELhashsize-1) {   
            ELhash[bucket] = he;
        }
        return he;
    }
    
    private void ELdelete (Halfedge he) {
        he.ELleft.ELright   = he.ELright;
        he.ELright.ELleft   = he.ELleft;        
        he.ELedge           = DELETED;    
    }
    
    private Halfedge ELright (Halfedge he) {
        return (he.ELright);
    }
    
    private Halfedge ELleft (Halfedge he) {
        return (he.ELleft);
    }
    
    private Site leftreg (Halfedge he) {
        if (he.ELedge == null) 
            return bottomsite;
        return (he.ELpm == le ? he.ELedge.reg[le] : he.ELedge.reg[re]);
    }
    
    private Site rightreg (Halfedge he) {
        if (he.ELedge == null) { 
            //System.err.println ("rightreg..returning bottomesite");
            return bottomsite;
        }
        //System.err.println ("rightreg..returning other");
        return (he.ELpm == le ? he.ELedge.reg[re] : he.ELedge.reg[le]);
    }

    //////////////////////////////////////////////////////////////////////////
    // voronoi.c
    //
    private void voronoi(boolean triangulate) {
        Site newsite, bot, top, temp, p;
        Site v;
        
        Point2D newintstar = new Point2D.Double();     // perhaps no need to allocate?
        int pm;
        
        Halfedge lbnd, rbnd, llbnd, rrbnd, bisector;
        Edge e;
        
        PQinitialize ();
        bottomsite = nextone ();
        //out_site (bottomsite);
        ELinitialize();
        
        newsite = nextone ();
           
        while (true) {
            
            if(!PQempty ()) 
                newintstar = PQ_min();
        
            if (newsite != null && 
                (PQempty() || pointCompare(newsite.coord, newintstar) < 0)) {
                    
                // new site is smallest 
                //out_site(newsite);
                //System.err.println ("new site is smallest");
                
                lbnd = ELleftbnd (newsite.coord);       
                rbnd = ELright (lbnd);                    
                bot  = rightreg (lbnd);
                
                e = bisect (bot, newsite);
                bisector = HEcreate (e, le);
                
                ELinsert (lbnd, bisector);
                
                if ((p = intersect (lbnd, bisector)) != null) {   
                    PQdelete (lbnd);
                    PQinsert (lbnd, p, p.dist (newsite));
                }
                
                lbnd = bisector;
                bisector = HEcreate (e, re);
                ELinsert (lbnd, bisector);
                
                if ((p = intersect (bisector, rbnd)) != null)
                    PQinsert (bisector, p, p.dist (newsite)); 

                newsite = nextone();
            }
            
            // intersection is smallest 
            else if (!PQempty ()) {
                
                lbnd  = PQextractmin();
                llbnd = ELleft (lbnd);
                rbnd  = ELright (lbnd);
                rrbnd = ELright (rbnd);
                bot   = leftreg (lbnd);
                top   = rightreg (rbnd);
                //out_triple (bot, top, rightreg (lbnd));
                v     = lbnd.vertex;
                
                makevertex (v);
                
                endpoint (lbnd.ELedge, lbnd.ELpm, v);
                endpoint (rbnd.ELedge, rbnd.ELpm, v);
                
                ELdelete (lbnd); 
                PQdelete (rbnd);
                ELdelete (rbnd); 
                
                pm = le;
                
                if (bot.coord.getY() > top.coord.getY()) {   
                    temp = bot; 
                    bot = top; 
                    top = temp; 
                    pm = re;
                }

                e = bisect (bot, top);
                bisector = HEcreate (e, pm);
                ELinsert (llbnd, bisector);
                endpoint (e, re-pm, v);

                if ((p = intersect (llbnd, bisector)) != null) {   
                    PQdelete (llbnd);
                    PQinsert (llbnd, p, p.dist (bot));
                }
                
                if ((p = intersect(bisector, rrbnd)) != null)
                    PQinsert (bisector, p, p.dist (bot));          
            }
            else 
                break;
                
        } // end while (true)        

        // print out the edges (here we store them in 'mEdges')
        for (lbnd = ELright(ELleftend); lbnd != ELrightend; lbnd = ELright(lbnd)) {
            e = lbnd.ELedge;
            out_ep (e);
        }
          
    } // end voronoi()
                           
} // end of SFVoronoi
