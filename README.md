This repository contains the source code of DynCoVer, a prototype tool devoloped for the paper:

[Dynamic Policies Revisited](https://github.com/amir-ahmadian/jpf-dyncover/blob/main/paper/Dynamic%20Policies%20Revisited.pdf),
Amir M. Ahmadian and Musard Balliu,
_IEEE European Symposium on Security and Privacy (Euro S&P)_, 2022.

---
# DynCoVer
DynCoVer is a prototype tool based on [ENCoVer](https://people.kth.se/~musard/files/encover.html) and [JavaPathFinder](https://github.com/javapathfinder). DynCoVer can be used to verify security in the presence of dynamic policies, perform policy consistency checks, and generate consistent policies. 

## Install

- DynCoVer requires [Z3](https://github.com/Z3Prover/z3). You can manually install Z3 for your platform and make sure that Z3 is in your path. DynCoVer is known to work with Z3 version 4.8; it may or may not work with more recent versions.

- You may also need to install ant.
  Test: running `ant -version` in a terminal should return `Apache Ant version 1.7.1 compiled on September 8 2010` or something similar.

- Clone the repo, or download it as a ZIP file.

## Compile

1. Enter the jpf-dyncover directory
   
   Command: `cd jpf-dyncover`.

2. Run the `compile` task.
   
   Command: `ant compile`.

## Use cases

- In the examples directory, we provide two use cases: A benchmark and a social network.

- Each use case has a configuration file called `conf_tests.txt`. The information in this file is used by DynCoVer, and it defines among other things:
	1. The method which is going to be executed symbolically by jpf
	2. The methods observable by the attacker (e.g. *print*)
	3. The type of attacker (e.g. *perfect*, *bounded*, *forgetful*)
	4. The capacity of the bounded attacker's memory (e.g. *bounded,1*) 
	5. The method used to deal with inconsistent policies (e.g. *reject* or *repair*).

## Running the use cases

1. Compile the examples.

   Command: `ant examples`

2. Enter the build/examples directory.

   Command: `cd build/examples`

3. Generate configuration files of a set of examples (e.g. Benchmarks).
 
   Command: `bash bin/generateTestConfFiles.sh Benchmarks`.
   
4. Return to the main directory.

   Command: `cd ../..`

5. Run the example (e.g. Program 1 in the Benchmarks).

   Command: `bash ./bin/dyncover.sh ./build/examples/testConf_encover.tests.Benchmarks_program1_perfect_reject.jpf`

6. Look at the results.

   Command: `less ./output.out`.
