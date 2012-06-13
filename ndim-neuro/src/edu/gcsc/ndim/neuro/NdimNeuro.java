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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.ndim.DataContainer;

/**
 * This is just a simple sketch.
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class NdimNeuro {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
        if (args.length!=3) {
            System.err.println(
                    ">> wrong number of arguments!");
            System.err.println(
                    ">> Usage: java -jar neighbours ndim-neuro.jar input.swc output.tiff");
            System.exit(1);
        }
        
        // required image size is 2^n for x and y
//        AbstractSizeConstraint po2c = new PowerOfTwoConstraint(0,1);
        
        // min size for x and y is 512
//        AbstractSizeConstraint minC = new MinSizeConstraint(512,512);
        
        // combine the two constraints
//        po2c.setInput(minC);
        
        // now we define a voxel processor that includes neighbour voxels
        AbstractEntityProcessor p = new AddNeigboursProcessor(Integer.parseInt(args[0]));
        
        // render swc file to ndim data container
        DataContainer cnt = 
                SWC2Image.renderSWCFile(
                new File(args[1]),
//                p, po2c);
                p, null);
        
        // write data container to tiff file
        SWC2Image.container2Image(
                cnt, new File(args[2]), "tiff");
        
        // create grid
        MarchingCubes mc = new MarchingCubes(0.0f, 1, 1, 1);
        
        ByteBuffer buffer = (ByteBuffer) cnt.layer(0).v2;
        
        
        mc.exec(cnt.gridTopo(), cnt.layer(0).v1, buffer.array());
        
        mc.writeSurfaceObj("/Users/miho/out.obj");
    }
}
