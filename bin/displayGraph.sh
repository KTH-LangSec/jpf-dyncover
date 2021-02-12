#!/bin/bash

# Copyright (C) 2011 Gurvan Le Guernic
# 
# This file is part of ENCoVer. ENCoVer is a JavaPathFinder extension allowing
# to verify if a Java method respects different epistemic noninterference
# properties.
# 
# ENCoVer is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# ENCoVer is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with ENCoVer. If not, see <http://www.gnu.org/licenses/>.

graphFile="$1";

# GENERATE ENCOVER CLASSPATH
jpfComponents="jpf-core jpf-symbc jpf-encover";
jpfPropertiesFile="site.properties";
  
# Listing jar files into ${classpath}
classpath="";
jpfHomeDir=`grep "jpf.home =" ${jpfPropertiesFile} \
    | cut -d ' ' -f 3 \
    | sed "s/\\${user.home}/${HOME//\//\\/}/"`;
for jpfComp in ${jpfComponents};
do

    jpfCompDir=`grep "${jpfComp} =" ${jpfPropertiesFile} \
	| cut -d ' ' -f 3 \
	| sed "s/\\${user.home}/${HOME//\//\\/}/"\
	| sed "s/\\${jpf.home}/${jpfHomeDir//\//\\/}/"`;

    # echo ${jpfComp} "->" ${jpfCompDir};

    for jarFile in `find -L ${jpfCompDir} -name "*.jar"`;
    do
	if [[ -z "${classpath}" ]];
	then
	    classpath="${jarFile}";
	else
	    jarBasename=`basename ${jarFile}`;
	    jarAlreadyPresent=`echo ${classpath} | grep -c ${jarBasename}`;
	    if [[ ${jarAlreadyPresent} -eq 0 ]];
	    then
		classpath="${classpath}:${jarFile}";
	    fi;
	fi;
    done;
done;

# echo ${classpath};

if [[ "${graphFile##*.}" = "dot" ]];
then

  # PROCESS AND DISPLAY DOT FILE
  tmpPdf=`tempfile`;
  dot -T pdf -o ${tmpPdf} ${graphFile};

  associatedGraphFile=${graphFile%.*};
  if [[ "${associatedGraphFile##*_}" = "ofg" ]];
  then
      graphViewingClass="se.kth.csc.jpf_encover.OFG_Handler";
      java -cp ${classpath} ${graphViewingClass} ${associatedGraphFile%_*}.ofg "printGraphLegend";
  fi;

  evince ${tmpPdf};
  rm -f ${tmpPdf};
  
else

  # SELECT APPROPRIATE VIEWER
  graphViewingClass="";
  if [[ "${graphFile##*.}" = "jeg" ]];
  then
      graphViewingClass="se.kth.csc.jpf_encover.JPFEventsGraph";
  fi;
  if [[ "${graphFile##*.}" = "ofg" ]];
  then
      graphViewingClass="se.kth.csc.jpf_encover.OFG_Handler";
  fi;
  
  java -cp ${classpath} ${graphViewingClass} ${graphFile};
fi;
