/**
 * This class implements a group and its possible actions.
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

      EncoverTests.observableByAgent(newMember.getName(), "You are added to the Group "+ this.getName() +"!");
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

          EncoverTests.observableByAgent(member.getName(), "You are removed from Group "+ this.getName() +"!");
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
    if (isMember(requester))
    {
      for (int i=0; i<nbMembers; i++)
      {
          EncoverTests.observableByAgent(requester.getName(), membersList[i].getName());
      }
    }
    else
    {
      EncoverTests.observableByAgent(requester.getName(), "Only members can see the list of members!");
    }

  }
}
