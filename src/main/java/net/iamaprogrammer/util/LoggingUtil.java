package net.iamaprogrammer.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class LoggingUtil {
    public static int logPercentageCompleted(CommandContext<ServerCommandSource> context, int currentStatus, int completedStatus, int previousPercentage) {
        int percentageOf100 = (int) Math.floor(((double)currentStatus/completedStatus) * 100);
        if (previousPercentage != percentageOf100) {
            context.getSource().sendFeedback(() -> Text.of("Generated: " + percentageOf100 + "%"), false);
            return percentageOf100;
        }
        return -1;
    }
}
