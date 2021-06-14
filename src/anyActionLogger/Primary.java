package anyActionLogger;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.game.EventType.*;
import mindustry.net.Administration;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    //public static ArrayList<SendedPackage> netLogs = new ArrayList<>();
    //swear wars
    static ArrayList<String> swear = new ArrayList<>();
    //other vars
    static SimpleDateFormat format = new SimpleDateFormat("ddMMyyHHmm");

    public Primary() {
        try {
            swears();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Events.on(PlayerConnect.class, event -> Call.infoMessage(event.player.con(),
                "ДОБРО ПОЖАЛОВАТЬ НА СЕРВЕР\n" +
                        "STORAGE_05\n" +
                        "Правила и полезный контент есть на Discord сервере\n" +
                        "https://discord.gg/ejUvjeDkYv\n" +
                        "Обращем ваше внимание на то что этот сервер никак не связан с разработчиком игры!\n\n" +
                        "Welcome to server\n" +
                        "STORAGE_05\n" +
                        "Rules and info there:\n" +
                        "https://discord.gg/ejUvjeDkYv\n" +
                        "This server not owned by game developer!"
        ));
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
            logs[0] = new ArrayList<>();//messages
            logs[1] = new ArrayList<>();//build
            logs[2] = new ArrayList<>();//destroy
            logs[3] = new ArrayList<>();//votekick
            logs[4] = new ArrayList<>();//configure
        });
        Events.on(WorldLoadEvent.class, e -> {
            saved = false;
            isLogging = true;
        });
        Events.on(GameOverEvent.class, event -> {
            isLogging = false;
            writeLog();
            logs[0] = new ArrayList<>();//messages
            logs[1] = new ArrayList<>();//build
            logs[2] = new ArrayList<>();//destroy
            logs[3] = new ArrayList<>();//votekick
            logs[4] = new ArrayList<>();//configure
        });
        Events.on(PlayerBanEvent.class, e -> new SendedPackage(e.player.name()));
    }

    public static void log(int type, String text) {
        try {
            logs[type].add("[" + new Date() + "] " + text + "\n");
        } catch (ArrayIndexOutOfBoundsException ignored) {
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
            String date = format.format(new Date());
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
            new SendedPackage(args[2], tgt.name(), "Консоль");
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
            new SendedPackage(player.name(), time, args[2], tgt.name());
            System.out.println("posted");
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
        handler.<Player>register("post", "<ip> <msg...>", "message в лицо", (args, player) -> {
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
    }

    public static class SendedPackage {
        private final String ava = "https://i.pinimg.com/originals/2a/8d/a3/2a8da398c052ad3c34311073eb3110a3.jpg";

        public SendedPackage(String reason, String name, String author) {
            Webhook wh = new Webhook("nya");
            wh.setUsername("Банящая кошкодевочка");
            wh.setAvatarUrl(ava);
            wh.addEmbed(new Webhook.EmbedObject().setTitle("БАН").addField("Санитар", author, false).addField("Причина", reason, false).addField("Пациент", name, false).setColor(new Color(110, 237, 139)));
            try {
                wh.execute();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                wh = null;
            }
        }

        public SendedPackage(String name) {
            Webhook wh = new Webhook("nya");
            wh.setUsername("Банящая кошкодевочка");
            wh.setAvatarUrl(ava);
            wh.addEmbed(new Webhook.EmbedObject().setTitle("БАН").addField("Пациент", name, false).setColor(new Color(110, 237, 139)));
            try {
                wh.execute();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                wh = null;
            }
        }

        public SendedPackage(String author, int time, String reason, String target) {
            Webhook wh = new Webhook("nya");
            wh.setUsername("Банящая кошкодевочка");
            wh.setAvatarUrl(ava);
            wh.addEmbed(new Webhook.EmbedObject()
                    .setTitle("БАН")
                    .addField("Админ", author, false)
                    .addField("Причина", reason, false)
                    .addField("Длительность", parseTime(time), false)
                    .addField("Забаненый", target, false)
                    .addField("Сервер", Administration.Config.valueOf("name").get().toString(), false)
                    .setColor(new Color(110, 237, 139)))
            ;
            try {
                wh.execute();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                wh = null;
            }
        }

        private String parseTime(long time) {
            long days = TimeUnit.MILLISECONDS.toDays(time);
            long hours = TimeUnit.MILLISECONDS.toHours(time);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
            StringBuilder builder = new StringBuilder(" ");
            System.out.println(days + " " + hours + " " + minutes);
            if (days != 0) {
                if (days < 0) {
                    days *= -1;
                }
                builder.append(days + " д. ");
            }
            if (hours != 0) {
                if (hours < 0) {
                    hours *= -1;
                }
                hours = hours - days * 24;
                builder.append(hours + " ч. ");
            }
            if (minutes != 0) {
                if (minutes < 0) {
                    minutes *= -1;
                }
                minutes = minutes - (days * 24 * 60);
                minutes = minutes - (hours * 60);
                builder.append(minutes + " м. ");
            }
            return builder.toString();
        }
    }
}
