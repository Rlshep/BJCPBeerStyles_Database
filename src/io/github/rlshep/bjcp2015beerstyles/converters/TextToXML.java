package io.github.rlshep.bjcp2015beerstyles.converters;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TextToXML {
    protected static final String CATEGORY_END = "\t</category>\n";
    protected static final String NAME_START = "\t\t<name>";
    protected static final String NAME_END = "</name>\n";
    protected static final String NOTES_END = "\t\t</notes>\n";
    protected static final String BREAK = "<br />\n";
    protected static final String SUB_CATEGORY_START = "\t\t<subcategory id=\"";
    protected static final String SUB_CATEGORY_END = "\t\t</subcategory>\n";
    protected static final String NOTES = "\t\t<notes title=\"\">\n";
    protected static final String NOTES_TITLE = "\t\t<notes title=\"";
    protected static final String BODY_START = "\t\t\t<body>\n";
    protected static final String BODY_END = "\t\t\t</body>\n";

    protected BufferedReader br;
    protected boolean skipNextLine = false;

    protected abstract String preCleanUp(StringBuilder out);
    protected abstract String preLineCleanUp(String str);
    protected abstract StringBuilder formatLine(String str) throws IOException;
    protected abstract String postCleanUp(StringBuilder out);

    public void cleanUpTextFile(String i, String o) throws IOException {
        FileReader input = new FileReader(i);
        FileWriter output = new FileWriter(o);
        BufferedReader br = new BufferedReader(input);
        String str;
        StringBuilder out = new StringBuilder();

        while ((str = br.readLine()) != null) {
            out.append(preLineCleanUp(str) + "\n");
        }

        output.write(preCleanUp(out));

        br.close();
        input.close();
        output.close();
    }

    public void convertToXml(String i, String o) throws IOException {
        FileReader input = new FileReader(i);
        FileWriter output = new FileWriter(o);
        br = new BufferedReader(input);
        String str;
        StringBuilder out = new StringBuilder();

        while ((str = br.readLine()) != null) {
            if (skipNextLine) {
                skipNextLine = false;
            } else {
                out.append(formatLine(str));
            }
        }

        out.append(getFooterXml());
        output.write(postCleanUp(out));

        br.close();
        input.close();
        output.close();
    }

    protected String getHeaderXml(String revision) {

        return "<styleguide revision=\"" + revision + "\" language=\"en\">\n";
    }

    protected String getRevisionXml(String revision) {
        return "\t\t<revision number=\"1\">" + revision + "</revision>\n";
    }

    private String getFooterXml() {

        return CATEGORY_END + "</styleguide>\n";
    }

    protected String getRegExValue(String str, Pattern pattern) {
        String regEx = "";
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            regEx = matcher.group(1);
        }

        return regEx;
    }

    protected String getAllRegExValue(String str, Pattern pattern) {
        String regEx = "";
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            regEx = matcher.group(0);
        }

        return regEx;
    }

    protected String getRegExValue(String str, Pattern[] patterns) {
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

    protected String getTitleCase(String str) {
        StringBuilder title = new StringBuilder();
        String[] words = str.split(" ");

        for (String word : words) {
            title.append(StringUtils.capitalize(word.toLowerCase()));
            title.append(" ");
        }

        return title.toString();
    }

    protected String getStartTag(String name) {
        return "<" + name + ">";
    }

    protected String getEndTag(String name) {
        return "</" + name + ">";
    }

}
