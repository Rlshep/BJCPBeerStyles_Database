package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants;
import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Synonym;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CreateBjcpDatabase {

    private static final String XML_ENGLISH = "styleguide-2015_en.xml";
    private static final String XML_SPANISH = "styleguide-2015_es.xml";
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
            List<Category> categories =  loadDomainFromXML.loadXmlFromFile(XML_ENGLISH);
            categories.addAll(loadDomainFromXML.loadXmlFromFile(XML_SPANISH));

            bjcpDao.addCategories(stmt, categories, -1);
            bjcpDao.addMetaData(stmt);

            List<Synonym> synonyms = getCategoryNameSynonyms(categories);
            bjcpDao.addSynonyms(stmt, synonyms);

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

    private static List<Synonym> getCategoryNameSynonyms(List<Category> categories) {
        List<Synonym> synonyms = new ArrayList<>();

        for (Category category : categories) {
            if (!BjcpConstants.DEFAULT_LANGUAGE.equals(category.getLanguage())) {
               synonyms.add(createSynonym(categories, category));

               //Change if every more than one level
               for (Category childCategory : category.getChildCategories()) {
                   synonyms.add(createSynonym(categories, childCategory));
               }
            }
        }

        return synonyms;
    }

    private static String getDefaultLanguageName(List<Category> categories, Category targetCategory) {
        String defaultLanguageName = "";

        for (Category category : categories) {
            if (category.getCategoryCode().equals(targetCategory.getCategoryCode())
                    && !BjcpConstants.DEFAULT_LANGUAGE.equals(targetCategory.getLanguage())
                    && BjcpConstants.DEFAULT_LANGUAGE.equals(category.getLanguage())) {
                defaultLanguageName = category.getName();
            } else {
                for (Category childCategory : category.getChildCategories()) {
                    if (childCategory.getCategoryCode().equals(targetCategory.getCategoryCode())
                            && !BjcpConstants.DEFAULT_LANGUAGE.equals(targetCategory.getLanguage())
                            && BjcpConstants.DEFAULT_LANGUAGE.equals(childCategory.getLanguage())) {
                        defaultLanguageName = childCategory.getName();
                    }
                }
            }
        }

        return defaultLanguageName;
    }

    private static Synonym createSynonym(List<Category> categories, Category category) {
        Synonym synonym = new Synonym();

        if (!BjcpConstants.DEFAULT_LANGUAGE.equals(category.getLanguage())) {
            synonym.setTo(category.getName());
            synonym.setFrom(getDefaultLanguageName(categories, category));
            synonym.setLanguage(category.getLanguage());
        }

        return synonym;
    }
}
