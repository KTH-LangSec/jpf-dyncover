/**
 * This class implements a user and its possible actions.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package SocialNetwork;

class User 
{
  private String name;
  
  private int number;


  private boolean hideNumber = false;  
  private boolean hideMembership = false;  
  private boolean hideFollowing = false;  

  private static final int MAX = 100;

  private User[] followerList;
  private int nbFollowers;

  private User[] blockedList;
  private int nbBlockedUsers;

  private Post[] inboxList;
  private int nbInbox;
	
  User(String name, int number) 
  {
    this.name = name;
    this.number = number;

    this.followerList = new User[User.MAX];
    this.nbFollowers = 0;
    this.blockedList = new User[User.MAX];
    this.nbBlockedUsers = 0;
    this.inboxList = new Post[User.MAX];
    this.nbInbox = 0;
  }
	
  public String getName() 
  {
    return this.name;
  }

  public int getNumber() 
  {
    if (hideNumber)
      return 0;
    else
      return this.number;
  }

  ////////////////////////////////////// Privacy Settings //////////////////////////////////////
  public void setNumberPrivacy(boolean setting)
  {
    hideNumber = setting;
  }

  ////////////////////////////////////// Posting ////////////////////////////////////// 
  public void postToFollowers(Post post) 
  {
    for (int i=0; i<nbFollowers; i++)
    {
      EncoverTests.observableByAgent(followerList[i].getName(), post.getText());
    }
  }

  ////////////////////////////////////// DM //////////////////////////////////////
  public void viewDM()
  {
    EncoverTests.observableByAgent(this.getName(), popInbox().getText());
  }


  public void getDM(User sender, Post post)
  {
    if (!isBlocked(sender))
    {
      this.pushInbox(post);
    }
    else
    {
      System.out.println(sender.getName() + " has blocked " + this.getName() + " and will no recieve its DM.");
    }
  }

  public void sendDM(User receiver, Post post)
  {
    if (!isBlocked(receiver))
    {
      receiver.getDM(this, post);
    }
    else
    {
      System.out.println(this.getName() + " has blocked " + receiver.getName() + " and cannot send it a DM.");
    }
  }

  ////////////////////////////////////// Blocking a User //////////////////////////////////////
  public void block(User user)
  {
    user.unfollow(this); // makes that user to unfollow you
    this.unfollow(user); // unfollows that user

    EncoverTests.observableByAgent(user.getName(), this.getName() + " has blocked you!");

    this.blockedList[this.nbBlockedUsers] = user;  // adds it to the list of blocked users
    this.nbBlockedUsers++;
  }


  ////////////////////////////////////// Following a User //////////////////////////////////////
  public void follow(User target)
  {
    target.followedBy(this);
    EncoverTests.observableByAgent(this.getName(), "You have followed " + target.getName());
  }

  public void followedBy(User follower) 
  {
    if (!isBlocked(follower))
    {
      this.followerList[this.nbFollowers] = follower;
      this.nbFollowers++;
    }
    else
    {
      System.out.println(follower.getName() + " is blocked by "+ this.getName()+ " and cannot follow it.");
    }
  }


  ////////////////////////////////////// Unfollowing a User //////////////////////////////////////
  public void unfollow(User target)
  {
    target.unfollowedBy(this);
    EncoverTests.observableByAgent(this.getName(), "You have unfollowed " + target.getName());
  }

  public void unfollowedBy(User follower)
  {
      for (int i=0; i<nbFollowers; i++)
      {
        if (followerList[i].equals(follower))
        {
          followerList[i] = null;
          for (int j=i+1; j<nbFollowers; j++)
          {
            followerList[j-1] = followerList[j];
            followerList[j] = null;
          }
          nbFollowers--;
        }
      }
  }

  ////////////////////////////////////// Check if a User is Blocked //////////////////////////////////////
  public boolean isBlocked(User user)
  {
    for (int i=0; i<nbBlockedUsers; i++)
    {
      if (blockedList[i].equals(user))
      {
          return true;
      }
    }
    return false;
  }


  ////////////////////////////////////// Inbox //////////////////////////////////////
  public void pushInbox(Post message)
  {
      this.inboxList[this.nbInbox] = message;
      this.nbInbox++;
  }

  public Post popInbox()
  {
    if (inboxList.length != 0)
    {
      Post result = inboxList[0];
      inboxList[0] = null;
      for (int j=1; j<nbInbox; j++)
          {
            inboxList[j-1] = inboxList[j];
            inboxList[j] = null;
          }
      nbInbox--;
      return result;
    }
    else
    {
      return new Post("No new messages!");
    }
  }


  ///////////////////////////////////////// Malicious Functionalities Available to a User //////////////////////
  public void forwardDM(User user)
  {
    Post newDM = popInbox();
    EncoverTests.observableByAgent(user.getName(), newDM.getText());
  }

  public void leakDM()
  {
    Post newDM = popInbox();
    this.postToFollowers(newDM);
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
