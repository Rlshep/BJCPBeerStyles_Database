package io.github.rlshep.bjcp2015beerstyles.db;

import io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants;
import io.github.rlshep.bjcp2015beerstyles.domain.Category;
import io.github.rlshep.bjcp2015beerstyles.domain.Section;
import io.github.rlshep.bjcp2015beerstyles.domain.Tag;
import io.github.rlshep.bjcp2015beerstyles.domain.VitalStatistics;
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

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract.*;

public class LoadDomainFromXML {
    private static final String XML_HOME = "db//";
    private static final String[] ALLOWED_SECTIONS = {"notes", "body"};
    private final List<String> allowedSections = Arrays.asList(ALLOWED_SECTIONS);
    private final static HashMap<String, String> VALUES_TO_CONVERT = new HashMap<String, String>();
    private final static String DELIM = ",";

    static {
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

    public List<Category> loadXmlFromFile(String xmlFileName) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        InputStream is = new FileInputStream(XML_HOME + xmlFileName);
        xpp.setInput(is, null);

        List<Category> categories = loadCategoriesFromXml(xpp);

        return categories;
    }

    private List<Category> loadCategoriesFromXml(XmlPullParser xpp) throws Exception {
        List<Category> categories = new ArrayList<Category>();
        int eventType = xpp.getEventType();
        Category transferCategory = new Category();
        int orderNumber = 0;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && XML_HEAD.equals(xpp.getName())) {
                String language = xpp.getAttributeValue(null, XML_LANGUAGE);

                if (BjcpConstants.allowedLanguages.contains(language)) {
                    transferCategory.setLanguage(language);
                    transferCategory.setRevision(xpp.getAttributeValue(null, XML_REVISION));
                } else {
                    throw new Exception("Invalid language");
                }
            }
            if (eventType == XmlPullParser.START_TAG && XML_CATEGORY.equals(xpp.getName())) {
                categories.add(createCategory(xpp, orderNumber, XML_CATEGORY, transferCategory));
                transferCategory.setOrderNumber(transferCategory.getOrderNumber() + 1);
                orderNumber++;
            }

            eventType = xpp.next();
        }

        return categories;
    }

    private Category createCategory(XmlPullParser xpp, int orderNumber, String tagName, Category transferCategory) throws XmlPullParserException, IOException {
        List<Section> sections = new ArrayList<Section>();
        List<Category> childCategories = new ArrayList<Category>();
        List<VitalStatistics> statistics = new ArrayList<VitalStatistics>();
        List<Tag> tags = new ArrayList<>();
        int sectionOrder = 0;
        int subCatOrder = 1 + (orderNumber * 100);    // Increasing order number for search sort.
        Category category = new Category(transferCategory);
        category.setCategoryCode(xpp.getAttributeValue(null, XML_ID));

        while (isNotTheEnd(xpp, tagName)) {
            if (isStartTag(xpp, COLUMN_NAME)) {
                category.setName(getNextText(xpp));
            } else if (isStartTag(xpp, XML_SUBCATEGORY)) {
                childCategories.add(createCategory(xpp, subCatOrder, XML_SUBCATEGORY, transferCategory));
                subCatOrder++;
            } else if (isSection(xpp)) {
                sections.add(createSection(xpp, sectionOrder));
                sectionOrder++;
            } else if (isStartTag(xpp, XML_TAGS)) {
                tags.addAll(createTags(xpp));
                sectionOrder++;
            } else if (isStartTag(xpp, XML_STATS)) {
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
        category.setTags(tags);
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

        while (isNotTheEnd(xpp, name)) {
            if (xpp.getEventType() == XmlPullParser.START_TAG) {
                bodyText += createSectionStartTag(xpp);
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

    private String createSectionStartTag(XmlPullParser xpp) {
        String startTag = "";

        if (XML_LINK.equals(xpp.getName())) {
            startTag += " <" + xpp.getName() + " href='" + getLinkHref(xpp) + "'> ";
        } else {
            startTag += convertValue(" <" + xpp.getName() + "> ");
        }

        return startTag;
    }

    private String getSectionTitle(XmlPullParser xpp) {
        String title = xpp.getAttributeValue(null, XML_TITLE);

        if (null == title) {
            title = convertValue(xpp.getName());
        }

        return title;
    }

    private String getLinkHref(XmlPullParser xpp) {
        String href = xpp.getAttributeValue(null, XML_HREF);

        if (null == href) {
            href = convertValue(xpp.getName());
        }

        return href;
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

    private List<Tag> createTags(XmlPullParser xpp) throws IOException, XmlPullParserException {
        List<Tag> tags = new ArrayList<>();
        String name = xpp.getName();
        Tag tag;

        while (isNotTheEnd(xpp, name)) {
            if ((xpp.getEventType() != XmlPullParser.START_TAG)
                    && (xpp.getEventType() != XmlPullParser.END_TAG)) {

                String s = xpp.getText().trim().replaceAll("[\n\r]", "");
                String[] tokens = s.split(DELIM);

                for (String t : tokens) {
                    tag = new Tag();
                    tag.setTag(t.trim());
                    tags.add(tag);
                }
            }
        }

        return tags;
    }

    private VitalStatistics createVitalStatistics(XmlPullParser xpp) throws XmlPullParserException, IOException {
        VitalStatistics vitalStatistics = new VitalStatistics();

        if (isStartTag(xpp, XML_STATS)) {
            vitalStatistics.setHeader(getVitalStatisticsTitle(xpp));
        }

        while (isNotTheEnd(xpp, XML_STATS)) {
            if (isStartTag(xpp, XML_EXCEPTIONS)) {
                return null;
            } else {
                vitalStatistics = createVitalStatistic(xpp, vitalStatistics);
            }
        }

        return vitalStatistics;
    }

    private VitalStatistics createVitalStatistic(XmlPullParser xpp, VitalStatistics vitalStatistics) throws XmlPullParserException, IOException {
        if (isStartTag(xpp, XML_OG)) {
            vitalStatistics.setOgStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setOgEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
        } else if (isStartTag(xpp, XML_FG)) {
            vitalStatistics.setFgStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setFgEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
        } else if (isStartTag(xpp, XML_IBU)) {
            vitalStatistics.setIbuStart(Integer.parseInt(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setIbuEnd(Integer.parseInt(getNextByName(xpp, XML_HIGH)));
        } else if (isStartTag(xpp, XML_SRM)) {
            vitalStatistics.setSrmStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setSrmEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
        } else if (isStartTag(xpp, XML_ABV)) {
            vitalStatistics.setAbvStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setAbvEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
        }

        return vitalStatistics;
    }

    private String getVitalStatisticsTitle(XmlPullParser xpp) {
        String title = xpp.getAttributeValue(null, XML_TITLE);

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
