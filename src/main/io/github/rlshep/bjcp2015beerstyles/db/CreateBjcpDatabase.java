package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants;
import io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract;
import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Section;
import io.github.rlshep.bjcp2015beerstyles.domain.SubCategory;
import io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;


public class CreateBjcpDatabase {
    private static final String LOCALE = "en_US";
    private Statement stmt;

    public final static void main(String[] args) {
        Connection c = null;
        Statement stmt = null;
        CreateBjcpDatabase cbd = new CreateBjcpDatabase();

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db/" + BjcpConstants.DATABASE_NAME);
            stmt = c.createStatement();

            cbd.createTables(stmt);

            // Load database from xml file.
            cbd.addCategories(stmt, new LoadDataFromXML().loadXmlFromFile());
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

    private void createTables(Statement stmt) throws SQLException {
        List<String> queries = new ArrayList<String>();

        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_CATEGORY);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_SUB_CATEGORY);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_SECTION);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_VITALS);
        stmt.executeUpdate("DROP TABLE IF EXISTS " + BjcpContract.TABLE_META);

        queries.add("CREATE TABLE " + BjcpContract.TABLE_META + "(" + BjcpContract.COLUMN_LOCALE + " TEXT DEFAULT '" + LOCALE + "')");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT + " TEXT, " + BjcpContract.COLUMN_NAME + " TEXT, " + BjcpContract.COLUMN_REVISION + " NUMBER, " + BjcpContract.COLUMN_LANG + " TEXT," + BjcpContract.COLUMN_ORDER + " INTEGER" + " );");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_SUB_CATEGORY + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_SUB_CAT + " TEXT, " + BjcpContract.COLUMN_NAME + " TEXT, " + BjcpContract.COLUMN_TAPPED + " BOOLEAN, " + BjcpContract.COLUMN_ORDER + " INTEGER," + " FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_SUB_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_HEADER + " TEXT, " + BjcpContract.COLUMN_BODY + " TEXT, " + BjcpContract.COLUMN_ORDER + " INTEGER, FOREIGN KEY(" + BjcpContract.COLUMN_CAT_ID + ") REFERENCES " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_ID + "), FOREIGN KEY(" + BjcpContract.COLUMN_SUB_CAT_ID + " ) REFERENCES " + BjcpContract.TABLE_SUB_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");
        queries.add("CREATE TABLE " + BjcpContract.TABLE_VITALS + "(" + BjcpContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BjcpContract.COLUMN_SUB_CAT_ID + " INTEGER, " + BjcpContract.COLUMN_OG_START + " TEXT, " + BjcpContract.COLUMN_OG_END + " TEXT, " + BjcpContract.COLUMN_FG_START + " TEXT, " + BjcpContract.COLUMN_FG_END + " TEXT, " + BjcpContract.COLUMN_IBU_START + " TEXT, " + BjcpContract.COLUMN_IBU_END + " TEXT, " + BjcpContract.COLUMN_SRM_START + " TEXT, " + BjcpContract.COLUMN_SRM_END + " TEXT, " + BjcpContract.COLUMN_ABV_START + " TEXT, " + BjcpContract.COLUMN_ABV_END + " TEXT, FOREIGN KEY(" + BjcpContract.COLUMN_SUB_CAT_ID + " ) REFERENCES " + BjcpContract.TABLE_SUB_CATEGORY + "(" + BjcpContract.COLUMN_ID + "));");

        for (String query : queries) {
            stmt.executeUpdate(query);
        }
    }

    private void addCategories(Statement stmt, List<Category> categories) throws SQLException {
        for (Category category : categories) {
            addCategory(stmt, category);
        }
    }

    private void addCategory(Statement stmt, Category category) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_CATEGORY + "(" + BjcpContract.COLUMN_CAT + ", " + BjcpContract.COLUMN_NAME + ", " + BjcpContract.COLUMN_REVISION + ", " + BjcpContract.COLUMN_LANG + "," + BjcpContract.COLUMN_ORDER + ") ";
        sql += "VALUES('" + category.get_category() + "','" + category.get_name() + "'," + category.get_revision() + ",'" + category.get_language() + "'," + category.get_orderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
        long id = getId(stmt);

        //Insert sub-tables if available.
        for (SubCategory subCategory : category.get_subCategories()) {
            subCategory.set_categoryId(id);
            addSubCategory(stmt, subCategory);
        }

        for (Section section : category.get_sections()) {
            section.set_categoryId(id);
            addSection(stmt, section);
        }
    }

    private void addSubCategory(Statement stmt, SubCategory subCategory) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_SUB_CATEGORY + "(" + BjcpContract.COLUMN_CAT_ID + ", " + BjcpContract.COLUMN_SUB_CAT + ", " + BjcpContract.COLUMN_NAME + ", " + BjcpContract.COLUMN_TAPPED + ", " + BjcpContract.COLUMN_ORDER + ") ";
        sql += "VALUES(" + subCategory.get_categoryId() + ",'" + subCategory.get_subCategory() + "','" + subCategory.get_name() + "'," + ((subCategory.is_tapped()) ? 1 : 0) + "," + subCategory.get_orderNumber() + ");";

        //Write category to database.
        stmt.executeUpdate(sql);
        long id = getId(stmt);

        //Insert sub-tables if available.
        if (null != subCategory.get_vitalStatistics()) {
            subCategory.get_vitalStatistics().set_subCategoryId(id);
            addVitalStatistics(stmt, subCategory.get_vitalStatistics());
        }

        for (Section section : subCategory.get_sections()) {
            section.set_subCategoryId(id);
            addSection(stmt, section);
        }
    }

    private void addVitalStatistics(Statement stmt, VitalStatistics vitalStatistics) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_VITALS + "(" + BjcpContract.COLUMN_SUB_CAT_ID + ", " + BjcpContract.COLUMN_OG_START + ", " + BjcpContract.COLUMN_OG_END + ", " + BjcpContract.COLUMN_FG_START + ", " + BjcpContract.COLUMN_FG_END + ", " + BjcpContract.COLUMN_IBU_START + ", " + BjcpContract.COLUMN_IBU_END + ", " + BjcpContract.COLUMN_SRM_START + ", " + BjcpContract.COLUMN_SRM_END + ", " + BjcpContract.COLUMN_ABV_START + ", " + BjcpContract.COLUMN_ABV_END + ")";
        sql += "VALUES(" + vitalStatistics.get_subCategoryId() + ",'" + vitalStatistics.get_ogStart() + "','" + vitalStatistics.get_ogEnd() + "','" + vitalStatistics.get_fgStart() + "','" + vitalStatistics.get_fgEnd() + "','" + vitalStatistics.get_ibuStart() + "','" + vitalStatistics.get_ibuEnd() + "','" + vitalStatistics.get_srmStart() + "','" + vitalStatistics.get_srmEnd() + "','" + vitalStatistics.get_abvStart() + "','" + vitalStatistics.get_abvEnd() + "') ";

        //Write category to database.
        stmt.executeUpdate(sql);
    }


    private void addSection(Statement stmt, Section section) throws SQLException {
        String sql = "INSERT INTO " + BjcpContract.TABLE_SECTION + "(" + BjcpContract.COLUMN_CAT_ID + " , " + BjcpContract.COLUMN_SUB_CAT_ID + " , " + BjcpContract.COLUMN_HEADER + " , " + BjcpContract.COLUMN_BODY + " , " + BjcpContract.COLUMN_ORDER + ") VALUES(";

        // Only want to tie to Category if SubCategory is missing.
        if (section.get_subCategoryId() > 0) {
            sql += "NULL, " + section.get_subCategoryId() + ",'";
        } else {
            sql += section.get_categoryId() + ",NULL,'";
        }
        sql += section.get_header() + "','" + section.get_body().replace("'", "''") + "'," + section.get_orderNumber() + ");";

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
}
