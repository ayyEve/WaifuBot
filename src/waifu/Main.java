package waifu;

// imports
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.Timer;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.listener.message.MessageCreateListener;

// main class
public class Main {
  public static String version;
  public static String dev;
  public static final int saveInterval = 10_000;
  public static final int avatarInterval = 6_000_000;
  
  private static File file = new File("Data.dat");
  public static Map<String, ServerData> data;
  public static String roleName = "waifu";
  public static Message currentMessage;
  public static DiscordAPI api;
  private Timer t;
  
  private static JTextPane tp;
  JFrame f;
    
  public Main() {
    File settingsFile = new File("settings.dat");
    String token = "";
        
    try {
      BufferedReader br = new BufferedReader(new FileReader(settingsFile));
      
      token = br.readLine();
      dev = br.readLine();
      version = br.readLine();
      
      br.close();
    } catch (FileNotFoundException e) {
      // should never occur
    } catch (IOException e){
      printError(e);
    }
    
    api = Javacord.getApi(token, true);
    api.connectBlocking();
    
    MyMessageListener myListener = new MyMessageListener();
    api.registerListener(myListener);
    
    api.setGame("!info");
    
    data = new HashMap<String, ServerData>();
    
    try {
      t = new Timer(saveInterval, new ActionListener() {
        public void actionPerformed(ActionEvent arg0) {
          save();
        }
      });
            
      t.start();
    } catch (Exception e) {
      printError(e);
    }
    
    load();
    
    f = new JFrame("WaifuBot");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    tp = new JTextPane();
    f.setSize(150, 200);
    f.add(tp);
    f.setVisible(true);
  }

  void load() {
    if (!file.exists()) return;
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      while (br.ready()) {
        try {
          String id = br.readLine();
          data.put(id, ServerData.load(br));
        } catch(Exception e) {
          printError(e);
        }
      }

      br.close();
    } catch (FileNotFoundException e) {
      // should never occur
    } catch (IOException e) {
      printError(e);
    }
  }

    void save() {
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (Entry<String, ServerData> entry : data.entrySet()) {
          if (entry.getKey() == null) {
            continue;
          }
          
          // write the server id
          bw.write(entry.getKey()); 
          bw.newLine();

          // write the server data
          entry.getValue().save(bw);
        }
        bw.close();
      } catch (Exception e) {
        Main.printError(e);
      }
  }
  
  public static void print(String s) {
    System.out.println(s);
    tp.setText(tp.getText() + "\n" + s);
  }
  
  public static void printError(Exception e) {
    String stack = "";
    for (StackTraceElement e1 : e.getStackTrace()) {
      stack += e1.getClassName() + "." + e1.getMethodName() + "(" + e1.getFileName() + ":" + e1.getLineNumber() + ")\n";
    }
    
    print(e.getMessage() + "\n" + stack);
    e.printStackTrace();
  }
  
  public static <T> T randomIndex(T[] array) {
    return array[(int)(Math.random()*array.length)];
  }
  
  public class MyMessageListener implements MessageCreateListener {
    
    @Override
    public void onMessageCreate(DiscordAPI api, Message message) {
      if (message.getAuthor().equals(api.getYourself())) 
        return;
      }
      
      // set the current message
      currentMessage = message;
      
      // check if the message is an invite link
      if (message.isPrivateMessage() && message.getContent().contains("https://discord.gg")) {
        try {
          Future<Server> f = api.acceptInvite(message.getContent().replace("https://discord.gg/", ""));
          joinServer(f.get());
          return;
        } catch (Exception e) {
          printError(e);
        }
      }
      
      if (getServerData() == null) {
        print("ERROR: " + getServer().getId());
        print(data.containsKey(getServer().getId()) + "");
        return;
      }
             
      // waifubot cannot accept pms anymore because of the new storage system
      if (message.isPrivateMessage()) {
        message.getAuthor().sendMessage("pms are not accepted, sorry about that");
        return;
      
      
             
        // get the command and arguments
        String[] args = message.getContent().toLowerCase().split(" ");
        String mention = (message.isPrivateMessage() ? "" : message.getAuthor().getMentionTag() + ", ");
        String id = message.getAuthor().getId();
             
        try {
          if (args[0].equals("!info")) {
            sendMessage(mention + (!argCount(args, 0) ? getHelp() : help(args[1])));
            return;
          }
        } catch (Exception e) {
          printError(e);
          return;
        }
                
        if (!args[0].equals(getActivator())) return;
              
        if (args.length < 2) { 
          sendMessage(mention + "no command given, use '!info' for a list of commands");
        }
              
        // ============================= List ========================
        if (args[1].equals("list")) {
          if (argCount(args, 1)) {
            if (!message.getMentions().isEmpty()) {
              id = message.getMentions().get(0).getId();
              sendMessage(mention + message.getMentions().get(0).getName() + " has " + getServerData().list(id).get());
            }
          } else {
            sendMessage(mention + "you have " + getServerData().list(id).get());
          }
        }
              
        // ============================= clear ========================
        if (args[1].equals("clear")) {
          if (args.length > 2) {
            if (isMod()) {
              getServerData().clear(message.getMentions().get(0).getId());
              sendMessage(mention + "has just cleared " + message.getMentions().get(0).getName() + "'s list");
            } else {
               sendMessage(mention + "you don't have permission to clear other's lists!");
            }
          } else {
            getServerData().clear(id);
            sendMessage(mention + "your list has been cleared!");
          }
        }
                
        // ============================= add ==========================
        if (args[1].equals("add")) { 
          String name = "";
          
          if (message.getMentions().size() > 0) {
            sendMessage(mention + "you cannot add other people!");
            return;
          } else {
            try {
              for (int i = 2; i < args.length; i++) {
                name += fixName(args[i]) + (i < args.length - 1 ? " " : "");
              }
            } catch (Exception e) {
              printError(e);
            }
          }
                  
          switch (getServerData().add(id, name))  {
            // someone already has
            case 0: 
              sendMessage(mention + name + " is unavailable!"); 
              break;
                     
            // added
            case 1: 
              sendMessage(mention + "has added " + name + " to their " + getGroupText() + "!"); 
              break;
                      
            // needs to wait
            case 2: 
              sendMessage(mention + "you must wait " + (getTimeout() / 1_000) + " seconds before adding another " + getNameText() + "!"); 
              break;
          }
        }
           
        // ============================= remove =========================
        if (args[1].equals("remove") || args[1].equals("rem")) {
          String name = "";
                
          if (message.getMentions().size() > 0) {
            name = message.getMentions().get(0).getName();
          }  else  {
            try {
              for (int i = 2; i < args.length; i++) {
                name += fixName(args[i]) + (i < args.length - 1 ? " " : "");
              }
            } catch (Exception e) {
              printError(e);
            }
                   
            if (getServerData().remove(id, name)) { 
               sendMessage(mention + "has removed " + name + " from their " + getGroupText() + "!");
            } else {
              sendMessage(mention + name + " is not in your " + getGroupText() + "!");
            }
          }
        }
           
        // ============================= remove =========================
        if (args[1].equals("test")) {
          sendMessage(mention + "you are " + (isMod() ? " " : "not ") + "a mod");
        }
           
        // moderator commands
        if (isMod())  {
          // ============================= timeout =========================
          if (args[1].equals("timeout")) {
            if (argCount(args, 1)) {
              try {
                getServerData().timeout = Integer.parseInt(args[2]);
                sendMessage(mention + "has set the timeout to " + getServerData().timeout + "ms (" +  (getServerData().timeout / 1_000) + " seconds)");
              } catch (Exception e) {
                // not a number, or too big for an integer
                sendMessage(mention + args[2] + " is not a valid number");
              }
            }
          }
            
          // ============================= activator =========================
          if (args[1].equals("activator")) {
            if (argCount(args, 1)) { 
              getServerData().activator = args[2];
              sendMessage(mention + "has set the activator to " + args[2] + ", you must now use " + args[2] + " <command>");
            }
          }
             
          // ============================= setgroup =========================
          if (args[1].equals("setgroup")) {
            if (argCount(args, 1)) {
              getServerData().groupText = args[2];
              sendMessage(mention + "has set the group text to " + args[2]);
            }
          }
           
          // ============================= setname =========================
          if (args[1].equals("setname")) {
            if (argCount(args, 1)) {
              getServerData().nameText = args[2];
              sendMessage(mention + "has set the name to " + args[2]);
            }
          }
                
          // ============================= polygamy =========================
          if (args[1].equals("polygamy")) {
            if (argCount(args, 1)) {
              try {
                getServerData().allowPolygamy = parseBoolean(args[2]);
                sendMessage(getServerData().allowPolygamy ? "This server now allows polygamy!" : "This server no longer allows polygamy!");
              } catch (Exception e) {
                // shouldnt happen but who knows
              }
            }
          }
       
          // ============================= removefrom =========================
          if (args[1].equals("removefrom") || args[1].equals("remf")) {
            String name = "";
            
            // arg[2]
            User u = message.getMentions().get(0);
            
            if (message.getMentions().size() > 1) {
              name = message.getMentions().get(1).getName();
            } else {
              try {
                for (int i = 3; i < args.length; i++) {
                  name += fixName(args[i]);
                }
              } catch (Exception e) {
                printError(e);
              }
            }
               
            if (getServerData().remove(id, name)) {
              sendMessage(mention + "has removed " + name + " from " + u.getName() + "'s " + getGroupText() + "!");
            } else {
              sendMessage(mention + name + " is not in " + u.getName() + "'s " + getGroupText() + "!");
            }
          }
        }
      }
    }
    
    public boolean parseBoolean(String b) throws Exception {
      if (b.equals("true")) return true;
      if (b.equals("false")) return false;
      throw new Exception();
    }

    public void sendMessage(String message) {
      currentMessage.getChannelReceiver().sendMessage(message);
    }
    
    private String getHelp() {
      String h = getActivator() + " ";
      String w = getNameText();

      return "Developed by " + dev + ", version " + version + "\n"
           + " Here are your commands: :wink:" + "\n\n"
           + "add         : Add a " + w + ". (" + h + "add <name>)" + "\n"
           + "list          : Show what " + w + " you have (" + h + "list) or (" + h + "list @username)" + "\n"
           + "clear       : Removes all " + w + " ;w;w; (" + h + "clear)" + "\n"
           + "info         : Show this command list (" + h + "info <command>)" + "\n"
           + "remove  : Remove a " + w + " ;w; (" + h + " remove <name> <last name>) or (" + h + "remove <name>)"  + "\n"
           + "rem         : Same as remove" + "\n"
           + "test     : tests whether or not you are a mod (" + h + "test)" + "\n"
                 
           // moderator commands
           + "\n\nadmin/mod only commands" + "\n"
           + "timeout : Sets the timeout for adding " + w + " (" + h + "timeout <timeout in ms>)" + "\n"
           + "activator : sets the activator to use for commands. ie !waifu or ?taco (" + h + "activator <activator>)" + "\n"
           + "setgroup : sets the group text. ie harem or friends (" + h + "setgroup <name>)" + "\n"
           + "setname : sets the name to use. ie waifu or friend (this needs a new name) (" + h + "setname <name>)" + "\n"
           + "polygamy : allows multiple people to have the same name in their group (" + h + "polygamy <true/false>" + "\n"
           + "removefrom  : Remove a " + w + " from <@user> ;w; (" + h + "removefrom <@user> <name>)" + "\n"
           + "remf : same as removefrom" + "\n"
           + "clear : clears a user's list (" + h + "clear <@user>)" + "\n"
           + "setrole : sets the role needed to change waifubot settings (" + h + "setrole <new role name>)" + "\n"
           + "to use admin/mod commands you must have the **" + roleName + "** role";
    }
    
    private String help(String command) {
      String h = getNameText();
      if (command.equals("info"))   return "info <command>: gets information on <command>";
      if (command.equals("add"))    return "add <first>: adds <first> to your " + h + "\nadd <first> <last>: adds <first> <last> to your " + h + "";
      if (command.equals("list"))   return "list: view your " + h + "\nlist <@person>: view @person's " + h + "";
      if (command.equals("clear"))  return "clear: clears your " + h + "";
      if (command.equals("remove")) return "remove <first> <last>: removes <first> <last> from  your " + h + "\nremove <first> removes <first> from  your " + h + "";
        
      // mod controls
      if (command.equals("addmod"))    return "addmod <@name>: adds <@name> to the list of mods who can edit waifubot settings";
      if (command.equals("timeout"))   return "timeout <time>: sets the timeout length in ms to <time>";
      if (command.equals("ignore"))    return "ignore: Ignores messages from the channel this was sent from";
      if (command.equals("activator")) return "activator <activator>: sets the activator to use for commands ie !waifu or !taco";
      if (command.equals("setgroup"))  return "setgroup <name>: sets the group text ie harem or friends";
      if (command.equals("setname"))   return "setname <name>: sets the name to use ie waifu or friend (this needs a new name)";
      if (command.equals("polygamy"))   return "polygamy <true/false>: allows multiple people to have the same name in their group";
        
      return "unknown command " + command;
    }

    // getters
    private static Server getServer() {
      return currentMessage.getChannelReceiver().getServer();
    }
    public static ServerData getServerData() {
      if (!data.containsKey(getServer().getId())) {
        data.put(getServer().getId(), new ServerData());
      }
      //if (data.get(key))
      return data.get(getServer().getId());
    }
    public static String getNameText() {
      return getServerData().nameText;
    }
    public static String getActivator() {
      return getServerData().activator;
    }
    public static int getTimeout() {
      return getServerData().timeout;
    }
    public static String getGroupText() {
      return getServerData().groupText;
    }
    
    public static boolean isMod() {
      // if the author is me (Eve)
      if (currentMessage.getAuthor().getId().equals("138765720614993920")) {
        return true;
      }
        
      // if the author is in a role called "administrators"
      for (Role r : currentMessage.getAuthor().getRoles(getServer())) {
          if (r.getName().toLowerCase().equals(roleName)) {
            return true;
          }
      }
    
      return false;
    }
    
    private String fixName(String a) {
      if (a.length() == 1) return a.toUpperCase();
        
      String a1 = "";
      // go through the text and only add letters to the string
      for (Character c : a.toCharArray()) {
        if (Character.isLetter(c)) {
          a1 += c;
        }
      }
      return a1.toUpperCase().substring(0, 1) + a1.substring(1);
    }
    private boolean argCount(String[] args, int needed) {
      return (args.length == needed + 2);
    }

    public void joinServer(Server s) {
      data.put(s.getId(), new ServerData());
    }

    public static void main(String... args) {
      new Main();
    }
}
