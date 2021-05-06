## Install

- ENCoVer requires Z3. You can manually install Z3 for your platform (https://github.com/Z3Prover/z3) and make sure that Z3 is in your path. ENCoVer is known to work with Z3 version 4.8; it may or may not work with more recent versions.

- You may also need to install ant.
  Test: running `ant -version` in a terminal should return `Apache Ant version 1.7.1 compiled on September 8 2010` or something similar.

- Clone the repo, or download it as a ZIP file.

## Compile

1. Enter the jpf-encover directory
   
   Command: `cd jpf-encover`.

2. Run the `compile` task.
   
   Command: `ant compile`.

3. Run the `test` task.

   Command: `ant test`.

## Running some examples

1. Compile the examples.

   Command: `ant examples`.

2. Enter the build/examples directory

   Command: `cd build/examples`.

3. Generate configuration files of a set of example (e.g. Benchmarks)
 
   Command: `bash bin/generateTestConfFiles.sh Benchmarks`.

4. Run an example (e.g. Benchmarks.program10).

   Command: `bash ../../bin/encover.sh testConf_encover.tests.Benchmarks.program10.jpf`.

5. Look at the results (e.g. PaperCSF.Small_getSign). TODO

   Command: `less run__PaperCSF__Small__getSign#sym__getSign.out`.
