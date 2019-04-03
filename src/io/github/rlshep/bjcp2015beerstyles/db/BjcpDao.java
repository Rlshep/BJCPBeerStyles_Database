package io.github.rlshep.bjcp2015beerstyles.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class BjcpDao {
    private static final String LOCALE = "en_US";   //TODO: What is this?
    private static final String NULL = "NULL";

    public void setDatabaseVersion(Statement stmt) throws SQLException {
        stmt.executeUpdate("PRAGMA user_version = " + BjcpConstants.DATABASE_VERSION);
    }

    public void createTables(Statement stmt) throws SQLException {
        List<String> queries = new ArrayList<String>();

        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_CATEGORY);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_SECTION);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_VITALS);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_META);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_SYNONYMS);

        queries.add("CREATE TABLE " + BjcpContract.TABLE_META + "(" + BjcpContract.COLUMN_LOCALE + " TEXT DEFAULT '" + LOCALE + "')");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_PARENT_ID + " INTEGER, " + BjcpContract.COLUMN_CATEGORY_CODE + " TEXT, " + BjcpContract.COLUMN_NAME + " TEXT, " + BjcpContract.COLUMN_REVISION + " NUMBER, " + BjcpContract.COLUMN_LANG + " TEXT," + BjcpContract.COLUMN_BOOKMARKED + " BOOLEAN, " + BjcpContract.COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + BjcpContract.COLUMN_PARENT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_HEADER + " TEXT, " + BjcpContract.COLUMN_BODY + " TEXT, " + BjcpContract.COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_VITALS + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_HEADER + " TEXT, " + BjcpContract.COLUMN_OG_START + " REAL, " + BjcpContract.COLUMN_OG_END + " REAL, " + BjcpContract.COLUMN_FG_START + " REAL, " + BjcpContract.COLUMN_FG_END + " REAL, " + BjcpContract.COLUMN_IBU_START + " INTEGER, " + BjcpContract.COLUMN_IBU_END + " INTEGER, " + BjcpContract.COLUMN_SRM_START + " REAL, " + BjcpContract.COLUMN_SRM_END + " REAL, " + BjcpContract.COLUMN_ABV_START + " REAL, " + BjcpContract.COLUMN_ABV_END + " REAL, FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + " ) REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_SYNONYMS + "(" + BjcpContract.COLUMN_LEFT + " TEXT, " + BjcpContract.COLUMN_RIGHT  + " TEXT," + BjcpContract.COLUMN_LANG + " TEXT); ");

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
        String sql = "INSERT INTO " + BjcpContract.TABLE_META + " (" + BjcpContract.COLUMN_LOCALE + ") VALUES('" + LOCALE + "');";
        stmt.executeUpdate(sql);
    }

    private void addCategory(Statement stmt, Category category, long parentId) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_CATEGORY_CODE + ", " + BjcpContract.COLUMN_PARENT_ID  + ", " + BjcpContract.COLUMN_NAME + ", " + BjcpContract.COLUMN_REVISION + ", " + BjcpContract.COLUMN_LANG + "," + BjcpContract.COLUMN_ORDER + ") ";
        sql += "VALUES('" + category.getCategoryCode() + "'," + ((0 <= parentId) ? parentId : "NULL") + ",'" + category.getName() + "'," + category.getRevision() + ",'" + category.getLanguage() + "'," + category.getOrderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
        long id = getId(stmt);

        for (Section section : category.getSections()) {
            section.setCategoryId(id);
            addSection(stmt, section);
        }

        //Insert sub-tables if available.
        for (VitalStatistics vitalStatistics : category.getVitalStatisticses()) {
            vitalStatistics.setCategoryId(id);
            addVitalStatistics(stmt, vitalStatistics);
        }

        addCategories(stmt, category.getChildCategories(), id);
    }

    private void addVitalStatistics(Statement stmt, io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistics vitalStatistics) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_VITALS + "(" + BjcpContract.COLUMN_CAT_ID + ", " + BjcpContract.COLUMN_OG_START + ", " + BjcpContract.COLUMN_OG_END + ", " + BjcpContract.COLUMN_FG_START + ", " + BjcpContract.COLUMN_FG_END + ", " + BjcpContract.COLUMN_IBU_START + ", " + BjcpContract.COLUMN_IBU_END + ", " + BjcpContract.COLUMN_SRM_START + ", " + BjcpContract.COLUMN_SRM_END + ", " + BjcpContract.COLUMN_ABV_START + ", " + BjcpContract.COLUMN_ABV_END + ", " + BjcpContract.COLUMN_HEADER + ")";
        sql += "VALUES(" + vitalStatistics.getCategoryId() + "," + vitalStatistics.getOgStart() + "," + vitalStatistics.getOgEnd() + "," + vitalStatistics.getFgStart() + "," + vitalStatistics.getFgEnd() + "," + vitalStatistics.getIbuStart() + "," + vitalStatistics.getIbuEnd() + "," + vitalStatistics.getSrmStart() + "," + vitalStatistics.getSrmEnd() + "," + vitalStatistics.getAbvStart() + "," + vitalStatistics.getAbvEnd() + ", '" + vitalStatistics.getHeader() + "') ";

        //Write category to database.
        stmt.executeUpdate(sql);
    }


    private void addSection(Statement stmt, Section section) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_CAT_ID + " , "  + BjcpContract.COLUMN_BODY + " , " + BjcpContract.COLUMN_ORDER + ") VALUES(";
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
        // Used ISO-8859-1 to correct special characters.
        BufferedReader reader  = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"ISO-8859-1"));

        // Iterate through lines (assuming each insert has its own line and theres no other stuff)
        while (reader.ready()) {
            String insertStmt = reader.readLine();
            stmt.executeUpdate(insertStmt);
            numberOfRows++;
        }
        reader.close();

        return numberOfRows;
    }
}
