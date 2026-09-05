package sfBugs;

import java.io.Serializable;
import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBObject;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import javax.naming.Context;
import jakarta.transaction.UserTransaction;

public class Bug2421277 implements SessionBean, Serializable {

    static abstract class FakeHomeReference implements EJBHome {
    }

    static abstract class FakeRemoteReference implements EJBObject {

    }

    private static final long serialVersionUID = 1L;

    private SessionContext mySessionCtx;

    private UserTransaction transaction;

    private Context namingContext;

    private FakeHomeReference homeReference;

    private FakeRemoteReference remoteReference;

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

}
