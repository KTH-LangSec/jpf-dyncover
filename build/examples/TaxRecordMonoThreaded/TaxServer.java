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
 * This is the class implementing the tax server.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package TaxRecordMonoThreaded;

interface TaxServer4charity {
  int getCharity();
}

class TaxServer implements TaxServer4charity {
  static final int MAX_RECORDS = 10;
  TaxRecord[] taxRecords;
  int nbRecords;

  TaxServer() {
    this.taxRecords = new TaxRecord[TaxServer.MAX_RECORDS];
    this.nbRecords = 0;
  }
	
  void registerTaxRecord(TaxRecord tr) {
    this.taxRecords[this.nbRecords] = tr;
    this.nbRecords++;
  }
	
  public int getCharity() {
    int totalAmount = 0;
    int i = 0;
    while ( i < this.nbRecords ) {
      TaxRecord tr = taxRecords[i];
      int amount = tr.getCharity();
      totalAmount = totalAmount + amount;
      i = i + 1;
    }
    return totalAmount;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
