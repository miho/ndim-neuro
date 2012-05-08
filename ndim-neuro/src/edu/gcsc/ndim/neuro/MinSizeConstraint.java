/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class MinSizeConstraint extends AbstractSizeConstraint {

    SizeContraint parent;
    int[] include;
    int[] min;

    public MinSizeConstraint(int... min) {
        this.min = min;
    }


    @Override
    public void computeSize(int[] size) {

        super.computeSize(size);

        // compute max included size
        int max = 0;
        for (int i = 0; i < min.length; i++) {
            
            size[i] = Math.min(min[i],size[i]);
            size[i] = Math.max(min[i],size[i]);
        }
    }
}
