package jsr305;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.spotbugs.jsr305.annotation.meta.TypeQualifierNickname;
import com.github.spotbugs.jsr305.annotation.meta.When;

@Documented
@Foo(when = When.NEVER)
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface NeverFoo {

}
