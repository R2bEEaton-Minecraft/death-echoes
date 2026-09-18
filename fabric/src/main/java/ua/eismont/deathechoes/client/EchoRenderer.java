package ua.eismont.deathechoes.client;

// NOTE: this class is duplicated verbatim between the fabric and neoforge modules. The common
// module has no client-side Minecraft dependencies (this multiloader template has no shared
// client source set), so a renderer - which is unavoidably client-only Minecraft API - cannot
// live there. The class has zero fabric/neoforge-specific imports, so a byte-for-byte copy is the
// simplest option; mirror any future edits into the sibling module's copy of this file. A gradle
// task in the root build.gradle (wired into `check`) fails the build if the two copies drift.

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import ua.eismont.deathechoes.echo.EchoEntity;
import ua.eismont.deathechoes.echo.EchoFrame;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders {@link EchoEntity} as a translucent player-shaped ghost with equipment and held items,
 * using the dead player's skin resolved by name.
 */
public class EchoRenderer extends EntityRenderer<EchoEntity, EchoRenderer.EchoRenderState>
        implements RenderLayerParent<EchoRenderer.EchoRenderState, HumanoidModel<EchoRenderer.EchoRenderState>> {

    /** ~40% opacity, packed into the alpha byte of an otherwise-white tint. */
    private static final int ALPHA = (int) (0.4f * 255.0f);
    private static final int TINT_COLOR = (ALPHA << 24) | 0xFFFFFF;

    /** Floor so the ghost stays visible at night; a 0-15 block-light level like any lit mob. */
    private static final int MIN_BLOCK_LIGHT = 10;

    private final HumanoidModel<EchoRenderState> model;
    private final PlayerSkinRenderCache skinRenderCache;
    private final ItemModelResolver itemModelResolver;
    private final HumanoidArmorLayer<EchoRenderState, HumanoidModel<EchoRenderState>, HumanoidModel<EchoRenderState>> armorLayer;
    private final ItemInHandLayer<EchoRenderState, HumanoidModel<EchoRenderState>> itemInHandLayer;

    private final Map<String, ResolvableProfile> profileCache = new HashMap<>();

    public EchoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        this.skinRenderCache = context.getPlayerSkinRenderCache();
        this.itemModelResolver = context.getItemModelResolver();
        this.armorLayer = new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()
        );
        this.itemInHandLayer = new ItemInHandLayer<>(this);
    }

    @Override
    public HumanoidModel<EchoRenderState> getModel() {
        return this.model;
    }

    @Override
    public EchoRenderState createRenderState() {
        return new EchoRenderState();
    }

    @Override
    public void extractRenderState(EchoEntity entity, EchoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.bodyRot = entity.getYRot(partialTicks);
        state.yRot = 0.0F;
        state.xRot = entity.getXRot(partialTicks);
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.speedValue = 1.0F;
        state.walkAnimationPos = entity.getClientWalkAnimationPos(partialTicks);
        state.walkAnimationSpeed = entity.getClientWalkAnimationSpeed(partialTicks);
        state.isCrouching = entity.getSyncedPose() == EchoFrame.Pose.SNEAKING;

        state.attackTime = entity.getClientAttackAnim(partialTicks);
        state.attackArm = HumanoidArm.RIGHT;

        state.headEquipment = entity.getSyncedHelmet();
        state.chestEquipment = entity.getSyncedChestplate();
        state.legsEquipment = entity.getSyncedLeggings();
        state.feetEquipment = entity.getSyncedBoots();

        ItemStack mainHand = entity.getSyncedMainHand();
        ItemStack offHand = entity.getSyncedOffHand();
        this.itemModelResolver.updateForNonLiving(state.rightHandItemState, mainHand, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
        this.itemModelResolver.updateForNonLiving(state.leftHandItemState, offHand, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);

        state.rightArmPose = mainHand.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        state.leftArmPose = offHand.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;

        String ownerName = entity.getSyncedOwnerName();
        if (ownerName.isEmpty()) {
            state.skinRenderType = PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
        } else {
            ResolvableProfile profile = this.profileCache.computeIfAbsent(ownerName, ResolvableProfile::createUnresolved);
            PlayerSkinRenderCache.RenderInfo renderInfo = this.skinRenderCache.getOrDefault(profile);
            state.skinRenderType = renderInfo.renderType();
        }
    }

    @Override
    public Vec3 getRenderOffset(EchoRenderState state) {
        Vec3 offset = super.getRenderOffset(state);
        return state.isCrouching ? offset.add(0.0, state.scale * -2.0F / 16.0, 0.0) : offset;
    }

    @Override
    public void submit(EchoRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.scale, state.scale, state.scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        this.model.setupAnim(state);

        int light = state.lightCoords;
        if (LightTexture.block(light) < MIN_BLOCK_LIGHT) {
            light = LightTexture.pack(MIN_BLOCK_LIGHT, LightTexture.sky(light));
        }

        submitNodeCollector.submitModel(
                this.model, state, poseStack, state.skinRenderType,
                light, OverlayTexture.NO_OVERLAY, TINT_COLOR, null, state.outlineColor, null);

        this.armorLayer.submit(poseStack, submitNodeCollector, light, state, state.yRot, state.xRot);
        this.itemInHandLayer.submit(poseStack, submitNodeCollector, light, state, state.yRot, state.xRot);

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static class EchoRenderState extends HumanoidRenderState {
        RenderType skinRenderType = PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
    }
}
