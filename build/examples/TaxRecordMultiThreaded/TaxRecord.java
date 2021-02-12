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

package TaxRecordMultiThreaded;

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
  private static final int SLICE_WIDTH = 10; // in K$ (= 10 000 $)

  private TaxPayer taxpayer = null;
  private String taxpayerName = null;
  private int salary = 0;
  private TaxChecker checker = null;
  private int charity = 0;
  private int amountPayed = 0;
  private MyEditableBoolean frozen = new MyEditableBoolean(false);

  TaxRecord(int salary) { // in K$
    this.salary = salary;
  }

  public void setTaxpayer(TaxPayer tp) {
    this.taxpayer = tp;
    this.taxpayerName = this.taxpayer.getTaxpayerName();
  }

  public String getTaxpayerName() {
    return this.taxpayer.getTaxpayerName();
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
    taxes = salary * 100; // salary is to be given in K$. So 100 * (S/K) = S * 0.1
    return taxes;
  }

  private int computeTaxes_variableRate() { // all in K$
    int taxes = 0;
    int untaxedSalary = this.salary;
    int sliceNb = 0;
    while ( untaxedSalary > 0 ) {
      int taxedSlice = 0;
      if (untaxedSalary < SLICE_WIDTH) { taxedSlice = untaxedSalary; }
      else { taxedSlice = SLICE_WIDTH; }
      taxes = taxes + ((taxedSlice * 10) * (sliceNb * 5)); // (taxedSlice * 10) in K$ = (taxedSlice / 100) in $
      untaxedSalary = untaxedSalary - taxedSlice;
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
    if ( ! this.frozen.get() ) {
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
    synchronized(this.frozen) {
      this.frozen.set(true);
      this.frozen.notifyAll();
    }
  }

  int getCharity() {
    synchronized(this.frozen) { 
      while ( ! this.frozen.get() ) {
        try { this.frozen.wait(); }
        catch (InterruptedException e) { throw new Error(e); }
      }
    }
    return this.charity;
  }

  public static void main(String[] args) {
    TaxServer server = new TaxServer();
    TaxChecker tc = new TaxChecker("TaxChecker");
    Charity charity = new Charity(server);

    TaxRecord tr1 = new TaxRecord(50000);
    server.registerTaxRecord(tr1);
    tr1.registerChecker(tc);
    TaxPayer alice = new TaxPayer("Alice", tr1, TaxPayer.Scenario.UNDER, 0, 0);

    TaxRecord tr2 = new TaxRecord(75000);
    server.registerTaxRecord(tr2);
    tr2.registerChecker(tc);
    TaxPayer bob = new TaxPayer("Bob", tr2, TaxPayer.Scenario.OVER, 0, 0);

    charity.start();
    alice.start();
    bob.start();
  }
}

class MyEditableBoolean {
  private volatile boolean value = false;
  MyEditableBoolean(boolean b) { value = b; }
  synchronized boolean get() { return value; }
  synchronized void set(boolean b) { value = b; }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
