/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

/**
 * Size constraint. This interface should be used to define min, max constraints
 * etc.
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public interface SizeContraint {
    
    /**
     * Computes the size based on the constraints.
     * @param sizes sizes
     */
    void computeSize(int[] sizes);
    
    /**
     * Defines the constraint that shall be used as input.
     * @param input input constraint or <code>null</code> if no input shall
     *              be defined
     */
    public void setInput(SizeContraint input);

    /**
     * Returns the current input constraint.
     * @return the current input constraint or <code>null<code> if input has
     *         been defined.
     */
    public SizeContraint getInput();
}
