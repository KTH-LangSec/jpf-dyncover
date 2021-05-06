/*
 * Copyright (C) 2021 Amir M. Ahmadian
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
 * This class implements a user and its possible actions.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package SocialNetwork;

class Server 
{
  static final int MAX_USERS = 10;
  User[] usersList;
  int nbUsers;
	
  Server()
  {
    this.usersList = new User[Server.MAX_USERS];
    this.nbUsers = 0;
  }

  public User createUser(String name, int number) 
  {
    User newUser = new User(name, number);
    this.usersList[this.nbUsers] = newUser;
    this.nbUsers++;

    return newUser;
  }

  public Group createGroup(User requester, String name) 
  {
    // group list?
    return new Group(this, requester, name);
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
