package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.PriorityAdjustment;
import edu.umd.cs.findbugs.props.WarningProperty;

public class DeadLocalStoreProperty implements WarningProperty {
	private PriorityAdjustment priorityAdjustment;
	private String name;
	
	private DeadLocalStoreProperty(String name, PriorityAdjustment priorityAdjustment) {
		this.name = name;
		this.priorityAdjustment = priorityAdjustment;
	}
	
	public static final DeadLocalStoreProperty KILLED_BY_SUBSEQUENT_STORE =
		new DeadLocalStoreProperty("KILLED_BY_SUBSEQUENT_STORE",PriorityAdjustment.LOWER_PRIORITY);
	public static final DeadLocalStoreProperty DEFENSIVE_CONSTANT_OPCODE =
		new DeadLocalStoreProperty("DEFENSIVE_CONSTANT_OPCODE",PriorityAdjustment.FALSE_POSITIVE);
	public static final DeadLocalStoreProperty EXCEPTION_HANDLER =
		new DeadLocalStoreProperty("EXCEPTION_HANDLER",PriorityAdjustment.FALSE_POSITIVE);
	public static final DeadLocalStoreProperty DEAD_INCREMENT =
		new DeadLocalStoreProperty("DEAD_INCREMENT",PriorityAdjustment.LOWER_PRIORITY);
	public static final DeadLocalStoreProperty SINGLE_DEAD_INCREMENT =
		new DeadLocalStoreProperty("SINGLE_DEAD_INCREMENT",PriorityAdjustment.RAISE_PRIORITY);
	public static final DeadLocalStoreProperty DEAD_OBJECT_STORE =
		new DeadLocalStoreProperty("DEAD_OBJECT_STORE",PriorityAdjustment.RAISE_PRIORITY);
	public static final DeadLocalStoreProperty TWO_STORES_MULTIPLE_LOADS =
		new DeadLocalStoreProperty("TWO_STORES_MULTIPLE_LOADS",PriorityAdjustment.RAISE_PRIORITY);
	public static final DeadLocalStoreProperty SINGLE_STORE =
		new DeadLocalStoreProperty("SINGLE_STORE",PriorityAdjustment.LOWER_PRIORITY);
	public static final DeadLocalStoreProperty NO_LOADS =
		new DeadLocalStoreProperty("NO_LOADS",PriorityAdjustment.LOWER_PRIORITY);
	public static final DeadLocalStoreProperty PARAM_DEAD_ON_ENTRY =
		new DeadLocalStoreProperty("PARAM_DEAD_ON_ENTRY",PriorityAdjustment.RAISE_PRIORITY);
	public static final DeadLocalStoreProperty LOCAL_NAME =
		new DeadLocalStoreProperty("LOCAL_NAME",PriorityAdjustment.NO_ADJUSTMENT);

	//@Override
	public PriorityAdjustment getPriorityAdjustment() {
		return priorityAdjustment;
	}

	//@Override
	public String getName() {
		return DeadLocalStoreProperty.class.getName() + "." + name;
	}

}
