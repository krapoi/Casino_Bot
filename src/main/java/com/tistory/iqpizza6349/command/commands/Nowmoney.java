package com.tistory.iqpizza6349.command.commands;

import com.tistory.iqpizza6349.command.CommandContext;
import com.tistory.iqpizza6349.command.ICommand;
import com.tistory.iqpizza6349.database.MySQLDatabase;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Nowmoney implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        User member = ctx.getMessage().getAuthor();
        TextChannel channel = ctx.getChannel();
        int money = getUserMoney(member.getIdLong());
        channel.sendMessage(member.getName() + "님의 현재 소지금은 "+ money+ "원 입니다.").queue();
    }

    @Override
    public String getName() {
        return "money";
    }

    @Override
    public String getHelp() {
        return "You can see your money";
    }

    private int getUserMoney(long userId) {
        try (final PreparedStatement preparedStatement = MySQLDatabase
                .getConnection()
                .prepareStatement("SELECT money FROM user_info WHERE user_id = ?")) {
            preparedStatement.setString(1, String.valueOf(userId));

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("money");
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
