package ua.eismont.deathechoes.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ua.eismont.deathechoes.config.DeathEchoesConfig;

public final class DeathMessageObfuscator {

    private DeathMessageObfuscator() {}

    public static Component obfuscateDeathMessage(Component original, LivingEntity victim) {
        if (original == null) {
            return null;
        }
        DeathEchoesConfig config = DeathEchoesConfig.get();
        if (!config.obfuscateDeathMessages) {
            return original;
        }

        if (original.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            if (args == null || args.length == 0) {
                return original;
            }

            String key = translatable.getKey();
            Object[] newArgs = new Object[args.length];
            boolean modified = false;

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Component comp) {
                    boolean shouldObfuscate = false;
                    if (i == 0 && victim instanceof Player) {
                        shouldObfuscate = true;
                    } else if (isPlayer(comp, victim)) {
                        shouldObfuscate = true;
                    } else if (config.obfuscateWeaponNames && isWeapon(comp, key, i)) {
                        shouldObfuscate = true;
                    } else if (config.obfuscateMobNames && i > 0 && !isWeapon(comp, key, i)) {
                        shouldObfuscate = true;
                    }

                    if (shouldObfuscate) {
                        newArgs[i] = obfuscate(comp);
                        modified = true;
                    } else {
                        newArgs[i] = arg;
                    }
                } else {
                    newArgs[i] = arg;
                }
            }

            if (!modified) {
                return original;
            }

            MutableComponent result = Component.translatableWithFallback(key, translatable.getFallback(), newArgs);
            result.setStyle(original.getStyle());
            for (Component sibling : original.getSiblings()) {
                result.append(sibling);
            }
            return result;
        }

        return original;
    }

    public static Component obfuscate(Component component) {
        if (component == null) return null;
        MutableComponent mutable = MutableComponent.create(component.getContents());
        mutable.setStyle(component.getStyle().withObfuscated(true));
        for (Component sibling : component.getSiblings()) {
            mutable.append(obfuscate(sibling));
        }
        return mutable;
    }

    private static boolean isPlayer(Component component, LivingEntity victim) {
        MinecraftServer server = victim.level().getServer();
        if (server != null) {
            String text = component.getString().trim();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getGameProfile().name().equalsIgnoreCase(text) ||
                    player.getScoreboardName().equalsIgnoreCase(text)) {
                    return true;
                }
            }
        }

        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowEntity showEntity) {
            if (EntityType.PLAYER.equals(showEntity.entity().type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWeapon(Component component, String translationKey, int index) {
        if (translationKey != null && translationKey.endsWith(".item") && index >= 2) {
            return true;
        }
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowItem) {
            return true;
        }
        return false;
    }
}
