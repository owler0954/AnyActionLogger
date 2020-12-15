package anyActionLogger;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.game.EventType.*;
import mindustry.net.Packets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static mindustry.Vars.netServer;
import static mindustry.Vars.world;

public class Primary extends Plugin {
    //log wars
    static File msgLog;
    static File buildLog;
    static File destroyLog;
    static File voteKickLog;
    static ArrayList<String>[] logs = new ArrayList[4];
    public boolean isLogging = false;
    //Vote kick vars
    static int votes = 0;
    static Timer.Task task = null;
    static Player target;
    static Player starter;
    static ArrayList<Player> voters = new ArrayList<>();
    //other vars

    public Primary() {
        netServer.admins.addActionFilter(action -> {
            if (action.player == target || action.player == starter) {
                return false;
            }
            return true;
        });
        Events.on(WorldLoadEvent.class, event -> {
            isLogging = true;
            logs = new ArrayList[4];
            logs[0] = new ArrayList<String>();//messages
            logs[1] = new ArrayList<String>();//build
            logs[2] = new ArrayList<String>();//destroy
            logs[3] = new ArrayList<String>();//votekick
        });
        Events.on(GameOverEvent.class, event -> {
            isLogging = false;
            try {
                writeLog();
            } catch (IOException e) {
                //TODO сменить на лог еррор
                e.printStackTrace();
            }
        });
    }

    public void registerClientCommand(CommandHandler handler) {
        handler.<Player>register("votekick", "<plaeyr_name...>", "run kick vote", (args, player) -> {
            if (target != null) {
                Call.infoMessage(player.con, "alreadyGo");
                return;
            }
            if (Groups.player.size() < 3) {
                Call.infoMessage(player.con, "notEnoughtPlayers");
                return;
            }
            for (Player p : Groups.player) {
                if (p.name().equals(args[0])) {
                    target = p;
                    starter = player;
                    break;
                }
            }
            if (target == null) {
                Call.infoMessage(player.con, "notFound");
                return;
            }

            voters = new ArrayList<>();
            log(3, "Started by " + starter.name() + ". Target: " + target.name());
            task = Timer.schedule(() -> {
                voteChecker();
            }, 60f);
        });
        handler.<Player>register("vote", "<y/n>", "vote for something", (args, player) -> {
            if (voters.contains(player) || player == starter) {
                Call.infoMessage(player.con, "alreadyVote");
                return;
            }
            if (player == target) {
                Call.infoMessage(player.con, "noVoteForYouSelf");
                return;
            }
            if (args[0].equalsIgnoreCase("y")) {
                votes++;
                voters.add(player);
                Call.sendMessage("[lightgray]" + player.name() + "[lightgray] has voted on kicking[orange]" + target.name + "[].[accent] (" + votes + "/3)\n[lightgray]Type[orange] /vote <y/n>[] to agree.");
                return;
            }
            if (args[0].equalsIgnoreCase("n")) {
                votes--;
                voters.add(player);
                Call.sendMessage("[lightgray]" + player.name() + "[lightgray] has voted on kicking[orange]" + target.name + "[].[accent] (" + votes + "/3)\n[lightgray]Type[orange] /vote <y/n>[] to agree.");
                return;
            }
            player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
        });
    }

    public static void log(int type, String text) {
        try {
            logs[type].add("[" + new Date() + "] " + text + "\n");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public static void writeLog() throws IOException {
        msgLog = new File("config/aal/" + new Date().toString().replace(' ', '-') + "/messageLog.txt");
        buildLog = new File("config/aal/" + new Date().toString().replace(' ', '-') + "/buildLog.txt");
        destroyLog = new File("config/aal/" + new Date().toString().replace(' ', '-') + "/destroyLog.txt");
        voteKickLog = new File("config/aal/" + new Date().toString().replace(' ', '-') + "/voteKickLog.txt");
        FileWriter f = new FileWriter(msgLog);
        for (String s : logs[0]) {
            f.append(s);
        }
        f.flush();
        f = new FileWriter(buildLog);
        for (String s : logs[1]) {
            f.append(s);
        }
        f.flush();
        f = new FileWriter(destroyLog);
        for (String s : logs[2]) {
            f.append(s);
        }
        f.flush();
        f = new FileWriter(voteKickLog);
        for (String s : logs[3]) {
            f.append(s);
        }
        f.flush();
        f.close();
    }

    public static void voteChecker() {
        if (votes > 2) {
            Call.sendMessage("[orange]Vote passed.[scarlet]" + target.name + "[orange] will be banned from the server for 30 minutes.");
            target.getInfo().lastKicked = Time.millis() + (30 * 1000);
            log(3, "[Succsess vote]Started by " + starter.name() + ". Target: " + target.name());
            Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(Packets.KickReason.vote));
            target = null;
            starter = null;
        } else {
            Call.sendMessage("[lightgray]Vote failed. Not enough votes to kick[orange]" + target.name + "[lightgray].");
            log(3, "[Fail vote]Started by " + starter.name() + ". Target: " + target.name());
            target = null;
            starter = null;
        }
    }
}
