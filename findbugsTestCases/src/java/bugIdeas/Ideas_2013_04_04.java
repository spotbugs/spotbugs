package bugIdeas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2013_04_04 {

    // Java 7 try resource blocks generate redundant null blocks
    @NoWarning("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    public Ideas_2013_04_04 findByPK(final long centroCustoRegistro) throws SQLException {
        try (PreparedStatement pstmt = this.getConnection().prepareStatement(
                "SELECT * FROM orcamento.centrocusto WHERE registro = ?")) {
            pstmt.setLong(1, centroCustoRegistro);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? this.parseRS(rs) : null;
            }
        }
    }

    private Ideas_2013_04_04 parseRS(ResultSet rs) {
        // TODO Auto-generated method stub
        return null;
    }

    private Connection getConnection() {
        // TODO Auto-generated method stub
        return null;
    }
}
