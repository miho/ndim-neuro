/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import org.ndim.*;

/**
 * Utility class that allows to write SWC files to images.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class SWC2Image {

    /**
     * Writes the specified data container to an image file.
     *
     * @param cnt container to write
     * @param out image destination
     * @param codec codec, e.g.,
     * <code>"tiff"</code>
     * @throws IOException
     * @see CODEC#Reader
     */
    public static void container2Image(
            final DataContainer cnt, final File out, final String codec) throws IOException {
        final CODEC.Writer wr = CODECRegistry.getCODEC(codec).
                getWriter(out);
        wr.write(cnt);
    }

    /**
     * Renders the specified SWC file.
     *
     * @param f file to render
     * @param includeNeighbours defines whether to include neigbour voxel
     * @return data container that contains the rendered file
     * @throws IOException if an error occured while reading the specified file
     */
    public static DataContainer renderSWCFile(
            final File f, boolean includeNeighbours) throws IOException {

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;

        IOException exception = null;

        try {
            System.out.println(">> reading file: " + f);
            reader = new BufferedReader(new FileReader(f));

            while (reader.ready()) {
                builder.append(reader.readLine()).append("\n");
            }

        } catch (IOException ex) {
            exception = ex;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    //
                }
            }

            if (exception != null) {
                throw exception;
            }
        }

        String[] lines = builder.toString().split("\n");

        int[] max = new int[3];
        int[] min = new int[3];

        ArrayList<Point3i> values = new ArrayList<Point3i>();

        System.out.println(
                ">> converting coordinates and computing dimensions");

        for (String l : lines) {

            // we filter comments
            if (l.startsWith("#")) {
                continue;
            }

            // remove leading and trailing whitespaces
            l = l.trim();

            // read x,y,z values
            String[] token = l.split("\\s");
            int x = Math.round(Float.parseFloat(token[2]));
            int y = Math.round(Float.parseFloat(token[3]));
            int z = Math.round(Float.parseFloat(token[4]));

            Point3i p = new Point3i(x, y, z);

            if (!values.contains(p)) {
                values.add(p);
            }

            // compute min and max
            max[0] = Math.max(max[0], x);
            max[1] = Math.max(max[1], y);
            max[2] = Math.max(max[2], z);
            min[0] = Math.min(min[0], x);
            min[1] = Math.min(min[1], y);
            min[2] = Math.min(min[2], z);
        }

        //
        int containerSizeX = Math.abs(max[0] - min[0]) + 1;
        int containerSizeY = Math.abs(max[1] - min[1]) + 1;
        int containerSizeZ = Math.abs(max[2] - min[2]) + 1;

        int offsetX = -min[0];
        int offsetY = -min[1];
        int offsetZ = -min[2];

        System.out.println(">> container-size: "
                + containerSizeX + ", " + containerSizeY + ", " + containerSizeZ);

        final DataContainer cnt =
                new DataContainer(containerSizeX, containerSizeY, containerSizeZ);

        cnt.createLayer(float.class,
                new MemTopo(cnt.gridTopo().nrEntities(), 1, false));

        final GridTopo gridTopo = cnt.gridTopo();
        final MemTopo memTopo = cnt.layer(0).v1;
        final FloatBuffer buffer = (FloatBuffer) cnt.layer(0).v2;

        if (!buffer.hasArray()) {
            throw new IllegalArgumentException(
                    "FloatBuffer of layer 0 does not contain an array!");
        }

        final float[] data = buffer.array();
        final int[] pos = new int[gridTopo.nrDims()];
        final int[] nPos = new int[gridTopo.nrDims()];

        System.out.println(">> writing values to data-container");

        for (int i = 0; i < values.size(); i++) {

            pos[0] = values.get(i).x + offsetX;
            pos[1] = values.get(i).y + offsetY;
            pos[2] = values.get(i).z + offsetZ;

            int idx = gridTopo.addr(pos)
                    * memTopo.tupleIncr() + memTopo.elementIncr(0);

            data[idx] = 255.f;

            // include neigbour voxel
            if (includeNeighbours) {
                Stencil st = new Stencil(3, 3, 3);
                int[] index = new int[]{0, 0, 0};

                while (st.hasNext(index)) {

                    st.next(index);

                    int xN = pos[0] + (index[0] - 1);
                    int yN = pos[1] + (index[1] - 1);
                    int zN = pos[2] + (index[2] - 1);


                    boolean xInRange = xN > 0 && xN < containerSizeX - 1;
                    boolean yInRange = yN > 0 && yN < containerSizeY - 1;
                    boolean zInRange = zN > 0 && zN < containerSizeZ - 1;

                    boolean inRange = xInRange && yInRange && zInRange;

                    if (inRange) {

                        nPos[0] = xN;
                        nPos[1] = yN;
                        nPos[2] = zN;

                        int nIdx = gridTopo.addr(nPos)
                                * memTopo.tupleIncr() + memTopo.elementIncr(0);

                        data[nIdx] = 255.f;
                    }
                }
            }
        }

        return cnt;
    }
}
