package sfBugs;

import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;

public class Bug1897323 implements /* java.io.Serializable */ SessionBean {

    // Threads are non-serializable
    Thread t = new Thread() {
        @Override
        public void run() {
            System.out.println("Hello");
        }
    };

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

    public Thread getThread() {
        return t;
    }
}
