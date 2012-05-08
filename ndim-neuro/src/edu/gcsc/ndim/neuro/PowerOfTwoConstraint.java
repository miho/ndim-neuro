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

/**
 * This constraint ensures that all sizes meat the requirement <code>2^n</code>.
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class PowerOfTwoConstraint extends AbstractSizeConstraint {

    SizeContraint parent;
    int[] include;

    /**
     * Constructor.
     * @param include indices of the sizes that shall be evaluated,
     *  e.g., <code>1, 2</code>. All other sizes will be ignored.
     *                
     */
    public PowerOfTwoConstraint(int... include) {
        this.include = include;
    }


    @Override
    public void computeSize(int[] size) {

        super.computeSize(size);

        // compute max included size
        int max = 0;
        for (int i = 0; i < include.length; i++) {
            int idx = include[i];
            
            int val = size[idx];
            
            if (val < 0) {
                throw new IllegalArgumentException(
                        "negative sizes not allowed!");
            }

            max = Math.max(val, max);
        }

        int powerOfTwo = 1;

        // compute nearest powerOfTwo that is equal or > x
        while (powerOfTwo < max && powerOfTwo < Integer.MAX_VALUE) {
            powerOfTwo *= 2;
        }
  

        for (int i = 0; i < include.length; i++) {

            int idx = include[i];
            size[idx] = powerOfTwo;
        }
    }
}
