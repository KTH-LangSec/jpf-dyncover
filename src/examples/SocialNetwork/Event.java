/**
 * This class implements an event and its possible actions.
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
