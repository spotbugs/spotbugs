package sfBugs;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;

public class Bug1897323 implements /*java.io.Serializable*/ javax.ejb.SessionBean {
	
	// Threads are non-serializable
	Thread t = new Thread() {
		public void run() {
			System.out.println("Hello");
		}
	};

	public void ejbActivate() throws EJBException, RemoteException {
	    // TODO Auto-generated method stub
	    
    }

	public void ejbPassivate() throws EJBException, RemoteException {
	    // TODO Auto-generated method stub
	    
    }

	public void ejbRemove() throws EJBException, RemoteException {
	    // TODO Auto-generated method stub
	    
    }

	public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
	    // TODO Auto-generated method stub
	    
    }

	public Thread getThread() {return t;}
}
