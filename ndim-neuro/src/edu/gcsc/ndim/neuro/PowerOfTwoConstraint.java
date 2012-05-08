/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
