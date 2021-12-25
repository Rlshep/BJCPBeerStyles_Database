package io.github.rlshep.bjcp2015beerstyles.converters;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.BA_2021;

public class BrewersAssociationTextToXML {

    private static final String INPUT_FILE_NAME = "./db/txt_to_xml/2021_BA_Beer_Style_Guidelines_Final.txt";
    private static final String OUTPUT_FILE_NAME = "./db/txt_to_xml/2021_BA_Beer_Style_Guidelines_Final.xml";

    private static final String CATEGORY_START = "<category>";
    private static final String CATEGORY_END = "</category>";
    private static final String REVISION = "<revision number=\"1\">" + BA_2021 + "</revision>";
    private static final String NAME_START = "<name>";
    private static final String NAME_END = "</name>";

    private static final String[] IGNORED_LINES = {"LAGER STYLES", "HYBRID/MIXED LAGERS OR ALE"};
    private static final List ignoredLines = Arrays.asList(IGNORED_LINES);

    private static final String[] CATEGORIES = {"BRITISH ORIGIN ALE STYLES", "NORTH AMERICAN ORIGIN ALE STYLES", "BELGIAN AND FRENCH ORIGIN ALE STYLES", "IRISH ORIGIN ALE STYLES", "GERMAN ORIGIN ALE STYLES", "OTHER ORIGIN ALE STYLES", "EUROPEAN ORIGIN LAGER STYLES", "NORTH AMERICAN ORIGIN LAGER STYLES", "OTHER ORIGIN LAGER STYLES", "ALL ORIGIN HYBRID/MIXED LAGERS OR ALE"};
    private static final List categories = Arrays.asList(CATEGORIES);

    private static final String[] SUB_CATEGORIES = {"Ordinary Bitter", "Special Bitter or Best Bitter", "Extra Special Bitter", "Scottish-Style Light Ale", "Scottish-Style Heavy Ale", "Scottish-Style Export Ale", "English-Style Summer Ale", "Classic English-Style Pale Ale", "English-Style India Pale Ale", "Strong Ale", "Old Ale", "English-Style Pale Mild Ale", "English-Style Dark Mild Ale", "English-Style Brown Ale", "Brown Porter", "Robust Porter", "Sweet Stout or Cream Stout", "Oatmeal Stout", "Scotch Ale or Wee Heavy", "British-Style Imperial Stout", "British-Style Barley Wine Ale", "Irish-Style Red Ale", "Classic Irish-Style Dry Stout", "Export-Style Stout", "Golden or Blonde Ale", "Session India Pale Ale", "American-Style Amber/Red Ale", "American-Style Pale Ale", "Juicy or Hazy Pale Ale", "American-Style Strong Pale Ale", "Juicy or Hazy Strong Pale Ale", "American-Style India Pale Ale", "Juicy or Hazy India Pale Ale", "American-Belgo-Style Ale", "American-Style Brown Ale", "American-Style Black Ale", "American-Style Stout", "American-Style Imperial Porter", "American-Style Imperial Stout", "Double Hoppy Red Ale", "Imperial Red Ale", "American-Style Imperial or Double India Pale Ale", "Juicy or Hazy Imperial or Double India Pale Ale", "American-Style Barley Wine Ale", "American-Style Wheat Wine Ale", "Smoke Porter", "American-Style Sour Ale", "American-Style Fruited Sour Ale", "German-Style Koelsch", "German-Style Altbier", "Berliner-Style Weisse", "Leipzig-Style Gose", "Contemporary-Style Gose", "South German-Style Hefeweizen", "South German-Style Kristal Weizen", "German-Style Leichtes Weizen", "South German-Style Bernsteinfarbenes Weizen", "South German-Style Dunkel Weizen", "South German-Style Weizenbock", "German-Style Rye Ale", "Bamberg-Style Weiss Rauchbier", "Belgian-Style Table Beer", "Belgian-Style Session Ale", "Belgian-Style Speciale Belge", "Belgian-Style Blonde Ale", "Belgian-Style Strong Blonde Ale", "Belgian-Style Strong Dark Ale", "Belgian-Style Dubbel", "Belgian-Style Tripel", "Belgian-Style Quadrupel", "Belgian-Style Witbier", "Classic French & Belgian-Style Saison", "Specialty Saison", "French-Style Bière de Garde", "Belgian-Style Flanders Oud Bruin or Oud Red Ale", "Belgian-Style Lambic", "Traditional Belgian-Style Gueuze", "Contemporary Belgian-Style Spontaneous Fermented Ale", "Belgian-Style Fruit Lambic", "Other Belgian-Style Ale", "Grodziskie", "Adambier", "Dutch-Style Kuit, Kuyt or Koyt", "International-Style Pale Ale", "Classic Australian-Style Pale Ale", "Australian-Style Pale Ale", "New Zealand-Style Pale Ale", "New Zealand-Style India Pale Ale", "Finnish-Style Sahti", "Swedish-Style Gotlandsdricke", "Breslau-Style Schoeps", "Lager Styles", "German-Style Leichtbier", "German-Style Pilsener", "Bohemian-Style Pilsener", "Munich-Style Helles", "Dortmunder/European-Style Export", "Vienna-Style Lager", "Franconian-Style Rotbier", "German-Style Maerzen", "German-Style Oktoberfest/Wiesn", "Munich-Style Dunkel", "European-Style Dark Lager", "German-Style Schwarzbier", "Bamberg-Style Helles Rauchbier", "Bamberg-Style Maerzen Rauchbier", "Bamberg-Style Bock Rauchbier", "German-Style Heller Bock/Maibock", "Traditional German-Style Bock", "German-Style Doppelbock", "German-Style Eisbock", "American-Style Lager", "Contemporary American-Style Lager", "American-Style Light Lager", "Contemporary American-Style Light Lager", "American-Style Pilsener", "Contemporary American-Style Pilsener", "American-Style India Pale Lager", "American-Style Malt Liquor", "American-Style Amber Lager", "American-Style Maerzen/Oktoberfest", "American-Style Dark Lager", "Australasian, Latin American or Tropical-Style Light Lager", "International-Style Pilsener", "Baltic-Style Porter", "Hybrid/Mixed Lagers or Ale", "Session Beer", "American-Style Cream Ale", "California Common Beer", "Kentucky Common Beer", "American-Style Wheat Beer", "Kellerbier or Zwickelbier", "American-Style Fruit Beer", "Fruit Wheat Beer", "Belgian-Style Fruit Beer", "Field Beer", "Pumpkin Spice Beer", "Pumpkin/Squash Beer", "Chocolate or Cocoa Beer", "Coffee Beer", "Chili Pepper Beer", "Herb and Spice Beer", "Specialty Beer", "Specialty Honey Beer", "Rye Beer", "Brett Beer", "Mixed-Culture Brett Beer", "Ginjo Beer or Sake-Yeast Beer", "Fresh Hop Beer", "Wood- and Barrel-Aged Beer", "Wood- and Barrel-Aged Sour Beer", "Aged Beer", "Experimental Beer", "Experimental India Pale Ale", "Historical Beer", "Wild Beer", "Smoke Beer", "Other Strong Ale or Lager", "Gluten-Free Beer", "Non-Alcohol Malt Beverage"};
    private static final List subCategories = Arrays.asList(SUB_CATEGORIES);

    private static final String[] BOLD_WORDS = {"Color:", "Clarity:", "Body:"};
    private final List boldWords = Arrays.asList(BOLD_WORDS);

    private boolean skipNextLine = false;

    public void convert() {
        try {
            FileReader input = new FileReader(INPUT_FILE_NAME);
            FileWriter output = new FileWriter(OUTPUT_FILE_NAME);
            BufferedReader br = new BufferedReader(input);
            String str;
            StringBuilder out = new StringBuilder();

            while ((str = br.readLine()) != null) {
                if (skipNextLine) {
                    skipNextLine = false;
                }
                else {
                    out.append(formatLine(str));
                }
            }

            output.write(cleanUp(out));

            br.close();
            input.close();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder formatLine(String str) {
        final String NOTES_END = "</notes>\n";
        StringBuilder formattedLine = new StringBuilder();

        if (ignoredLines.contains(str.trim())) {
            // Ignore
        } else if (str.startsWith("Compiled by the Brewers Association")) {
            formattedLine.append(getHeader());
            formattedLine.append(getIntroduction());
            formattedLine.append(str);
        } else if (str.startsWith("ALE STYLES")) {
            formattedLine.append(NOTES_END);
        } else if (str.equals("")) {
            formattedLine.append("<br>\n");
        } else if (categories.contains(str.trim())) {
            formattedLine.append(formatCategory(str));
        } else if (subCategories.contains(str.trim())) {
            formattedLine.append(formatSubCategory(str));
        } else if (isInList(boldWords, str)) {
            formattedLine.append(formatBoldWords(str));
        } else if (str.startsWith("Original Gravity (°Plato)")) {
            formattedLine.append(formatStats(str));
        } else {
            formattedLine.append("\t\t\t\t" + str.trim() + "<br>\n");
        }

        return formattedLine;
    }

    private String getHeader() {
        return "<styleguide revision=\"" + BA_2021 + " language=\"en\">\n";
    }

    private StringBuilder getIntroduction() {
        final String INTRO = "Introduction";
        final String NOTES = "<notes title=\"\">\n";
        StringBuilder formatted = new StringBuilder();

        formatted.append("\t");
        formatted.append(CATEGORY_START);
        formatted.append("\n\t");
        formatted.append(REVISION);
        formatted.append("\n\t");
        formatted.append(NAME_START);
        formatted.append(INTRO);
        formatted.append(NAME_END);
        formatted.append("\n\t");
        formatted.append(NOTES);
        formatted.append("\n");

        return formatted;
    }

    private StringBuilder formatCategory(String str) {
        StringBuilder formatted = new StringBuilder();

        formatted.append("\n\t");
        formatted.append(CATEGORY_END);
        formatted.append("\n\t");
        formatted.append(CATEGORY_START);
        formatted.append("\n\t\t");
        formatted.append(REVISION);
        formatted.append("\n\t\t");
        formatted.append(NAME_START);
        formatted.append(str);
        formatted.append(NAME_END);
        formatted.append("\n");

        skipNextLine = true;

        return formatted;
    }

    private StringBuilder formatSubCategory(String str) {
        StringBuilder formatted = new StringBuilder();
        final String SUB_CATEGORY_START = "<subcategory>";
        final String BODY_START = "<body>\n";

        formatted.append("\t\t");
        formatted.append(SUB_CATEGORY_START);
        formatted.append("\n\t\t\t");
        formatted.append(NAME_START);
        formatted.append(str);
        formatted.append(NAME_END);
        formatted.append("\n\t\t\t");
        formatted.append(BODY_START);

        skipNextLine = true;

        return formatted;
    }

    private String formatBoldWords(String str) {
        final String BIG_START = "<big><b>";
        final String BIG_END = "</b></big>";

        String word = getWordFromList(boldWords, str);
        str = str.replace(word, BIG_START + word + BIG_END);
        str = "\t\t\t\t" + str + "<br>\n";

        return str;
    }

    private StringBuilder formatStats(String str) {
        StringBuilder formatted = new StringBuilder();
        final String SUB_CATEGORY_END = "</subcategory>";
        final String BODY_END = "\t\t</body>\n";
        final String STATS_START = "\t\t<stats>\n\t\t\t<og flexible=\"false\">\n\t\t\t\t\t<low>";
        final String STATS_END = "\t\t</stats>\n";

        formatted.append(BODY_END);
        formatted.append(STATS_START);
        formatted.append("\n\t");
        formatted.append(str);
        formatted.append("\n\t");
        formatted.append(STATS_END);

        formatted.append("\n\t");
        formatted.append(SUB_CATEGORY_END);

        return formatted;
    }

    //Utilities
    private boolean isInList(List<String> list, String str) {
        return !StringUtils.isEmpty(getWordFromList(list, str));
    }

    private String getWordFromList(List<String> list, String str) {
        String word = "";

        for (String item : list) {
            if (str.toUpperCase().contains(item.toUpperCase())) {
                word = item;
            }
        }

        return word;
    }

    private String cleanUp(StringBuilder out) {
        String cleaned = out.toString();

        cleaned = cleaned.replace("<br>\n<br>\n<br>\n<br>\n", "\n<br>\n<br>\n");

        return cleaned;
    }
}
