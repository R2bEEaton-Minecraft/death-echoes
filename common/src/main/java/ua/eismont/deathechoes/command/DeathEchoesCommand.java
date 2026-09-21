package ua.eismont.deathechoes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ua.eismont.deathechoes.echo.EchoEntity;
import ua.eismont.deathechoes.echo.EchoTracker;

import java.util.UUID;

/**
 * {@code /deathechoes clear <all|nearest>}, letting operators wipe stray or unwanted echoes
 * without waiting for a player to collect or despawn them. Shared between loaders; each loader's
 * bootstrap wires {@link #register} into its own command-registration hook.
 */
public final class DeathEchoesCommand {

    private DeathEchoesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deathechoes")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("clear")
                        .then(Commands.literal("all").executes(DeathEchoesCommand::clearAll))
                        .then(Commands.literal("nearest").executes(DeathEchoesCommand::clearNearest))));
    }

    private static int clearAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel overworld = source.getServer().overworld();
        EchoTracker tracker = EchoTracker.get(overworld);

        int count = 0;
        for (UUID echoId : tracker.allEchoes()) {
            Entity entity = overworld.getEntityInAnyDimension(echoId);
            if (entity != null) {
                entity.discard();
                count++;
            }
        }
        tracker.clearAll();

        int cleared = count;
        source.sendSuccess(() -> Component.literal(
                "Cleared " + cleared + " echo" + (cleared == 1 ? "" : "es") + "."), true);
        return count;
    }

    private static int clearNearest(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        EchoTracker tracker = EchoTracker.get(level);

        EchoEntity nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (UUID echoId : tracker.allEchoes()) {
            Entity entity = level.getServer().overworld().getEntityInAnyDimension(echoId);
            if (entity instanceof EchoEntity echo && echo.level() == level) {
                double distSq = echo.distanceToSqr(pos.x, pos.y, pos.z);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    nearest = echo;
                }
            }
        }

        if (nearest == null) {
            source.sendFailure(Component.literal("No echoes found in this dimension."));
            return 0;
        }

        if (nearest.getOwnerUUID() != null) {
            tracker.unregister(nearest.getOwnerUUID(), nearest.getUUID());
        }
        nearest.discard();
        source.sendSuccess(() -> Component.literal("Cleared the nearest echo."), true);
        return 1;
    }
}
