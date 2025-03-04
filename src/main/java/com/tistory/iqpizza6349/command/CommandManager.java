package com.tistory.iqpizza6349.command;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.tistory.iqpizza6349.command.commands.*;
import com.tistory.iqpizza6349.command.commands.Crawlercommands.BitCoinCommand;
import com.tistory.iqpizza6349.command.commands.Crawlercommands.CoupangCommand;
import com.tistory.iqpizza6349.command.commands.Crawlercommands.DictionaryCommand;
import com.tistory.iqpizza6349.command.commands.Gamecommands.Slotmachine;
import com.tistory.iqpizza6349.command.commands.game.Dice;
import com.tistory.iqpizza6349.command.commands.game.FlipCoin;
import com.tistory.iqpizza6349.command.commands.game.OddAndEven;
import com.tistory.iqpizza6349.command.commands.information.*;
import com.tistory.iqpizza6349.command.commands.Gamecommands.Todayluck;
import com.tistory.iqpizza6349.command.commands.Gamecommands.UpgradetheSword;
import com.tistory.iqpizza6349.command.commands.Gamecommands.sellcommand;
import com.tistory.iqpizza6349.command.commands.music.*;
import com.tistory.iqpizza6349.command.commands.utility.Calculator;
import com.tistory.iqpizza6349.command.commands.utility.Translate;
import com.tistory.iqpizza6349.database.MySQLDatabase;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CommandManager {

    private final List<ICommand> commandList = new ArrayList<>();
    private static final int[] levelExpArray = new int[20];

    static {
        int temp = 1;
        int x = 5;
        for (int i = 0; i < levelExpArray.length; i++) {
             x = x == 10 ? 5 : 10;
            levelExpArray[i] = (i * temp++) * x;
        }
    }

    public CommandManager(EventWaiter waiter) {
        addCommand(new PingCommand());  // 핑 명령어
        addCommand(new HelpCommand(this)); // 명령어 도움말
        addCommand(new PasteCommand()); // 일반적인 코드 전달 시, 디코로 편하게 보내기 위한 명령어
        addCommand(new HasteCommand()); // HTML 코드 전달 시, 디코로 편하게 보내기 위한 명령어
        addCommand(new KickCommand());  // 멤버 추방
        //addCommand(new WebhookCommand());   // 웹 훅
        addCommand(new JokeCommand());
        addCommand(new SetPrefixCommand()); // 커스텀 prefix

        addCommand(new sellcommand());
        addCommand(new UpgradetheSword());
        addCommand(new Nowmoney());
        addCommand(new Todayluck());
        addCommand(new DictionaryCommand());
        addCommand(new CoupangCommand());
        addCommand(new Slotmachine());

        addCommand(new JoinCommand());  // 음악 봇 참가 명령어
        addCommand(new PlayCommand());
        addCommand(new StopCommand());
        addCommand(new SkipCommand());
        addCommand(new LeaveCommand());
        addCommand(new NowPlayingCommand());
        addCommand(new QueueCommand());
        addCommand(new RepeatCommand());

        addCommand(new EventWaiterCommand(waiter));

        // 게임 명령어
        addCommand(new FlipCoin());
        addCommand(new OddAndEven());
        addCommand(new Dice());

        // 유틸리티
        addCommand(new Calculator());
        addCommand(new Translate());

        // information
        addCommand(new ExchangeRate());
        addCommand(new MovieSearcher());
        addCommand(new SchoolMealsSearcher());
        addCommand(new WeatherSearcher());
        addCommand(new NamuWiki());
        addCommand(new Dictionary());
    }

    private void addCommand(ICommand cmd) {
        boolean nameFound = this.commandList.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

        // 이미 추가된 명령어라면 오류를 뱉어냄
        if (nameFound) {
            throw new IllegalArgumentException("A Command with this name is already present");
        }

        commandList.add(cmd);
    }

    public List<ICommand> getCommandList() {
        return commandList;
    }

    @Nullable
    public ICommand getCommand(String search) {
        String searchLower = search.toLowerCase();

        for (ICommand cmd : this.commandList) {
            if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower)) {
                return cmd;
            }
        }

        return null;
    }

    public void handle(GuildMessageReceivedEvent event, String prefix) {
        String[] split = event.getMessage().getContentRaw()
                .replaceFirst("(?i)" + Pattern.quote(prefix), "")
                .split("\\s+");

        String invoke = split[0].toLowerCase();
        ICommand cmd = this.getCommand(invoke);

        // 대충 대소문자와 무관하게 prefix뒤에 있는 명령어를 invoke에 넣고
        // invoke는 전부 소문자로 바꿔, 해당 명령어가 있는 지 확인

        // 예) .ping -> invoke에 ping을 담음(모든 문자를 소문자로 변환)
        // 해당 invoke를 실제 만든 명령어리스트안에 있다면 명령어 작동

        if (cmd != null) {
            event.getChannel().sendTyping().queue();
            List<String> strings = Arrays.asList(split).subList(1, split.length);

            CommandContext ctx = new CommandContext(event, strings);

            levelUp(event.getAuthor().getIdLong(), event.getChannel());

            cmd.handle(ctx);
        }
    }

    private int getExp(long userId) {
        try (final PreparedStatement preparedStatement = MySQLDatabase
                .getConnection()
                .prepareStatement("SELECT exp FROM user_info WHERE user_id = ?")) {
            preparedStatement.setString(1, String.valueOf(userId));

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("exp");
                }
            }
            try (final PreparedStatement insertStatement = MySQLDatabase
                    .getConnection()
                    .prepareStatement("INSERT INTO user_info(user_id) VALUES(?)")) {

                insertStatement.setString(1, String.valueOf(userId));

                insertStatement.execute();
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getLevel(long userId) {
        try (final PreparedStatement preparedStatement = MySQLDatabase
                .getConnection()
                .prepareStatement("SELECT level FROM user_info WHERE user_id = ?")) {
            preparedStatement.setString(1, String.valueOf(userId));

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("level");
                }
            }
            try (final PreparedStatement insertStatement = MySQLDatabase
                    .getConnection()
                    .prepareStatement("INSERT INTO user_info(user_id) VALUES(?)")) {

                insertStatement.setString(1, String.valueOf(userId));

                insertStatement.execute();
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void updateLevel(long userId, int updateLevel) {
        try (final PreparedStatement preparedStatement = MySQLDatabase
                .getConnection()
                .prepareStatement("UPDATE user_info SET level = ? WHERE user_id = ?")) {
            preparedStatement.setString(1, String.valueOf(updateLevel));
            preparedStatement.setString(2, String.valueOf(userId));

            preparedStatement.executeUpdate();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateExp(long userId, int updateExp) {
        try (final PreparedStatement preparedStatement = MySQLDatabase
                .getConnection()
                .prepareStatement("UPDATE user_info SET exp = ? WHERE user_id = ?")) {
            preparedStatement.setString(1, String.valueOf(updateExp));
            preparedStatement.setString(2, String.valueOf(userId));

            preparedStatement.executeUpdate();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void levelUp(long userId, TextChannel channel) {
        int exp = getExp(userId);
        int lvl = getLevel(userId);

        if (exp == levelExpArray[lvl-1]) {
            // 대충 레벨 업함
            updateLevel(userId, lvl+1);
            channel.sendMessageFormat("<@%s>, level up (%d -> %d)", userId, lvl-1, lvl).queue();
        }
        else {
            updateExp(userId, exp+5);
        }

    }

}
