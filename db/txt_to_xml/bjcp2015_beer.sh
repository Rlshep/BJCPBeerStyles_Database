#!/usr/bin/env bash
#sed 's/<impression>/<body><big><b>Impression<\/b><\/big><br \/>/; s/<\/impression>/<br \/><br \/>/; s/<stats>/<\/body><stats>/; s/<stats title=\"all\">/<\/body><stats title=\"all\">/; s/<aroma>/<big><b>Aroma<\/b><\/big><br \/>/; s/<\/aroma>/<br \/><br \/>/; s/<appearance>/<big><b>Appearance<\/b><\/big><br \/>/; s/<\/appearance>/<br \/><br \/>/; s/<flavor>/<big><b>Flavor<\/b><\/big><br \/>/; s/<\/flavor>/<br \/><br \/>/; s/<mouthfeel>/<big><b>Mouthfeel<\/b><\/big><br \/>/; s/<\/mouthfeel>/<br \/><br \/>/; s/<comments>/<big><b>Comments<\/b><\/big><br \/>/; s/<\/comments>/<br \/><br \/>/; s/<history>/<big><b>History<\/b><\/big><br \/>/; s/<\/history>/<br \/><br \/>/; s/<ingredients>/<big><b>Ingredients<\/b><\/big><br \/>/; s/<\/ingredients>/<br \/><br \/>/; s/<comparison>/<big><b>Comparison<\/b><\/big><br \/>/; s/<\/comparison>/<br \/><br \/>/; s/<examples>/<big><b>Examples<\/b><\/big><br \/>/; s/<\/examples>/<br \/><br \/>/; s/<tags>/<big><b>Tags<\/b><\/big><br \/>/; s/<\/tags>/<br \/><br \/>/; s/<entryinstructions>/<big><b>Entry Instructions<\/b><\/big><br \/>/; s/<\/entryinstructions>/<br \/><br \/>/g' ../styleguide-2015_en.xml > styleguide-2015_en.xml

dos2unix styleguide-2015_en_old.xml
dos2unix styleguide-2015_es_old.xml
dos2unix styleguide-2015_uk_old.xml
echo "Running python script bjcp2015_beer_en.py"
python bjcp2015_beer_en.py
echo "Running python script bjcp2015_beer_es.py"
python bjcp2015_beer_es.py
echo "Running python script bjcp2015_beer_uk.py"
python bjcp2015_beer_uk.py
