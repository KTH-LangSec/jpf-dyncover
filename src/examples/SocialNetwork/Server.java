/**
 * This class implements the server
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

  public Event createEvent(User requester, String name, int date) 
  {
    // events list?
    return new Event(this, requester, name, date);
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
