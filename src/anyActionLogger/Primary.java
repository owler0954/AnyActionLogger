package anyActionLogger;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
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
    public static boolean isLogging = false;
    public static boolean saved = false;
    //Vote kick vars
    static int votes = 0;
    static Timer.Task task = null;
    static Player target;
    static Player starter;
    static ArrayList<Player> voters = new ArrayList<>();
    //swear wars
    static ArrayList<String> swear = new ArrayList<>();
    //other vars

    public Primary() {
        swears();
        Events.on(PlayerConnect.class, event -> {
            Call.infoMessage(event.player.con(),
                    "ДОБРО ПОЖАЛОВАТЬ НА СЕРВЕР\n" +
                            "ШИЗА ШИЗА ШИЗА minigames\n" +
                            "Правила и полезный контент есть на Discord сервере\n" +
                            "https://discord.gg/Efya9AUmf2\n" +
                            "Обращаю ваше внимание на то что этот сервер никак не связан с разработчиком игры!\n\n" +
                            "Welcome to server\n" +
                            "ShizaShizaShiza minigames\n" +
                            "Rules and info there:\n" +
                            "https://discord.gg/Efya9AUmf2\n" +
                            "This server not owned by game developer!"
            );
        });
        Events.on(PlayerChatEvent.class, event -> {
            if (!isLogging || swear == null) {
                return;
            }
            for (String s : swear) {
                if (event.message.contains(s)) {
                    log(0, event.player.name() + " : " + event.message);
                    return;
                }
            }
        });
        Events.on(BlockBuildEndEvent.class, event -> {
            if (event.unit.getPlayer() == null || !isLogging) {
                return;
            }
            if (event.breaking) {
                log(2, event.unit.getPlayer().name() + " destroy " + event.tile.block().name + " at " + event.tile.x + " " + event.tile.y);
            }
            if (!event.breaking) {
                log(1, event.unit.getPlayer().name() + " build " + event.tile.block().name + " at " + event.tile.x + " " + event.tile.y);
            }
        });
        Events.on(ServerLoadEvent.class, event -> {
            isLogging = true;
            logs = new ArrayList[4];
            netServer.admins.addActionFilter(action -> {
                if (action.player == target || action.player == starter) {
                    return false;
                }
                return true;
            });
            logs[0] = new ArrayList<String>();//messages
            logs[1] = new ArrayList<String>();//build
            logs[2] = new ArrayList<String>();//destroy
            logs[3] = new ArrayList<String>();//votekick
        });
        Events.on(WorldLoadEvent.class, e -> {
            saved = false;
            isLogging = true;
        });
        Events.on(GameOverEvent.class, event -> {
            isLogging = false;
            writeLog();
            logs[0] = new ArrayList<String>();//messages
            logs[1] = new ArrayList<String>();//build
            logs[2] = new ArrayList<String>();//destroy
            logs[3] = new ArrayList<String>();//votekick
        });
    }

    public static void log(int type, String text) {
        try {
            logs[type].add("[" + new Date() + "] " + text + "\n");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public static void writeLog() {
        try {
            if (saved) {
                return;
            }
            saved = true;
            System.out.println("Start log save");
            if (logs[0].size() < 1 && logs[1].size() < 1 && logs[2].size() < 1 && logs[3].size() < 1) {
                Log.err("no actions found");
                return;
            }
            String date = "" + new Date().toString().replace(' ', '-').replace(':', '-');
            File path = new File("config/aal/" + date);
            path.mkdir();
            msgLog = new File("config/aal/" + date + "/messageLog.txt");
            buildLog = new File("config/aal/" + date + "/buildLog.txt");
            destroyLog = new File("config/aal/" + date + "/destroyLog.txt");
            voteKickLog = new File("config/aal/" + date + "/voteKickLog.txt");
            FileWriter f = new FileWriter(msgLog, false);
            for (String s : logs[0]) {
                f.append(s);
            }
            System.out.print(1);
            f.flush();
            f = new FileWriter(buildLog, false);
            for (String s : logs[1]) {
                f.append(s);
            }
            f.flush();
            System.out.print(2);
            f = new FileWriter(destroyLog, false);
            for (String s : logs[2]) {
                f.append(s);
            }
            f.flush();
            System.out.print(3);
            f = new FileWriter(voteKickLog, false);
            for (String s : logs[3]) {
                f.append(s);
            }
            f.flush();
            System.out.print(4);
            f.close();
            System.out.println("logged done");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void swears() {
        swear = ByteCode.getSwears();
    }
}
