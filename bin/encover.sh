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


sitePropertiesFile="$(dirname $0)/../site.properties";

currentDir=$(pwd);
cd $(dirname ${sitePropertiesFile}); 
sitePropertiesDir=$(pwd);
cd ${currentDir};

#jpfHomeDir=`\
#    grep "jpf.home =" ${sitePropertiesFile} \
#    | sed 's/ *//' | cut -d '=' -f 2 \
#    | sed "s/${config_path}/${sitePropertiesDir//\//\\/}/" \
#    | sed "s/${user.home}/${HOME//\//\\/}/"`;
    
#jpfCore=`\
#    grep "jpf-core =" ${sitePropertiesFile} \
#    | sed 's/ *//' | cut -d '=' -f 2 \
#    | sed "s/\\${config_path}/${sitePropertiesDir//\//\\/}/" \
#    | sed "s/\\${user.home}/${HOME//\//\\/}/"\
#    | sed "s/\\${jpf.home}/${jpfHomeDir//\//\\/}/"`;
#jpfSymbc=`\
#    grep "jpf-symbc =" ${sitePropertiesFile} \
#    | sed 's/ *//' | cut -d '=' -f 2 \
#    | sed "s/\\${config_path}/${sitePropertiesDir//\//\\/}/" \
#    | sed "s/\\${user.home}/${HOME//\//\\/}/"\
#    | sed "s/\\${jpf.home}/${jpfHomeDir//\//\\/}/"`;

#currentDir=$(pwd);
#cd $(dirname ${sitePropertiesFile}); cd ${jpfCore}; jpfCore=$(pwd);
#cd $(dirname ${sitePropertiesFile}); cd ${jpfSymbc}; jpfSymbc=$(pwd);
#cd $(dirname ${sitePropertiesFile}); jpfEncover=$(pwd);
#cd ${currentDir};

jpfCore="$sitePropertiesDir/lib/jpf-core-r644"
jpfSymbc="$sitePropertiesDir/lib/jpf-symbc-r374"

LD_LIBRARY_PATH=${jpfSymbc}/lib JVM_FLAGS="-Xmx2048m -ea" \
bash ${jpfCore}/bin/jpf +site=${sitePropertiesFile} $@
