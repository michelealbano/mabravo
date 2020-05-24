/*
 * VAST, a scalable peer-to-peer network for virtual environments
 * Copyright (C) 2006 Shun-Yun Hu (syhu@yahoo.com)
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

import java.awt.geom.Line2D;

public class line2d extends Line2D.Double
{
    public double  a,b,c;
    public int     bisectingID[] = new int[2];    // NOTE: we need to change this from storing node index to node id
    public int     vertexIndex[] = new int[2];

	public line2d (double x1, double y1, double x2 , double y2)
    {
		super(x1, y1, x2, y2);
        
        if (y1 == y2) 
        {   
            a = 0; b = 1; c = y1;
        }
        else if (x1 == x2) 
        {
            a = 1; b = 0; c = x1;
        }
        else
        {
            double dx = x1 - x2;
            double dy = y1 - y2;
            double m = dx / dy;
            a = -1 * m;
            b = 1;
            c = a*x1 + b*y1;
        }
    }

    public line2d () 
    {
		super();
        a = b = c = 0.0;
        
        vertexIndex[0] = -1;
        vertexIndex[1] = -1;
    }

    public line2d (double A, double B, double C)
    {
		super();
        a = A;
        b = B;
        c = C;
        
        vertexIndex[0] = -1;
        vertexIndex[1] = -1;
    }
};
