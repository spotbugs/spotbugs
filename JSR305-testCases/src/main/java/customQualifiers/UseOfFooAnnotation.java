package customQualifiers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.meta.Qualifier;
import javax.annotation.meta.When;

public class UseOfFooAnnotation {
	@Documented
	@Qualifier(strict=true)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Foo {	
		When when() default When.ALWAYS;
		ElementType[] applyTo() default {};
	}
	@Target(ElementType.TYPE)
	public @interface Foo2 {	
	}
	public static final @Foo int FOO1 = 1;
	public static final @Foo int FOO2 = 2;
	
	public static void main(String args[]) {
		int x1 = f(1); // error
		int x2 = f(FOO1); // OK
		int x3 = f2();
		System.out.println(x1+x2+x3);
		
	}
	public static int f(@Foo int y) { 
		return y;
	}

	public static @Foo int f2() {
		return 1; // error
	}
	

}
