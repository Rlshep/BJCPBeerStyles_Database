package io.github.rlshep.bjcp2015beerstyles.converters;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.BJCP_2021;
import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract.*;

public class BJCP2021TextToXML extends TextToXML {
    private static final String INPUT_FILE_NAME = "./db/txt_to_xml/bjcp-beer-2021_en.txt";    //Exported from LibreWriter to wiki text
    private static final String CLEANED_FILE = "./db/txt_to_xml/bjcp-beer-2021_en_cleaned.txt";
    private static final String OUTPUT_FILE_NAME = "./db/bjcp-beer-2021_en.xml";
    private static final String CATEGORY_START = "\t<category id=\"";
    private static final String INTRO1 = "= {{anchor|Toc418087720}} {{anchor|Toc91058084}} Introduction to the 2021 Guidelines =";
    private static final String INTRO2 = "= {{anchor|Toc91058090}} Introduction to Beer Styles =";
    private static final String INTRO3 = "= {{anchor|Toc418087866}} {{anchor|Toc91058232}} Introduction to Specialty-Type Beer =";
    private static final String START_STYLES = "= {{anchor|Toc91058103}} 1. Standard American Beer =";
    private static final String START_VITALS = "'''Vital Statistics:'''";
    private static final String START_VITALS_ES = "'''Estadísticas vitales:'''";
    private static final String OG = "OG:";
    private static final String FG = "FG:";
    private static final String SRM = "SRM:";
    private static final String ABV = "ABV:";
    private static final String IBU = "IBUs:";
    private static final String GUIDE_END = "</styleguide>\n";

    //= {{anchor|Toc418087720}} {{anchor|Toc91058084}} Introduction to the 2021 Guidelines =
    private static final Pattern introNamePattern1 = Pattern.compile("\\{\\{.*\\}\\}\\s\\{\\{.*\\}\\}\\s(.*?)\\s=");
    private static final Pattern introNamePattern2 = Pattern.compile("\\{\\{.*\\}\\}\\s\\{\\{.*\\}\\}\\s'''(.*?)'''");
    //=== Best Fit ===
    private static final Pattern introNotesPattern = Pattern.compile("===?\\s(.*)\\s===?");
    private static final Pattern statsExceptionPattern = Pattern.compile("'''Vital Statistics:'''\\s(.*)?");

    //= {{anchor|Toc91058103}} 1. Standard American Beer =
    private static final Pattern categoryPattern = Pattern.compile("\\{\\{.*\\}\\}\\s(.*?)\\s=");

    private boolean introFound = false;
    private boolean introEnd = false;
    private boolean categoryNotesEnd = false;
    private StringBuilder formattedStats = new StringBuilder(); //Need to place at end of Sub Category
    private int introCount = 1;

    public static void main(String args[]) {
        BJCP2021TextToXML converter = new BJCP2021TextToXML();

        converter.convert();
    }

    public void convert() {
        try {
            // Manually converted Intro Tags table.
            cleanUpTextFile(INPUT_FILE_NAME, CLEANED_FILE);
            convertToXml(CLEANED_FILE, OUTPUT_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String preLineCleanUp(String str) {
        final String ANCHOR1 = "''== {{anchor";
        final String ANCHOR2 = "''{{anchor";
        StringBuilder out = new StringBuilder(str);

        if (str.contains(ANCHOR1) && !str.startsWith(ANCHOR1)) {
            int index = str.indexOf(ANCHOR1);
            out.insert(index, "\n\n").toString();
        } else if (str.contains(ANCHOR2) && !str.startsWith(ANCHOR2)) {
            int index = str.indexOf(ANCHOR2);
            out.insert(index, "\n\n").toString();
        }
        str = out.toString();

        str = str.replaceAll("\\{\\{anchor.*\\}\\}\\s'''Overall Impression:\\s?'''", "'''Overall Impression:'''");
        str = str.replaceAll("\\{\\{anchor.*\\}\\}\\s'''Tags:\\s?'''", "'''Tags:'''");
        str = str.replaceAll("'''Vital Statistics:'''\\s*OG:", "'''Vital Statistics:''' \nOG:");
        str = str.replaceAll("Vital Statistics:OG:1.050 – 1.057", "'''Vital Statistics:''' \nOG:1.050 – 1.057");
        str = str.replace("FG:", "\nFG:");
        str = str.replace("ABV:", "\nABV:");
        str = str.replace("{{anchor|Hlk64894127}} ", "");
        str = str.replace("{{anchor|Hlk68700167}} ", "");
        str = str.replace("{{anchor|Hlk68700392}} ", "");
        str = str.replace("{{anchor|Hlk68012083}} ", "");
        str = str.replace("{{anchor|Toc418087807}} ", "");
        str = str.replace("{{anchor|Hlk65858075}} ", "");
        str = str.replace("{{anchor|Hlk67328466}} ", "");
        str = str.replace("{{anchor|Toc180319634}} {{anchor|Toc418087742}}", "");
        str = str.replace("a {{anchor|Hlk65076713}} Base", "a Base");
        str = str.replace("''{{anchor|Toc418087867}} {{anchor|Toc91058233}} ", "");
        str = str.replace("character.{{anchor|Toc418087883}}  See", "character. See");
        str = str.replace(" <4% ABV", " &#60;4% ABV");
        str = str.replace("&", "&amp;");
        str = str.replace("= {{anchor|Toc418087895}} {{anchor|Toc91058271}} Appendix A: Alternate Categorizations =", "= {{anchor|Toc418087895}} {{anchor|Toc91058271}} A. Appendix: Alternate Categorizations =");
        str = str.replace("1. Styles Sorted Using 2008 Categories (Strict)", "A1. Styles Sorted Using 2008 Categories (Strict)");
        str = str.replace("2. Styles Sorted Using 2008 Guidelines (Modified)", "A2. Styles Sorted Using 2008 Guidelines (Modified)");
        str = str.replace("3. Styles Sorted Using Style Family", "A3. Styles Sorted Using Style Family");
        str = str.replace("4. Styles Sorted Using Country of Origin", "A4. Styles Sorted Using Country of Origin");
        str = str.replace("5. Styles Sorted Using History", "A5. Styles Sorted Using History");
        str = str.replace("}} Specialty IPA: Belgian IPA ===", "}} 21B-belgian. Specialty IPA: Belgian IPA ===");
        str = str.replace("}} Specialty IPA: Black IPA  ===", "}} 21B-black. Specialty IPA: Black IPA ===");
        str = str.replace("}} Specialty IPA: Brown IPA ===", "}} 21B-brown. Specialty IPA: Brown IPA ===");
        str = str.replace("}} Specialty IPA: Red IPA ===", "}} 21B-red. Specialty IPA: Red IPA ===");
        str = str.replace("}} Specialty IPA: Rye IPA ===", "}} 21B-rye. Specialty IPA: Rye IPA ===");
        str = str.replace("}} Specialty IPA: White IPA ===", "}} 21B-white. Specialty IPA: White IPA ===");
        str = str.replace("}} Specialty IPA: Brut IPA ===", "}} 21B-brut. Specialty IPA: Brut IPA ===");
        str = str.replace("= {{anchor|Toc418087901}} {{anchor|Toc91058277}} Appendix B: Local Styles =", "= {{anchor|Toc418087901}} {{anchor|Toc91058277}} X. Local Styles =");
        str = str.replace("'''Pampas Golden Ale'''", "== X1A. Pampas Golden Ale ==");
        str = str.replace(START_VITALS_ES, START_VITALS);   //TODO: Double check in app, translate if needed
        str = str.replace("IBU:", IBU);
        str = str.replace("'''Argentine IPA'''", "== X2A. Argentine IPA ==");
        str = str.replace("'''Catharina Sour'''", "== X4A. Catharina Sour ==");
        str = str.replace("<div style=\"margin-left:0.5in;margin-right:0in;\">", "");
        str = str.replace("</div>*", "");
        str = str.replace("# <div style=\"margin-left:0.3752in;margin-right:0in;\">", "</div>");
        str = str.replace("</div>", "");
        str = str.replace("<div style=\"color:#2e74b5;\">", "");
        str = str.replace("* '''", "'''");
        str = str.replace("Straw2-3Yellow3-4Gold5-6Amber6-9Deep amber/light copper10-14Copper14-17Deep copper/light brown17-18Brown19-22Dark Brown22-30Very Dark Brown30-35Black30+Black, opaque40+", "Straw: 2-3\nYellow: 3-4\nGold: 5-6\nAmber: 6-9\nDeep amber/light copper: 10-14\nCopper: 14-17\nDeep copper/light brown: 17-18\nBrown: 19-22\nDark Brown: 22-30\nVery Dark Brown: 30-35\nBlack: 30+\nBlack, opaque: 40+\n\n");
        str = str.replace("new uses:'''The BJCP", "new uses:\n'''The BJCP");
        str = str.replace("disqualified. '''Commercial Examples'''", "disqualified.\n '''Commercial Examples'''");
        str = str.replace("alone. '''Tags'''. ", "alone.\n '''Tags'''. ");
        return str;
    }

    protected String preCleanUp(StringBuilder str) {
        String s = str.toString();

        s = s.replace("\n\n" + IBU, "\n" + IBU);
        s = s.replace("\n\n" + SRM, "\n" + SRM);
        s = s.replace("\n15 – 22 ''(dark)''5.0 – 7.0% ''(standard)''", "SRM:15 – 22 ''(dark)'\n" + ABV + "'5.0 – 7.0% ''(standard)''");
        s = s.replace("\n\n7.0 – 9.5% ''(super)''", "\n" + ABV + "7.0 – 9.5% ''(super)''");
        s = s.replaceAll("\\{\\{anchor\\|.*\\}\\}\\s*\n", "");
        s = s.replace("malty= {{anchor|Toc91058140}}", "malty\n\n= {{anchor|Toc91058140}}");
        s = s.replace("roasty= {{anchor|Toc91058185}} 21. IPA =", "roasty\n\n= {{anchor|Toc91058185}} 21. IPA =");
        s = s.replace("malty= {{anchor|Toc91058222}} {{anchor|Toc418087856}} 27. Historical Beer =", "malty\n\n= {{anchor|Toc91058222}} {{anchor|Toc418087856}} 27. Historical Beer =");
        s = s.replace("Sahti''.''=== {{anchor|Toc418087858}} {{anchor|Toc91058223}} {{anchor|Toc418087767}} Historical Beer: Kellerbier ===", "Sahti.\n\n=== {{anchor|Toc418087858}} {{anchor|Toc91058223}} {{anchor|Toc418087767}} 27-kellerbier. Historical Beer: Kellerbier ===");
        s = s.replace("=== {{anchor|Toc91058224}} Historical Beer: Kentucky Common ===", "=== {{anchor|Toc91058224}} 27-kentucky. Historical Beer: Kentucky Common ===");
        s = s.replace("balanced=== {{anchor|Toc91058225}} Historical Beer: Lichtenhainer ===", "balanced=== {{anchor|Toc91058225}} 27-lichtenhainer. Historical Beer: Lichtenhainer ===");
        s = s.replace("}} Historical Beer: London Brown Ale ===", "}} 27-london. Historical Beer: London Brown Ale ===");
        s = s.replace("}} Historical Beer: Piwo Grodziskie ===", "}} 27-piwo. Historical Beer: Piwo Grodziskie ===");
        s = s.replace("}} Historical Beer: Pre-Prohibition Lager ===", "}} 27-preprolager. Historical Beer: Pre-Prohibition Lager ===");
        s = s.replace("}} Historical Beer: Pre-Prohibition Porter ===", "}} 27-preproporter. Historical Beer: Pre-Prohibition Porter ===");
        s = s.replace("malty=== {{anchor|Toc91058230}} Historical Beer: Roggenbier ===", "malty\n\n=== {{anchor|Toc91058230}} 27-roggen. Historical Beer: Roggenbier ===");
        s = s.replace("}} Historical Beer: Sahti ===", "}} 27-sahti. Historical Beer: Sahti ===");
        s = s.replace("'''Strength classifications:'''", "");
        s = s.replaceAll("Session –\\s\\n\\s*ABV: 3.0 – 5.0%", "ABV: 3.0 – 5.0% ''(session)''");
        s = s.replaceAll("Standard –\\s\\n\\s*ABV: 5.0 – 7.5%", "ABV: 5.0 – 7.5% ''(standard)''");
        s = s.replaceAll("Double –\\s\\n\\s*ABV: 7.5 – 10.0%", "ABV: 7.5 – 10.0% ''(double)''");
        s = s.replaceAll("===\\s\\{\\{.*\\}\\}\\s", "=== ");
        s = s.replaceAll("==\\s\\{\\{.*\\}\\}\\s\\{\\{.*\\}\\}\\s", "== ");
        s = s.replace("presentation.= {{anchor|Toc91058243}} 28. American Wild Ale =", "presentation.\n\n= {{anchor|Toc91058243}} 28. American Wild Ale =");
        s = s.replaceAll("''== <span style=\"color:#1f4e79;\">Argentine Styles</span> ==", "");
        s = s.replace("'''Vital Statistics:'''D.I.:1.042 – 1.054", "'''Vital Statistics:'''\nOG:1.042 – 1.054");
        s = s.replace("IBUs:15 – 22D.F.:1.009 – 1.013", "IBUs:15 – 22\nFG:1.009 – 1.013");
        s = s.replace("SRM:3 – 5G.A.:4,3º – 5,5º", "SRM:3 – 5");
        s = s.replace("'''Vital Statistics:'''DO:1055 – 1065", "'''Vital Statistics:'''\nOG:1.055 – 1.065");
        s = s.replace("IBUs:35 – 60DF:1008 – 1015", "IBUs:35 – 60\nFG:1.008 – 1.015");
        s = s.replace("SRM:6 – 15GA5.0 – 6.5%.", "SRM:6 – 15\nABV:5.0 – 6.5%.");
        s = s.replaceAll("== \\{\\{anchor\\|Toc91058281}} <span style=\"color:#1f4e79;\">Italian Styles</span> ==\n", "");
        s = s.replace("'''Vital Statistics:''' OG: 1.045 – 1.100IBUs: 6 – 30", "'''Vital Statistics:''' \nOG: 1.045 – 1.100\nIBUs: 6 – 30");
        s = s.replace("FG: 1.005 – 1.015 SRM: 4 – 25 ", "FG: 1.005 – 1.015 \nSRM: 4 – 25 ");
        s = s.replace("== {{anchor|Toc91058283}} <span style=\"color:#1f4e79;\">Brazilian Styles</span> ==\n", "");
        s = s.replace("'''Estatisticas “Vitais”:'''OG:1.039 – 1.048", "'''Vital Statistics:'''\nOG:1.039 – 1.048");
        s = s.replaceAll("== \\{\\{anchor\\|Toc91058285\\}\\} <span style=\"color:#1f4e79;\">New Zealand Styles</span> ==", "");
        s = s.replace("OG: 1.045 – 1.100IBUs: 6 – 30 ", "OG: 1.045 – 1.100\nIBUs: 6 – 30 ");

        return s;
    }

    protected StringBuilder formatLine(String str) {
        StringBuilder formatted = new StringBuilder();

        if (!str.contains(INTRO1) && !introFound) {
            skipNextLine = true;
        } else if (str.contains(INTRO1)) {
            introFound = true;
            skipNextLine = true;
            formatted.append(getHeaderXml(BJCP_2021));
            formatted.append(getIntroduction());
            formatted.append(getIntroSubCategory(str, "I" + introCount, introNamePattern1));
            introCount++;
        } else if (str.contains(INTRO2) || str.contains(INTRO3)) {
            // Manually moved I3 to Intro section
            formatted.append(NOTES_END);
            formatted.append(SUB_CATEGORY_END);
            formatted.append(getIntroSubCategory(str, "I" + introCount, categoryPattern));
            introCount++;
        } else if (str.contains(START_STYLES)) {
            formatted.append("\t");
            formatted.append(NOTES_END);
            formatted.append(getCategory(str));
            introEnd = true;
            categoryNotesEnd = true;
        } else if (!introEnd && isIntroNotes(str)) {
            formatted.append(formatIntroNotes(str));
        } else if (introEnd && isStartCategory(str)) {
            formatted.append(getCategory(str));
            formattedStats = new StringBuilder();
            categoryNotesEnd = true;
        } else if (introEnd && isStartSubCategory(str)) {
            formatted.append(getSubCategory(str));
            formattedStats = new StringBuilder();
        } else if (str.contains(START_VITALS) && isStatsException(str)) {
            formattedStats.append(getStats(str, XML_EXCEPTIONS, XML_ALL));
        } else if (str.contains(START_VITALS)) {
            // Nothing for now. Header in android app.
        } else if (isStatistic(str)) {
            formattedStats.append(formatStats(str));
        } else {
            formatted.append(formatNormalLine(str));
        }

        return formatted;
    }

    private StringBuilder getIntroduction() {
        final String INTRO = "Introductions";
        StringBuilder formatted = new StringBuilder();

        formatted.append(CATEGORY_START);
        formatted.append("I\">\n");
        formatted.append(getRevisionXml(BJCP_2021));
        formatted.append(NAME_START);
        formatted.append(INTRO);
        formatted.append(NAME_END);
        skipNextLine = true;

        return formatted;
    }

    private StringBuilder getIntroSubCategory(String str, String categoryCode, Pattern pattern) {
        final String NOTES_START = "\t\t\t<notes>\n";
        StringBuilder formatted = new StringBuilder();

        formatted.append(SUB_CATEGORY_START);
        formatted.append(categoryCode);
        formatted.append("\">\n\t");
        formatted.append(NAME_START);
        formatted.append(getRegExValue(str, pattern));
        formatted.append(NAME_END);
        formatted.append(NOTES_START);
        skipNextLine = true;

        return formatted;
    }

    private boolean isIntroNotes(String str) {
        return !StringUtils.isEmpty(getRegExValue(str, introNamePattern1)) ||
                !StringUtils.isEmpty(getRegExValue(str, introNamePattern2)) ||
                !StringUtils.isEmpty(getRegExValue(str, categoryPattern)) ||
                !StringUtils.isEmpty(getRegExValue(str, introNotesPattern));
    }

    private StringBuilder formatIntroNotes(String str) {
        StringBuilder formatted = new StringBuilder();

        formatted.append("\t");
        formatted.append(NOTES_END);
        formatted.append("\t");
        formatted.append(NOTES);

        if (!StringUtils.isEmpty(getRegExValue(str, introNamePattern1))) {
            formatted.append("\t\t\t\t<b>");
            formatted.append(getRegExValue(str, introNamePattern1));
            formatted.append("</b>\n");
        } else if (!StringUtils.isEmpty(getRegExValue(str, introNamePattern2))) {
            formatted.append("\t\t\t\t<b>");
            formatted.append(getRegExValue(str, introNamePattern2));
            formatted.append("</b>\n");
        } else if (!StringUtils.isEmpty(getRegExValue(str, categoryPattern))) {
            formatted.append("\t\t\t\t<b>");
            formatted.append(getRegExValue(str, categoryPattern));
            formatted.append("</b>\n");
        } else if (!StringUtils.isEmpty(getRegExValue(str, introNotesPattern))) {
            formatted.append("\t\t\t\t<b>");
            formatted.append(getRegExValue(str, introNotesPattern));
            formatted.append("</b>\n");
        }

        formatted.append("<br/>\n\t");
        skipNextLine = true;

        return formatted;
    }

    //= {{anchor|Toc91058108}} 2. International Lager =
    private boolean isStartCategory(String str) {
        final Pattern[] patterns = { Pattern.compile("=\\s\\{\\{.*\\}\\}\\s(([A-Z]+|[0-9_]+)\\.\\s.*?)?\\s=") };
        boolean isStart = false;

        if (!StringUtils.isEmpty(getRegExValue(str, patterns))) {
            isStart = true;
        }

        return isStart;
    }

    //= {{anchor|Toc91058103}} 1. Standard American Beer =
    //== {{anchor|Toc418087897}} {{anchor|Toc91058273}} 2. Styles Sorted Using 2008 Guidelines (Modified) =
    private StringBuilder getCategory(String str) {
        final Pattern categoryCode = Pattern.compile("\\{\\{.*\\}\\}\\s(([A-Z]+|[0-9_]+))?\\.\\s(.*?)\\s=");
        final Pattern categoryName = Pattern.compile("\\{\\{.*\\}\\}\\s[A-Z0-9]+\\.\\s(.*?)\\s=");
        StringBuilder formatted = new StringBuilder();

        if (introEnd) {
            formatted.append(BODY_END);
        }

        formatted.append(formattedStats);
        formatted.append(SUB_CATEGORY_END);
        formatted.append(CATEGORY_END);
        formatted.append(CATEGORY_START);
        formatted.append(getRegExValue(str, categoryCode));
        formatted.append("\">\n");
        formatted.append(getRevisionXml(BJCP_2021));
        formatted.append(NAME_START);
        formatted.append(getRegExValue(str, categoryName));
        formatted.append(NAME_END);
        formatted.append(NOTES);
        skipNextLine = true;

        return formatted;
    }

    //''{{anchor|Toc418087738}} {{anchor|Toc91058104}} '''1A. American Light Lager'''
    //{{anchor|Toc180319633}} {{anchor|Toc91058105}} {{anchor|Toc418087739}} '''1B. American Lager'''
    //== {{anchor|Toc91058106}} 1C. Cream Ale ==
    //== X1. Dorada Pampeana ==
    private boolean isStartSubCategory(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''(.*?)'''"),
                Pattern.compile("\\{\\{.*\\}\\}\\s(.*?)\\s=+"),
                Pattern.compile("==\\s[A-Za-z0-9-]+\\.\\s(.*)\\s==")};
        boolean isStart = false;

        if (!StringUtils.isEmpty(getRegExValue(str, patterns))) {
            isStart = true;
        }

        return isStart;
    }

    private StringBuilder getSubCategory(String str) {
        StringBuilder formatted = new StringBuilder();

        if (categoryNotesEnd) {
            formatted.append("\t");
            formatted.append(NOTES_END);
        } else {
            formatted.append(BODY_END);
            formatted.append(formattedStats);
            formatted.append(SUB_CATEGORY_END);
        }

        formatted.append(SUB_CATEGORY_START);
        formatted.append(getSubCategoryCode(str));
        formatted.append("\">\n\t");
        formatted.append(NAME_START);
        formatted.append(getSubCategoryName(str));
        formatted.append(NAME_END);
        formatted.append(BODY_START);
        skipNextLine = true;
        categoryNotesEnd = false;

        return formatted;
    }

    //''{{anchor|Toc418087738}} {{anchor|Toc91058104}} '''1A. American Light Lager'''
    //{{anchor|Toc180319633}} {{anchor|Toc91058105}} {{anchor|Toc418087739}} '''1B. American Lager'''
    //== {{anchor|Toc91058106}} 1C. Cream Ale ==
    private String getSubCategoryCode(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''([a-zA-Z0-9_]*)?\\.\\s"),
                Pattern.compile("\\{\\{.*\\}\\}\\s([a-zA-Z0-9-]*)?\\.\\s"),
                Pattern.compile("==\\s([A-Za-z0-9-]+)?\\.\\s") };

        return getRegExValue(str, patterns);
    }

    //''{{anchor|Toc418087738}} {{anchor|Toc91058104}} '''1A. American Light Lager'''
    //{{anchor|Toc180319633}} {{anchor|Toc91058105}} {{anchor|Toc418087739}} '''1B. American Lager'''
    //== {{anchor|Toc91058106}} 1C. Cream Ale ==
    private String getSubCategoryName(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''[a-zA-Z0-9_]*\\.\\s(.*?)?'''"),
                Pattern.compile("\\{\\{.*\\}\\}\\s[a-zA-Z0-9-]*\\.\\s(.*?)?\\s=+"),
                Pattern.compile("==\\s[A-Za-z0-9-]+\\.\\s(.*)\\s==") };

        return getRegExValue(str, patterns);
    }

    private StringBuilder formatNormalLine(String str) {
        String s = formatHeader(str);
        StringBuilder formatted = new StringBuilder();

        if (StringUtils.isEmpty(s)) {
            formatted.append(BREAK);
            formatted.append("\t\t\t\t");
            formatted.append(BREAK);
        } else {
            formatted.append("\t\t\t\t");
            s = getBoldWords(s);
            formatted.append(s.replace("''", ""));
            formatted.append(BREAK);
        }

        return formatted;
    }

    //'''Overall Impression:''' A highly carbonated
    private String formatHeader(String str) {
        String fullHeader = getFullHeader(str);
        String header = getHeaderName(str);

        if (!StringUtils.isEmpty(header)) {
            StringBuilder formattedHeader = new StringBuilder();
            formattedHeader.append("<big><b>");
            formattedHeader.append(header);
            formattedHeader.append("</b></big><br/>\n\n");

            str = str.replace(fullHeader, formattedHeader.toString());
        }

        return str;
    }

    private String getFullHeader(String str) {
        final Pattern[] patterns = { Pattern.compile("(\\'\\'\\'.*:\\s*?\\'\\'\\')?") };

        return getRegExValue(str, patterns);
    }

    private String getHeaderName(String str) {
        final Pattern[] patterns = { Pattern.compile("\\'\\'\\'(.*?)?:\\s*?\\'\\'\\'") };

        return getRegExValue(str, patterns);
    }
    private String getBoldWords(String str) {
        final Pattern pattern = Pattern.compile("\\'\\'\\'(.*?)?\\'\\'\\'");
        Matcher matcher = pattern.matcher(str);
        String regEx;

        while (matcher.find()) {
            regEx = matcher.group(0);
            str = str.replace(regEx, "<b>" + regEx + "</b>");
        }

        str = str.replace("'''", "");

        return str;
    }

    private boolean isStatistic(String str) {
        return (str.contains(OG) || str.contains(FG) || str.contains(SRM) || str.contains(ABV) || str.contains(IBU));
    }

    private boolean isStatsException(String str) {
        return !StringUtils.isEmpty(getRegExValue(str, statsExceptionPattern));
    }

    private StringBuilder formatStats(String str) {
        StringBuilder formatted = new StringBuilder();

        if (str.contains(OG)) {
            formatted.append(getStats(str, OG, XML_OG));
        } else if (str.contains(FG)) {
            formatted.append(getStats(str, FG, XML_FG));
        } else if (str.contains(SRM)) {
            formatted.append(getStats(str, SRM, XML_SRM));
        } else if (str.contains(ABV)) {
            formatted.append(getStats(str, ABV, XML_ABV));
        } else if (str.contains(IBU)) {
            formatted.append(getStats(str, IBU, XML_IBU));
        }

        return formatted;
    }

    //    '''Vital Statistics:'''
    //    OG:1.048 – 1.055
    //    IBUs:18 – 30
    //    FG:1.010 – 1.014
    //    SRM:9 – 15
    //    ABV:4.7 – 5.5%
    // '''Vital Statistics:''' Variable by type, see individual styles
    private StringBuilder getStats(String str, String pattern, String target) {
        final Pattern lowPattern = Pattern.compile(pattern + "(\\s?\\d+.?\\d*)?%?\\s–");
        final Pattern highPattern = Pattern.compile(pattern + "\\s?\\d+.?\\d*%?\\s–\\s(\\d*\\.*\\d*)\\s?%?'?");
        final Pattern headerPattern = Pattern.compile("''(.*?)''");
        StringBuilder formatted = new StringBuilder();

        formatted.append("\t\t\t");
        formatted.append(getStartTag(XML_STATS));
        formatted.append("\n\t\t\t\t");
        formatted.append(getStartTag(XML_TYPE));
        formatted.append(target);
        formatted.append(getEndTag(XML_TYPE));
        formatted.append("\n\t\t\t\t");
        formatted.append(getStartTag(XML_HEADER));

        if (!XML_EXCEPTIONS.equals(pattern)) {
            formatted.append(getRegExValue(str, headerPattern));
        }

        formatted.append(getEndTag(XML_HEADER));
        formatted.append("\n\t\t\t\t");
        formatted.append(getStartTag(XML_NOTES));
        formatted.append(getRegExValue(str, statsExceptionPattern));
        formatted.append(getEndTag(XML_NOTES));
        formatted.append("\n\t\t\t\t");
        formatted.append(getStartTag(XML_LOW));
        formatted.append(getRegExValue(str,lowPattern));
        formatted.append(getEndTag(XML_LOW));
        formatted.append("\n\t\t\t\t");
        formatted.append(getStartTag(XML_HIGH));
        formatted.append(getRegExValue(str,highPattern));
        formatted.append(getEndTag(XML_HIGH));
        formatted.append("\n\t\t\t");
        formatted.append(getEndTag(XML_STATS));
        formatted.append("\n");

        return formatted;
    }

    protected String postCleanUp(StringBuilder out) {
        String cleaned = out.toString();

        cleaned = cleaned.replace("<br/>\n<br/>\n\t\t\t</body>\n\t\t\t<stats>", "\n\t\t\t</body>\n\t\t\t<stats>");
        cleaned = cleaned.replace("<br />", "<br/>");
        cleaned = cleaned.replaceAll("(?:<br/>\\s*){3,}", "<br/>\n<br/>\n");
        cleaned = cleaned.replaceAll("<br/>\\s<br/>\\s</body>", "</body>");
        cleaned = cleaned.replace("</subcategory>\n\t\t<subcategory id=\"17B\">","</subcategory><subcategory id=\"17A-burton\"> <name>British Strong Ale: Burton Ale</name> <body> <big> <b>Impression</b> </big> <br/>A rich, malty, sweet, and bitter dark ale of moderately strong alcohol. Full bodied and chewy with a balanced hoppy finish and complex malty and hoppy aroma. Fruity notes accentuate the malt richness, while the hops help balance the sweeter finish. <br/> <br/> <big> <b>Aroma</b> </big> <br/>Moderately strong, rich, and sweet malty aroma with deep toast or dark caramel notes. No roasty or burnt malt apparent, but a bready and biscuity base is common. Dark or dried fruit (plums, figs, prunes, raisins) often present at up to a moderate level. A light alcohol presence may be noted, but should not be sharp. Hops can be light to moderate, and reflective of fruity, floral, woody, or spicy English varieties. The malt makes the strongest impression in the balance, but the other aspects add an aromatic complexity. <br/> <br/> <big> <b>Appearance</b> </big> <br/>Light copper to dark brown in color. Darker versions can be nearly opaque, but clarity should be good when noted. Moderate-sized, fine-textured, cream-colored head, persistent. <br/> <br/> <big> <b>Flavor</b> </big> <br/>Similar to the aroma, the malt is initially noted with a rich character and a somewhat sweet finish. The bitterness level is medium-high to high and helps balance the strong malt flavor. The malt flavors have a bready and biscuity character with substantial deep toast or dark caramel flavors; overly roasted and burnt flavors are inappropriate. Hop flavor can be medium to low, with a fruity, floral, spicy, or woody English quality. Dark or dried fruit flavors (plum, prune, fig, or raisin) are often present at up to a moderate level. A light alcohol flavor might be detected, but the sweetness in the finish usually masks it. The sweetness should be balanced by hops and never be cloying or clashing. <br/> <br/> <big> <b>Mouthfeel</b> </big> <br/>Medium-full to full body with a smooth, rich, luscious character. Warming alcohol should be noticeable in stronger versions. Moderate carbonation, lower when served on hand pump. <br/> <br/> <big> <b>Comments</b> </big> <br/>The beer has a long and storied history and many versions existed over time. The style represented her mostly represents the beer at its peak before WWI, although the parameters allow for later era lower-gravity versions as well. A keeping ale, the beer was typically aged before consuming. <br/> <br/> <big> <b>History</b> </big> <br/>Popular in Burton before IPAs were invented, widely exported to the Baltic countries. After 1822, reformulated to be less sweet and strong. Most popular in the Victorian Era, with several different strengths available in the family. The strongest versions evolved into English Barleywines. Became less popular after WWII, eventually dying out around 1970. Some versions exist as Winter Warmers, Barleywines, or Old Ales, but the name has lost favor in the market. <br/> <br/> <big> <b>Ingredients</b> </big> <br/>Bready and biscuity English base malts. Substantial portion of 'high kilned' malt. Historical versions often used brewing sugars and corn. More modern versions can use crystal malts for flavor and chocolate malt for color. English ale yeast. Traditional English hops, often dry hopped. <br/> <br/> <big> <b>Comparison</b> </big> <br/>Has some similarity in malt flavor to Wee Heavy, but with substantially more bitterness. Less strong than an English Barleywine. <br/> <br/> <big> <b>Examples</b> </big> <br/>The Laboratory Gone for a Burton <br/> <br/> <big> <b>Tags</b> </big> <br/> high-strength, brown-color, top-fermented, british-isles, traditional-style, strong-ale-family, malty, balanced </body> <stats> <type>og</type> <header></header> <notes></notes> <low>1.055</low> <high>1.075</high> </stats> <stats> <type>fg</type> <header></header> <notes></notes> <low>1.018</low> <high>1.024</high> </stats> <stats> <type>ibu</type> <header></header> <notes></notes> <low>40</low> <high>50</high> </stats> <stats> <type>srm</type> <header></header> <notes></notes> <low>14</low> <high>22</high> </stats> <stats> <type>abv</type> <header></header> <notes></notes> <low>5.0</low> <high>7.5</high> </stats> </subcategory><subcategory id=\"17B\">");   // Manually add Provisional Style 17A Burton Ale

        return cleaned;
    }

    protected StringBuilder getFooterXml() {
        StringBuilder formatted = new StringBuilder();
        formatted.append(BODY_END);
        formatted.append(formattedStats);
        formatted.append(SUB_CATEGORY_END);
        formatted.append(CATEGORY_END);
        formatted.append(GUIDE_END);

        return  formatted;
    }
}