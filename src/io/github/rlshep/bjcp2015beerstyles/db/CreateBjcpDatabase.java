package io.github.rlshep.bjcp2015beerstyles.db;

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
    private static final String LOCALE = "en_US";
    private static final String NULL = "NULL";
    private Statement stmt;

    public final static void main(String[] args) {
        Connection c = null;
        Statement stmt = null;
        CreateBjcpDatabase cbd = new CreateBjcpDatabase();

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db/" + BjcpConstants.DATABASE_NAME);
            stmt = c.createStatement();

            cbd.setDatabaseVersion(stmt);
            cbd.createTables(stmt);

            // Load database from xml file.
            cbd.addCategories(stmt, new LoadDataFromXML().loadXmlFromFile(), -1);
            cbd.addMetaData(stmt);
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
    private void setDatabaseVersion(Statement stmt) throws SQLException {
        stmt.executeUpdate("PRAGMA user_version = " + BjcpConstants.DATABASE_VERSION);
    }

    private void createTables(Statement stmt) throws SQLException {
        List<String> queries = new ArrayList<String>();

        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_CATEGORY);
        stmt.executeUpdate("DROP TABLE IF EXISTS SUB_CATEGORY");
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_SECTION);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_VITALS);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_META);

        queries.add("CREATE TABLE " + BjcpContract.TABLE_META + "(" + BjcpContract.COLUMN_LOCALE + " TEXT DEFAULT '" + LOCALE + "')");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_PARENT_ID + " INTEGER, " + BjcpContract.COLUMN_CATEGORY_CODE + " TEXT, " + BjcpContract.COLUMN_NAME + " TEXT, " + BjcpContract.COLUMN_REVISION + " NUMBER, " + BjcpContract.COLUMN_LANG + " TEXT," + BjcpContract.COLUMN_BOOKMARKED + " BOOLEAN, " + BjcpContract.COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + BjcpContract.COLUMN_PARENT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_HEADER + " TEXT, " + BjcpContract.COLUMN_BODY + " TEXT, " + BjcpContract.COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_VITALS + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_HEADER + " TEXT, " + BjcpContract.COLUMN_OG_START + " TEXT, " + BjcpContract.COLUMN_OG_END + " TEXT, " + BjcpContract.COLUMN_FG_START + " TEXT, " + BjcpContract.COLUMN_FG_END + " TEXT, " + BjcpContract.COLUMN_IBU_START + " TEXT, " + BjcpContract.COLUMN_IBU_END + " TEXT, " + BjcpContract.COLUMN_SRM_START + " TEXT, " + BjcpContract.COLUMN_SRM_END + " TEXT, " + BjcpContract.COLUMN_ABV_START + " TEXT, " + BjcpContract.COLUMN_ABV_END + " TEXT, FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + " ) REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");

        for (String query : queries) {
            stmt.executeUpdate(query);
        }
    }

    private void addCategories(Statement stmt, List<Category> categories, long parentId) throws SQLException {
        for (Category category : categories) {
            addCategory(stmt, category, parentId);
        }
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
        sql += "VALUES(" + vitalStatistics.getCategoryId() + "," + handleNull(vitalStatistics.getOgStart()) + "," + handleNull(vitalStatistics.getOgEnd()) + "," + handleNull(vitalStatistics.getFgStart()) + "," + handleNull(vitalStatistics.getFgEnd()) + "," + handleNull(vitalStatistics.getIbuStart()) + "," + handleNull(vitalStatistics.getIbuEnd()) + "," + handleNull(vitalStatistics.getSrmStart()) + "," + handleNull(vitalStatistics.getSrmEnd()) + "," + handleNull(vitalStatistics.getAbvStart()) + "," + handleNull(vitalStatistics.getAbvEnd()) + "," + handleNull(vitalStatistics.getHeader()) + ") ";

        //Write category to database.
        stmt.executeUpdate(sql);
    }


    private void addSection(Statement stmt, Section section) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_CAT_ID + " , "  + BjcpContract.COLUMN_HEADER + " , " + BjcpContract.COLUMN_BODY + " , " + BjcpContract.COLUMN_ORDER + ") VALUES(";
        sql += section.getCategoryId() + ",'" + section.getHeader() + "','" + section.getBody().replace("'", "''") + "'," + section.getOrderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
    }

    private void addMetaData(Statement stmt) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_META + " (" + BjcpContract.COLUMN_LOCALE + ") VALUES('" + LOCALE + "');";
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

    private String handleNull(String myString) {
        String formatted = "";

        if (null != myString) {
            formatted = "'" + myString + "'";
        } else {
            formatted = NULL;
        }

        return formatted;
    }
}
