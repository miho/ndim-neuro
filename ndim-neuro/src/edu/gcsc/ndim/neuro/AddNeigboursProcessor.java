/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.ndim.DataContainer;
import org.ndim.GridTopo;
import org.ndim.MemTopo;
import org.ndim.Stencil;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class AddNeigboursProcessor extends AbstractEntityProcessor {

    private int nrNeigbours;

    /**
     * Constructor.
     *
     * @param neigbourDist number of neigbours to add
     */
    public AddNeigboursProcessor(int nrNeigbours) {
        this.nrNeigbours = nrNeigbours;
    }

    @Override
    public void process(DataContainer cnt, int[] pos) {
        
        super.process(cnt, pos);

        byte[] data = validateInput(cnt);


        GridTopo gridTopo = cnt.gridTopo();
        MemTopo memTopo = cnt.layer(0).v1;

        int dim = gridTopo.nrDims();
        int[] size = gridTopo.extent();

        int stencilSize = nrNeigbours * 2 + 1;

        // include neigbour voxel
        Stencil st = new Stencil(stencilSize, stencilSize, stencilSize);
        int[] index = new int[dim];

        int[] values = new int[dim];

        while (st.hasNext(index)) {

            st.next(index);

            boolean inRange = false;

            for (int i = 0; i < dim; i++) {
                values[i] = pos[i] + (index[i] - nrNeigbours);

                inRange = values[i] > 0 && values[i] < size[i] - 1;

                if (!inRange) {
                    break;
                }
            }

            if (inRange) {

                int nIdx = gridTopo.addr(values)
                        * memTopo.tupleIncr() + memTopo.elementIncr(0);

                data[nIdx] = (byte) 255;
            }
        }
    }

    private byte[] validateInput(final DataContainer cnt) {
        if (cnt.nrLayers() < 1) {
            throw new IllegalArgumentException(
                    "Specified data container is invalid:"
                    + " contains no layer with element size 3!");
        }

        if (!(cnt.layer(0).v2 instanceof ByteBuffer)) {
            throw new IllegalArgumentException(
                    "only FloatBuffer layers are supported!");
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
