package ua.eismont.deathechoes.echo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a rolling {@link EchoRecording} of every online player's recent movement, so a ghost
 * echo can be spawned at their last known frames when they die.
 *
 * <p>Main server thread only; ConcurrentHashMap protects map structure only.
 */
public final class RecordingManager {
    private static final Map<UUID, EchoRecording> RECORDINGS = new ConcurrentHashMap<>();

    private RecordingManager() {}

    public static void tickServer(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) tickPlayer(p);
    }

    public static void tickPlayer(Player p) {
        EchoFrame.Pose pose = p.isCrouching() ? EchoFrame.Pose.SNEAKING
            : p.isSprinting() ? EchoFrame.Pose.SPRINTING : EchoFrame.Pose.STANDING;
        // defensive copy is load-bearing: frame must never hold a live inventory reference
        RECORDINGS.computeIfAbsent(p.getUUID(), u -> new EchoRecording())
            .push(new EchoFrame(
                    p.getX(), p.getY(), p.getZ(),
                    p.getYRot(), p.getXRot(),
                    pose,
                    p.getMainHandItem().copy(),
                    p.getOffhandItem().copy(),
                    p.getItemBySlot(EquipmentSlot.HEAD).copy(),
                    p.getItemBySlot(EquipmentSlot.CHEST).copy(),
                    p.getItemBySlot(EquipmentSlot.LEGS).copy(),
                    p.getItemBySlot(EquipmentSlot.FEET).copy(),
                    p.swinging,
                    p.getAttackAnim(1.0f)
            ));
    }

    public static EchoRecording recordingFor(UUID uuid) {
        return RECORDINGS.get(uuid);
    }

    public static void clear(UUID uuid) {
        RECORDINGS.remove(uuid);
    }
}
