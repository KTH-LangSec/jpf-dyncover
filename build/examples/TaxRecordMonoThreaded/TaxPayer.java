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
 * This is the class implementing the possible taxpayer actions.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package TaxRecordMonoThreaded;

class TaxPayer {
  private String name;
  private TaxRecord4taxPayer taxRecord;
	
  TaxPayer(String name, TaxRecord4taxPayer tr) {
    this.name = name;
    this.taxRecord = tr;
    this.taxRecord.setTaxpayer(this);
  }
	
  String getName() {
    return this.name;
  }

  int getTaxes() {
    int taxes = taxRecord.getTaxes();
    System.out.println(this.name + " learns that he/she has to pay " + taxes);
    return taxes;
  }

  int getAmountPayed() {
    int amountPayed = taxRecord.getAmountPayed();
    System.out.println(this.name + " learns that he/she has already payed " + amountPayed);
    return amountPayed;
  }

  int payTaxes(int charity, int amount) {
    System.out.println(this.name + " sets charity amount to " + charity + " and pays " + amount);
    int taxBalance = taxRecord.payTaxes(charity, amount);
    System.out.println(this.name + " learns that he/she has a " + taxBalance + "  tax balance.");
    return taxBalance;
  }

  void payTaxesInFull(int charity) {
    System.out.println(this.name + " pays full taxes with charity amount to " + charity);
    int taxes = this.getTaxes();
    int amountPayed = this.getAmountPayed();
    int balance = (charity + taxes) - amountPayed;
    this.payTaxes(charity, balance);
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
