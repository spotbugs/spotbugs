package sfBugsNew;

import java.io.Serializable;

public class Bug1242 {


    public static class Child extends Parent<Something>
    {
        @Override
        public Something getDuplicateValue()
        {
            Something value = super.getDuplicateValue();
            
            System.err.println("getValue() called.");
            
            return value;
        }
    }
    public static interface Duplicatable<T extends Duplicatable<T>> extends Cloneable
    {
        T getDuplicate();
    }
    public static class Interim<T extends Interim<T>> implements Duplicatable<T>
    {
        @Override
        public T getDuplicate()
        {
            try
            {
                return (T) clone();
            }
            catch (CloneNotSupportedException cnse)
            {
                return null;
            }
        }
    }
    // Alternative that produces no bug warnings:
    //public class Parent<T extends Duplicatable<T>>
    public static class Parent<T extends Interim<T>>
    {
        private T value;
        
        public void setValue(T newValue)
        {
            value = newValue;
        }
        
        public T getDuplicateValue()
        {
            if (value == null)
            {
                return null;
            }
            T duplicate = value.getDuplicate();
            return duplicate;
        }
    }
    public static class Something extends Interim<Something>
    {
        public void doStuff()
        {
            System.err.println("Hello");
        }
    }

}
