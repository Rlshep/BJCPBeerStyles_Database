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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract.*;

public class LoadDomainFromXML {
    private static final String XML_HOME = "db//";
    private static final String[] ALLOWED_SECTIONS = {"notes", "body", "table", "td", "tr"};
    private final List<String> allowedSections = Arrays.asList(ALLOWED_SECTIONS);
    private final static HashMap<String, String> VALUES_TO_CONVERT = new HashMap<String, String>();
    private final static String DELIM = ",";
    private final static String BREAK = "br";
    private boolean allowHeaderTarget = false;

    static {
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

    public List<Category> loadXmlFromFile(String xmlFileName, String language) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        InputStream is = new FileInputStream(XML_HOME + xmlFileName);
        xpp.setInput(is, null);

        List<Category> categories = loadCategoriesFromXml(xpp, language);

        return categories;
    }

    private List<Category> loadCategoriesFromXml(XmlPullParser xpp, String language) throws Exception {
        List<Category> categories = new ArrayList<Category>();
        int eventType = xpp.getEventType();
        Category transferCategory = new Category();
        int orderNumber = 0;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && XML_HEAD.equals(xpp.getName())) {
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
                Section section = createSection(xpp, sectionOrder);
                sections.add(section);
                tags.addAll(createExamplesTags(section.getBody()));
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
                if (!BREAK.equalsIgnoreCase( xpp.getName())) {
                    bodyText += convertValue(" </" + xpp.getName() + "> ");
                }
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
        } else if (BREAK.equalsIgnoreCase(xpp.getName())) {
            startTag += convertValue(" <" + xpp.getName() + "/> ");
        } else {
            startTag += convertValue(" <" + xpp.getName() + "> ");
        }

        return startTag;
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

    private List<Tag> createExamplesTags(String str)  {
        final Pattern pattern1 = Pattern.compile("<big>\\s*<b>\\s*Examples\\s*</b>\\s*</big>\\s*<br/>\\s*(.*?)?<br/>");
        final Pattern pattern2 = Pattern.compile("<big>\\s*<b>\\s*Examples\\s*</b>\\s*</big>\\s*<br/>\\s*(.*)?");
        final Pattern pattern3 = Pattern.compile("<big>\\s*<b>\\s*Ejemplos Comerciales\\s*</b>\\s*</big>\\s*<br/>\\s*(.*?)?<br/>");
        final Pattern pattern4 = Pattern.compile("<big>\\s*<b>\\s*Ejemplos Comerciales\\s*</b>\\s*</big>\\s*<br/>\\s*(.*)?");
        final Pattern pattern5 = Pattern.compile("<big>\\s*<b>\\s*Комерційні зразки\\s*</b>\\s*</big>\\s*<br/>\\s*(.*?)?<br/>");
        final Pattern pattern6 = Pattern.compile("<big>\\s*<b>\\s*Комерційні зразки\\s*</b>\\s*</big>\\s*<br/>\\s*(.*)?");

        Pattern[] patterns = {pattern1, pattern2, pattern3, pattern4, pattern5, pattern6};

        List<Tag> tags = new ArrayList<>();
        Tag tag;

        String s = cleanupExamples(str);
        s = getRegExValue(s, patterns);

        if (!StringUtils.isEmpty(s)) {
            String[] tokens = s.split(DELIM);

            for (String t : tokens) {
                tag = new Tag();
                tag.setTag(formatTag(t));
                tags.add(tag);
            }
        }

        return tags;
    }

    private String getRegExValue(String str, Pattern[] patterns) {
        String regEx = "";

        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(str);

            if (matcher.find()) {
                regEx = matcher.group(1);
                break;
            }
        }

        return regEx;
    }

    private String formatTag(String s) {
        StringBuilder tag = new StringBuilder();
        s = s.trim();

        if (!StringUtils.isEmpty(s)) {
            String[] tokens = s.split(" ");

            for (String t : tokens) {
                if (!StringUtils.isEmpty(t)) {
                    tag.append(t.trim());
                    tag.append(" ");
                }
            }
        }

        return tag.toString().trim();
    }

    private String cleanupExamples(String str) {
        String s = str.trim();
        s = s.replaceAll("[\n\r]", "");
        s = s.replaceAll("'", "''");
        s = s.replaceAll("\\.","");
        s = s.replace("(local) ", "");
        s = s.replace("(bottled)", ",");
        s = s.replace("(embotellada)", "");
        s = s.replace("<strong> Dark Versions </strong> - ","");
        s = s.replace("Versiones Oscuras –","");
        s = s.replace("<strong> Темні </strong> : ","");
        s = s.replace("; <strong> Pale Versions </strong> - ","");
        s = s.replace("; Versiones Pálidas –","");
        s = s.replace("<strong>Світлі</strong>: ","");
        s = s.replace("<strong> Dark </strong> -","");
        s = s.replace("Oscuras –","");
        s = s.replace("<strong>Темні</strong>: ","");
        s = s.replace("; <strong> Pale </strong> -",",");
        s = s.replace("; Pálidas –",",");
        s = s.replace("<strong>Світлі</strong>: ",",");
        s = s.replace("(US version)","");
        s = s.replace("(versión US)","");
        s = s.replace("<strong> American </strong> - ","");
        s = s.replace("Americanos –","");
        s = s.replace("<strong> Американські </strong> :","");
        s = s.replace("; <strong> English </strong> - ",",");
        s = s.replace("; Ingleses –",",");
        s = s.replace("<strong> Англійські </strong> :",",");
        s = s.replace(" (standard)","");
        s = s.replace(" (double)","");
        s = s.replace(" (estándar)","");
        s = s.replace(" (doble)","");
        s = s.replace("The only bottled version readily available is Cantillon Grand Cru Bruocsella of whatever                single batch vintage the brewer deems worthy to bottle De Cam sometimes bottles their very old (5                years) lambic In and around Brussels there are specialty cafes that often have draught lambics from                traditional brewers or blenders such as","Cantillon Grand Cru Bruocsella,");
        s = s.replace("La única versión embotellada fácilmente disponible es Cantillon Grand Cru                    Bruocsella de cualquier batch antiguo que el cervecero considere digno de embotellar De Cam a veces                    embotella su Lambic más antigua (5 años) En los alrededores de Bruselas hay cafés de especialidad                    que a                    menudo tienen proyectos Lambic de cerveceros tradicionales o mezcladores como","Cantillon Grand Cru Bruocsella,");
        s = s.replace("Єдина пляшкова версія, яку можна придбати на постійній основі, це ламбік                Cantillon Grand Cru Bruocsella, який розливають у пляшки некупажованим з партій, які                пивовари вважатимуть годними до розливу De Cam інколи розливають дуже старі свої                ламбіки (5 річні) У брюсельських і навколишніх кафе часто бувають розливні ламбіки                від традиційних пивоварень і блендерій, таких як ","Cantillon Grand Cru Bruocsella,");
        s = s.replace("(Unfiltered)", "");
        s = s.replace("(Black Label)", "");
        s = s.replace("(brown and blond)", "");
        s = s.replace("(amber and blond)", "");
        s = s.replace("(all 3 versions)", "");
        s = s.replace("(brown)", "");
        s = s.replace("(blond)", "");
        s = s.replace("(Marrón y Rubia)", "");
        s = s.replace("(Ámbar y Rubia)", "");
        s = s.replace("(las 3 versiones)", "");
        s = s.replace("(Marrón)", "");
        s = s.replace("(Rubia)", "");
        s = s.replace("(брунатний і блонд)", "");
        s = s.replace("(бурштиновий і блонд)", "");
        s = s.replace("(всі                три версії)", "");
        s = s.replace("(брунатний)", "");
        s = s.replace("(блонд)", "");
        s = s.replace("Now made year-round by several breweries in Finland", "");
        s = s.replace("Actualmente elaborada durante todo el año por varias cervecerías finlandesas", "");
        s = s.replace("Сьогодні кілька фінських пивоварень варять це пиво цілий рік", "");
        s = s.replace("; many                microbreweries have specialty beers served only on premises often directly from the cask", "");
        s = s.replace(";                    muchas                    microcervecerías tienen cervezas de especialidad servidas solamente en locales a menudo directamente                    de                    la barrica", "");
        s = s.replace("; many microbreweries have specialty beers served only on premises                often directly from the cask", "");
        s = s.replace("[US]", "");
        s = s.replace("(NY)", "");
        s = s.replace("(MI)", "");
        s = s.replace("(MA)", "");
        s = s.replace("(WI)", "");
        s = s.replace("(OR)", "");
        s = s.replace("(NH)", "");
        s = s.replace("(OR)", "");
        s = s.replace("(MT)", "");
        s = s.replace("[UK]", "");
        s = s.replace("[France]", "");
        s = s.replace("[Francia]", "");
        s = s.replace("(various)", "");
        s = s.replace("(varias)", "");
        s = s.replace("(IN).", "");
        s = s.replace("[Canada]", "");
        s = s.replace("(Quebec)", "");
        s = s.replace("(CO)", "");
        s = s.replace("(WA)", "");

        return s;
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
            vitalStatistics.setHeaderTarget(XML_OG);

        } else if (isStartTag(xpp, XML_FG)) {
            vitalStatistics.setFgStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setFgEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
            vitalStatistics.setHeaderTarget(XML_FG);
        } else if (isStartTag(xpp, XML_IBU)) {
            vitalStatistics.setIbuStart(Integer.parseInt(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setIbuEnd(Integer.parseInt(getNextByName(xpp, XML_HIGH)));
            vitalStatistics.setHeaderTarget(XML_IBU);
        } else if (isStartTag(xpp, XML_SRM)) {
            vitalStatistics.setSrmStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setSrmEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
            vitalStatistics.setHeaderTarget(XML_SRM);
        } else if (isStartTag(xpp, XML_ABV)) {
            vitalStatistics.setAbvStart(Double.parseDouble(getNextByName(xpp, XML_LOW)));
            vitalStatistics.setAbvEnd(Double.parseDouble(getNextByName(xpp, XML_HIGH)));
            vitalStatistics.setHeaderTarget(XML_ABV);
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


