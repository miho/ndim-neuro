/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * copyright (c) 2007 - 2008
 * Simulation in Technology
 * University of Heidelberg
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Heidelberg nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.gcsc.ndim.neuro;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import org.ndim.AddrOp;
import org.ndim.GridTopo;
import org.ndim.MemTopo;
import org.ndim.Stencil;
import org.ndim.math.Vec;
import org.ndim.Arrays.Algo;

/**
 *
 * @author Alexander Heusel
 */
public class MarchingCubes
{

    protected static final class Node3f extends Point3f
    {

        public int id;

        public Node3f()
        {
            super();
        }

        public Node3f(float x, float y, float z)
        {
            super(x, y, z);
        }
    }

    protected static final class Triangle
    {

        public int n0;
        public int n1;
        public int n2;

        public Triangle(int id0, int id1, int id2)
        {
            n0 = id0;
            n1 = id1;
            n2 = id2;
        }

        public final void copyTo(int startIdx, int[] array)
        {
            array[startIdx] = n0;
            array[startIdx + 1] = n1;
            array[startIdx + 2] = n2;
        }
    }
    // Cell length in x, y, and z directions.
    protected final float[] h = new float[3];
    // Offset to add to the generated Mesh
    protected final float[] offs = new float[3];
    // The threshold for the isosurface.
    protected float threshold;
    // Indicates whether a valid surface is present.
    protected boolean isEmpty;
    // The vertices which make up the isosurface.
    protected Point3f[] vertices;
    // The indices of the vertices which make up the triangles.
    protected int[] triangles;
    // The normals.
    protected Vector3f[] normals;
    // Switch debug output
    private boolean debug = false;
    // The number of sub samples to take
    private int samples;


    public MarchingCubes()
    {
        setGridSpacing(1.0f, 1.0f, 1.0f);
        setOffset(0f, 0f, 0f);       
    }

    public MarchingCubes(float threshold, float h0, float h1, float h2)
    {
        setThreshold(threshold);
        setGridSpacing(h0, h1, h2);
        setOffset(0f, 0f, 0f);
    }

    public MarchingCubes(float threshold, float h0, float h1, float h2, boolean debug)
    {
        setThreshold(threshold);
        setGridSpacing(h0, h1, h2);
        setDebug(debug);
    }

    public final boolean getDebug()
    {
        return debug;
    }

    public final void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public final float getThreshold()
    {
        return threshold;
    }

    public final void setThreshold(float threshold)
    {
        this.threshold = threshold;
    }

    public final float[] getGridSpacing()
    {
        return h.clone();
    }

    public final void setGridSpacing(float h0, float h1, float h2)
    {
        h[0] = h0;
        h[1] = h1;
        h[2] = h2;
    }

    public final void setGridSpacing(float[] h)
    {
        System.arraycopy(h, 0, this.h, 0, this.h.length);
    }

    public final void setOffset(float d0, float d1, float d2)
    {
        offs[0] = d0;
        offs[1] = d1;
        offs[2] = d2;
    }
    
    public final void setOffset(float[] offs)
    {
        System.arraycopy(offs, 0, this.offs, 0, this.offs.length);
    }

    public final void setSubsampling(int samples)
    {
        this.samples = samples;
    }

    public final int getSubsampling()
    {
        return samples;
    }

    // Returns false if a valid surface has been generated.
    public final boolean isEmpty()
    {
        return isEmpty;
    }

    // Deletes the isosurface.
    public final void clear()
    {
        vertices = null;
        triangles = null;
        normals = null;
        isEmpty = true;
    }

    // Generates the isosurface from the scalar field contained in the
    // buffer ptScalarField[].
    public void exec(final GridTopo gridTopo, final MemTopo memTopo, final byte[] data)
    {
        // List of POINT3Ds which form the isosurface.
        HashMap<Integer, MarchingCubes.Node3f> vertexMap = new HashMap<Integer, MarchingCubes.Node3f>();
        // List of TRIANGLES which form the triangulation of the isosurface.
        ArrayList<MarchingCubes.Triangle> triangleList = new ArrayList<MarchingCubes.Triangle>();

        if(!isEmpty)
        {
            clear();
        }


        final GridTopo cropTopo = gridTopo.trimEnd(1);
        final Stencil stencil = new Stencil(cropTopo.extent());
        final AddrOp op = new AddrOp(cropTopo, memTopo);
        

        float[] elem = new float[8];
        int[] edgeID = new int[12];

        // Generate isosurface.
        final int incrX = op.incr(GridTopo.X);
        final int incrY = op.incr(GridTopo.Y);
        final int incrZ = op.incr(GridTopo.Z);
        int addr;
        final float[] posf = Algo.fill(new float[cropTopo.nrDims()], 0.0f);
        final int[] pos = Algo.fill(new int[cropTopo.nrDims()], 0);
        while(stencil.hasNext(pos))
        {
            addr =  op.addr(pos, 0);

            elem[0] = data[addr];
            elem[1] = data[addr + incrY];
            elem[2] = data[addr + incrX + incrY];
            elem[3] = data[addr + incrX];
            elem[4] = data[addr + incrZ];
            elem[5] = data[addr + incrY + incrZ];
            elem[6] = data[addr + incrX + incrY + incrZ];
            elem[7] = data[addr + incrX + incrZ];

            for(int i = 0; i < edgeID.length; i++)
            {
                edgeID[i] = getEdgeID(addr, incrX, incrY, incrZ, i);
            }

            posf[GridTopo.X] = pos[GridTopo.X]; posf[GridTopo.Y] = pos[GridTopo.Y]; posf[GridTopo.Z] = pos[GridTopo.Z];
            Vec.elementwiseMul(posf, h);
            triangulateCell(posf, stencil.atEnd(pos), elem, edgeID, vertexMap, triangleList);

            stencil.next(pos);
        }

        transcribeVerticesAndTriangles(vertexMap, triangleList);
        isEmpty = false;

    }


    private void triangulateCell(float[] pos, int atEnd, float[] elem, int[] edgeID,
            HashMap<Integer, MarchingCubes.Node3f> vertexMap, ArrayList<MarchingCubes.Triangle> triangleList)
    {
        int tableIndex = getTableIndex(elem, threshold);
        if(edgeLUT[tableIndex] != 0)
        {
            if((edgeLUT[tableIndex] & 8) > 0)
            {
                vertexMap.put(edgeID[3], intersect(pos, elem, 3));
            }
            if((edgeLUT[tableIndex] & 1) > 0)
            {
                vertexMap.put(edgeID[0], intersect(pos, elem, 0));
            }
            if((edgeLUT[tableIndex] & 256) > 0)
            {
                vertexMap.put(edgeID[8], intersect(pos, elem, 8));
            }

            if((atEnd & 0x1) > 0) // X
            {
                if((edgeLUT[tableIndex] & 4) > 0)
                {
                    vertexMap.put(edgeID[2], intersect(pos, elem, 2));
                }
                if((edgeLUT[tableIndex] & 2048) > 0)
                {
                    vertexMap.put(edgeID[11], intersect(pos, elem, 11));
                }
            }
            if((atEnd & 0x2) > 0) // Y
            {
                if((edgeLUT[tableIndex] & 2) > 0)
                {
                    vertexMap.put(edgeID[1], intersect(pos, elem, 1));
                }
                if((edgeLUT[tableIndex] & 512) > 0)
                {
                    vertexMap.put(edgeID[9], intersect(pos, elem, 9));
                }
            }
            if((atEnd & 0x4) > 0) // Z
            {
                if((edgeLUT[tableIndex] & 16) > 0)
                {
                    vertexMap.put(edgeID[4], intersect(pos, elem, 4));
                }
                if((edgeLUT[tableIndex] & 128) > 0)
                {
                    vertexMap.put(edgeID[7], intersect(pos, elem, 7));
                }
            }
            if((atEnd & 0x3) > 0) // XY
            {
                if((edgeLUT[tableIndex] & 1024) > 0)
                {
                    vertexMap.put(edgeID[10], intersect(pos, elem, 10));
                }
            }
            if((atEnd & 0x5) > 0) // XZ
            {
                if((edgeLUT[tableIndex] & 64) > 0)
                {
                    vertexMap.put(edgeID[6], intersect(pos, elem, 6));
                }
            }
            if((atEnd & 0x6) > 0) // YZ
            {
                if((edgeLUT[tableIndex] & 32) > 0)
                {
                    vertexMap.put(edgeID[5], intersect(pos, elem, 5));
                }
            }

            for(int i = 0; triangleLUT[tableIndex][i] != -1; i += 3)
            {
                triangleList.add(
                        new MarchingCubes.Triangle(
                        edgeID[triangleLUT[tableIndex][i]],
                        edgeID[triangleLUT[tableIndex][i + 1]],
                        edgeID[triangleLUT[tableIndex][i + 2]]));
            }
        }
    }

    public void writeSurfaceObj(String fileName) throws IOException
    {
        FileWriter fw = new FileWriter(fileName);
        Formatter formatter = new Formatter(fw, Locale.US);

        // Writer vertices
        for(int i = 0; i < vertices.length; i++)
        {
            //fw.write(String.format("v %f %f %f\n", vertices[i].x, vertices[i].y, vertices[i].z));
            formatter.format("v %f %f %f\n", vertices[i].x, vertices[i].y, vertices[i].z);
        }

        // Write normals
        for(int i = 0; i < normals.length; i++)
        {
            //fw.write(String.format("vn %f %f %f\n", normals[i].x, normals[i].y, normals[i].z));
            formatter.format("vn %f %f %f\n", normals[i].x, normals[i].y, normals[i].z);
        }

        // Write triangles
        for(int i = 0; i < triangles.length; i += 3)
        {
//            fw.write(String.format("f %d//%d %d//%d %d//%d\n",
//                    triangles[i] + 1, triangles[i] + 1,
//                    triangles[i + 1] + 1, triangles[i + 1] + 1,
//                    triangles[i + 2] + 1, triangles[i + 2] + 1));
            formatter.format("f %d//%d %d//%d %d//%d\n",
                    triangles[i] + 1, triangles[i] + 1,
                    triangles[i + 1] + 1, triangles[i + 1] + 1,
                    triangles[i + 2] + 1, triangles[i + 2] + 1);

        }
        fw.flush();
    }

    public void writeSurfaceVTK(String fileName, String dataSetName) throws IOException
    {
        FileWriter fw = new FileWriter(fileName);
        Formatter formatter = new Formatter(fw, Locale.US);

        fw.write("# vtk DataFile Version 1.0\n");
        fw.write(dataSetName + "\n");
        fw.write("ASCII\n\n");

        fw.write("DATASET POLYDATA\n");

        // Write vertices
        //fw.write(String.format("POINTS %d float\n", vertices.length));
        formatter.format("POINTS %d float\n", vertices.length);

        for(int i = 0; i < vertices.length; i++)
        {
            //fw.write(String.format("%f %f %f\n", vertices[i].x, vertices[i].y, vertices[i].z));
            formatter.format("%f %f %f\n", vertices[i].x, vertices[i].y, vertices[i].z);
        }

        // Write triangles
        //fw.write(String.format("POLYGONS %d %d\n", (triangles.length/3), (triangles.length/3)*4));
        formatter.format("POLYGONS %d %d\n", (triangles.length/3), (triangles.length/3)*4);
        for(int i = 0; i < triangles.length; i += 3)
        {
            //fw.write(String.format("3 %d %d %d\n", triangles[i], triangles[i + 1], triangles[i + 2]));
            formatter.format("3 %d %d %d\n", triangles[i], triangles[i + 1], triangles[i + 2]);
        }
        fw.flush();
    }

    // Returns the edge ID.
    private static int getEdgeID(int addr, int incrX, int incrY, int incrZ, int nEdgeNo)
    {
        switch (nEdgeNo)
        {
            case 0:
                return 3 * addr + 1;
            case 1:
                return 3 * (addr + incrY);
            case 2:
                return 3 * (addr + incrX) + 1;
            case 3:
                return 3 * addr;
            case 4:
                return 3 * (addr + incrZ) + 1;
            case 5:
                return 3 * (addr + incrY + incrZ);
            case 6:
                return 3 * (addr + incrX + incrZ) + 1;
            case 7:
                return 3 * (addr + incrZ);
            case 8:
                return 3 * addr + 2;
            case 9:
                return 3 * (addr + incrY) + 2;
            case 10:
                return 3 * (addr + incrX + incrY) + 2;
            case 11:
                return 3 * (addr + incrX) + 2;
            default:
                // Invalid edge no.
                return -1;
        }

    }

    // Calculates the intersection point of the isosurface with an
    // edge.
    protected MarchingCubes.Node3f intersect(float[] pos, float[] elem, int nEdgeNo)
    {
        float x1 = pos[GridTopo.X], y1 = pos[GridTopo.Y], z1 = pos[GridTopo.Z];
        float x2 = pos[GridTopo.X], y2 = pos[GridTopo.Y], z2 = pos[GridTopo.Z];
        int idx1 = 0, idx2 = 0;

        switch (nEdgeNo)
        {
            case 0:
                y2 += h[GridTopo.Y];
                idx1 = 0;
                idx2 = 1;
                break;
            case 1:
                y1 += h[GridTopo.Y];
                x2 += h[GridTopo.X];
                y2 += h[GridTopo.Y];
                idx1 = 1;
                idx2 = 2;
                break;
            case 2:
                x1 += h[GridTopo.X];
                y1 += h[GridTopo.Y];
                x2 += h[GridTopo.X];
                idx1 = 2;
                idx2 = 3;
                break;
            case 3:
                x1 += h[GridTopo.X];
                idx1 = 3;
                idx2 = 0;
                break;
            case 4:
                z1 += h[GridTopo.Z];
                y2 += h[GridTopo.Y];
                z2 += h[GridTopo.Z];
                idx1 = 4;
                idx2 = 5;
                break;
            case 5:
                y1 += h[GridTopo.Y];
                z1 += h[GridTopo.Z];
                x2 += h[GridTopo.X];
                y2 += h[GridTopo.Y];
                z2 += h[GridTopo.Z];
                idx1 = 5;
                idx2 = 6;
                break;
            case 6:
                x1 += h[GridTopo.X];
                y1 += h[GridTopo.Y];
                z1 += h[GridTopo.Z];
                x2 += h[GridTopo.X];
                z2 += h[GridTopo.Z];
                idx1 = 6;
                idx2 = 7;
                break;
            case 7:
                x1 += h[GridTopo.X];
                z1 += h[GridTopo.Z];
                z2 += h[GridTopo.Z];
                idx1 = 7;
                idx2 = 4;
                break;
            case 8:
                z2 += h[GridTopo.Z];
                idx1 = 0;
                idx2 = 4;
                break;
            case 9:
                y1 += h[GridTopo.Y];
                y2 += h[GridTopo.Y];
                z2 += h[GridTopo.Z];
                idx1 = 1;
                idx2 = 5;
                break;
            case 10:
                x1 += h[GridTopo.X];
                y1 += h[GridTopo.Y];
                x2 += h[GridTopo.X];
                y2 += h[GridTopo.Y];
                z2 += h[GridTopo.Z];
                idx1 = 2;
                idx2 = 6;
                break;
            case 11:
                x1 += h[GridTopo.X];
                x2 += h[GridTopo.X];
                z2 += h[GridTopo.Z];
                idx1 = 3;
                idx2 = 7;
                break;
        }

        return interpolate(x1, y1, z1, x2, y2, z2, elem[idx1], elem[idx2], threshold);
    }

    // Interpolates between two grid points to produce the point at which
    // the isosurface intersects an edge.
    private static MarchingCubes.Node3f interpolate(float fX1, float fY1, float fZ1, float fX2, float fY2, float fZ2, float tVal1, float tVal2, float tIsoLevel)
    {
        float mu = (tIsoLevel - tVal1) / (tVal2 - tVal1);
        return new MarchingCubes.Node3f(fX1 + mu * (fX2 - fX1), fY1 + mu * (fY2 - fY1), fZ1 + mu * (fZ2 - fZ1));
    }

    // Renames vertices and triangles so that they can be accessed more
    // efficiently.
    private void transcribeVerticesAndTriangles(HashMap<Integer, MarchingCubes.Node3f> vertexMap, ArrayList<MarchingCubes.Triangle> triangleList)
    {
        int idIter = 0;

        Iterator<Entry<Integer, MarchingCubes.Node3f>> mapIterator = vertexMap.entrySet().iterator();
        Entry<Integer, MarchingCubes.Node3f> currentEntry;
        Iterator<MarchingCubes.Triangle> vecIterator = triangleList.iterator();
        MarchingCubes.Triangle currentTri;

        // Rename vertices.
        while(mapIterator.hasNext())
        {
            currentEntry = mapIterator.next();
            currentEntry.getValue().id = idIter;
            idIter++;
        }

        // Now rename triangles.
        while(vecIterator.hasNext())
        {
            currentTri = vecIterator.next();
            currentTri.n0 = vertexMap.get(currentTri.n0).id;
            currentTri.n1 = vertexMap.get(currentTri.n1).id;
            currentTri.n2 = vertexMap.get(currentTri.n2).id;
        }

        // Copy all the vertices and triangles into two arrays so that they
        // can be efficiently accessed.
        // Copy vertices.
        mapIterator = vertexMap.entrySet().iterator();
        vertices = new Point3f[vertexMap.size()];
        for(int i = 0; i < vertices.length; i++)
        {
            currentEntry = mapIterator.next();
            vertices[i] = new Point3f(  currentEntry.getValue().x + offs[0],
                                        currentEntry.getValue().y + offs[1],
                                        currentEntry.getValue().z + offs[2]);
        }
        // Copy vertex indices which make triangles.
        vecIterator = triangleList.iterator();
        triangles = new int[triangleList.size() * 3];
        for(int i = 0; i < triangles.length; i += 3)
        {
            currentTri = vecIterator.next();
            currentTri.copyTo(i, triangles);
        }


        // Calculate normals.
        Vector3f vec1 = new Vector3f();
        Vector3f vec2 = new Vector3f();
        Vector3f normal = new Vector3f();

        int id0;
        int id1;
        int id2;

        normals = new Vector3f[vertices.length];

        // Set all normals to 0.
        for(int i = 0; i < normals.length; i++)
        {
            normals[i] = new Vector3f(0, 0, 0);
        }

        // Calculate normals.
        for(int i = 0; i < triangles.length; i += 3)
        {
            id0 = triangles[i];
            id1 = triangles[i + 1];
            id2 = triangles[i + 2];

            vec1.sub(vertices[id1], vertices[id0]);
            vec2.sub(vertices[id2], vertices[id0]);
            normal.cross(vec2, vec1);

            normals[id0].add(normal);
            normals[id1].add(normal);
            normals[id2].add(normal);
        }

        // Normalize normals.
        for(int i = 0; i < normals.length; i++)
        {
            normals[i].normalize();
        }

    }

    private static int getTableIndex(float[] elem, float threshold)
    {
        int tableIndex = 0;
        if(elem[0] < threshold)
        {
            tableIndex |= 1;
        }
        if(elem[1] < threshold)
        {
            tableIndex |= 2;
        }
        if(elem[2] < threshold)
        {
            tableIndex |= 4;
        }
        if(elem[3] < threshold)
        {
            tableIndex |= 8;
        }
        if(elem[4] < threshold)
        {
            tableIndex |= 16;
        }
        if(elem[5] < threshold)
        {
            tableIndex |= 32;
        }
        if(elem[6] < threshold)
        {
            tableIndex |= 64;
        }
        if(elem[7] < threshold)
        {
            tableIndex |= 128;
        }
        return tableIndex;

    }
    static final int[] edgeLUT =
    {
        0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
        0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
        0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
        0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
        0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
        0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
        0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
        0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
        0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
        0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
        0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
        0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
        0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
        0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
        0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
        0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
        0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
        0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
        0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
        0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
        0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
        0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
        0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
        0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
        0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
        0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
        0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
        0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
        0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
        0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
        0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
        0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0
    };
    static final int[][] triangleLUT =
    {
        {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1
        },
        {
            8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1
        },
        {
            3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1
        },
        {
            4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1
        },
        {
            4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1
        },
        {
            9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1
        },
        {
            10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1
        },
        {
            5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1
        },
        {
            5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1
        },
        {
            8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1
        },
        {
            2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1
        },
        {
            2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1
        },
        {
            11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1
        },
        {
            5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1
        },
        {
            11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1
        },
        {
            11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1
        },
        {
            2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1
        },
        {
            6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1
        },
        {
            3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1
        },
        {
            6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1
        },
        {
            6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1
        },
        {
            8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1
        },
        {
            7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1
        },
        {
            3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1
        },
        {
            0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1
        },
        {
            9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1
        },
        {
            8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1
        },
        {
            5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1
        },
        {
            0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1
        },
        {
            6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1
        },
        {
            10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1
        },
        {
            1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1
        },
        {
            0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1
        },
        {
            3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1
        },
        {
            6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1
        },
        {
            9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1
        },
        {
            8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1
        },
        {
            3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1
        },
        {
            10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1
        },
        {
            10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1
        },
        {
            2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1
        },
        {
            7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1
        },
        {
            2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1
        },
        {
            1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1
        },
        {
            11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1
        },
        {
            8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1
        },
        {
            0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1
        },
        {
            7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1
        },
        {
            7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1
        },
        {
            10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1
        },
        {
            0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1
        },
        {
            7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1
        },
        {
            6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1
        },
        {
            4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1
        },
        {
            10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1
        },
        {
            8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1
        },
        {
            1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1
        },
        {
            10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1
        },
        {
            10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1
        },
        {
            9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1
        },
        {
            7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1
        },
        {
            3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1
        },
        {
            7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1
        },
        {
            3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1
        },
        {
            6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1
        },
        {
            9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1
        },
        {
            1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1
        },
        {
            4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1
        },
        {
            7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1
        },
        {
            6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1
        },
        {
            0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1
        },
        {
            6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1
        },
        {
            0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1
        },
        {
            11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1
        },
        {
            6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1
        },
        {
            5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1
        },
        {
            9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1
        },
        {
            1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1
        },
        {
            10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1
        },
        {
            0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1
        },
        {
            11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1
        },
        {
            9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1
        },
        {
            7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1
        },
        {
            2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1
        },
        {
            9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1
        },
        {
            9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1
        },
        {
            1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1
        },
        {
            0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1
        },
        {
            10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1
        },
        {
            2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1
        },
        {
            0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1
        },
        {
            0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1
        },
        {
            9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1
        },
        {
            5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1
        },
        {
            5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1
        },
        {
            8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1
        },
        {
            9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1
        },
        {
            1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1
        },
        {
            3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1
        },
        {
            4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1
        },
        {
            9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1
        },
        {
            11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1
        },
        {
            11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1
        },
        {
            2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1
        },
        {
            9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1
        },
        {
            3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1
        },
        {
            1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1
        },
        {
            4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1
        },
        {
            0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1
        },
        {
            9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1
        },
        {
            1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        },
        {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        }
    };
}
