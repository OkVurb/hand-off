package com.studio.planeshift.common.item;

import com.studio.planeshift.server.FormService;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/**
 * A deterministic Form pickup ("Do not place required progression behind a random drop.
 * Use deterministic teaching pickups.").
 *
 * <p>Using the charm asks the server to grant the bound Form; all replacement/reserve
 * rules run in {@link FormService}. The item is consumed only when the grant succeeds.
 */
public class FormCharmItem extends Item {

    private final Identifier formId;

    public FormCharmItem(Identifier formId, Properties properties) {
        super(properties);
        this.formId = formId;
    }

    public Identifier formId() {
        return formId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        boolean granted = FormService.grant(serverPlayer, formId);
        if (granted) {
            stack.consume(1, player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
