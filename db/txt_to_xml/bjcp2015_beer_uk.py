#!/usr/bin/python
# -*- coding: utf-8 -*-
import os, sys
import re


# Convert Old BJCP 2015 xml file to new Vital Statistics format.

inputFile = open("styleguide-2015_uk_old.xml","r")
outputFile = open("../styleguide-2015_uk.xml","w+")

str = inputFile.read()

# To:
# <stats>
# 	<type>abv</type>
# 	<header></header>
# 	<notes></notes>
# 	<low>4.2</low>
# 	<high>5.6</high>
# <stats>

str = re.sub("<stats>\n\t*\s*<og flexible=\"false\">\n", "<stats>\n\t\t\t\t<type>og</type>\n\t\t\t\t<header></header>\n\t\t\t\t<notes></notes>\n", str)
str = re.sub("\t*</og>", "\t\t\t</stats>", str)

str = re.sub("\t*<fg flexible=\"false\">\n", "\t\t\t<stats>\n\t\t\t\t<type>fg</type>\n\t\t\t\t<header></header>\n\t\t\t\t<notes></notes>\n", str)
str = re.sub("\t*</fg>", "\t\t\t</stats>", str)

str = re.sub("\t*<ibu flexible=\"false\">\n", "\t\t\t<stats>\n\t\t\t\t<type>ibu</type>\n\t\t\t\t<header></header>\n\t\t\t\t<notes></notes>\n", str)
str = re.sub("\t*</ibu>", "\t\t\t</stats>", str)

str = re.sub("\t*<srm flexible=\"false\">\n", "\t\t\t<stats>\n\t\t\t\t<type>srm</type>\n\t\t\t\t<header></header>\n\t\t\t\t<notes></notes>\n", str)
str = re.sub("\t*</srm>", "\t\t\t</stats>", str)

str = re.sub("\t*<abv flexible=\"false\">\n", "\t\t\t<stats>\n\t\t\t\t<type>abv</type>\n\t\t\t\t<header></header>\n\t\t\t\t<notes></notes>\n", str)
str = re.sub("\t*</abv>\n", "", str)

str = re.sub("\t*<low>", "\t\t\t\t<low>", str)
str = re.sub("\t*<high>", "\t\t\t\t<high>", str)

# 25B Saison Exception
str = re.sub("<stats title=\"all\">\n\t*\s*<stats>\n\t*\s*<type>ibu</type>\n\t*\s*<header></header>", "<stats>\n\t\t\t\t<type>ibu</type>\n\t\t\t\t<header>all</header>", str)
str = re.sub("<stats title=\"світлий\">\n\t*\s*<stats>\n\t*\s*<type>srm</type>\n\t*\s*<header></header>", "<stats>\n\t\t\t\t<type>srm</type>\n\t\t\t\t<header>світлий</header>", str)
str = re.sub("<stats title=\"темний\">\n\t*\s*<stats>\n\t*\s*<type>srm</type>\n\t*\s*<header></header>", "<stats>\n\t\t\t\t<type>srm</type>\n\t\t\t\t<header>темний</header>", str)
str = re.sub("<stats title=\"стандарт\">\n\t*\s*<og flexible=\"false\">\n\t*\s*<low>(.*)*</low>\n\t*\s*<high>(.*)*</high>\n\t*\s*</stats>\n\t*\s*" + 
            "<stats>\n\t*\s*<type>fg</type>\n\t*\s*<header></header>\n\t*\s*<notes></notes>\n\t*\s*<low>1.002</low>\n\t*\s*<high>1.008</high>\n\t*\s*</stats>\n\t*\s*" + 
            "<stats>\n\t*\s*<type>abv</type>\n\t*\s*<header></header>\n\t*\s*<notes></notes>\n\t*\s*<low>5.0</low>\n\t*\s*<high>7.0</high>\n\t*\s*</stats>", 

            "\n\t\t\t<stats>\n\t\t\t\t<type>og</type>\n\t\t\t\t<header>стандарт</header>\n\t\t\t\t<notes></notes>\n\t\t\t\t<low>1.048</low>\n\t\t\t\t<high>1.065</high>\n\t\t\t</stats>" + 
            "\n\t\t\t<stats>\n\t\t\t\t<type>fg</type>\n\t\t\t\t<header>стандарт</header>\n\t\t\t\t<notes></notes>\n\t\t\t\t<low>1.002</low>\n\t\t\t\t<high>1.008</high>\n\t\t\t</stats>" + 
            "\n\t\t\t<stats>\n\t\t\t\t<type>abv</type>\n\t\t\t\t<header>стандарт</header>\n\t\t\t\t<notes></notes>\n\t\t\t\t<low>5.0</low>\n\t\t\t\t<high>7.0</high>\n\t\t\t</stats>", str)
str = re.sub("<stats title=\"столовий\">\n\t*\s*<stats>\n\t*\s*<type>abv</type>\n\t*\s*<header></header>", "<stats>\n\t\t\t\t<type>abv</type>\n\t\t\t\t<header>столовий</header>", str)
str = re.sub("<stats title=\"супер\">\n\t*\s*<stats>\n\t*\s*<type>abv</type>\n\t*\s*<header></header>", "<stats>\n\t\t\t\t<type>abv</type>\n\t\t\t\t<header>супер</header>", str)
str = re.sub("</stats>\n\t*\s*</stats>", "</stats>", str)

# Old cider problem?
str = re.sub("<varieties>","<big><b>Varieties</b></big>\n<br />\n", str)
str = re.sub("</varieties>","<br /><br />", str)

# Convert Tags
str = re.sub("</body>\n\t*\s*<tags>","", str)
#str = re.sub("</body>\n\t*\s*<stats>\n\t*\s*<exceptions>\n\t*\s*.*\n.*\n\t*\s*</exceptions>\n\t*\s*</stats>\n\t*\s*<tags>", "</body>\n\t*\s*<stats>\n\t*\s*<exceptions>\n\t*\s*.*\n.*\n\t*\s*</exceptions>\n\t*\s*</stats>\n\t*\s*<tags>", str)
str = re.sub("</tags>","</body>", str)

outputFile.write(str)

inputFile.close()
outputFile.close()