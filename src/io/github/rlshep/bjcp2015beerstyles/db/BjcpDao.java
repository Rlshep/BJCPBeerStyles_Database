package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.domain.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.*;
import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract.*;

public class BjcpDao {
    private static final String LOCALE = "en_US";   //TODO: What is this?

    public void setDatabaseVersion(Statement stmt) throws SQLException {
        stmt.executeUpdate("PRAGMA user_version = " + DATABASE_VERSION);
}

    public void createTables(Statement stmt) throws SQLException {
        List<String> queries = new ArrayList<String>();

        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_SECTION);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_VITALS);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_META);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_SYNONYMS);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_TAG);

        queries.add("CREATE TABLE " + TABLE_META + "(" + COLUMN_LOCALE + " TEXT DEFAULT '" + LOCALE + "')");
        queries.add("CREATE TABLE " + TABLE_CATEGORY + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_PARENT_ID + " INTEGER, " + COLUMN_CATEGORY_CODE + " TEXT, " + COLUMN_NAME + " TEXT, " + COLUMN_REVISION + " TEXT, " + COLUMN_LANG + " TEXT," + COLUMN_BOOKMARKED + " BOOLEAN, " + COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + COLUMN_PARENT_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_ID + "));");
        queries.add("CREATE TABLE " + TABLE_SECTION + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_CAT_ID + " INTEGER, " + COLUMN_HEADER + " TEXT, " + COLUMN_BODY + " TEXT, " + COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + COLUMN_CAT_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_ID + "));");
        queries.add("CREATE TABLE " + TABLE_VITALS + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_CAT_ID + " INTEGER, " + COLUMN_TYPE + " TEXT NOT NULL, "  + COLUMN_HEADER + " TEXT, "  + COLUMN_NOTES + " TEXT, " + COLUMN_LOW + " REAL, " + COLUMN_HIGH + " REAL, " + " FOREIGN KEY(" + COLUMN_CAT_ID + " ) REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_ID + "));");
        queries.add("CREATE TABLE " + TABLE_SYNONYMS + "(" + COLUMN_LEFT + " TEXT, " + COLUMN_RIGHT  + " TEXT," + COLUMN_REVISION + " TEXT, " + COLUMN_LANG + " TEXT); ");
        queries.add("CREATE TABLE " + TABLE_TAG + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_CAT_ID + " INTEGER, " + COLUMN_TAG  + " TEXT, FOREIGN KEY(" + COLUMN_CAT_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + COLUMN_ID + "));");

        for (String query : queries) {
            stmt.executeUpdate(query);
        }
    }

    public void addCategories(Statement stmt, List<Category> categories, long parentId) throws SQLException {
        for (Category category : categories) {
            addCategory(stmt, category, parentId);
        }
    }

    public void addMetaData(Statement stmt) throws SQLException {
        String sql = "INSERT INTO " + TABLE_META + " (" + COLUMN_LOCALE + ") VALUES('" + LOCALE + "');";
        stmt.executeUpdate(sql);
    }

    private void addCategory(Statement stmt, Category category, long parentId) throws SQLException {
        String sql = "INSERT INTO " + TABLE_CATEGORY + "(" + COLUMN_CATEGORY_CODE + ", " + COLUMN_PARENT_ID  + ", " + COLUMN_NAME + ", " + COLUMN_REVISION + ", " + COLUMN_LANG + "," + COLUMN_ORDER + ") ";
        sql += "VALUES('" + category.getCategoryCode() + "'," + ((0 <= parentId) ? parentId : "NULL") + ",'" + category.getName() + "', '" + category.getRevision() + "','" + category.getLanguage() + "'," + category.getOrderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
        long id = getId(stmt);

        for (Section section : category.getSections()) {
            section.setCategoryId(id);
            addSection(stmt, section);
        }

        for (Tag tag : category.getTags()) {
            tag.setCategoryId(id);
            addTag(stmt, tag);
        }

        //Insert sub-tables if available.
        for (VitalStatistic vitalStatistics : category.getVitalStatisticses()) {
            vitalStatistics.setCategoryId(id);
            addVitalStatistic(stmt, vitalStatistics);
        }

        if (!category.getChildCategories().isEmpty()) {
            addCategories(stmt, category.getChildCategories(), id);
        }
    }

    private void addTag(Statement stmt, Tag tag) throws SQLException {
        String sql = "INSERT INTO " + TABLE_TAG + "(" + COLUMN_CAT_ID + " , "  + COLUMN_TAG + ") VALUES(";
        sql += tag.getCategoryId() + ", '" + tag.getTag() + "');";
        try {
            stmt.executeUpdate(sql);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVitalStatistic(Statement stmt, io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistic vitalStatistics) throws SQLException {
        String sql = "INSERT INTO " + TABLE_VITALS + "(" + COLUMN_CAT_ID + ", " + COLUMN_TYPE + ", " + COLUMN_HEADER + ", " + COLUMN_NOTES + ", " + COLUMN_LOW + ", " + COLUMN_HIGH + ")";
        sql += "VALUES(" + vitalStatistics.getCategoryId() + ",'" + vitalStatistics.getType() + "','" + vitalStatistics.getHeader() + "','" + vitalStatistics.getNotes() + "'," + vitalStatistics.getLow() + "," + vitalStatistics.getHigh() + ") ";

        //Write category to database.
        stmt.executeUpdate(sql);
    }


    private void addSection(Statement stmt, Section section) throws SQLException {
        String sql = "INSERT INTO " + TABLE_SECTION + "(" + COLUMN_CAT_ID + " , "  + COLUMN_BODY + " , " + COLUMN_ORDER + ") VALUES(";
        sql += section.getCategoryId() + ", '" + section.getBody().replace("'", "''") + "'," + section.getOrderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
    }

    private long getId(Statement statement) throws SQLException {
        long id;
        ResultSet generatedKeys = statement.getGeneratedKeys();

        if (generatedKeys.next()) {
            id = generatedKeys.getLong(1);
        } else {
            throw new SQLException("Creating user failed, no ID obtained.");
        }

        return id;
    }

    public int insertFromFile(Statement stmt, String fileName) throws IOException, SQLException {
        int numberOfRows = 0;
        // Used UTF-8 to correct special characters.
        BufferedReader reader  = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));

        // Iterate through lines (assuming each insert has its own line and theres no other stuff)
        while (reader.ready()) {
            String insertStmt = reader.readLine();
            stmt.executeUpdate(insertStmt);
            numberOfRows++;
        }
        reader.close();

        return numberOfRows;
    }

    public void addSynonyms(Statement statement, List<Synonym> synonyms) throws SQLException {
        for (Synonym synonym : synonyms) {
            addSynonym(statement, synonym);
        }
    }

    public void addSynonym(Statement statement, Synonym synonym) throws SQLException {
        String sql = "INSERT INTO " + TABLE_SYNONYMS + "(" + COLUMN_LEFT + " , "  + COLUMN_RIGHT + " , " + COLUMN_LANG + ") VALUES('";
        sql += synonym.getFrom() + "', '" + synonym.getTo() + "','" + synonym.getLanguage() + "');";

        //Write category to database.
        statement.executeUpdate(sql);
    }
}
