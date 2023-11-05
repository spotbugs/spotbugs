/**
 * 
 */
package org.junit.jupiter.params.provider;

/**
 * @author gtoison
 *
 */
public @interface MethodSource {
	String[] value() default "";
}
