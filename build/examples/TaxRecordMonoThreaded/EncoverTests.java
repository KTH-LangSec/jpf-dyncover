/*
 * Copyright (C) 2012 Gurvan Le Guernic
 * 
 * This file is part of ENCoVer. ENCoVer is a JavaPathFinder extension allowing
 * to verify if a Java method respects different epistemic noninterference
 * properties.
 * 
 * ENCoVer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ENCoVer is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ENCoVer. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2011 Gurvan Le Guernic
 * 
 * This file is part of TaxRecord. TaxRecord is the case study for task 4.1 of
 * the HATS project. It simulates a simplified tax paying process.
 * 
 * TaxRecord is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TaxRecord is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TaxRecord. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This is the class implementing the tests to be run by Encover.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package TaxRecordMonoThreaded;

class EncoverTests {

  public static void observableByAgent(String agent, int i) {
    System.out.println(agent + " observes '" + i + "'.");
  }

  public static void simplestTest(int incomeAlice) {

    TaxServer server = new TaxServer();
    TaxChecker checker = new TaxChecker("TaxChecker");
    Charity charity = new Charity(server);

    TaxRecord record_Alice = new TaxRecord(incomeAlice);
    server.registerTaxRecord(record_Alice);
    record_Alice.registerChecker(checker);
    TaxPayer alice = new TaxPayer("Alice", record_Alice);

    alice.payTaxesInFull(0);

    charity.getCharity();
  }

  public static void onePayer(int incomeAlice, int charityAlice, int paymentAlice) {

    TaxServer server = new TaxServer();
    TaxChecker checker = new TaxChecker("TaxChecker");
    Charity charity = new Charity(server);

    TaxRecord recordAlice = new TaxRecord(incomeAlice);
    server.registerTaxRecord(recordAlice);
    recordAlice.registerChecker(checker);
    TaxPayer alice = new TaxPayer("Alice", recordAlice);

    int balanceAlice = alice.payTaxes(charityAlice, paymentAlice);
    if ( balanceAlice < 0 ) alice.payTaxesInFull(charityAlice);

    charity.getCharity();
  }

  public static void twoPayers(int incomeAlice, int charityAlice, int paymentAlice,
                               int incomeBob, int charityBob, int paymentBob) {

    TaxServer server = new TaxServer();
    TaxChecker checker = new TaxChecker("TaxChecker");
    Charity charity = new Charity(server);

    TaxRecord recordAlice = new TaxRecord(incomeAlice);
    server.registerTaxRecord(recordAlice);
    recordAlice.registerChecker(checker);
    TaxPayer alice = new TaxPayer("Alice", recordAlice);

    TaxRecord recordBob = new TaxRecord(incomeBob);
    server.registerTaxRecord(recordBob);
    recordBob.registerChecker(checker);
    TaxPayer bob = new TaxPayer("Bob", recordBob);

    int balanceAlice = alice.payTaxes(charityAlice, paymentAlice);
    if ( balanceAlice < 0 ) alice.payTaxesInFull(charityAlice);

    int balanceBob = bob.payTaxes(charityBob, paymentBob);
    if ( balanceBob < 0 ) bob.payTaxesInFull(charityBob);

    charity.getCharity();
  }

  public static void main(String[] args) {
    String[] params = args[0].split(" ");

    if (params.length > 1 && params[1].equals("FIX"))
      TaxRecord.taxRateType = TaxRecord.TaxRate.FIX;
    if (params.length > 1 && params[1].equals("VARIABLE"))
      TaxRecord.taxRateType = TaxRecord.TaxRate.VARIABLE;

    int testNb = Integer.parseInt(params[0]);
    switch (testNb) {
    case 0: 
      EncoverTests.simplestTest(50); // salary is given in K$
      break;
    case 1: 
      EncoverTests.onePayer(50, 50, 100); // salary is given in K$
      break;
    case 2: 
      EncoverTests.twoPayers(50, 50, 100, 75, 10, 10000); // salary is given in K$
      break;
    }
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
