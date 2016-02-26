package io.github.rlshep.bjcp2015beerstyles.db;

import org.apache.commons.lang.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract;
import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Section;
import io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistics;

public class LoadDataFromXML {
    private static final String XML_FILE_NAME = "styleguide-2015.xml";
    private static final String[] ALLOWED_SECTIONS = {"notes", "impression", "aroma", "appearance", "flavor", "mouthfeel", "comments", "history",
            "ingredients", "comparison", "examples", "tags", "entryinstructions", "exceptions"};
    private final List<String> allowedSections = Arrays.asList(ALLOWED_SECTIONS);
    private final static HashMap<String, String> VALUES_TO_CONVERT = new HashMap<String, String>();

    static {
        VALUES_TO_CONVERT.put("entryinstructions", "Entry Instructions");
        VALUES_TO_CONVERT.put("<ul>", "");
        VALUES_TO_CONVERT.put("</ul>", "");
        VALUES_TO_CONVERT.put("<li>", "<br>");
        VALUES_TO_CONVERT.put("</li>", "<br>");
        VALUES_TO_CONVERT.put("<definitionlist>", "");
        VALUES_TO_CONVERT.put("</definitionlist>", "");
        VALUES_TO_CONVERT.put("<definitionitem>", "");
        VALUES_TO_CONVERT.put("</definitionitem>", "");
        VALUES_TO_CONVERT.put("<definitionterm>", "<strong>");
        VALUES_TO_CONVERT.put("</definitionterm>", ": </strong>");
        VALUES_TO_CONVERT.put("<definition>", "");
        VALUES_TO_CONVERT.put("</definition>", "<br><br>");
        VALUES_TO_CONVERT.put("<colgroup>", "");
        VALUES_TO_CONVERT.put("</colgroup>", "");
        VALUES_TO_CONVERT.put("<4% ABV", "&lt;4% ABV");
    }

    public List<Category> loadXmlFromFile() throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        InputStream is = new FileInputStream("C://Users//Richard//Documents//GitHub//LoadBjcpDb//db//" + XML_FILE_NAME);

        xpp.setInput(is, null);

        return loadCategoriesFromXml(xpp);
    }

    private List<Category> loadCategoriesFromXml(XmlPullParser xpp) throws XmlPullParserException, IOException {
        List<Category> categories = new ArrayList<Category>();
        int orderNumber = 0;
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && BjcpContract.XML_CATEGORY.equals(xpp.getName())) {
                categories.add(createCategory(xpp, orderNumber, BjcpContract.XML_CATEGORY));
                orderNumber++;
            }

            eventType = xpp.next();
        }

        return categories;
    }

    private Category createCategory(XmlPullParser xpp, int orderNumber, String tagName) throws XmlPullParserException, IOException {
        Category category = new Category(xpp.getAttributeValue(null, BjcpContract.XML_ID));
        List<Section> sections = new ArrayList<Section>();
        List<Category> childCategories = new ArrayList<Category>();
        List<VitalStatistics> statistics = new ArrayList<VitalStatistics>();
        int sectionOrder = 0;
        int subCatOrder = 1 + (orderNumber * 100);    // Increasing order number for search sort.

        while (isNotTheEnd(xpp, tagName)) {
            if (isStartTag(xpp, BjcpContract.COLUMN_NAME)) {
                category.setName(getNextText(xpp));
            } else if (isStartTag(xpp, BjcpContract.XML_SUBCATEGORY)) {
                childCategories.add(createCategory(xpp, subCatOrder, BjcpContract.XML_SUBCATEGORY));
                subCatOrder++;
            } else if (isSection(xpp)) {
                sections.add(createSection(xpp, sectionOrder));
                sectionOrder++;
            } else if (isStartTag(xpp, BjcpContract.XML_STATS)) {
                VitalStatistics vitalStatistics = createVitalStatistics(xpp);

                // If statistics is an exception then add to sections.
                if (null == vitalStatistics) {
                    sections.add(createSection(xpp, sectionOrder));
                    sectionOrder++;
                } else {
                    statistics.add(vitalStatistics);
                }
            }
        }

        category.setSections(sections);
        category.setChildCategories(childCategories);
        category.setOrderNumber(orderNumber);
        category.setVitalStatisticses(statistics);

        return category;
    }

    private boolean isSection(XmlPullParser xpp) throws XmlPullParserException {
        return xpp.getEventType() == XmlPullParser.START_TAG && allowedSections.contains(xpp.getName());
    }

    private Section createSection(XmlPullParser xpp, int orderNumber) throws XmlPullParserException, IOException {
        String name = xpp.getName();
        String bodyText = "";
        Section section = new Section(orderNumber);

        if (isStartTag(xpp, name)) {
            section.setHeader(getSectionTitle(xpp));
        }

        while (isNotTheEnd(xpp, name)) {
            if (xpp.getEventType() == XmlPullParser.START_TAG) {
                bodyText += convertValue(" <" + xpp.getName() + "> ");
            } else if (xpp.getEventType() == XmlPullParser.END_TAG) {
                bodyText += convertValue(" </" + xpp.getName() + "> ");
            } else {
                bodyText += convertValue(xpp.getText().trim());
            }
        }

        bodyText = bodyText.replaceAll("[\n\r]", "").trim();
        section.setBody(bodyText);

        return section;
    }

    private String getSectionTitle(XmlPullParser xpp) {
        String title = xpp.getAttributeValue(null, BjcpContract.XML_TITLE);

        if (null == title) {
            title = convertValue(xpp.getName());
        }

        return title;
    }

    private String convertValue(String value) {
        String converted = value.trim();

        if (VALUES_TO_CONVERT.containsKey(converted)) {
            converted = VALUES_TO_CONVERT.get(converted);
        } else {
            converted = StringUtils.capitalize(value);
        }

        return converted;
    }

    private VitalStatistics createVitalStatistics(XmlPullParser xpp) throws XmlPullParserException, IOException {
        VitalStatistics vitalStatistics = new VitalStatistics();

        if (isStartTag(xpp, BjcpContract.XML_STATS)) {
            vitalStatistics.setHeader(getVitalStatisticsTitle(xpp));
        }

        while (isNotTheEnd(xpp, BjcpContract.XML_STATS)) {
            if (isStartTag(xpp, BjcpContract.XML_EXCEPTIONS)) {
                return null;
            } else {
                vitalStatistics = createVitalStatistic(xpp, vitalStatistics);
            }
        }

        return vitalStatistics;
    }

    private VitalStatistics createVitalStatistic(XmlPullParser xpp, VitalStatistics vitalStatistics) throws XmlPullParserException, IOException {
        if (isStartTag(xpp, BjcpContract.XML_OG)) {
            vitalStatistics.setOgStart(getNextByName(xpp, BjcpContract.XML_LOW));
            vitalStatistics.setOgEnd(getNextByName(xpp, BjcpContract.XML_HIGH));
        } else if (isStartTag(xpp, BjcpContract.XML_FG)) {
            vitalStatistics.setFgStart(getNextByName(xpp, BjcpContract.XML_LOW));
            vitalStatistics.setFgEnd(getNextByName(xpp, BjcpContract.XML_HIGH));
        } else if (isStartTag(xpp, BjcpContract.XML_IBU)) {
            vitalStatistics.setIbuStart(getNextByName(xpp, BjcpContract.XML_LOW));
            vitalStatistics.setIbuEnd(getNextByName(xpp, BjcpContract.XML_HIGH));
        } else if (isStartTag(xpp, BjcpContract.XML_SRM)) {
            vitalStatistics.setSrmStart(getNextByName(xpp, BjcpContract.XML_LOW));
            vitalStatistics.setSrmEnd(getNextByName(xpp, BjcpContract.XML_HIGH));
        } else if (isStartTag(xpp, BjcpContract.XML_ABV)) {
            vitalStatistics.setAbvStart(getNextByName(xpp, BjcpContract.XML_LOW));
            vitalStatistics.setAbvEnd(getNextByName(xpp, BjcpContract.XML_HIGH));
        }

        return vitalStatistics;
    }

    private String getVitalStatisticsTitle(XmlPullParser xpp) {
        String title = xpp.getAttributeValue(null, BjcpContract.XML_TITLE);

        if (null == title) {
            title = "";
        }

        return title;
    }

    private boolean isStartTag(XmlPullParser xpp, String name) throws XmlPullParserException {
        return xpp.getEventType() == XmlPullParser.START_TAG && name.equals(xpp.getName());
    }

    private String getNextText(XmlPullParser xpp) throws IOException, XmlPullParserException {
        String text = "";
        int eventType = xpp.next();

        if (eventType != XmlPullParser.END_DOCUMENT
                && eventType == XmlPullParser.TEXT) {
            text = xpp.getText();
        }

        return text;
    }

    private String getNextByName(XmlPullParser xpp, String name) throws IOException, XmlPullParserException {
        String text = "";

        while (isNotTheEnd(xpp, name)) {
            if (isStartTag(xpp, name)) {
                text = getNextText(xpp);
            }
        }

        return text;
    }

    private boolean isNotTheEnd(XmlPullParser xpp, String name) throws IOException, XmlPullParserException {
        int eventType = xpp.next();

        return eventType != XmlPullParser.END_DOCUMENT && !(eventType == XmlPullParser.END_TAG && name.equals(xpp.getName()));
    }
}
