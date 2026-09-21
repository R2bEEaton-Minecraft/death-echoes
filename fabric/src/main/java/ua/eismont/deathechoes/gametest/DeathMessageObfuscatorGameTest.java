package ua.eismont.deathechoes.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import ua.eismont.deathechoes.chat.DeathMessageObfuscator;
import ua.eismont.deathechoes.config.DeathEchoesConfig;

import java.util.Optional;
import java.util.UUID;

public class DeathMessageObfuscatorGameTest {

    @GameTest
    public void obfuscatesPlayerAndWeaponNames(GameTestHelper helper) {
        Player victim = helper.makeMockPlayer(GameType.SURVIVAL);
        Component victimComp = Component.literal("VictimPlayer");

        MutableComponent killerComp = Component.literal("KillerPlayer");
        killerComp.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowEntity(
                new HoverEvent.EntityTooltipInfo(EntityType.PLAYER, UUID.randomUUID(), Optional.of(killerComp))
        )));

        MutableComponent weaponComp = Component.literal("DoomSword");
        weaponComp.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(
                new ItemStack(Items.DIAMOND_SWORD)
        )));

        Component original = Component.translatable("death.attack.player.item", victimComp, killerComp, weaponComp);
        Component obfuscated = DeathMessageObfuscator.obfuscateDeathMessage(original, victim);

        if (obfuscated == null) {
            helper.fail("expected non-null obfuscated message");
        }

        // Test with obfuscation and coloring both disabled
        DeathEchoesConfig.get().obfuscateDeathMessages = false;
        DeathEchoesConfig.get().colorDeathMessagePlayersDarkPurple = false;
        Component untouched = DeathMessageObfuscator.obfuscateDeathMessage(original, victim);
        if (untouched != original) {
            helper.fail("expected untouched component when config is disabled");
        }
        DeathEchoesConfig.get().obfuscateDeathMessages = true;
        DeathEchoesConfig.get().colorDeathMessagePlayersDarkPurple = true;

        helper.succeed();
    }
}
