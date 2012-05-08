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
import org.ndim.DataContainer;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class NdimNeuro {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        
        
        
        DataContainer cnt = 
                SWC2Image.renderSWCFile(
                new File("/Users/miho/Downloads/NeuroMorpho-test-neuron.txt"),
                false);
        
        System.out.println(">> writing container to image file");
        
        SWC2Image.container2Image(
                cnt, new File("/Users/miho/Downloads/NeuroMorpho-test-neuron.tiff"), "tiff");
        
        DataContainer cnt2 = 
                SWC2Image.renderSWCFile(
                new File("/Users/miho/Downloads/NeuroMorpho-test-neuron.txt"),
                true);
        
        System.out.println(">> writing container to image file");
        
        SWC2Image.container2Image(
                cnt2, new File("/Users/miho/Downloads/NeuroMorpho-test-neuron-neighbours.tiff"), "tiff");
        
        
    }
}
