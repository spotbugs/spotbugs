package sfBugs;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.transaction.UserTransaction;

public class Bug2421277 implements javax.ejb.SessionBean, Serializable {

    static abstract class FakeHomeReference implements EJBHome {
    }

    static abstract class FakeRemoteReference implements EJBObject {

    }

    private static final long serialVersionUID = 1L;

    private javax.ejb.SessionContext mySessionCtx;

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
