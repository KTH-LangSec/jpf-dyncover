/*
 * Copyright (C) 2021 Amir M. Ahmadian
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

/**
 * This class implements the tests to be run by Encover.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package SocialNetwork;

class EncoverTests 
{

  public static void simplestTest(String alicePost) 
  {
    //setPolicy("alicePost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);
    

    Post alice_post_1 = new Post("Something for all");
    alice.postToFollowers(alice_post_1);
    //alice.block(eve);

    Post alice_post_2 = new Post(alicePost);
    alice.postToFollowers(alice_post_2);
  }

  public static void simplestTestTwo(String publicPost, String privatePost) 
  {
    setPolicy("publicPost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);
    

    Post alice_post_1 = new Post(publicPost);
    alice.postToFollowers(alice_post_1);
    //alice.block(eve);

    Post alice_post_2 = new Post(privatePost);
    alice.postToFollowers(alice_post_2);
  }

  public static void forwardingDM(String privatePost) 
  {
    //setPolicy("privatePost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);

    eve.follow(bob);
    
    alice.block(eve);

    Post alice_post = new Post(privatePost);
    alice.sendDM(bob, alice_post);
    alice.postToFollowers(alice_post);

    bob.leakDM();
  }

  public static void hidePhoneNumber(int alicePhoneNumber) 
  {
    setPolicy("alicePhoneNumber");

    Server server = new Server();
    User alice = server.createUser("Alice", alicePhoneNumber);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);

    alice.setNumberPrivacy(true);
    setPolicy(" ");

    observableByAgent(eve.getName(), alice.getNumber());
  }


  public static void leakMembership(String aliceName) 
  {
    setPolicy("aliceName");

    Server server = new Server();
    User alice = server.createUser(aliceName, 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    Group group1 = server.createGroup(alice, "group1");
    group1.addMember(alice, bob);

    alice.setMembershipPrivacy(true);
    setPolicy(" ");

    group1.seeMembers(eve);
  }

  public static void main(String[] args) 
  {
    //EncoverTests.simplestTest("HelloWorld");
    //EncoverTests.simplestTestTwo("HelloWorld", "Secret");
    //EncoverTests.forwardingDM("Secret");
    //EncoverTests.hidePhoneNumber(42);
    EncoverTests.leakMembership("Alice");
  }

  /////////////////////////////////////////////////////////////////
  /////////////////////// Helper Methods //////////////////////////
  /////////////////////////////////////////////////////////////////

  /**
   * Outputs @param out such that it is only observable by @param agent
   *
  */ 
  public static void observableByAgent(String agent, String out) 
  {
    System.out.println(agent + " observes '" + out + "'.");
  }

  /**
   * Outputs @param out such that it is only observable by @param agent
   *
  */ 
  public static void observableByAgent(String agent, int out)
  {
    System.out.println(agent + " observes '" + out + "'.");
  }

  /**
   *  Sets the inputs in @param policy as leaked
  */ 
  public static void setPolicy(String policy) {};

}