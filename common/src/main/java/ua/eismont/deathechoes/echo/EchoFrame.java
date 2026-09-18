package ua.eismont.deathechoes.echo;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/**
 * A single tick of a recorded player death echo: position, look direction, pose, equipment and attack animation.
 */
public record EchoFrame(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        Pose pose,
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        boolean swinging,
        float attackAnim
) {

    public enum Pose {
        STANDING,
        SNEAKING,
        SPRINTING
    }

    public EchoFrame(double x, double y, double z, float yaw, float pitch, Pose pose, ItemStack mainHand) {
        this(x, y, z, yaw, pitch, pose, mainHand, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, false, 0.0f);
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putByte("pose", (byte) pose.ordinal());
        var context = registries.createSerializationContext(NbtOps.INSTANCE);
        if (!mainHand.isEmpty()) {
            tag.store("item", ItemStack.CODEC, context, mainHand);
        }
        if (!offHand.isEmpty()) {
            tag.store("offhand", ItemStack.CODEC, context, offHand);
        }
        if (!helmet.isEmpty()) {
            tag.store("helmet", ItemStack.CODEC, context, helmet);
        }
        if (!chestplate.isEmpty()) {
            tag.store("chestplate", ItemStack.CODEC, context, chestplate);
        }
        if (!leggings.isEmpty()) {
            tag.store("leggings", ItemStack.CODEC, context, leggings);
        }
        if (!boots.isEmpty()) {
            tag.store("boots", ItemStack.CODEC, context, boots);
        }
        if (swinging) {
            tag.putBoolean("swinging", true);
        }
        if (attackAnim > 0.0f) {
            tag.putFloat("attackAnim", attackAnim);
        }
        return tag;
    }

    public static EchoFrame fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        double x = tag.getDoubleOr("x", 0.0);
        double y = tag.getDoubleOr("y", 0.0);
        double z = tag.getDoubleOr("z", 0.0);
        float yaw = tag.getFloatOr("yaw", 0f);
        float pitch = tag.getFloatOr("pitch", 0f);
        byte poseOrdinal = tag.getByteOr("pose", (byte) 0);
        Pose[] poses = Pose.values();
        Pose pose = (poseOrdinal >= 0 && poseOrdinal < poses.length) ? poses[poseOrdinal] : Pose.STANDING;

        var context = registries.createSerializationContext(NbtOps.INSTANCE);
        ItemStack mainHand = tag.read("item", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        ItemStack offHand = tag.read("offhand", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        ItemStack helmet = tag.read("helmet", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        ItemStack chestplate = tag.read("chestplate", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        ItemStack leggings = tag.read("leggings", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        ItemStack boots = tag.read("boots", ItemStack.CODEC, context).orElse(ItemStack.EMPTY);
        boolean swinging = tag.getBooleanOr("swinging", false);
        float attackAnim = tag.getFloatOr("attackAnim", 0.0f);

        return new EchoFrame(x, y, z, yaw, pitch, pose, mainHand, offHand, helmet, chestplate, leggings, boots, swinging, attackAnim);
    }
}
