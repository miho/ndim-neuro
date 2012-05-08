/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

/**
 * Usually this class should be extended
 * for custom implementations of the constraints interface.
 * 
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class AbstractSizeConstraint implements SizeContraint{
    
    private SizeContraint input;

    @Override
    public void computeSize(int[] size) {
        if (input!=null) {
            input.computeSize(size);
        }
    }
    
    @Override
    public void setInput(SizeContraint input) {
        this.input = input;
    }

    @Override
    public SizeContraint getInput() {
        return input;
    }
    
}
