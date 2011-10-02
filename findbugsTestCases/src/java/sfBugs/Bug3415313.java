package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.WillClose;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3415313 {


    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void fp1(Connection sesscon) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT groupcounter,");
            sql.append(" grouppoolcode,");
            sql.append(" groupdescription");
            sql.append(" FROM DataGroup");
            sql.append(" ORDER BY groupcounter");
            ps = sesscon.prepareStatement(sql.toString());
            int col = 1;
            rs = ps.executeQuery();
            while (rs.next()) {
                // get the data
            }
        } finally {
            WorkflowUtils.closeResultSet(rs);
            WorkflowUtils.closeStatement(ps);
        }
    }

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION")
    public void tp(Connection sesscon) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT groupcounter,");
        sql.append(" grouppoolcode,");
        sql.append(" groupdescription");
        sql.append(" FROM DataGroup");
        sql.append(" ORDER BY groupcounter");
        ps = sesscon.prepareStatement(sql.toString());
        int col = 1;
        rs = ps.executeQuery();
        while (rs.next()) {
            // get the data
        }

    }

    @DesireWarning(value="OBL_UNSATISFIED_OBLIGATION", confidence=Confidence.LOW)
    public void maybe(Connection sesscon) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = sesscon.prepareStatement("SELECT groupcounter FROM DataGroup");
            rs = ps.executeQuery();
            while (rs.next()) {
                // get the data
            }
        } finally {
            System.out.println("yo");
            WorkflowUtils.bar(rs);
            WorkflowUtils.baz(ps);
        }
    }
    
    @DesireWarning(value="OBL_UNSATISFIED_OBLIGATION", confidence=Confidence.MEDIUM)
    public void tp2(Connection sesscon) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = sesscon.prepareStatement("SELECT groupcounter FROM DataGroup");
            rs = ps.executeQuery();
            while (rs.next()) {
                // get the data
            }
        } finally {
            WorkflowUtils.bar();
            WorkflowUtils.baz();
        }
    }


    

    static class WorkflowUtils {
        private static void baz(PreparedStatement ps) {
            // TODO Auto-generated method stub

        }

        public static void bar(ResultSet rs) {
            // TODO Auto-generated method stub

        }
        private static void baz() {
            // TODO Auto-generated method stub

        }

        public static void bar() {
            // TODO Auto-generated method stub

        }

        public static void closeStatement(@WillClose PreparedStatement ps) {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        private static void closeResultSet(ResultSet rs) {
            try {
                rs.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
