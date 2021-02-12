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


texFile="testSummary.tex";

rm -f ${texFile};

echo "\begin{sidewaysfigure}" >> ${texFile};
echo "  \centering \scriptsize" >> ${texFile};
echo "" >> ${texFile};

echo "  \begin{tabular}{| l | c | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | c | r | r | r | r | r | r | r | r | r | r | r | r |}" >> ${texFile};
echo "    \hline" >> ${texFile};
echo "    TEST RUN &                                                                            \multicolumn{20}{|c|}{JPF}                                                                             &                               \multicolumn{13}{|c|}{ENCOVER}                              \\\\" >> ${texFile}
echo "    \hline" >> ${texFile};
echo "             & Elapsed time & \multicolumn{4}{|c|}{States} & \multicolumn{2}{|c|}{Search} & \multicolumn{5}{|c|}{Choices} & \multicolumn{4}{|c|}{Heap} & Inst & Mem & \multicolumn{2}{|c|}{Code} & NI & \multicolumn{5}{|c|}{Timing} & \multicolumn{4}{|c|}{OFG} & \multicolumn{3}{|c|}{Fml} \\\\" >> ${texFile}
echo "    \hline" >> ${texFile};
echo "             &              &   N   &   V   &   B   &   E  &       D       &       C      &   T  &   S  &  L  &  R  &  D  &   N   &   R  &   L  &   G  &      &     &       C      &      M      &    &   O  &  E  &  G  &  S  &  M  &   N  &   E  &   D  &   W  &    V    &    A   &    I   \\\\" >> ${texFile}
echo "    \hline" >> ${texFile};

testIndexFile=$(tempfile);

for f in $(ls data_*.dat);
do
    testName=$(echo ${f} | sed 's/^.*_\([^_]\+\)\.dat/\1/');
    restOfLine=$(sed -n '/^AVG:/ p' $f | tail -n 1 | sed 's/^[^|]\+|//; s/|$//; s/|/\&/g');

    echo "    ${testName} & ${restOfLine} \\\\" >> ${texFile};
    echo "    \hline" >> ${texFile};

    echo "%    \item ${testName}: $(echo ${f} | sed 's/_/\\_/g')" >> ${testIndexFile};
done;

echo "  \end{tabular}" >> ${texFile};
echo "" >> ${texFile};
echo "  \hspace*{\stretch{1}}" >> ${texFile};
echo "\fbox{%" >> ${texFile};
echo "  \begin{minipage}{\textheight}" >> ${texFile};
echo "    \begin{itemize}" >> ${texFile};
echo "    \item Tests:" >> ${texFile};
echo "%      \begin{itemize}" >> ${texFile};
cat ${testIndexFile}  >> ${texFile}; rm ${testIndexFile};
echo "%      \end{itemize}" >> ${texFile};
echo "    \end{itemize}" >> ${texFile};
echo "  \end{minipage}" >> ${texFile};
echo "}" >> ${texFile};
echo "\fbox{%" >> ${texFile};
echo "  \hspace*{\stretch{1}}" >> ${texFile};
echo "  \begin{minipage}{1.2\textwidth}" >> ${texFile};
echo "    \begin{itemize}" >> ${texFile};
echo "    \item SPF" >> ${texFile};
echo "      \begin{itemize}" >> ${texFile};
echo "      \item Elapsed time: total time spent running JPF with its two extensions jpf-symbc and jpf-encover activated" >> ${texFile};
echo "      \item States: states encountered while doing the concolic testing (N: new; V: visited; B: backtracked; E: end)" >> ${texFile};
echo "      \item Search: path search related information (D: maximum search depth; C: number of constraints hit)" >> ${texFile};
echo "      \item Choices: number of choice generators of the different types created (T: thread; S: signal; L: lock; R: shared ref; D: data)" >> ${texFile};
echo "      \item Heap: heap related information (N: new; R: released; L: max live; G: gc cycles)" >> ${texFile};
echo "      \item Inst: total number of instructions executed (including start up)" >> ${texFile};
echo "      \item Mem: maximum memory used (in MB)" >> ${texFile};
echo "      \item Code: number of classes and methods loaded (C: classes; M: methods) (including initialization classes and methods)" >> ${texFile};
echo "      \end{itemize}" >> ${texFile};
echo "    \item ENCOVER" >> ${texFile};
echo "      \begin{itemize}" >> ${texFile};
echo "      \item NI: \texttt{Y} iff Encover concludes that the program is non-interfering" >> ${texFile};
echo "      \item Timing: given in ms (O: overall; E: model extraction (SPF); G: interference formula generation; S: interference formula satisfiability checking; M: MCMAS model generation)" >> ${texFile};
echo "      \item SOT: information related to the SOT (N: number of nodes; E: number of edges; D: depth of the SOT (correspond to the longest possible sequence of outputs); W: width of the SOT (corresponds to the maximum number of nodes at any level)" >> ${texFile};
echo "      \item Fml: information related to the interference formula (V: number of distinct variables; A: number of atomic formulas; I: number of instances of variables or constants)" >> ${texFile};
echo "      \end{itemize}" >> ${texFile};
echo "    \end{itemize}" >> ${texFile};
echo "  \end{minipage}" >> ${texFile};
echo "}" >> ${texFile};
echo "  \hspace*{\stretch{1}}" >> ${texFile};

echo "" >> ${texFile};
echo "  \caption{Raw evaluation data}" >> ${texFile};
echo "  \label{fig:raw-evaluation-data}" >> ${texFile};
echo "\end{sidewaysfigure}" >> ${texFile};