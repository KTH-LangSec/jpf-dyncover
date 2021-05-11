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

class Event 
{
  private String name;
  private User creator;
  private int date;

  static final int MAX_PARTICIPANTS = 10;
  User[] participantsList;
  int nbParticipants;
	
  Event(Server server, User creator, String name, int date) 
  {
    this.creator = creator;
    this.name = name;
    this.date = date;

    this.participantsList = new User[Event.MAX_PARTICIPANTS];
    this.nbParticipants = 0;

    this.participantsList[this.nbParticipants] = creator;
    this.nbParticipants++;
  }
	
  public String getName(User requester)
  {
    if (isParticipant(requester))
    {
      return this.name;
    }
    else
    {
      return "Private Event!!, you are not allowed to see this event";
    }
  }

  public int getDate(User requester)
  {
    if (isParticipant(requester))
    {
      return this.date;
    }
    else
    {
      return 0;
    }
  }

  ////////////////////////////////////// Broadcasting ////////////////////////////////////// 
  public void broadcast(User sender, Post post) 
  {
    if (isParticipant(sender))
    {
      for (int i=0; i<nbParticipants; i++)
      {
        EncoverTests.observableByAgent(participantsList[i].getName(), post.getText());
      }
    }
    else
    {
      System.out.println("Only memebers can broadcast something in an event");
    }
  }


  ////////////////////////////////////// Add/Remove Participant ////////////////////////////////////// 
  public void addParticipant(User requester, User newParticipant) 
  {
    if (isParticipant(requester))
    {
      this.participantsList[this.nbParticipants] = newParticipant;
      this.nbParticipants++;
    }
    else
    {
      System.out.println("Only current participants can add new participants to an event!");
    }
  }

  public void leaveEvent(User requester)
  {
    for (int i=0; i<nbParticipants; i++)
      {
        if (participantsList[i] == requester)
        {
          participantsList[i] = null;
          for (int j=i+1; j<nbParticipants; j++)
          {
            participantsList[j-1] = participantsList[j];
            participantsList[j] = null;
          }
          nbParticipants--;
        }
      }
  }


  ////////////////////////////////////// Check if a User is a Participant //////////////////////////////////////
  private boolean isParticipant(User user)
  {
    for (int i=0; i<nbParticipants; i++)
    {
      if (participantsList[i].equals(user))
      {
          return true;
      }
    }
    return false;
  }
}
