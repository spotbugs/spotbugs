package sfBugs;

import javax.annotation.Nonnull;


public class Bug3424096 {
    
    public enum MyEnum {
        CATCHED_IS_NULL, CATCHED_IS_NOT_NULL

    }

    public static void main(String[] args) throws Exception {
        
        MyEnum myEnum = MyEnum.CATCHED_IS_NULL;
        Exception catched = null;
        boolean throwException = false;//switch this boolean
        
        try {
            doSomethingWhichCouldThrowAnException(throwException);
        } catch (Exception e) {
            myEnum = MyEnum.CATCHED_IS_NOT_NULL;
            catched = e;
        } finally {
            
            //if all if clauses are active only one bug is reported
            //if this code will be disabled the next if clause with a bug is reported
            
            if (myEnum != MyEnum.CATCHED_IS_NULL) {
                nullIsNotAllowed(catched);//Method call passes null to a nonnull parameter of nullIsNotAllowed(Exception)
            } 
            if (myEnum == MyEnum.CATCHED_IS_NOT_NULL) {
                nullIsNotAllowed(catched);//Method call passes null to a nonnull parameter of nullIsNotAllowed(Exception)
            } 
            if(throwException)
            {
                nullIsNotAllowed(catched);//Method call passes null to a nonnull parameter of nullIsNotAllowed(Exception)
            }
            if(!throwException)
            {
                nullIsNotAllowed(catched);//Method call passes null to a nonnull parameter of nullIsNotAllowed(Exception)
            } 
        } // finally
    }

    private static void nullIsNotAllowed(@Nonnull Exception catched) {
        catched.printStackTrace();
    }

    private static void doSomethingWhichCouldThrowAnException(boolean throwException) throws Exception {
        if(throwException) throw new Exception();       
    }
}
