package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;

/**
 * Warning property for FindDeadLocalStores.
 * 
 * @author David Hovemeyer
 */
public class DeadLocalStoreProperty extends AbstractWarningProperty {
	private DeadLocalStoreProperty(String name, PriorityAdjustment priorityAdjustment) {
		super(name, priorityAdjustment);
	}
	
	/** Store is killed by a subsequent store. */
	public static final DeadLocalStoreProperty KILLED_BY_SUBSEQUENT_STORE =
		new DeadLocalStoreProperty("KILLED_BY_SUBSEQUENT_STORE",PriorityAdjustment.LOWER_PRIORITY);
	/** Dead store is of a defense programming constant value. */
	public static final DeadLocalStoreProperty DEFENSIVE_CONSTANT_OPCODE =
		new DeadLocalStoreProperty("DEFENSIVE_CONSTANT_OPCODE",PriorityAdjustment.NO_ADJUSTMENT);
	/** Dead store is likely to be the exception object in an exception handler. */
	public static final DeadLocalStoreProperty EXCEPTION_HANDLER =
		new DeadLocalStoreProperty("EXCEPTION_HANDLER",PriorityAdjustment.FALSE_POSITIVE);
	/** The dead store is an increment. */
	public static final DeadLocalStoreProperty DEAD_INCREMENT =
		new DeadLocalStoreProperty("DEAD_INCREMENT",PriorityAdjustment.LOWER_PRIORITY);
	/** The dead store is an increment: the only one in the method. */
	public static final DeadLocalStoreProperty SINGLE_DEAD_INCREMENT =
		new DeadLocalStoreProperty("SINGLE_DEAD_INCREMENT",PriorityAdjustment.RAISE_PRIORITY);
	/** Dead store is of a newly allocated object. */
	public static final DeadLocalStoreProperty DEAD_OBJECT_STORE =
		new DeadLocalStoreProperty("DEAD_OBJECT_STORE",PriorityAdjustment.RAISE_PRIORITY);
	/** Method contains two stores and multiple loads of this local. */
	public static final DeadLocalStoreProperty TWO_STORES_MULTIPLE_LOADS =
		new DeadLocalStoreProperty("TWO_STORES_MULTIPLE_LOADS",PriorityAdjustment.NO_ADJUSTMENT);
	/** There is only one store of this local. (Maybe it's final?) */
	public static final DeadLocalStoreProperty SINGLE_STORE =
		new DeadLocalStoreProperty("SINGLE_STORE",PriorityAdjustment.FALSE_POSITIVE);
	/** There are no loads of this local. (Maybe it's final?). */
	public static final DeadLocalStoreProperty NO_LOADS =
		new DeadLocalStoreProperty("NO_LOADS",PriorityAdjustment.LOWER_PRIORITY);
    public static final DeadLocalStoreProperty SYNTHETIC_NAME =
        new DeadLocalStoreProperty("SYNTHETIC_NAME",PriorityAdjustment.AT_MOST_LOW);
	/** This local is a parameter which is dead on entry to the method. */
	public static final DeadLocalStoreProperty PARAM_DEAD_ON_ENTRY =
		new DeadLocalStoreProperty("PARAM_DEAD_ON_ENTRY",PriorityAdjustment.RAISE_PRIORITY_TO_HIGH);
	/** Name of the local variable. */
	public static final DeadLocalStoreProperty LOCAL_NAME =
		new DeadLocalStoreProperty("LOCAL_NAME",PriorityAdjustment.NO_ADJUSTMENT);

	/** Caching value */
	public static final DeadLocalStoreProperty CACHING_VALUE =
		new DeadLocalStoreProperty("CACHING_VALUE", PriorityAdjustment.LOWER_PRIORITY);
	/** many stores */
	public static final DeadLocalStoreProperty MANY_STORES =
		new DeadLocalStoreProperty("MANY_STORES", PriorityAdjustment.LOWER_PRIORITY);
	
	public static final DeadLocalStoreProperty STORE_OF_NULL =
		new DeadLocalStoreProperty("STORE_OF_NULL", PriorityAdjustment.AT_MOST_LOW);
    public static final DeadLocalStoreProperty STORE_OF_CONSTANT =
        new DeadLocalStoreProperty("STORE_OF_CONSTANT", PriorityAdjustment.LOWER_PRIORITY);
    public static final DeadLocalStoreProperty IS_PARAMETER =
        new DeadLocalStoreProperty("IS_PARAMETER", PriorityAdjustment.RAISE_PRIORITY);

}
