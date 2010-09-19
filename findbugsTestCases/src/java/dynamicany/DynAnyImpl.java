package dynamicany;

import com.sun.corba.se.impl.logging.ORBUtilSystemException;
import com.sun.corba.se.spi.logging.CORBALogDomains;
import com.sun.corba.se.spi.orb.ORB;

public class DynAnyImpl {
    protected DynAnyImpl(Object orb, Object any, boolean copyValue) {

        ORBUtilSystemException.get((ORB) orb, CORBALogDomains.RPC_PRESENTATION);
        orb.hashCode();

    }

}
