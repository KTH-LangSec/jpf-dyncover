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


nbLoops=$1;

logFile="iterations.log";
rm -f ${logFile};

if [[ -z "${nbLoops}" ]]; then nbLoops=200; fi;

nbIterations=0;
while [[ ${nbIterations} -lt ${nbLoops} ]];
do
    nbIterations=$(( ${nbIterations} + 1 ));

    echo "";
    echo "Starting iteration " ${nbIterations};
    echo "Starting iteration " ${nbIterations} >> ${logFile};
    echo "";
    sleep 2;

    for f in $(ls testConf_*.jpf);
    do
	jpfFileNameStripped=$(echo "${f}" | sed 's/^testConf_//; s/\.jpf$//; s/\./_/');
	logFile="log_${jpfFileNameStripped}.log";

	echo "RUNNING TEST FOR: ${f}";
	echo "  RUNNING TEST FOR: ${f}" >> ${logFile};
	./bin/runTestAndLogResults.sh ${f} &
	testPid=$!;
	sleep 1;
	tail -f ${logFile} &
	tailPid=$!;
	wait ${testPid};
	sleep 5;
	kill -9 ${tailPid};
	echo "";
    done;

    echo "";
    echo "Iteration " ${nbIterations} " done!";
    echo "";
    echo "Iteration " ${nbIterations} " done!" >> ${logFile};
    echo "" >> ${logFile};
done;
