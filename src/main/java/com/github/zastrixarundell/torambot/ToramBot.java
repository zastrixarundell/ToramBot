/*
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                     Version 2, December 2004
 *
 * Copyright (C) 2019, Zastrix Arundell, https://github.com/ZastrixArundell
 *
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 *
 */

package com.github.zastrixarundell.torambot;

import com.github.zastrixarundell.torambot.commands.HelpCommand;
import com.github.zastrixarundell.torambot.commands.gameinfo.*;
import com.github.zastrixarundell.torambot.commands.player.CookingCommand;
import com.github.zastrixarundell.torambot.commands.crafting.MatsCommand;
import com.github.zastrixarundell.torambot.commands.crafting.ProficiencyCommand;
import com.github.zastrixarundell.torambot.commands.player.LevelCommand;
import com.github.zastrixarundell.torambot.commands.player.PointsCommand;
import com.github.zastrixarundell.torambot.commands.search.items.ItemCommand;
import com.github.zastrixarundell.torambot.commands.search.items.weapons.*;
import com.github.zastrixarundell.torambot.commands.search.monsters.BossCommand;
import com.github.zastrixarundell.torambot.commands.search.monsters.MiniBossCommand;
import com.github.zastrixarundell.torambot.commands.search.monsters.MonsterCommand;
import com.github.zastrixarundell.torambot.commands.search.monsters.NormalMonsterComand;
import com.github.zastrixarundell.torambot.commands.torambot.DonateCommand;
import com.github.zastrixarundell.torambot.commands.torambot.InviteCommand;
import com.github.zastrixarundell.torambot.commands.torambot.SupportCommand;
import com.github.zastrixarundell.torambot.commands.search.items.extra.GemCommand;
import com.github.zastrixarundell.torambot.commands.search.items.extra.UpgradeCommand;
import com.github.zastrixarundell.torambot.commands.search.items.extra.XtalCommand;
import com.github.zastrixarundell.torambot.commands.search.items.gear.AdditionalCommand;
import com.github.zastrixarundell.torambot.commands.search.items.gear.ArmorCommand;
import com.github.zastrixarundell.torambot.commands.search.items.gear.ShieldCommand;
import com.github.zastrixarundell.torambot.commands.search.items.gear.SpecialCommand;

import com.github.zastrixarundell.torambot.commands.torambot.VoteCommand;
import com.github.zastrixarundell.torambot.entities.ToramForumsUser;
import com.github.zastrixarundell.torambot.utils.AESHelper;
import org.discordbots.api.client.DiscordBotListAPI;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.*;

public class ToramBot
{



    public static void main(String[] args)
    {

        if(args.length == 0)
        {
            System.out.println("The token is not specified... shutting down!");
            System.exit(-1);
        }

        if(args.length > 1) { Values.setPrefix(args[1]); }

        System.out.println("Prefix set to: " + Values.getPrefix());

        String token = args[0];
        DiscordApi bot;
        try
        {
            bot = new DiscordApiBuilder().setToken(token).login().join();
        }
        catch (Exception e)
        {
            System.out.println("Error! Is the token correct?");
            return;
        }

        Values.getMavenVersion();

        bot.updateActivity("Starting up! Please wait!");

        updateCount(bot);
        addCommands(bot);

        //vote command is here
        setupDiscordBotListApi(bot);

        //Just to refresh
        updateCount(bot);

        System.out.println("Started! Type in \"stop\" to stop the bot!");

        String input;
        Scanner scanner = new Scanner(System.in);

        Timer activity = updateActivity(bot);
        Timer dyeImage = updateDyesImage(bot, token);

        while(true)
        {
            System.out.print("User input: ");
            input = scanner.nextLine();
            if (input.equalsIgnoreCase("stop"))
            {
                bot.disconnect();
                activity.cancel();
                dyeImage.cancel();
                System.exit(0);
            }
        }
    }

    private static Timer updateActivity(DiscordApi bot)
    {
        Timer timer = new Timer();
        TimerTask task = new TimerTask()
        {

            int status = 0;

            @Override
            public void run()
            {
                updateCount(bot);

                if(Values.getApi() != null)
                    Values.getApi().setStats(bot.getServers().size());

                switch(status)
                {
                    case 0:
                        bot.updateActivity(Values.getPrefix() + "help | " + Values.getUserCount() + " users!");
                        break;
                    case 1:
                        bot.updateActivity(Values.getPrefix() + "invite | " + Values.getGuildCount() + " servers!");
                        break;
                    case 2:
                        Values.getApi().getBot("600302983305101323").whenComplete((bot1, throwable) -> bot.updateActivity(Values.getPrefix() + "vote | " + bot1.getMonthlyPoints() + " votes this month!"));
                }

                status ++;
                status = status % (Values.getApi() != null ? 3 : 2);
            }
        };

        timer.schedule(task, 0, 1000*60);
        return timer;
    }

    private static Timer updateDyesImage(DiscordApi bot, String token)
    {
        Timer timer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    System.out.println("Starting forums user!");
                    ToramForumsUser user = new ToramForumsUser(token);
                    System.out.println("Starting monthly dyes!");
                    user.setDye();
                    System.out.println("Finished monthly dyes!");
                    user.close();

                    if(Values.getDyeImages() == null)
                    {
                        System.out.println("There are monthly dyes!");

                        if(MonthlyCommand.instance != null)
                        {
                            bot.removeListener(MonthlyCommand.instance);
                            MonthlyCommand.instance = null;
                        }
                    }
                    else
                        if(MonthlyCommand.instance == null)
                        {
                            bot.addListener(new MonthlyCommand());
                            System.out.println("Updated monthly dyes!");
                        }
                }
                catch (Exception e)
                {
                    Values.setDyeImages(null);
                    System.out.println("An error happened while updating the monthly dye data!");
                    e.printStackTrace();

                    if(MonthlyCommand.instance != null)
                    {
                        bot.removeListener(MonthlyCommand.instance);
                        MonthlyCommand.instance = null;
                    }
                }
            }
        };

        timer.schedule(task,0, 250*60*60*24);
        return timer;
    }

    private static void updateCount(DiscordApi bot)
    {
        List<String> doNotCheckThese =Arrays.asList
                (
                        "264445053596991498",
                        "446425626988249089"
                );

        Set<String> userIDs = new HashSet<>();

        for (Server server : bot.getServers())
            if(!doNotCheckThese.contains(server.getIdAsString()))
                for (User user : server.getMembers())
                    if (!user.isBot())
                        userIDs.add(user.getIdAsString());

        Values.setUserCount(userIDs.size());

        Values.setGuildCount(bot.getServers().size());

        Values.setCommandCount(bot.getListeners().size());
    }

    private static void addCommands(DiscordApi bot)
    {
        //Crafting
        bot.addListener(new ProficiencyCommand());
        bot.addListener(new CookingCommand());
        bot.addListener(new MatsCommand());

        //items
        bot.addListener(new ItemCommand());
        bot.addListener(new AdditionalCommand());
        bot.addListener(new ArmorCommand());
        bot.addListener(new ArrowCommand());
        bot.addListener(new BowCommand());
        bot.addListener(new BowGunCommand());
        bot.addListener(new DaggerCommand());
        bot.addListener(new GemCommand());
        bot.addListener(new HalberdCommand());
        bot.addListener(new KatanaCommand());
        bot.addListener(new KnucklesCommand());
        bot.addListener(new MagicDeviceCommand());
        bot.addListener(new OneHandedSwordCommand());
        bot.addListener(new ShieldCommand());
        bot.addListener(new SpecialCommand());
        bot.addListener(new StaffCommand());
        bot.addListener(new TwoHandedSwordCommand());
        bot.addListener(new XtalCommand());
        bot.addListener(new UpgradeCommand());

        //monsters
        bot.addListener(new MonsterCommand());
        bot.addListener(new NormalMonsterComand());
        bot.addListener(new MiniBossCommand());
        bot.addListener(new BossCommand());

        //player
        bot.addListener(new LevelCommand());
        bot.addListener(new PointsCommand());

        //torambot
        bot.addListener(new HelpCommand());
        bot.addListener(new InviteCommand());
        bot.addListener(new DonateCommand());
        bot.addListener(new SupportCommand());

        //gameinfo
        bot.addListener(new NewsCommand());
        bot.addListener(new LatestCommand());
        bot.addListener(new MaintenanceCommand());
        bot.addListener(new EventsCommand());
        bot.addListener(new DyeCommand());
    }

    private static void setupDiscordBotListApi(DiscordApi bot)
    {
        try
        {
            AESHelper aesHelper = new AESHelper(bot.getToken());
            String token = aesHelper.decryptData("OjImYbN/dPbEBjjxc+X5sjV5dHC+lU95tnSXwpt2PmQlJXwaXgBRAwdpZtmAGmkYEuu5PU+GMD/+RFibTqrM0367bNnkEE2Hrr77BtP7zyvXocbkRW8G0BRedaLf3EMndt0G/39av7zbWCo2RVYQ99LYhzG8gXWbfd04pJtd6JaXILD0Z3VBfElICQm7D/lS/WufLRG7n2YZsC+jrURXfg==");

            DiscordBotListAPI api = new DiscordBotListAPI.Builder()
                    .token(token)
                    .botId("600302983305101323")
                    .build();

            Values.setApi(api);

            api.setStats(bot.getServers().size());

            if(Values.getApi() != null)
                bot.addListener(new VoteCommand());
        }
        catch (Exception ignore)
        {

        }
    }
}
