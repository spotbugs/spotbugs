package sfBugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3418588 {

    @NoWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void init(String mCmd, String connString) throws Exception {
        Connection dbCon = null;
        PreparedStatement stmt = null;
        try {
            dbCon = DriverManager.getConnection(connString);
            stmt = dbCon.prepareCall(mCmd);
            // some additional code not related to blocker
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            try {
                if (null != stmt)
                    stmt.close();
            } catch (Throwable e) {/* do nothing */
            }
            try {
                if (null != dbCon)
                    dbCon.close();
            } catch (Throwable e) {/* do nothing */
            }
        }
    }

    @NoWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void init2(String mCmd, String connString) throws Exception {
        Connection dbCon = null;
        PreparedStatement stmt = null;
        try {
            dbCon = DriverManager.getConnection(connString);
            stmt = dbCon.prepareStatement(mCmd);
            // some additional code not related to blocker
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            try {
                if (null != stmt)
                    stmt.close();
            } catch (Throwable e) {/* do nothing */
            }
            try {
                if (null != dbCon)
                    dbCon.close();
            } catch (Throwable e) {/* do nothing */
            }
        }
    }

    @NoWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void init3(String mCmd, String connString) throws Exception {
        Connection dbCon = null;
        Statement stmt = null;
        try {
            dbCon = DriverManager.getConnection(connString);
            stmt = dbCon.prepareCall(mCmd);
            // some additional code not related to blocker
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            try {
                if (null != stmt)
                    stmt.close();
            } catch (Throwable e) {/* do nothing */
            }
            try {
                if (null != dbCon)
                    dbCon.close();
            } catch (Throwable e) {/* do nothing */
            }
        }
    }

    @NoWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void init4(String mCmd, String connString) throws Exception {
        Connection dbCon = null;
        Statement stmt = null;
        try {
            dbCon = DriverManager.getConnection(connString);
            stmt = dbCon.prepareCall(mCmd);
            // some additional code not related to blocker
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
             try {
                if (null != stmt)
                    stmt.close();
            } catch (Throwable e) {/* do nothing */
            }
            try {
                if (null != dbCon)
                    dbCon.close();
            } catch (Throwable e) {/* do nothing */
            }
        }
    }

    @NoWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void init5(String mCmd, String connString) throws Exception {
        Connection dbCon = null;
        PreparedStatement stmt = null;
        try {
            dbCon = DriverManager.getConnection(connString);
            stmt = dbCon.prepareCall(mCmd);
            // some additional code not related to blocker
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            try {
                if (null != stmt)
                    stmt.close();
            } catch (Throwable e) {/* do nothing */
            }
            try {
                if (null != dbCon)
                    dbCon.close();
            } catch (Throwable e) {/* do nothing */
            }
        }
    }

}
