/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

import org.ndim.DataContainer;

/**
 * Abstract entity processor. Usually this class should be extended
 * for custom implementations of the processor interface.
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class AbstractEntityProcessor implements EntityProcessor{
    
    EntityProcessor input;

    @Override
    public void process(DataContainer cnt, int[] pos) {
        if (input!=null) {
            input.process(cnt, pos);
        }
    }

    @Override
    public void setInput(EntityProcessor input) {
        this.input = input;
    }

    @Override
    public EntityProcessor getInput() {
        return input;
    }
    
}
