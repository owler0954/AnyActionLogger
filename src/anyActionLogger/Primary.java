package anyActionLogger;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.game.EventType.*;
import mindustry.net.Administration;
import mindustry.net.Packets;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

import static mindustry.Vars.*;

public class Primary extends Plugin {
    //log wars
    static File msgLog;
    static File buildLog;
    static File destroyLog;
    static File voteKickLog;
    static File configLog;
    static ArrayList<String>[] logs = new ArrayList[5];
    public static boolean isLogging = false;
    public static boolean saved = false;
    public static ArrayList<SocketConnector.SendedPackage> netLogs = new ArrayList<>();
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
        Timer.schedule(() -> {
            for (SocketConnector.SendedPackage s : netLogs) {
                new SocketConnector(s);
            }
            netLogs = new ArrayList<>();
        }, 0, 60);
        try {
            swears();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Call.infoMessage(event.player.con(),
                    "Партнеры:\n" +
                            "pandorum.su:8000 - сервер Mindustry\n" +
                            "Obvilionnetwork.ru | Комплекс серверов Minecraft и Mindustry\n"
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
        Events.on(ConfigEvent.class, event -> {
            if (event.player == null || event.tile == null || event.value == null) {
                return;
            }
            log(4, event.player.name() + " configure " + event.tile.block().name + " at " + event.tile.x + " " + event.tile.y + " to " + event.value);
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
            logs = new ArrayList[5];
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
            logs[4] = new ArrayList<String>();//configure
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
            logs[4] = new ArrayList<String>();//configure
        });
        Events.on(PlayerBanEvent.class, e -> {
            netLogs.add(new SocketConnector.SendedPackage("Ban", "не указано", e.player.name(), Administration.Config.valueOf("name").get().toString(), "Не указано", 0, new Date()));
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
            if (logs[0].size() < 1 && logs[1].size() < 1 && logs[2].size() < 1 && logs[3].size() < 1 && logs[4].size() < 1) {
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
            configLog = new File("config/aal/" + date + "/configLog.txt");
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

            f = new FileWriter(configLog, false);
            for (String s : logs[4]) {
                f.append(s);
            }
            f.flush();
            System.out.print(5);

            f.close();
            System.out.println();
            System.out.println("logged done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void swears() throws IOException {
        FileReader r = new FileReader("config/aal/swears.txt");
        BufferedReader reader = new BufferedReader(r);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            swear.add(line);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("tempban", "<min> <ip> <reason...>", "tempban", args -> {
            int time = Integer.parseInt(args[0]) * 60 * 1000;
            if (time == 0) {
                time = 3600 * 1000;
            }
            Player tgt = Groups.player.find(p -> p.con.address.equals(args[1]));
            for (Player p : Groups.player) {
                if (p.con().address.equals(tgt.con.address)) {
                    p.getInfo().lastKicked = Time.millis() + time;
                    p.kick("ВЫ забанены на " + time / 1000 / 60 + " минут по причине: " + args[2]);
                }
            }
            netLogs.add(new SocketConnector.SendedPackage("Ban", args[2], tgt.name(), Administration.Config.valueOf("name").get().toString(), "Консоль", time, new Date()));
            System.out.println("posted");
            return;
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("tempban", "<min> <ip> <reason...>", "tempban", (args, player) -> {
            if (!player.admin()) {
                return;
            }
            int time = Integer.parseInt(args[0]) * 60 * 1000;
            if (time == 0) {
                time = 3600 * 1000;
            }
            Player tgt = Groups.player.find(p -> p.con.address.equals(args[1]));
            for (Player p : Groups.player) {
                if (p.con().address.equals(tgt.con.address)) {
                    p.getInfo().lastKicked = Time.millis() + time;
                    p.kick("ВЫ забанены на " + time / 1000 / 60 + " минут по причине: " + args[2]);
                }
            }
            netLogs.add(new SocketConnector.SendedPackage("Ban", args[2], tgt.name(), Administration.Config.valueOf("name").get().toString(), player.name(), time, new Date()));
            System.out.println("posted");
            return;
        });
        handler.<Player>register("spec", "<ip>", "hentai", (args, player) -> {
            if (!player.admin()) {
                return;
            }
            Player tgt = Groups.player.find(p -> p.con.address.equals(args[0]));
            if (tgt == null) {
                Call.infoMessage(player.con(), "Не найден");
                return;
            }
            tgt.team(Team.all[113]);
        });
        handler.<Player>register("team", "<id>", "hentai", (args, player) -> {
            if (!player.admin()) {
                return;
            }
            Player tgt = player;
            int team = 1;
            try {
                team = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                tgt.sendMessage("Ошибка в преобразовании номера");
            }
            tgt.team(Team.all[team]);
        });
        handler.<Player>register("post", "<ip> <msg...>", "send message to face", (args, player) -> {
            if (!player.admin()) {
                return;
            }
            Player tgt = Groups.player.find(p -> p.con.address.equals(args[0]));
            if (tgt == null) {
                Call.infoMessage(player.con(), "Не найден");
                return;
            }
            Call.infoMessage(tgt.con(), args[1]);
        });
        handler.<Player>register("swipe", "<td/zk>", "Перемезение по серверам", (args, player) -> {
            int port = 0;
            if (args[0].equals("td")) {
                port = 6568;
            }
            if (args[0].equals("zk")) {
                port = 6567;
            }
            if (port == 0) {
                return;
            }
            Call.connect(player.con, "shizashizashiza.ml", port);
        });
    }
}
