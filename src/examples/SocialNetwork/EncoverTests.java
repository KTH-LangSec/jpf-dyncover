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
 * This class implements the Social Network test scenarios.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package SocialNetwork;

import java.util.Random;

class EncoverTests 
{
  static Random rand;

  public static void postForFollowers(String alicePost1, String alicePost2) 
  {
    setPolicy("alicePost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);
    eve.follow(bob);

    randomPosts(alice, 10);
    Post alice_post1 = new Post(alicePost1);
    alice.postToFollowers(alice_post1);

    randomPosts(bob, 10);

    setPolicy(" ");
    eve.unfollow(alice);
    
    Post alice_post2 = new Post(alicePost2);
    alice.postToFollowers(alice_post2);
  }

  public static void blockingUser(String alicePost1, String alicePost2) 
  {
    setPolicy("alicePost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);
    eve.follow(bob);

    randomPosts(alice, 10);
    randomPosts(bob, 10);

    Post alice_post1 = new Post(alicePost1);
    alice.postToFollowers(alice_post1);

    setPolicy(" ");
    alice.block(eve);
    
    Post alice_post2 = new Post(alicePost2);
    alice.postToFollowers(alice_post2);
  }

  public static void forwardingDM(String privatePost) 
  {
    setPolicy("privatePost");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);

    randomPosts(alice, 10);
    randomPosts(bob, 10);

    Post alice_post = new Post(privatePost);
    alice.sendDM(bob, alice_post);

    setPolicy(" ");

    bob.forwardDM(eve);
  }

  public static void phoneNumberPrivacy(int alicePhoneNumber) 
  {
    setPolicy("alicePhoneNumber");

    Server server = new Server();
    User alice = server.createUser("Alice", alicePhoneNumber);
    User bob = server.createUser("Bob", 0);
    User eve = server.createUser("Eve", 0);

    bob.follow(alice);
    eve.follow(alice);

    randomPosts(alice, 7);

    alice.setNumberPrivacy(true);
    setPolicy(" ");

    randomPosts(alice, 5);

    observableByAgent(eve.getName(), alice.getNumber());

    alice.setNumberPrivacy(false);
    setPolicy("alicePhoneNumber");

    randomPosts(alice, 6);

    observableByAgent(eve.getName(), alice.getNumber());
  }


  public static void leakMembership(String bobName) 
  {
    setPolicy("bobName");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser(bobName, 0);
    User charlie = server.createUser("Charlie", 0);
    User eve = server.createUser("Eve", 0);

    Group groupA = server.createGroup(alice, "groupA");
    groupA.addMember(alice, bob);
    groupA.addMember(alice, charlie);

    eve.follow(alice);
    randomPosts(groupA, alice, 10);

    setPolicy(" ");

    groupA.seeMembers(eve);

    groupA.addMember(alice, eve);
    setPolicy("bobName");

    randomPosts(groupA, alice, 12);

    groupA.seeMembers(eve);
  }

  public static void leakMembership_leave(String bobName) 
  {
    setPolicy("bobName");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser(bobName, 0);
    User charlie = server.createUser("Charlie", 0);
    User eve = server.createUser("Eve", 0);

    Group groupA = server.createGroup(alice, "groupA");
    groupA.addMember(alice, bob);
    groupA.addMember(alice, charlie);

    eve.follow(alice);
    randomPosts(groupA, alice, 10);

    setPolicy(" ");

    groupA.seeMembers(eve);

    groupA.addMember(alice, eve);
    setPolicy("bobName");

    randomPosts(groupA, alice, 12);

    groupA.seeMembers(eve);

    groupA.removeMember(alice, eve);
    setPolicy(" ");

    groupA.seeMembers(eve);
  }

  public static void leakEventInfo(String eventName, int eventDate) 
  {
    setPolicy(" ");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User charlie = server.createUser("Charlie", 0);
    User eve = server.createUser("Eve", 0);

    Event bDayEvent = server.createEvent(alice, eventName, eventDate);
    bDayEvent.addParticipant(alice, bob);
    bDayEvent.addParticipant(bob, charlie);

    eve.follow(alice);
    eve.follow(bob);

    randomPosts(bDayEvent, alice, 7);
    randomPosts(bDayEvent, alice, 4);

    setPolicy(" ");

    observableByAgent(eve.getName(), bDayEvent.getName(eve));
    observableByAgent(eve.getName(), bDayEvent.getDate(eve));
  }


  public static void leakEventInfo_mistake(String eventName, int eventDate) 
  {
    setPolicy(" ");

    Server server = new Server();
    User alice = server.createUser("Alice", 0);
    User bob = server.createUser("Bob", 0);
    User charlie = server.createUser("Charlie", 0);
    User eve = server.createUser("Eve", 0);

    Event bDayEvent = server.createEvent(alice, eventName , eventDate);
    bDayEvent.addParticipant(alice, bob);
    bDayEvent.addParticipant(bob, charlie);

    eve.follow(alice);
    eve.follow(bob);

    randomPosts(bDayEvent, alice, 7);
    randomPosts(bDayEvent, alice, 4);

    setPolicy(" ");

    observableByAgent(eve.getName(), bDayEvent.getName(eve));
    observableByAgent(eve.getName(), bDayEvent.getDate(eve));

    bDayEvent.addParticipant(bob, eve);

    observableByAgent(eve.getName(), bDayEvent.getName(eve));
    observableByAgent(eve.getName(), bDayEvent.getDate(eve));
  }

  public static void main(String[] args) 
  {
    rand = new Random();
    rand.setSeed(System.currentTimeMillis());

    String[] params = args[0].split(" ");

    int testNb = Integer.parseInt(params[0]);
    switch (testNb) {
    case 1: 
      EncoverTests.postForFollowers("HelloWorld", "SecretMessage");
      break;
    case 2: 
      EncoverTests.blockingUser("HelloWorld", "SecretMessage");
      break;
    case 3: 
      EncoverTests.forwardingDM("SecretDM");
      break;
    case 4: 
      EncoverTests.phoneNumberPrivacy(42);
      break;
    case 5: 
      EncoverTests.leakMembership("Bob");
      break;
    case 6: 
      EncoverTests.leakMembership_leave("Bob");
      break;
    case 7: 
      EncoverTests.leakEventInfo("Eve Birthday", 25);
      break;
    case 8: 
      EncoverTests.leakEventInfo_mistake("Eve Birthday", 25);
      break;
    }
  }


  /////////////////////////////////////////////////////////////////
  /////////////////////// Helper Methods //////////////////////////
  /////////////////////////////////////////////////////////////////
  private static void randomPosts(User user, int MAX)
  {
    //MAX = rand.nextInt(10) + 1;
    int i = 0;
    while (i < MAX)
    {
      Post temp_post = new Post(generateString());
      user.postToFollowers(temp_post);
      i++;
    }
  }

  private static void randomPosts(Group group, User user, int MAX)
  {
    //MAX = rand.nextInt(10) + 1;
    int i = 0;
    while (i < MAX)
    {
      Post temp_post = new Post(generateString());
      group.post(user, temp_post);
      i++;
    }
  }

  private static void randomPosts(Event event, User user, int MAX)
  {
    //MAX = rand.nextInt(10) + 1;
    int i = 0;
    while (i < MAX)
    {
      Post temp_post = new Post(generateString());
      event.broadcast(user, temp_post);
      i++;
    }
  }

  private static String generateString()
  {
    String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
    char[] text = new char[10];
    for (int i = 0; i < 10; i++)
    {
        text[i] = characters.charAt(rand.nextInt(characters.length()));
    }
    return new String(text);
  }


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
