## TODO
- Make encover.sh script dynamic


## Install

- ENCoVer requires Z3 which we can not distributed due to license requirements. You need to manually install Z3 for your platform (http://research.microsoft.com/en-us/um/redmond/projects/z3/download.html) and make sure that Z3 is in your path. ENCoVer is known to work with Z3 version 3.2; it may or may not work with more recent versions.
Test: running 'z3 -version' in a terminal should return 'Z3 version 3.2' or something similar.

- You may also need to install ant.
Test: running 'ant -version' in a terminal should return 'Apache Ant version 1.7.1 compiled on September 8 2010' or something similar.

- Unzip this file.
Command: 'unzip jpf-encover_r307_src.zip'.

## Compile

 Enter the jpf-encover directory
 Command: 'cd jpf-encover'.

 Run the 'compile' task.
 Command: 'ant compile'.

 Run the 'test' task.
 Command: 'ant test'.

## Running some examples

 Compile the examples.
 Command: 'ant examples'.

 Enter the build/examples directory
 Command: 'cd build/examples'.

 Generate configuration files of a set of example (e.g. PaperCSF)
 Command: 'bash bin/generateTestConfFiles.sh PaperCSF'.

 Run an example (e.g. PaperCSF.Small_getSign).
 Command: 'bash ../../bin/encover.sh testConf_PaperCSF.Small_getSign.jpf'.

 Look at the results (e.g. PaperCSF.Small_getSign).
 Command: 'less run__PaperCSF__Small__getSign#sym__getSign.out'.