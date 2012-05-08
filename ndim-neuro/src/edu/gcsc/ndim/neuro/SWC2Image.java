/*
 * Copyright 2012 Goethe Center for Scientific Computing (G-CSC) All rights reserved.
 * 
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Goethe Center for Scientific Computing (G-CSC).
 */
package edu.gcsc.ndim.neuro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
     * @param processor processor that can manipulate data entity-wise
     * @return data container that contains the rendered file
     * @throws IOException if an error occured while reading the specified file
     */
    public static DataContainer renderSWCFile(
            final File f, EntityProcessor processor, SizeContraint sc) throws IOException {

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
        
        int[] sizes = new int[3];
        
        // set image size depending on file size
        for(int i = 0; i < sizes.length; i++) {
            sizes[i] = Math.abs(max[i] - min[i]) + 1;
        }
        
        sc.computeSize(sizes);

        int offsetX = -min[0];
        int offsetY = -min[1];
        int offsetZ = -min[2];

        System.out.println(">> container-size: "
                + sizes[0] + ", " + sizes[1] + ", " + sizes[2]);

        final DataContainer cnt =
                new DataContainer(sizes[0], sizes[1], sizes[2]);

        cnt.createLayer(byte.class,
                new MemTopo(cnt.gridTopo().nrEntities(), 1, false));

        final GridTopo gridTopo = cnt.gridTopo();
        final MemTopo memTopo = cnt.layer(0).v1;
        final ByteBuffer buffer = (ByteBuffer) cnt.layer(0).v2;

        if (!buffer.hasArray()) {
            throw new IllegalArgumentException(
                    "FloatBuffer of layer 0 does not contain an array!");
        }

        final byte[] data = buffer.array();
        final int[] pos = new int[gridTopo.nrDims()];

        System.out.println(">> writing values to data-container");

        for (int i = 0; i < values.size(); i++) {

            pos[0] = values.get(i).x + offsetX;
            pos[1] = values.get(i).y + offsetY;
            pos[2] = values.get(i).z + offsetZ;

            int idx = gridTopo.addr(pos)
                    * memTopo.tupleIncr() + memTopo.elementIncr(0);

            data[idx] = (byte) 255;

            if (processor != null) {
                // process
                processor.process(cnt, pos);
            }

        }

        return cnt;
    }
}
