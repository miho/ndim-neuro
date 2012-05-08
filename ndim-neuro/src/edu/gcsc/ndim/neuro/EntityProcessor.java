/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.ndim.neuro;

import org.ndim.DataContainer;

/**
 * Entity processor interface. 
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public interface EntityProcessor {
    /**
     * Processes the specified entity, e.g., voxel.
     * @param cnt data container
     * @param pos position of the entity
     */
    public void process(final DataContainer cnt, int[] pos);
    
    /**
     * Defines the processor that shall be used as input.
     * @param input input processor or <code>null</code> if no input shall
     *              be defined
     */
    public void setInput(EntityProcessor input);

    /**
     * Returns the current input processor.
     * @return the current input processor or <code>null<code> if input has
     *         been defined.
     */
    public EntityProcessor getInput();
}
