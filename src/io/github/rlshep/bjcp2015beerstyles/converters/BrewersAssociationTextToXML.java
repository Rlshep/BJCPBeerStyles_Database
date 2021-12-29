package io.github.rlshep.bjcp2015beerstyles.converters;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpConstants.BA_2021;
import static io.github.rlshep.bjcp2015beerstyles.constants.BjcpContract.*;

public class BrewersAssociationTextToXML {

    private static final String INPUT_FILE_NAME = "./db/txt_to_xml/2021_BA_Beer_Style_Guidelines_Final.txt";    //Exported from PDF Reader
    private static final String CLEANED_FILE = "./db/txt_to_xml/2021_BA_Beer_Style_Guidelines_Cleaned.txt";
    private static final String OUTPUT_FILE_NAME = "./db/2021_BA_Beer_Style_Guidelines_Final.xml";

    private static final String CATEGORY_START = "\t<category>\n";
    private static final String CATEGORY_END = "\t</category>\n";
    private static final String REVISION = "\t\t<revision number=\"1\">" + BA_2021 + "</revision>\n";
    private static final String NAME_START = "\t\t<name>";
    private static final String NAME_END = "</name>\n";

    private static final String[] IGNORED_LINES = {"LAGER STYLES", "HYBRID/MIXED LAGERS OR ALE"};
    private static final List<String> ignoredLines = Arrays.asList(IGNORED_LINES);

    private static final String[] CATEGORIES = {"BRITISH ORIGIN ALE STYLES", "NORTH AMERICAN ORIGIN ALE STYLES", "BELGIAN AND FRENCH ORIGIN ALE STYLES", "IRISH ORIGIN ALE STYLES", "GERMAN ORIGIN ALE STYLES", "OTHER ORIGIN ALE STYLES", "EUROPEAN ORIGIN LAGER STYLES", "NORTH AMERICAN ORIGIN LAGER STYLES", "OTHER ORIGIN LAGER STYLES", "ALL ORIGIN HYBRID/MIXED LAGERS OR ALE"};
    private static final List<String> categories = Arrays.asList(CATEGORIES);

    private static final String[] SUB_CATEGORIES = {"Ordinary Bitter", "Special Bitter or Best Bitter", "Extra Special Bitter", "Scottish-Style Light Ale", "Scottish-Style Heavy Ale", "Scottish-Style Export Ale", "English-Style Summer Ale", "Classic English-Style Pale Ale", "English-Style India Pale Ale", "Strong Ale", "Old Ale", "English-Style Pale Mild Ale", "English-Style Dark Mild Ale", "English-Style Brown Ale", "Brown Porter", "Robust Porter", "Sweet Stout or Cream Stout", "Oatmeal Stout", "Scotch Ale or Wee Heavy", "British-Style Imperial Stout", "British-Style Barley Wine Ale", "Irish-Style Red Ale", "Classic Irish-Style Dry Stout", "Export-Style Stout", "Golden or Blonde Ale", "Session India Pale Ale", "American-Style Amber / Red Ale", "American-Style Pale Ale", "Juicy or Hazy Pale Ale", "American-Style Strong Pale Ale", "Juicy or Hazy Strong Pale Ale", "American-Style India Pale Ale", "Juicy or Hazy India Pale Ale", "American-Belgo-Style Ale", "American-Style Brown Ale", "American-Style Black Ale", "American-Style Stout", "American-Style Imperial Porter", "American-Style Imperial Stout", "Double Hoppy Red Ale", "Imperial Red Ale", "American-Style Imperial or Double India Pale Ale", "Juicy or Hazy Imperial or Double India Pale Ale", "American-Style Barley Wine Ale", "American-Style Wheat Wine Ale", "Smoke Porter", "American-Style Sour Ale", "American-Style Fruited Sour Ale", "German-Style Koelsch", "German-Style Altbier", "Berliner-Style Weisse", "Leipzig-Style Gose", "Contemporary-Style Gose", "South German-Style Hefeweizen", "South German-Style Kristal Weizen", "German-Style Leichtes Weizen", "South German-Style Bernsteinfarbenes Weizen", "South German-Style Dunkel Weizen", "South German-Style Weizenbock", "German-Style Rye Ale", "Bamberg-Style Weiss Rauchbier", "Belgian-Style Table Beer", "Belgian-Style Session Ale", "Belgian-Style Speciale Belge", "Belgian-Style Blonde Ale", "Belgian-Style Strong Blonde Ale", "Belgian-Style Strong Dark Ale", "Belgian-Style Dubbel", "Belgian-Style Tripel", "Belgian-Style Quadrupel", "Belgian-Style Witbier", "Classic French &amp; Belgian-Style Saison", "Specialty Saison", "French-Style Bière de Garde", "Belgian-Style Flanders Oud Bruin or Oud Red Ale", "Belgian-Style Lambic", "Traditional Belgian-Style Gueuze", "Contemporary Belgian-Style Spontaneous Fermented Ale", "Belgian-Style Fruit Lambic", "Other Belgian-Style Ale", "Grodziskie", "Adambier", "Dutch-Style Kuit, Kuyt or Koyt", "International-Style Pale Ale", "Classic Australian-Style Pale Ale", "Australian-Style Pale Ale", "New Zealand-Style Pale Ale", "New Zealand-Style India Pale Ale", "Finnish-Style Sahti", "Swedish-Style Gotlandsdricke", "Breslau-Style Schoeps", "Lager Styles", "German-Style Leichtbier", "German-Style Pilsener", "Bohemian-Style Pilsener", "Munich-Style Helles", "Dortmunder / European-Style Export", "Vienna-Style Lager", "Franconian-Style Rotbier", "German-Style Maerzen", "German-Style Oktoberfest / Wiesn", "Munich-Style Dunkel", "European-Style Dark Lager", "German-Style Schwarzbier", "Bamberg-Style Helles Rauchbier", "Bamberg-Style Maerzen Rauchbier", "Bamberg-Style Bock Rauchbier", "German-Style Heller Bock / Maibock", "Traditional German-Style Bock", "German-Style Doppelbock", "German-Style Eisbock", "American-Style Lager", "Contemporary American-Style Lager", "American-Style Light Lager", "Contemporary American-Style Light Lager", "American-Style Pilsener", "Contemporary American-Style Pilsener", "American-Style India Pale Lager", "American-Style Malt Liquor", "American-Style Amber Lager", "American-Style Maerzen / Oktoberfest", "American-Style Dark Lager", "Australasian, Latin American or Tropical-Style Light Lager", "International-Style Pilsener", "Baltic-Style Porter", "Hybrid / Mixed Lagers or Ale", "Session Beer", "American-Style Cream Ale", "California Common Beer", "Kentucky Common Beer", "American-Style Wheat Beer", "Kellerbier or Zwickelbier", "American-Style Fruit Beer", "Fruit Wheat Beer", "Belgian-Style Fruit Beer", "Field Beer", "Pumpkin Spice Beer", "Pumpkin / Squash Beer", "Chocolate or Cocoa Beer", "Coffee Beer", "Chili Pepper Beer", "Herb and Spice Beer", "Specialty Beer", "Specialty Honey Beer", "Rye Beer", "Brett Beer", "Mixed-Culture Brett Beer", "Ginjo Beer or Sake-Yeast Beer", "Fresh Hop Beer", "Wood- and Barrel-Aged Beer", "Wood- and Barrel-Aged Sour Beer", "Aged Beer", "Experimental Beer", "Experimental India Pale Ale", "Historical Beer", "Wild Beer", "Smoke Beer", "Other Strong Ale or Lager", "Gluten-Free Beer", "Non-Alcohol Malt Beverage"};
    private static final List<String> subCategories = Arrays.asList(SUB_CATEGORIES);

    private static final String[] BOLD_WORDS = {"Intensity Level Terminology:", "Color Ranges:", "Bitterness:", "Notes on Beer Competitions: ", "Competition Categories: ", "Notes on Beer Style Guidelines:", "Beer Style Guidelines:", "Pouring:"};
    private final List<String> boldWords = Arrays.asList(BOLD_WORDS);

    private static final String[] HEADER_WORDS = {"Color:", "Clarity:", "Body:", "Additional notes:", "Fermentation Characteristics:", "Perceived Malt Aroma &amp; Flavor:", "Perceived Hop Aroma &amp; Flavor:", "Perceived bitterness:"};
    private final List<String> headerWords = Arrays.asList(HEADER_WORDS);

    private boolean skipNextLine = false;
    private BufferedReader br;

    public void convert() {
        try {
            cleanUpTextFile();
            convertToXml();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanUpTextFile() throws IOException {
        FileReader input = new FileReader(INPUT_FILE_NAME);
        FileWriter output = new FileWriter(CLEANED_FILE);
        BufferedReader br = new BufferedReader(input);
        String str;
        StringBuilder out = new StringBuilder();

        while ((str = br.readLine()) != null) {
            out.append(str + "\n");
        }

        output.write(preCleanUp(out));

        br.close();
        input.close();
        output.close();
    }

    private String preCleanUp(StringBuilder out) {
        String cleaned = out.toString();

        cleaned = cleaned.replace("\n\n\n \n\n", "\n\n");
        cleaned = cleaned.replace("\n\n \n\n", "\n\n");
        cleaned = cleaned.replace("\n\n\n \n\n", "\n\n");
        cleaned = cleaned.replace("\n\n\n\n \n\n", "\n\n");
        cleaned = cleaned.replace("CupSM", "Cup&#8480;");
        cleaned = cleaned.replace("&", "&amp;");
        cleaned = cleaned.replace("American-Style Brown Ale", "\nAmerican-Style Brown Ale");
        cleaned = cleaned.replace("American-Style Imperial or Double \nIndia Pale Ale", "American-Style Imperial or Double India Pale Ale");
        cleaned = cleaned.replace("American-Style Amber/Red Ale", "American-Style Amber / Red Ale");
        cleaned = cleaned.replace("American-Style \nMaerzen/Oktoberfest", "American-Style Maerzen / Oktoberfest");
        cleaned = cleaned.replace("American-Style Wheat Beer", "\nAmerican-Style Wheat Beer");
        cleaned = cleaned.replace("American-Style Fruit Beer", "\nAmerican-Style Fruit Beer");
        cleaned = cleaned.replace("Hybrid/Mixed Lagers or Ale", "Hybrid / Mixed Lagers or Ale");
        cleaned = cleaned.replace("Juicy or Hazy Imperial or Double \nIndia Pale Ale", "Juicy or Hazy Imperial or Double India Pale Ale");
        cleaned = cleaned.replace("South German-Style \nBernsteinfarbenes Weizen", "South German-Style Bernsteinfarbenes Weizen");
        cleaned = cleaned.replace("South German-Style Dunkel \nWeizen", "South German-Style Dunkel Weizen");
        cleaned = cleaned.replace("Classic French &amp; Belgian-Style \nSaison", "Classic French &amp; Belgian-Style Saison");
        cleaned = cleaned.replace("Belgian-Style Flanders Oud Bruin \nor Oud Red Ale", "Belgian-Style Flanders Oud Bruin or Oud Red Ale");
        cleaned = cleaned.replace("Contemporary Belgian-Style \nSpontaneous Fermented Ale", "Contemporary Belgian-Style Spontaneous Fermented Ale");
        cleaned = cleaned.replace("Belgian-Style Fruit Lambic", "\nBelgian-Style Fruit Lambic");
        cleaned = cleaned.replace("New Zealand-Style Pale Ale", "\nNew Zealand-Style Pale Ale");
        cleaned = cleaned.replace("Dortmunder/European-Style \nExport", "Dortmunder / European-Style Export");
        cleaned = cleaned.replace("German-Style Oktoberfest/Wiesn", "German-Style Oktoberfest / Wiesn");
        cleaned = cleaned.replace("German-Style Heller Bock/Maibock", "German-Style Heller Bock / Maibock");
        cleaned = cleaned.replace("German-Style Doppelbock", "\nGerman-Style Doppelbock");
        cleaned = cleaned.replace("Contemporary American-Style \nLager", "Contemporary American-Style Lager");
        cleaned = cleaned.replace("Contemporary American-Style \nLight Lager", "Contemporary American-Style Light Lager");
        cleaned = cleaned.replace("Contemporary American-Style \nPilsener", "Contemporary American-Style Pilsener");
        cleaned = cleaned.replace("Australasian, Latin American or \nTropical-Style Light Lager", "Australasian, Latin American or Tropical-Style Light Lager");
        cleaned = cleaned.replace("Pumpkin/Squash Beer", "Pumpkin / Squash Beer");
        cleaned = cleaned.replace("\nField Beer \n", "\n\nField Beer\n");
        cleaned = cleaned.replace("temperatures. \nPerceived Malt Aroma", "temperatures. \n\nPerceived Malt Aroma");
        cleaned = cleaned.replace("Plato) 1.030-1.140+ ", "Plato) 1.030-1.140 ");
        cleaned = cleaned.replace("Final Gravity (°Plato) \n1.006-1.030+ (1.5-7.6+ °Plato)", "Final Gravity (°Plato) 1.006-1.030 (1.5-7.6 °Plato)");
        cleaned = cleaned.replace("SRM (EBC) 30+ (60+ EBC)", "SRM (EBC) 30-41 (60-100 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 40+ (80+ EBC)", "SRM (EBC) 40-41 (80-100 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 20+ (40+ EBC", "SRM (EBC) 20-41 (40-80 EBC");
        cleaned = cleaned.replace("SRM (EBC) 20-35+ (40-70+ EBC)", "SRM (EBC) 20-41 (40-70 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 35+ (70+ EBC)", "SRM (EBC) 35-41 (70-82 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 5+ (10+ EBC)", "SRM (EBC) 5-41 (10-80 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 2-40+ (4-80+ EBC)", "SRM (EBC) 2-41 (4-80 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 2+ (4+ EBC)", "SRM (EBC) 2-41 (4-80 EBC)");
        cleaned = cleaned.replace("SRM (EBC) 40+ (80+ EBC)", "SRM (EBC) 40-41 (4-80 EBC)");
        cleaned = cleaned.replace("Color SRM (EBC) 1-100 (2-200 EBC)", "Color SRM (EBC) 1-41 (2-200 EBC)");
        cleaned = cleaned.replace("Color SRM (EBC) 5-50 (10-100 EBC)", "Color SRM (EBC) 5-41 (10-100 EBC)");
        cleaned = cleaned.replace("Color SRM \n(EBC) 15-50 (30-100 EBC)", "Color SRM (EBC) 15-41 (30-100 EBC)");
        cleaned = cleaned.replace("Color SRM (EBC) 30+ (60+ EBC)", "Color SRM (EBC) 15-41 (30-100 EBC)");
        cleaned = cleaned.replace("Weight (Volume) >4.0% (>5.0%)", "Weight (Volume) 4.0%-100.0% (5.0%-100.0%)");
        cleaned = cleaned.replace("Weight (Volume) <0.4% abw (<0.5% abv)", "Weight (Volume) 0.0%-0.4% abw (0.0%-0.5% abv)");
        cleaned = cleaned.replace("Alcohol by Weight (Volume) 6.4%+ (8%+)", "Alcohol by Weight (Volume) 6.4%-100.0% (8.0%-100.0%)");
        cleaned = cleaned.replace("(Volume) 2.0%-20+% (2.5%-25+%)", "(Volume) 2.0%-20.0% (2.5%-25.0%)");
        cleaned = cleaned.replace(" <0.5% abv.", " &#60;0.5% abv.");
        cleaned = cleaned.replace("\n\nOriginal Gravity (°Plato)", "\nOriginal Gravity (°Plato)");
        cleaned = cleaned.replace("brewers-\nassociation-beer-style-guidelines", "brewers-association-beer-style-guidelines");
        cleaned = cleaned.replace("* None \n* Very low \n* Low \n* Medium-low \n* Medium \n* Medium-high \n* High \n* Very high \n* Intense", "<ul><li>None</li><li>Very low</li><li>Low</li><li>Medium-low</li><li>Medium</li><li>Medium-high</li><li>High</li><li>Very high</li><li>Intense</li></ul>");
        cleaned = cleaned.replace("Color Description \n\nSRM \n\nVery light \n\n1-1.5 \n\nStraw \n\n2-3 \n\nPale \n\n4 \n\nGold \n\n5-6 \n\nLight amber \n\n7 \n\nAmber \n\n8 \n\nMedium amber \n\n9 \n\nCopper/garnet \n\n10-12 \n\nLight brown \n\n13-15 \n\nBrown/Reddish brown/chestnut brown \n\n16-17 \n\nDark brown \n\n18-24 \n\nVery dark \n\n25-39 \n\nBlack \n\n40+ ","<b>Color Description: </b> <b>SRM </b><br />\nVery light: 1-1.5<br />\nStraw: 2-3\n<br />Pale: 4\n<br />Gold: 5-6\n<br />Light amber: 7\n<br />Amber : 8\n<br />Medium amber : 9\n<br />Copper/garnet : 10-12\n<br />Light brown : 13-15\n<br />Brown/Reddish brown/chestnut brown : 16-17\n<br />Dark brown : 18-24\n<br />Very dark : 25-39\n<br />Black : 40+");
        cleaned = cleaned.replace(" \n3. Pouring:", " \n\n3. Pouring:");

        cleaned = addHtmlLinks(cleaned);
        cleaned = fixSectionHeaders(cleaned);
        return cleaned;
    }

    private String fixSectionHeaders(String str) {
        for (String title : headerWords) {
            str = str.replace(" \n" + title, " \n\n" + title);
        }

        return str;
    }

    public void convertToXml() throws IOException {
        FileReader input = new FileReader(CLEANED_FILE);
        FileWriter output = new FileWriter(OUTPUT_FILE_NAME);
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

        out.append(getFooter());
        output.write(postCleanUp(out));

        br.close();
        input.close();
        output.close();
    }

    private StringBuilder formatLine(String str) throws IOException {
        final String NOTES_END = "\t\t</notes>\n";
        StringBuilder formattedLine = new StringBuilder();

        if (ignoredLines.contains(str.trim())) {
            // Ignore
        } else if (str.startsWith("Compiled by the Brewers Association")) {
            formattedLine.append(getHeader());
            formattedLine.append(getIntroduction());
            formattedLine.append("\t\t\t\t");
            formattedLine.append(str);
            formattedLine.append(" ");
        } else if (str.startsWith("ALE STYLES")) {
            formattedLine.append(NOTES_END);
            skipNextLine = true;
        } else if (str.equals("")) {
            formattedLine.append("<br/><br/>\n");
        } else if (categories.contains(str.trim())) {
            formattedLine.append(formatCategory(str));
        } else if (subCategories.contains(str.trim())) {
            formattedLine.append(formatSubCategory(str));
        } else if (isInList(headerWords, str)) {
            formattedLine.append(formatSectionHeader(str));
        } else if (isInList(boldWords, str)) {
            formattedLine.append(formatBoldWords(str));
        } else if (str.startsWith("Original Gravity (°Plato)")) {
            formattedLine.append(formatStats(getAllStats(str)));
        } else {
            formattedLine.append(str.trim() + " ");
        }

        return formattedLine;
    }

    private String getHeader() {
        return "<styleguide revision=\"" + BA_2021 + "\" language=\"en\">\n";
    }

    private String getFooter() {
        return CATEGORY_END + "</styleguide>\n";
    }

    private StringBuilder getIntroduction() {
        final String INTRO = "Introduction";
        final String NOTES = "\t\t<notes title=\"\">\n";
        StringBuilder formatted = new StringBuilder();

        formatted.append(CATEGORY_START);
        formatted.append(REVISION);
        formatted.append(NAME_START);
        formatted.append(INTRO);
        formatted.append(NAME_END);
        formatted.append(NOTES);

        return formatted;
    }

    private StringBuilder formatCategory(String str) {
        StringBuilder formatted = new StringBuilder();

        formatted.append(CATEGORY_END);
        formatted.append(CATEGORY_START);
        formatted.append(REVISION);
        formatted.append(NAME_START);
        formatted.append(getTitleCase(str));
        formatted.append(NAME_END);

        skipNextLine = true;

        return formatted;
    }

    private StringBuilder formatSubCategory(String str) {
        StringBuilder formatted = new StringBuilder();
        final String SUB_CATEGORY_START = "\t\t<subcategory>\n";
        final String BODY_START = "\t\t\t<body>\n";

        formatted.append(SUB_CATEGORY_START);
        formatted.append("\t");
        formatted.append(NAME_START);
        formatted.append(str.trim());
        formatted.append(NAME_END);
        formatted.append(BODY_START);

        skipNextLine = true;

        return formatted;
    }

    private String formatBoldWords(String str) {
        final String BOLD_START = "<b>";
        final String BOLD_END = "</b>";

        String word = getWordFromList(boldWords, str);
        str = str.replace(word, BOLD_START + word + BOLD_END);

        return str;
    }

    private String formatSectionHeader(String str) {
        final String BIG_START = "<big><b>";
        final String BIG_END = "</b></big><br/>";

        String word = getWordFromList(headerWords, str);
        str = str.replace(word, BIG_START + word + BIG_END);
        str = str.replace(":", "");

        return str;
    }

    private String getAllStats(String str) throws IOException {
        String stat = "";

        // Append until new line
        while (!StringUtils.isEmpty(stat = br.readLine())) {
            str += stat;
        }

        return str;
    }

    private StringBuilder formatStats(String str) {
        StringBuilder formatted = new StringBuilder();
        final String SUB_CATEGORY_END = "\t\t</subcategory>\n";
        final String BODY_END = "\t\t\t</body>\n";

        formatted.append(BODY_END);
        formatted.append(getOriginalGravity(str));
        formatted.append(getFinalGravity(str));
        formatted.append(getIBU(str));
        formatted.append(getSRM(str));
        formatted.append(getABV(str));

        formatted.append(SUB_CATEGORY_END);

        return formatted;
    }

    //Ex: Original Gravity (°Plato) 1.033-1.038 (8.3-9.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.006-1.012 (1.5-3.1 °Plato) • Alcohol by Weight (Volume) 2.4%-3.3% (3.0%-4.2%) • Hop Bitterness (IBU) 20-35 • Color SRM (EBC) 5-12 (10-24 EBC)
    private StringBuilder getOriginalGravity(String str) {
        final Pattern lowPattern = Pattern.compile("Original Gravity\\s\\(°Plato\\)\\s(\\d.\\d+?)-");
        final Pattern highPattern = Pattern.compile("Original Gravity\\s\\(°Plato\\)\\s\\d.\\d+-(\\d.\\d+?)\\s");
        final Pattern titlePattern = Pattern.compile("Original Gravity\\s\\(°Plato\\)\\s(.*)?\\s•\\sApparent");

        return getStats(str, XML_OG, new Pattern[]{lowPattern}, new Pattern[]{highPattern}, new Pattern[]{titlePattern});
    }

    //Ex: Original Gravity (°Plato) 1.033-1.038 (8.3-9.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.006-1.012 (1.5-3.1 °Plato) • Alcohol by Weight (Volume) 2.4%-3.3% (3.0%-4.2%) • Hop Bitterness (IBU) 20-35 • Color SRM (EBC) 5-12 (10-24 EBC)
    private StringBuilder getFinalGravity(String str) {
        final Pattern lowPattern = Pattern.compile("Final Gravity\\s\\(°Plato\\)\\s(\\d.\\d+?)-");
        final Pattern highPattern = Pattern.compile("Final Gravity\\s\\(°Plato\\)\\s\\d.\\d+-(\\d.\\d+?)\\s\\(");
        final Pattern titlePattern = Pattern.compile("Final Gravity\\s\\(°Plato\\)\\s(.*)?\\s•\\sAlcohol");

        return getStats(str, XML_FG, new Pattern[]{lowPattern}, new Pattern[]{highPattern}, new Pattern[]{titlePattern});
    }

    //Ex: Original Gravity (°Plato) 1.033-1.038 (8.3-9.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.006-1.012 (1.5-3.1 °Plato) • Alcohol by Weight (Volume) 2.4%-3.3% (3.0%-4.2%) • Hop Bitterness (IBU) 20-35 • Color SRM (EBC) 5-12 (10-24 EBC)
    //Ex: Bitterness (IBU) Varies with style • Color SRM (EBC)
    private StringBuilder getIBU(String str) {
        final Pattern lowPattern = Pattern.compile("\\s\\(IBU\\)\\s(\\d+)?");
        final Pattern highPattern = Pattern.compile("\\s\\(IBU\\)\\s\\d+-(\\d+)?");
        final Pattern titlePattern = Pattern.compile("\\s\\(IBU\\)\\s(.*)?\\s•");

        return getStats(str, XML_IBU, new Pattern[]{lowPattern}, new Pattern[]{highPattern}, new Pattern[]{titlePattern});
    }

    //Ex: Original Gravity (°Plato) 1.033-1.038 (8.3-9.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.006-1.012 (1.5-3.1 °Plato) • Alcohol by Weight (Volume) 2.4%-3.3% (3.0%-4.2%) • Hop Bitterness (IBU) 20-35 • Color SRM (EBC) 5-12 (10-24 EBC)
    //Ex: Original Gravity (°Plato) 1.040-1.072 (10-17.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.008-1.016 (2.1-4.1 °Plato) • Alcohol by Weight (Volume) 4.0%-7.0% (5.0%-8.9%) • Hop Bitterness (IBU) 15-21• Color SRM (EBC) Color takes on hue of fruit (Color takes on hue of fruit EBC)
    //Ex: Color SRM (EBC) Varies with style
    private StringBuilder getSRM(String str) {
        final Pattern lowPattern = Pattern.compile("\\s\\(EBC\\)\\s(\\d+)?");
        final Pattern highPattern = Pattern.compile("\\s\\(EBC\\)\\s\\d+-(\\d+)?");
        final Pattern titlePattern1 = Pattern.compile("\\s\\(EBC\\)\\s(.*)?\\s\\(");
        final Pattern titlePattern2 = Pattern.compile("\\s\\(EBC\\)\\s(.*)?\\s");

        return getStats(str, XML_SRM, new Pattern[]{lowPattern}, new Pattern[]{highPattern}, new Pattern[]{titlePattern1, titlePattern2});
    }

    //Ex: Original Gravity (°Plato) 1.033-1.038 (8.3-9.5 °Plato) • Apparent Extract/Final Gravity (°Plato) 1.006-1.012 (1.5-3.1 °Plato) • Alcohol by Weight (Volume) 2.4%-3.3% (3.0%-4.2%) • Hop Bitterness (IBU) 20-35 • Color SRM (EBC) 5-12 (10-24 EBC)
    //Ex: Alcohol by Weight (Volume) Varies with style • Hop
    //Alcohol by Weight (Volume) 0.0%-0.4% abw (0.0%-0.5% abv)
    private StringBuilder getABV(String str) {
        final Pattern lowPattern1 = Pattern.compile("\\s\\(Volume\\)\\s\\d+.\\d+%-\\d+.\\d+% \\((\\d+.\\d+)?%-");
        final Pattern lowPattern2 = Pattern.compile("\\s\\(Volume\\)\\s\\d+.\\d+%-\\d+.\\d+%\\sabw\\s\\((\\d+.\\d+)?%-");
        final Pattern highPattern1 = Pattern.compile("\\s\\(Volume\\)\\s\\d+.\\d+%-\\d+.\\d+%\\s\\(\\d+.\\d+%-(\\d+.\\d)?%\\)");
        final Pattern highPattern2 = Pattern.compile("\\s\\(Volume\\)\\s\\d+.\\d+%-\\d+.\\d+%\\sabw\\s\\(\\d+.\\d+%-(\\d+.\\d)?%\\sabv");
        final Pattern titlePattern = Pattern.compile("\\s\\(Volume\\)\\s(.*)?\\s•\\sHop");

        return getStats(str, XML_ABV, new Pattern[]{lowPattern1, lowPattern2}, new Pattern[]{highPattern1, highPattern2}, new Pattern[]{titlePattern});
    }

    private StringBuilder getStats(String str, String tag, Pattern[] lowPattern, Pattern[] highPattern, Pattern[] titlePattern) {
        final String STATS_END = "\t\t\t</stats>\n";
        final String START = "\t\t\t\t<" + tag + ">\n";
        final String END = "\t\t\t\t</" + tag + ">\n";
        final String LOW_ZERO = "\t\t\t\t\t<low>0</low>\n";
        final String HIGH_ZERO = "\t\t\t\t\t<high>0</high>\n";
        StringBuilder formatted = new StringBuilder();

        StringBuilder low = getHighLowRegEx(str, lowPattern, "low");
        StringBuilder high = getHighLowRegEx(str, highPattern, "high");
        boolean isTitleNeeded = LOW_ZERO.equals(low.toString()) && HIGH_ZERO.equals(high.toString());

        formatted.append(getStatsStartTag(str, titlePattern, isTitleNeeded));
        formatted.append(START);
        formatted.append(low);
        formatted.append(high);
        formatted.append(END);
        formatted.append(STATS_END);

        return formatted;
    }

    private StringBuilder getStatsStartTag(String str, Pattern[] titlePattern, boolean isTitleNeeded) {
        final String START = "\t\t\t<stats>\n";
        final String START_TITLE = "\t\t\t<stats title=\"";
        StringBuilder formatted = new StringBuilder();

        if (isTitleNeeded) {
            formatted.append(START_TITLE);

            for (int i = 0; i < titlePattern.length; i++) {
                String title = getRegExValue(str, titlePattern[i]);

                if (!StringUtils.isEmpty(title)) {
                    formatted.append(title);
                    break;
                }
            }

            formatted.append("\">\n");
        } else {
            formatted.append(START);
        }

        return formatted;
    }

    private StringBuilder getHighLowRegEx(String str, Pattern[] pattern, String highLow) {
        final String START = "\t\t\t\t\t<" + highLow + ">";
        final String END = "</" + highLow + ">\n";
        final String ZERO = "0";
        StringBuilder formatted = new StringBuilder();

        String regEx = "";
        for (int i = 0; i < pattern.length; i++) {
            String found = getRegExValue(str, pattern[i]);

            if (!StringUtils.isEmpty(found)) {
                regEx = found;
                break;
            }
        }

        formatted.append(START);
        formatted.append((StringUtils.isEmpty(regEx) ? ZERO : regEx));
        formatted.append(END);

        return formatted;
    }

    private String addHtmlLinks(String str) {
        final Pattern pattern = Pattern.compile("(http|ftp|https)://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&amp;:/~+#-]*[\\w@?^=%&amp;/~+#-])?");

        String found = getAllRegExValue(str, pattern);

        if (!StringUtils.isEmpty(found)) {
            str = str.replace(found, "<a href=\"" + found + "\">" + found + "</a> ");
        }

        return str;
    }


    private String postCleanUp(StringBuilder out) {
        String cleaned = out.toString();

        cleaned = cleaned.replace("<br/>\n<br/>\n\t\t\t</body>\n\t\t\t<stats>", "\n\t\t\t</body>\n\t\t\t<stats>");

        return cleaned;
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

    private String getRegExValue(String str, Pattern pattern) {
        String regEx = "";
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            regEx = matcher.group(1);
        }

        return regEx;
    }

    private String getAllRegExValue(String str, Pattern pattern) {
        String regEx = "";
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            regEx = matcher.group(0);
        }

        return regEx;
    }

    private String getTitleCase(String str) {
        StringBuilder title = new StringBuilder();
        String[] words = str.split(" ");

        for (String word : words) {
            title.append(StringUtils.capitalize(word.toLowerCase()));
            title.append(" ");
        }

        return title.toString();
    }
}
