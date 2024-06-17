package sfBugsNew;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature316 {
    @ExpectWarning("IIL_PREPARE_STATEMENT_IN_LOOP")
    public int calcSumBug(Connection c, List<String> rows) throws SQLException {
        int sum = 0;
        for(String row : rows) {
            try(PreparedStatement ps = c.prepareStatement("SELECT count FROM myTable WHERE name=?")) {
                ps.setString(1, row);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) sum+=rs.getInt(1);
                }
            }
        }
        return sum;
    }

    @NoWarning("IIL_PREPARE_STATEMENT_IN_LOOP")
    public int calcSumOk(Connection c, List<String> rows) throws SQLException {
        int sum = 0;
        try(PreparedStatement ps = c.prepareStatement("SELECT count FROM myTable WHERE name=?")) {
            for(String row : rows) {
                ps.setString(1, row);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) sum+=rs.getInt(1);
                }
            }
        }
        return sum;
    }

    @NoWarning("IIL_PREPARE_STATEMENT_IN_LOOP")
    public int calcSumLazy(Connection c, List<String> rows) throws SQLException {
        int sum = 0;
        PreparedStatement ps = null;
        try {
            for(String row : rows) {
                if(ps == null) {
                    ps = c.prepareStatement("SELECT count FROM myTable WHERE name=?");
                }
                ps.setString(1, row);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) sum+=rs.getInt(1);
                }
            }
        }
        finally {
            if(ps != null) ps.close();
        }
        return sum;
    }

    @ExpectWarning("IIL_PATTERN_COMPILE_IN_LOOP")
    public int countByPattern(List<String> list) {
        int count = 0;
        for(String element : list) {
            Pattern pattern = Pattern.compile("^test");
            if(pattern.matcher(element).matches()) count++;
        }
        return count;
    }

    @ExpectWarning("IIL_PATTERN_COMPILE_IN_LOOP_INDIRECT")
    public int countByPatternIndirect(List<String> list) {
        int count = 0;
        for(String element : list) {
            if(element.matches("^test")) count++;
        }
        return count;
    }

    @NoWarning("IIL_PATTERN_COMPILE_IN_LOOP")
    public int countByPatternOk(List<String> list) {
        int count = 0;
        Pattern pattern = Pattern.compile("^test");
        for(String element : list) {
            if(pattern.matcher(element).matches()) count++;
        }
        return count;
    }
}
