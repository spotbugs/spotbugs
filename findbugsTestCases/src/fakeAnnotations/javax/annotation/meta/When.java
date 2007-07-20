package javax.annotation.meta;

/**
 * Used to describe the relationship between a qualifier T and the set of values S possible 
 * on an annotated element.
 *
 */
public enum When {
	/** Assume that S is a subset of T, but don't check on assignment/return */ ASSUME_ALWAYS,
	/** S is a subset of T */ ALWAYS, 
	/** nothing definitive is known about the relation between S and T */ UNKNOWN, 
	/** S - T is nonempty */ MAYBE_NOT, 
	/** S intersection T is empty */ NEVER;
	
}
