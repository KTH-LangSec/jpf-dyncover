#!/bin/bash

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


dir=$(readlink -f ${1});

aliasesFile=${dir}"/conf_aliases.txt";
testsFile=${dir}"/conf_tests.txt";
domainsFile=${dir}"/conf_domains.txt";
observablesFile=${dir}"/conf_observableByAgents.txt";
dirSpecificsFile=${dir}"/conf_dirSpecifics.txt";

grep -Ev "^#|^[[:space:]]*$" ${testsFile} | while read line
do
    testName=$(echo $(echo ${line} | cut -d '|' -f 1));
    target=$(echo $(echo ${line} | cut -d '|' -f 2));
    args=$(echo $(echo ${line} | cut -d '|' -f 3));
    method=$(echo $(echo ${line} | cut -d '|' -f 4));
    agent=$(echo $(echo ${line} | cut -d '|' -f 5));
    leaked=$(echo $(echo ${line} | cut -d '|' -f 6));
    harbored=$(echo $(echo ${line} | cut -d '|' -f 7));
    methodName=$(echo ${method} | sed 's/^[^(]*\.//g; s/(.*$//');
    inputDomains=$(echo $(grep "${methodName}" ${domainsFile} | sed 's/^[^|]*|//'));
    observable=$(echo $(grep "${agent}" ${observablesFile} | sed 's/^[^|]*|//'));

    testConfFileName="testConf_${target}_${testName}.jpf"
    if [[ -f ${aliasesFile} ]];
    then
	while read line
	do
	    aliasName=$(echo $(echo ${line} | cut -d '|' -f 1));
	    aliasCntt=$(echo $(echo ${line} | cut -d '|' -f 2));
	    testConfFileName="$(echo "${testConfFileName}" | sed "s/{{${aliasName}}}/${aliasCntt}/g")";
	done < <(grep -Ev "^#|^[[:space:]]*$" ${aliasesFile});
    fi;
	    echo "${testConfFileName}";

    cat framework_testConf.jpf \
	| sed "s/\[target\]/${target}/" \
	| sed "s/\[target_args\]/${args}/" \
	| sed "s/\[symbolic\.method\]/${method}/" \
	| sed "s/\[encover\.testNameSuffix\]/${testName}/" \
	| sed "s/\[encover\.inputDomains\]/${inputDomains}/" \
	| sed "s/\[encover\.leakedInputs\]/${leaked}/" \
	| sed "s/\[encover\.harboredInputs\]/${harbored}/" \
	| sed "s/\[encover\.observable\]/${observable}/" \
	> ${testConfFileName};

    minInt=;
    maxInt=;
    if [[ -n "$(echo ${inputDomains} | sed 's/ //g')" ]];
    then
	for dom in $(echo ${inputDomains} | sed 's/ //g; s/;/ /g');
	do
	    lowerBound=$(echo ${dom} | sed 's/[^[]*\[\([0-9]\+\),[0-9]\+\]/\1/');
	    upperBound=$(echo ${dom} | sed 's/[^[]*\[[0-9]\+,\([0-9]\+\)\]/\1/');
	    if [[ -z "${minInt}" ]];
	    then
		minInt=${lowerBound};
	    else
		if [[ ${lowerBound} -lt ${minInt} ]]; then minInt=${lowerBound}; fi;
	    fi;
	    if [[ -z "${maxInt}" ]];
	    then
		maxInt=${upperBound};
	    else
		if [[ ${maxInt} -lt ${upperBound} ]]; then maxInt=${upperBound}; fi;
	    fi;
	done;
	sed -i "s/\[symbolic\.minint\]/${minInt}/" ${testConfFileName};
	sed -i "s/\[symbolic\.maxint\]/${maxInt}/" ${testConfFileName};
    else
	sed -i '/\[symbolic\.minint\]/ d' ${testConfFileName};
	sed -i '/\[symbolic\.maxint\]/ d' ${testConfFileName};
    fi;

    if [[ -f ${aliasesFile} ]];
    then
	grep -Ev "^#|^[[:space:]]*$" ${aliasesFile} | while read line
	do
	    aliasName=$(echo $(echo ${line} | cut -d '|' -f 1));
	    aliasCntt=$(echo $(echo ${line} | cut -d '|' -f 2));
	    sed -i "s^{{${aliasName}}}^${aliasCntt}^g" ${testConfFileName};
	done;
    fi;

    if [[ -f ${dirSpecificsFile} ]];
    then
	cat ${dirSpecificsFile} >> ${testConfFileName};
    fi;

    #echo "\"${testConfFileName}\"" "\"${method}\"" "\"${agent}\"" "\"${leaked}\"" "\"${harbored}\"" "\"${observable}\"";
done
