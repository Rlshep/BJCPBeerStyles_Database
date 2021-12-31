package io.github.rlshep.bjcp2015beerstyles.converters;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.BJCP_2021;

public class BJCPTextToXML extends TextToXML {
    private static final String INPUT_FILE_NAME = "./db/txt_to_xml/2021_BJCP_Guidelines_Beer.txt";    //Exported from LibreWriter to wiki text
    private static final String CLEANED_FILE = "./db/txt_to_xml/2021_BJCP_Guidelines_Beer_Cleaned.txt";
    private static final String OUTPUT_FILE_NAME = "./db/2021_BJCP_Guidelines_Beer.xml";
    private static final String CATEGORY_START = "\t<category id=\"";
    private static final String INTRO1 = "= {{anchor|Toc418087720}} {{anchor|Toc91058084}} Introduction to the 2021 Guidelines =";
    private static final String INTRO2 = "= {{anchor|Toc91058090}} Introduction to Beer Styles =";
    private static final String START_STYLES = "= {{anchor|Toc91058103}} 1. Standard American Beer =";

    //= {{anchor|Toc418087720}} {{anchor|Toc91058084}} Introduction to the 2021 Guidelines =
    private static final Pattern introNamePattern = Pattern.compile("\\{\\{.*\\}\\}\\s\\{\\{.*\\}\\}\\s(.*?)\\s=");

    //= {{anchor|Toc91058103}} 1. Standard American Beer =
    private static final Pattern categoryPattern = Pattern.compile("\\{\\{.*\\}\\}\\s(.*?)\\s=");

    private boolean introFound = false;
    private boolean introEnd = false;

    public static void main(String args[]) {
        BJCPTextToXML converter = new BJCPTextToXML();

        converter.convert();
    }

    public void convert() {
        try {
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

        return out.toString();
    }

    protected String preCleanUp(StringBuilder str) {
        String s = str.toString();
        StringBuilder out = new StringBuilder(s);

        return out.toString();
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
            formatted.append(getIntroSubCategory(str, "I1", introNamePattern));
        } else if (str.contains(INTRO2)) {
            formatted.append(NOTES_END);
            formatted.append(SUB_CATEGORY_END);
            formatted.append(getIntroSubCategory(str, "I2", categoryPattern));
        } else if (!introEnd && !StringUtils.isEmpty(getRegExValue(str, introNamePattern))) {
            formatted.append(formatIntroNotes(str));
        } else if (str.contains(START_STYLES)) {
            formatted.append(NOTES_END);
            formatted.append(getCategory(str));
            introEnd = true;
        } else if (introEnd && isStartCategory(str)) {
            formatted.append(getCategory(str));
        } else if (introEnd && isStartSubCategory(str)) {
            formatted.append(getSubCategory(str));
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

    private StringBuilder formatIntroNotes(String str) {
        StringBuilder formatted = new StringBuilder();

        formatted.append("\t");
        formatted.append(NOTES_END);
        formatted.append("\t");
        formatted.append(NOTES_TITLE);
        formatted.append(getRegExValue(str, introNamePattern));
        formatted.append("\">\n\t");
        skipNextLine = true;

        return formatted;
    }

    //= {{anchor|Toc91058108}} 2. International Lager =
    private boolean isStartCategory(String str) {
        final Pattern[] patterns = { Pattern.compile("=\\s\\{\\{.*\\}\\}\\s(\\d+\\.\\s.*?)?\\s=") };
        boolean isStart = false;

        for (int i = 0; i < patterns.length; i++) {
            if (!StringUtils.isEmpty(getRegExValue(str, patterns[i]))) {
                isStart = true;
            }
        }

        return isStart;
    }

    //= {{anchor|Toc91058103}} 1. Standard American Beer =
    private StringBuilder getCategory(String str) {
        final Pattern categoryCode = Pattern.compile("\\{\\{.*\\}\\}\\s(\\d+)?\\.\\s(.*?)\\s=");
        final Pattern categoryName = Pattern.compile("\\{\\{.*\\}\\}\\s\\d+\\.\\s(.*?)?\\s=");
        StringBuilder formatted = new StringBuilder();


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
    private boolean isStartSubCategory(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''(.*?)'''"), Pattern.compile("\\{\\{.*\\}\\}\\s(.*?)\\s==}") };
        boolean isStart = false;

        for (int i = 0; i < patterns.length; i++) {
            if (!StringUtils.isEmpty(getRegExValue(str, patterns[i]))) {
                isStart = true;
            }
        }

        return isStart;
    }

    private StringBuilder getSubCategory(String str) {
        StringBuilder formatted = new StringBuilder();

        formatted.append(BODY_END);
        formatted.append(SUB_CATEGORY_END);
        formatted.append(SUB_CATEGORY_START);
        formatted.append(getSubCategoryCode(str));
        formatted.append("\">\n\t");
        formatted.append(NAME_START);
        formatted.append(getSubCategoryName(str));
        formatted.append(NAME_END);
        formatted.append(BODY_START);
        skipNextLine = true;

        return formatted;
    }

    //''{{anchor|Toc418087738}} {{anchor|Toc91058104}} '''1A. American Light Lager'''
    //{{anchor|Toc180319633}} {{anchor|Toc91058105}} {{anchor|Toc418087739}} '''1B. American Lager'''
    //== {{anchor|Toc91058106}} 1C. Cream Ale ==
    private String getSubCategoryCode(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''([a-zA-Z0-9_]*)?\\.\\s"), Pattern.compile("\\{\\{.*\\}\\}\\s([a-zA-Z0-9_]*)?\\.\\s") };
        String code = "";


        for (int i = 0; i < patterns.length; i++) {
            String regExValue = getRegExValue(str, patterns[i]);

            if (!StringUtils.isEmpty(regExValue)) {
                code = regExValue;
                break;
            }
        }

        return code;
    }

    //''{{anchor|Toc418087738}} {{anchor|Toc91058104}} '''1A. American Light Lager'''
    //{{anchor|Toc180319633}} {{anchor|Toc91058105}} {{anchor|Toc418087739}} '''1B. American Lager'''
    //== {{anchor|Toc91058106}} 1C. Cream Ale ==
    private String getSubCategoryName(String str) {
        final Pattern[] patterns = { Pattern.compile("\\{\\{.*\\}\\}\\s'''[a-zA-Z0-9_]*\\.\\s(.*?)?'''"), Pattern.compile("\\{\\{.*\\}\\}\\s[a-zA-Z0-9_]*\\.\\s(.*?)?\\s==") };
        String name = "";


        for (int i = 0; i < patterns.length; i++) {
            String regExValue = getRegExValue(str, patterns[i]);

            if (!StringUtils.isEmpty(regExValue)) {
                name = regExValue;
                break;
            }
        }

        return name;
    }

    private StringBuilder formatNormalLine(String str) {
        StringBuilder formatted = new StringBuilder();

        if (StringUtils.isEmpty(str)) {
            formatted.append(BREAK);
            formatted.append("\t\t\t\t");
            formatted.append(BREAK);
        } else {
            formatted.append("\t\t\t\t");
            formatted.append(str.replace("''", ""));
        }

        return formatted;
    }

    protected String postCleanUp(StringBuilder out) {
        String cleaned = out.toString();

        return cleaned;
    }
}
