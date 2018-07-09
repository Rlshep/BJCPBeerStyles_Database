package io.github.rlshep.bjcp2015beerstyles.db;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants;
import io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract;
import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Section;
import io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistics;

public class CreateBjcpDatabase {

    private static final String XML_FILE_NAME = "styleguide-2015_en.xml";
    private static final String SYNONYM_FILE_NAME = "db//load_synonyms.sql";
    private static final String FTS_FILE_NAME = "db//load_fts_search.sql";

    public final static void main(String[] args) {
        Connection c = null;
        Statement stmt = null;
        BjcpDao bjcpDao = new BjcpDao();
        LoadDomainFromXML loadDomainFromXML = new LoadDomainFromXML();

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db/" + BjcpConstants.DATABASE_NAME);
            stmt = c.createStatement();

            bjcpDao.setDatabaseVersion(stmt);
            bjcpDao.createTables(stmt);

            // Load database from xml file.
            List<Category> categories =  loadDomainFromXML.loadXmlFromFile(XML_FILE_NAME);
            bjcpDao.addCategories(stmt, categories, -1);
            bjcpDao.addMetaData(stmt);

            loadSqlFiles(bjcpDao, stmt, SYNONYM_FILE_NAME);
            loadSqlFiles(bjcpDao, stmt, FTS_FILE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
                c.close();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }

    private final static void loadSqlFiles(BjcpDao bjcpDao, Statement stmt, String fileName) {
        try {
            int insertCount = bjcpDao.insertFromFile(stmt, fileName);
            System.out.println("Rows loaded from file= " + fileName + ": " + insertCount);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
