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

import java.nio.ByteBuffer;
import org.ndim.*;


/**
 * Adds neigbours to the image (paints them white), i.e., paints a cube around
 * each voxel.
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class AddNeigboursProcessor extends AbstractEntityProcessor {

    private int cubeSize;

    /**
     * Constructor.
     *
     * @param cubeSize  size of the cube
     */
    public AddNeigboursProcessor(int cubeSize) {
        this.cubeSize = cubeSize;
    }

    @Override
    public void process(DataContainer cnt, int[] pos) {
        
        super.process(cnt, pos);

        byte[] data = validateInput(cnt);

        GridTopo gridTopo = cnt.gridTopo();
        MemTopo memTopo = cnt.layer(0).v1;
        AddrOp addrOp = new AddrOp(gridTopo, memTopo);

        int dim = gridTopo.nrDims();
        int[] size = gridTopo.extent();

        int stencilSize = cubeSize * 2 + 1;

        // include neigbour voxel
        Stencil st = new Stencil(stencilSize, stencilSize, stencilSize);
        int[] index = new int[dim];

        int[] values = new int[dim];

        while (st.hasNext(index)) {

            st.next(index);

            boolean inRange = false;

            for (int i = 0; i < dim; i++) {
                values[i] = pos[i] + (index[i] - cubeSize);

                inRange = values[i] > 0 && values[i] < size[i] - 1;

                if (!inRange) {
                    break;
                }
            }

            if (inRange) {
                int nIdx = addrOp.addr(values, 0);
                
//                int nIdx = gridTopo.addr(values)
//                        * memTopo.tupleIncr() + memTopo.elementIncr(0);

                data[nIdx] = (byte) 255;
            }
        }
    }

    private byte[] validateInput(final DataContainer cnt) {
        if (cnt.nrLayers() < 1) {
            throw new IllegalArgumentException(
                    "Specified data container is invalid:"
                    + " contains no layer!");
        }

        if (!(cnt.layer(0).v2 instanceof ByteBuffer)) {
            throw new IllegalArgumentException(
                    "only ByteBuffer layers are supported!");
        }

        final ByteBuffer buffer = (ByteBuffer) cnt.layer(0).v2;

        if (!buffer.hasArray()) {
            throw new IllegalArgumentException(
                    "FloatBuffer of layer 0 does not contain an array!");
        }

        final byte[] data = buffer.array();

        return data;
    }
}
