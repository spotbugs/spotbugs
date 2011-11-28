/*
 * Name:    $Id: SwingDeclarationDeleter.java,v 1.1 2006-12-08 13:16:34 t724z Exp $
 * Project: RM90
 * (C) 2004 Bundesamt  Informatik und Telekommunikation
 *
 */
package sfBugs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Allows to delete a declaration from the database.
 * 
 * @author t722d
 * 
 */
public class Bug3438355 extends JFrame {

    private static final long serialVersionUID = 1L;

    Bug3438355() {
        super("Declaration Deleter");

        init();
    }

    private void init() {
        final JTextField deklarationNr = new JTextField();
        final JTextField spediteurNr = new JTextField();

        JLabel traderDeclarationNumberLabel = new JLabel("traderDeclarationNumber: ");
        JLabel traderNumberLabel = new JLabel("traderNumber: ");
        JLabel statusLabel = new JLabel("status: ");
        final JLabel statusText = new JLabel("");

        JButton deleteButton = new JButton("Lsche Deklaration");

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(traderDeclarationNumberLabel);
        panel.add(deklarationNr);

        panel.add(traderNumberLabel);
        panel.add(spediteurNr);

        panel.add(statusLabel);
        panel.add(statusText);

        panel.add(new JLabel());
        panel.add(deleteButton);

        getContentPane().add(panel);
        pack();

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    statusText.setText("Work in progress ...");
                    int records = deleteDeclaration1(deklarationNr.getText(), spediteurNr.getText());
                    statusText.setText(records + " Deklaration(en) wurden entfernt.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusText.setText("");
                    JOptionPane.showMessageDialog(Bug3438355.this, ex);
                }
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public int foo(PreparedStatement stmt) throws Exception {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery();
            rs.next();
            int x = rs.getInt(1);
            stmt.executeQuery();
            return x;
        } finally {
            if (rs != null)
                rs.close();
        }

    }

    private int deleteDeclaration1(String spediDeklNr, String spediNr) throws Exception {
        Hashtable ht = new Hashtable();
        ht.put(InitialContext.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        int countBefore = 0;
        int countAfter = 0;

        InitialContext initialContext = new InitialContext(ht);
        DataSource ds = (DataSource) initialContext.lookup("jdbc/EdecDataSourceNonXA");
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = ds.getConnection();
            // count affected rows
            statement = con
                    .prepareStatement("select count(*) as rowcount from edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ? ");
            statement.setString(1, spediDeklNr);
            statement.setString(2, spediNr);
            rs = statement.executeQuery();
            rs.next();
            countBefore = rs.getInt("rowcount");
            rs.close();

            // delete declarations
            statement = null;
            statement = con
                    .prepareStatement("DELETE FROM edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ?");
            statement.setString(1, spediDeklNr);
            statement.setString(2, spediNr);
            statement.executeQuery();

            statement = null;
            statement = con
                    .prepareStatement("DELETE FROM edec_deklarationen WHERE dek_id IN (SELECT d.dek_id FROM edec_deklarationen d "
                            + "INNER JOIN edec_dekl_kopf k ON d.dek_id = k.dko_dek_id WHERE k.dko_spediteur_dekl_nr like ? )");
            statement.setString(1, spediDeklNr);
            statement.executeQuery();
            
             // count again
             statement = null;
             statement =
             con.prepareStatement("select count(*) as rowcount from edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ? ");
             statement.setString(1, spediDeklNr);
             statement.setString(2, spediNr);
             rs = statement.executeQuery();
            
             rs.next();
             countAfter = rs.getInt("rowcount");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return (countBefore - countAfter);
    }

    private int deleteDeclaration2(String spediDeklNr, String spediNr) throws Exception {
        Hashtable ht = new Hashtable();
        ht.put(InitialContext.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        int countBefore = 0;
        int countAfter = 0;

        InitialContext initialContext = new InitialContext(ht);
        DataSource ds = (DataSource) initialContext.lookup("jdbc/EdecDataSourceNonXA");
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = ds.getConnection();
            // count affected rows
            statement = con
                    .prepareStatement("select count(*) as rowcount from edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ? ");
            statement.setString(1, spediDeklNr);
            statement.setString(2, spediNr);
            rs = statement.executeQuery();
            rs.next();
            countBefore = rs.getInt("rowcount");
            rs.close();

            // delete declarations
            statement.close();
            statement = con
                    .prepareStatement("DELETE FROM edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ?");
            statement.setString(1, spediDeklNr);
            statement.setString(2, spediNr);
            statement.executeQuery();

            statement.close();
            statement = con
                    .prepareStatement("DELETE FROM edec_deklarationen WHERE dek_id IN (SELECT d.dek_id FROM edec_deklarationen d "
                            + "INNER JOIN edec_dekl_kopf k ON d.dek_id = k.dko_dek_id WHERE k.dko_spediteur_dekl_nr like ? )");
            statement.setString(1, spediDeklNr);
            statement.executeQuery();

            // count again
            statement.close();
            statement = con
                    .prepareStatement("select count(*) as rowcount from edec_deklarationen_mgt where dko_spediteur_dekl_nr like ? and dko_spediteur_nr = ? ");
            statement.setString(1, spediDeklNr);
            statement.setString(2, spediNr);
            rs = statement.executeQuery();

            rs.next();
            countAfter = rs.getInt("rowcount");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return (countBefore - countAfter);
    }

    public static void main(String[] args) {
        Bug3438355 frame = new Bug3438355();
        frame.show();
    }

}
