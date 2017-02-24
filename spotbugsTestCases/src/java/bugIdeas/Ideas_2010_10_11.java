package bugIdeas;

import java.util.Collection;

import javax.ejb.EJBLocalObject;
import javax.ejb.RemoveException;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2010_10_11 {
    
    final static Integer FOO = 1;
    final static Integer BAR = 2;
    
    Integer state = FOO;
    
    public void setFoo() {
        state = FOO;
    }
    public void setBar() {
        state = BAR;
    }
    
    @DesireNoWarning("RC_REF_COMPARISON_BAD_PRACTICE")
    public boolean isFoo() {
       return state == FOO;
    }
    
    @ExpectWarning("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY")
    public void cascadeDelete(Collection value) throws RemoveException
    {
       if(!value.isEmpty())
       {
          EJBLocalObject[] locals = (EJBLocalObject[])value.toArray();
          for(int i = 0; i < locals.length; ++i)
          {
             locals[i].remove();
          }
       }
    }
}
