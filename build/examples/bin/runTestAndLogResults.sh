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


jpfFile=${1};

jpfFileNameStripped=$(echo "${jpfFile}" | sed 's/^testConf_//; s/\.jpf$//; s/\./_/');
logFile="log_${jpfFileNameStripped}.log";
errorFile="log_${jpfFileNameStripped}.err";
dataFile="data_${jpfFileNameStripped}.dat";
bkpFile="data_${jpfFileNameStripped}.dat.bkp";

formattedSymbolicMethodName=$(grep "symbolic.method" ${jpfFile} | cut -d ' ' -f 3 | sed 's/\./__/g; s/(/#/; s/)//');
outputSuffix=$(grep "encover.testNameSuffix" ${jpfFile} | cut -d ' ' -f 3);
outputFileName="run__${formattedSymbolicMethodName}__${outputSuffix}.out";

date=$(date +"%y/%m/%d %H:%M:%S");
jpf.sh ${jpfFile} 1>${logFile} 2>${errorFile};

if [[ ! -e ${dataFile} ]];
then
    echo "TEST RUN |                                                                                                   JPF                                                                                                              |                                                                    ENCOVER                                                                                      |" > ${dataFile}
    echo "         | elapsed time |              states               |          search            |              choice generators             |            heap                       | instructions | max memory |    loaded code    | NI |                                             Timing                                              |              OFG              |           Formula        |" >> ${dataFile}
    echo "         |              | new | visited | backtracked | end | maxDepth | constraints hit | thread | signal | lock | shared ref | data | new | released | max live | gc-cycles |              |            | classes | methods |    | overall | model extraction | formula generation | formula satisfaction | mcmas model generation | nodes | edges | depth | width | vars | atoms | instances |" >> ${dataFile}
else
    cp -f ${dataFile} ${bkpFile};
fi;

jpf_elapsedTime=$(sed '0,/^=\+ statistics$/ d; s/ \+/ /g' ${logFile} | grep "^elapsed time:" | cut -d ' ' -f 3);
jpf_statesNew=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^states:" | cut -d '|' -f 2);
jpf_statesVisited=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^states:" | cut -d '|' -f 4);
jpf_statesBacktracked=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^states:" | cut -d '|' -f 6);
jpf_statesEnd=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^states:" | cut -d '|' -f 8);
jpf_searchMaxDepth=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^search:" | cut -d '|' -f 2);
jpf_searchConstraints=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^search:" | cut -d '|' -f 4);
jpf_choiceThread=$(sed '0,/^=\+ statistics$/ d; s/[=, )]\+/|/g' ${logFile} | grep "^choice|generators:" | cut -d '|' -f 4);
jpf_choiceSignal=$(sed '0,/^=\+ statistics$/ d; s/[=, )]\+/|/g' ${logFile} | grep "^choice|generators:" | cut -d '|' -f 6);
jpf_choiceLock=$(sed '0,/^=\+ statistics$/ d; s/[=, )]\+/|/g' ${logFile} | grep "^choice|generators:" | cut -d '|' -f 8);
jpf_choiceSharedRef=$(sed '0,/^=\+ statistics$/ d; s/[=, )]\+/|/g' ${logFile} | grep "^choice|generators:" | cut -d '|' -f 11);
jpf_choiceData=$(sed '0,/^=\+ statistics$/ d; s/[=, )]\+/|/g' ${logFile} | grep "^choice|generators:" | cut -d '|' -f 13);
jpf_heapNew=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^heap:" | cut -d '|' -f 2);
jpf_heapReleased=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^heap:" | cut -d '|' -f 4);
jpf_heapMaxLive=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^heap:" | cut -d '|' -f 6);
jpf_heapGcCycles=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^heap:" | cut -d '|' -f 8);
jpf_instructions=$(sed '0,/^=\+ statistics$/ d; s/ \+/ /g' ${logFile} | grep "^instructions:" | cut -d ' ' -f 2);
jpf_MaxMemory=$(sed '0,/^=\+ statistics$/ d; s/ \+/ /g' ${logFile} | grep "^max memory:" | cut -d ' ' -f 3);
jpf_loadedClasses=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^loaded code:" | cut -d '|' -f 2);
jpf_loadedMethods=$(sed '0,/^=\+ statistics$/ d; s/[=,]/|/g' ${logFile} | grep "^loaded code:" | cut -d '|' -f 4);

enc_timing_overall=$(sed '0,/^TIMING ESTIMATIONS:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ overall:" | cut -d ' ' -f 3);
enc_timing_modelExtraction=$(sed '0,/^TIMING ESTIMATIONS:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ model extraction:" | cut -d ' ' -f 4);
enc_timing_fmlGeneration=$(sed '0,/^TIMING ESTIMATIONS:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ interference formula generation:" | cut -d ' ' -f 5);
enc_timing_fmlSatisfaction=$(sed '0,/^TIMING ESTIMATIONS:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ interference formula satisfaction:" | cut -d ' ' -f 5);
enc_timing_mcmasMdlGen=$(sed '0,/^TIMING ESTIMATIONS:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ MCMAS model generation:" | cut -d ' ' -f 5);
enc_ofg_nodes=$(sed '0,/^OFG SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ number of nodes:" | cut -d ' ' -f 5);
enc_ofg_edges=$(sed '0,/^OFG SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ number of edges:" | cut -d ' ' -f 5);
enc_ofg_depth=$(sed '0,/^OFG SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ depth of OFG:" | cut -d ' ' -f 5);
enc_ofg_width=$(sed '0,/^OFG SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ width of OFG:" | cut -d ' ' -f 5);
enc_fml_vars=$(sed '0,/^FORMULA SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ number of distinct variables:" | cut -d ' ' -f 6);
enc_fml_atom=$(sed '0,/^FORMULA SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ number of atomic formulas:" | cut -d ' ' -f 6);
enc_fml_inst=$(sed '0,/^FORMULA SIZE:$/ d; s/ \+/ /g' ${outputFileName} | grep "^ number of instances of variables or constants:" | cut -d ' ' -f 9);

enc_ni="?";
echo "${outputFileName}";
if [[ $(grep -c "The program is \(non\)\?interfering." ${outputFileName}) -eq 1 ]];
then
    if [[ $(grep -c "The program is interfering." ${outputFileName}) -eq 1 ]];
    then
	enc_ni="N";
    else
	enc_ni="Y";
    fi;
fi;

if [[ -f ${dataFile} ]];
then
    echo "${date} | ${jpf_elapsedTime} | ${jpf_statesNew} | ${jpf_statesVisited} | ${jpf_statesBacktracked} | ${jpf_statesEnd} | ${jpf_searchMaxDepth} | ${jpf_searchConstraints} | ${jpf_choiceThread} | ${jpf_choiceSignal} | ${jpf_choiceLock} | ${jpf_choiceSharedRef} | ${jpf_choiceData} | ${jpf_heapNew} | ${jpf_heapReleased} | ${jpf_heapMaxLive} | ${jpf_heapGcCycles} | ${jpf_instructions} | ${jpf_MaxMemory} | ${jpf_loadedClasses} | ${jpf_loadedMethods} | ${enc_ni} | ${enc_timing_overall} | ${enc_timing_modelExtraction} | ${enc_timing_fmlGeneration} | ${enc_timing_fmlSatisfaction} | ${enc_timing_mcmasMdlGen} | ${enc_ofg_nodes} | ${enc_ofg_edges} | ${enc_ofg_depth} | ${enc_ofg_width} | ${enc_fml_vars} | ${enc_fml_atom} | ${enc_fml_inst} |" >> ${dataFile}
fi;

cp -f ${dataFile} ${bkpFile};
