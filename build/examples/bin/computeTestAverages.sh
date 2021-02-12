#! /bin/bash

 # Copyright (C) 2012 Gurvan Le Guernic
 # 
 # This file is part of ENCoVer. ENCoVer is a JavaPathFinder extension allowing
 # to verify if a Java method respects different epistemic noninterference
 # properties.
 # 
 # ENCoVer is free software: you can redistribute it and/or modify it under the
 # terms of the GNU General Public License as published by the Free Software
 # Foundation, either version 3 of the License, or (at your option) any later
 # version.
 # 
 # ENCoVer is distributed in the hope that it will be useful, but WITHOUT ANY
 # WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 # A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 # 
 # You should have received a copy of the GNU General Public License along with
 # ENCoVer. If not, see <http://www.gnu.org/licenses/>.


function computeAverageFor {
    dataFile=$1;

    nbFields=$(sed -n "$(wc -l ${dataFile} | cut -d ' ' -f 1) p" ${dataFile} | tr ' ' '\n' | grep -c '|');
    nbLines=$(sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ p" ${dataFile} | wc -l);

    fieldsAvg="|";
    for (( fieldNb=2; fieldNb <= ${nbFields}; fieldNb++ ));
    do

	fieldValueExample=$(\
            sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ {p;q;}" ${dataFile} \
	    | cut -d '|' -f ${fieldNb} | sed "s/^ \+//g; s/ \+$//g;");
	isNumber=$(echo ${fieldValueExample} | grep -c "^[0-9.]\+$");
	isTime=$(echo ${fieldValueExample} | grep -c "^[0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}$");
	isMB=$(echo ${fieldValueExample} | grep -c "^[0-9]\+MB$");

	if [[ ${isNumber} -eq 1 ]];
	then
	    avgField=$(\
                sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ p" ${dataFile} \
		| awk -F '|' "{ total += \$${fieldNb}; count++ } END { avg = sprintf(\"%.3f\", total/count); sub(/.?0+$/, \"\", avg); print avg; }");
	else if  [[ ${isTime} -eq 1 ]];
	then
	    avgField=$(\
                sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ p" ${dataFile} \
		| awk -F '|' "{ split(\$${fieldNb}, a , \":\"); total += (a[1]*3600+a[2]*60+a[3]); count++; } END { avg = int(total/count + 0.5); print strftime(\"%H:%M:%S\", avg, 1); }");
	else if  [[ ${isMB} -eq 1 ]];
	then
	    avgField=$(\
                sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ p" ${dataFile} \
		| awk -F '|' "{ mb = \$${fieldNb}; sub(/MB/, \"\", mb); total += mb; count++; } END { avg = int(total/count + 0.5); print avg; }");
	else
	    avgField=$(\
                sed -n "/^[0-9]\{2\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}/ p" ${dataFile} \
		| awk -F '|' "{ if (fieldValue == \"\") fieldValue = \$${fieldNb}; if (fieldValue != \$${fieldNb}) fieldValue = \"?\"; } END { print fieldValue }");
	fi; fi; fi;

	fieldsAvg="${fieldsAvg} ${avgField} |";
    done;

    echo "AVG: ${nbLines} ${fieldsAvg}" >> ${dataFile};
}

for f in $(ls data_*.dat);
do
    echo "Computing averages for $f";
    computeAverageFor $f;
done;
