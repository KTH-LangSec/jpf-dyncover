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
 * This is the class implementing the tax records.
 * 
 * @author Gurvan Le Guernic
 * @version 0.2
 */

package TaxRecordMonoThreaded;

interface TaxRecord4taxPayer {
  void setTaxpayer(TaxPayer tp);
  int getTaxes();
  int getAmountPayed();
  int payTaxes(int charity, int amount);
}

interface TaxRecord4taxChecker {
  String getTaxpayerName();
  int verifyPayment();
  void freeze();
}

class TaxRecord implements TaxRecord4taxPayer, TaxRecord4taxChecker {
  static enum TaxRate { FIX, VARIABLE };
  static TaxRate taxRateType = TaxRate.FIX;
  private final int FIX_RATE_PERCENTAGE = 10;
  private final int VARIABLE_RATE_PERCENTAGE = 1;
  private final int SLICE_WIDTH = 10; // in K$

  private TaxPayer taxpayer = null;
  private String taxpayerName = null;
  private int salary = 0;
  private TaxChecker checker = null;
  private int charity = 0;
  private int amountPayed = 0;
  private Boolean frozen = false;

  TaxRecord(int salary) { // in K$
    this.salary = salary;
  }

  public void setTaxpayer(TaxPayer tp) {
    this.taxpayer = tp;
    this.taxpayerName = this.taxpayer.getName();
  }

  public String getTaxpayerName() {
    return this.taxpayer.getName();
  }

  int computeTaxes() {
    int taxes = -1;
    switch(taxRateType) {
    case FIX: taxes = computeTaxes_fixRate(); break;
    case VARIABLE: taxes = computeTaxes_variableRate(); break;
    }
    return taxes;
  }

  private int computeTaxes_fixRate() {
    int taxes;
    taxes = FIX_RATE_PERCENTAGE * (salary * 10); // salary is to be given in K$. So 10 * (S/K) = S * 1%
    return taxes;
  }

  private int computeTaxes_variableRate() { // all in K$
    int taxes = 0;
    int untaxedSalary = this.salary;
    int sliceNb = 1;
    while ( untaxedSalary > 0 ) {
      int sliceAmt = 0;
      if (untaxedSalary < SLICE_WIDTH) { sliceAmt = untaxedSalary; }
      else { sliceAmt = SLICE_WIDTH; }
      int sliceTaxes = (sliceNb * VARIABLE_RATE_PERCENTAGE) * (sliceAmt * 10); // (taxedSlice * 10) in K$ = (taxedSlice / 100) in $
      taxes = taxes + sliceTaxes;
      untaxedSalary = untaxedSalary - sliceAmt;
      sliceNb = sliceNb + 1;
    }
    return taxes;
  }

  void registerChecker(TaxChecker checker) {
    this.checker = checker;
  }

  public int getAmountPayed() {
    EncoverTests.observableByAgent(this.taxpayerName, this.amountPayed);
    return this.amountPayed;
  }

  public int getTaxes() {
    int res = this.computeTaxes();
    EncoverTests.observableByAgent(this.taxpayerName, res);
    return res;
  }

  int getTaxBalance() {
    int taxAmount = this.computeTaxes();
    int taxBalance = this.amountPayed - (taxAmount + this.charity);
    return taxBalance;
  }

  public int payTaxes(int charity, int amount) {
    if ( ! this.frozen ) {
      this.charity = charity;
      this.amountPayed = this.amountPayed + amount;
      if (checker != null) checker.checkTaxes(this);
    }
    int taxBalance = this.getTaxBalance();
    EncoverTests.observableByAgent(this.taxpayerName, taxBalance);
    return taxBalance;
  }

  public int verifyPayment() {
    int taxBalance = this.getTaxBalance();
    if (taxBalance < 0) { taxBalance = -1; }
    return taxBalance;
  }

  public void freeze() {
    this.frozen = true;
  }

  int getCharity() {
    while ( ! this.frozen ) {};
    return this.charity;
  }

  public static void main(String[] args) {
    TaxServer server = new TaxServer();
    TaxChecker tc = new TaxChecker("TaxChecker");
    Charity charity = new Charity(server);

    TaxRecord tr1 = new TaxRecord(50000);
    server.registerTaxRecord(tr1);
    tr1.registerChecker(tc);
    TaxPayer alice = new TaxPayer("Alice", tr1);

    TaxRecord tr2 = new TaxRecord(75000);
    server.registerTaxRecord(tr2);
    tr2.registerChecker(tc);
    TaxPayer bob = new TaxPayer("Bob", tr2);

    alice.payTaxes(50, 100);
    alice.payTaxesInFull(15);
    bob.payTaxes(10,25000);

    charity.getCharity();
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
