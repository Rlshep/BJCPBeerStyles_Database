package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Synonym;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.*;

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
        List<Category> categories;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db/" + DATABASE_NAME);
            c.setAutoCommit(false);
            stmt = c.createStatement();

            bjcpDao.setDatabaseVersion(stmt);
            bjcpDao.createTables(stmt);

            // Load database from xml file.
            List<Category> categoriesEnglish =  loadDomainFromXML.loadXmlFromFile(XML_ENGLISH);
            List<Category> categoriesSpanish =  loadDomainFromXML.loadXmlFromFile(XML_SPANISH);

            categories = createHybridCategories(categoriesEnglish, categoriesSpanish, SPANISH_HYBRID);
            categories.addAll(categoriesEnglish);
            categories.addAll(categoriesSpanish);

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

    private static String getDefaultLanguageName(List<Category> categories, Category targetCategory) {
        String defaultLanguageName = "";

        for (Category category : categories) {
            if (category.getCategoryCode().equals(targetCategory.getCategoryCode())
                    && !DEFAULT_LANGUAGE.equals(targetCategory.getLanguage())
                    && DEFAULT_LANGUAGE.equals(category.getLanguage())) {
                defaultLanguageName = category.getName();
            } else {
                for (Category childCategory : category.getChildCategories()) {
                    if (childCategory.getCategoryCode().equals(targetCategory.getCategoryCode())
                            && !DEFAULT_LANGUAGE.equals(targetCategory.getLanguage())
                            && DEFAULT_LANGUAGE.equals(childCategory.getLanguage())) {
                        defaultLanguageName = childCategory.getName();
                    }
                }
            }
        }

        return defaultLanguageName;
    }

    private static List<Category> createHybridCategories(List<Category> defaultCategories, List<Category> languageCategories, String hybridLanguage) {
        List<Category> categories = new ArrayList<>();
        Category category;

        for (Category languageCategory : languageCategories) {
            category = createHybridCategory(defaultCategories, languageCategory, hybridLanguage);
            category.getChildCategories().clear();

            for (Category childCat : languageCategory.getChildCategories()) {
                category.getChildCategories().add(createHybridCategory(defaultCategories, childCat, hybridLanguage));
            }

            categories.add(category);
        }

        return categories;
    }

    private static Category createHybridCategory(List<Category> defaultCategories, Category languageCategory, String hybridLanguage) {
        Category category = new Category(languageCategory);

        category.setLanguage(hybridLanguage);
        category.setName(getDefaultLanguageName(defaultCategories, languageCategory));

        return category;
    }

}
