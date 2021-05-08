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

class Group 
{
  private String name;
  private User admin;

  static final int MAX_MEMBERS = 10;
  User[] membersList;
  int nbMembers;
	
  Group(Server server, User admin, String name) 
  {
    this.name = name;
    this.admin = admin;

    this.membersList = new User[Group.MAX_MEMBERS];
    this.nbMembers = 0;

    this.addMember(admin, admin);
  }
	
  public String getName()
  {
    return this.name;
  }

  ////////////////////////////////////// Posting ////////////////////////////////////// 
  public void post(User sender, Post post) 
  {
    if (isMember(sender))
    {
      for (int i=0; i<nbMembers; i++)
      {
        EncoverTests.observableByAgent(membersList[i].getName(), post.getText());
      }
    }
    else
    {
      System.out.println("Only memebers can post to a group");
    }
  }


  ////////////////////////////////////// Add/Remove Member ////////////////////////////////////// 
  public void addMember(User requester, User newMember) 
  {
    if (requester == admin)
    {
      this.membersList[this.nbMembers] = newMember;
      this.nbMembers++;
    }
    else
    {
      System.out.println("Only admin can add new members");
    }
  }

  public void removeMember(User requester, User member)
  {
    if (requester == admin)
    {
      for (int i=0; i<nbMembers; i++)
      {
        if (membersList[i] == member)
        {
          membersList[i] = null;
          for (int j=i+1; j<nbMembers; j++)
          {
            membersList[j-1] = membersList[j];
            membersList[j] = null;
          }
          nbMembers--;
        }
      }
    }
    else
    {
      System.out.println("Only admin can remove members");
    }
  }

  public void leaveGroup(User requester)
  {
    for (int i=0; i<nbMembers; i++)
    {
      if (membersList[i] == requester)
      {
        membersList[i] = null;
        for (int j=i+1; j<nbMembers; j++)
        {
          membersList[j-1] = membersList[j];
          membersList[j] = null;
        }
        nbMembers--;
      }
    }
  }


  ////////////////////////////////////// Check if a User is a Member //////////////////////////////////////
  private boolean isMember(User user)
  {
    for (int i=0; i<nbMembers; i++)
    {
      if (membersList[i].equals(user))
      {
          return true;
      }
    }
    return false;
  }

  public void seeMembers(User requester)
  {
    for (int i=0; i<nbMembers; i++)
    {
      EncoverTests.observableByAgent(requester.getName(), membersList[i].getPolicyRespectingInfo());
    }
  }
}
