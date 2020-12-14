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
import java.util.ArrayList;

public class Primary extends Plugin {
    //log wars
    File msgLog;
    File buildLog;
    File destroyLog;
    File voteKickLog;
    ArrayList<String>[] logs = new ArrayList[4];
    public boolean isLogging = false;
    //Vote kick vars
    static int votes = 0;
    static Timer.Task task = null;
    static Player target;
    static Player starter;
    static ArrayList<Player> voters = new ArrayList<>();
    //other vars

    public Primary() {
        Events.on(WorldLoadEvent.class, event -> {
            isLogging = true;
            logs = new ArrayList[4];
            logs[0] = new ArrayList<String>();
            logs[1] = new ArrayList<String>();
            logs[2] = new ArrayList<String>();
            logs[3] = new ArrayList<String>();
        });
        Events.on(GameOverEvent.class, event -> {
            isLogging = false;
            writeLog();
        });
    }

    public void registerClientCommand(CommandHandler handler) {
        handler.<Player>register("votekickBT", "<plaeyr_name...>", "run kick vote", (args, player) -> {
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
            task = Timer.schedule(() -> {
                voteChecker();
            }, 60f);
        });
        handler.<Player>register("voteBT", "<y/n>", "vote for something", (args, player) -> {
            if (voters.contains(player)) {
                Call.infoMessage(player.con, "alreadyVote");
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

    public static void writeLog() {

    }

    public static void voteChecker() {
        if (votes > 2) {
            Call.sendMessage("[orange]Vote passed.[scarlet]" + target.name + "[orange] will be banned from the server for 30 minutes.");
            target.getInfo().lastKicked = Time.millis() + (30 * 1000);
            Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(Packets.KickReason.vote));
        } else {
            Call.sendMessage("[lightgray]Vote failed. Not enough votes to kick[orange]" + target.name + "[lightgray].");
            target = null;
            starter = null;
        }
    }
}
