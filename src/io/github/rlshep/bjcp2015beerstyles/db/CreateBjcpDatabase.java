package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.domain.Category;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.*;

public class CreateBjcpDatabase {

    private static final String XML_ENGLISH_2015 = "styleguide-2015_en.xml";
    private static final String XML_SPANISH_2015 = "styleguide-2015_es.xml";
    private static final String XML_UKRAINIAN_2015 = "styleguide-2015_uk.xml";
    private static final String XML_BA_2021 = "2021_BA_Beer_Style_Guidelines_Final.xml";
    private static final String SYNONYM_FILE_NAME = "db//load_synonyms.sql";
    private static final String FTS_FILE_NAME = "db//load_fts_search.sql";

    public final static void main(String[] args) {
        Connection c = null;
        Statement stmt = null;
        BjcpDao bjcpDao = new BjcpDao();
        LoadDomainFromXML loadDomainFromXML = new LoadDomainFromXML();
        List<Category> categories = new ArrayList<>();

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db/" + DATABASE_NAME);
            c.setAutoCommit(false);
            stmt = c.createStatement();

            bjcpDao.setDatabaseVersion(stmt);
            bjcpDao.createTables(stmt);

            // Load database from xml file.
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_ENGLISH_2015, ENGLISH));
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_SPANISH_2015, SPANISH));
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_UKRAINIAN_2015, UKRANIAN));
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_BA_2021, ENGLISH));
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_BA_2021, SPANISH));
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_BA_2021, UKRANIAN));

            bjcpDao.addCategories(stmt, categories, -1);
            bjcpDao.addMetaData(stmt);

            loadSqlFiles(bjcpDao, stmt, SYNONYM_FILE_NAME);
            loadSqlFiles(bjcpDao, stmt, FTS_FILE_NAME);

            c.commit();
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
