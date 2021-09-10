/**
 * This class implements a post and its possible actions.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package SocialNetwork;

class Post 
{
  private String postText;
	
  Post(String text) 
  {
    this.postText = text;
  }
	
  public String getText() 
  {
    return this.postText;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
