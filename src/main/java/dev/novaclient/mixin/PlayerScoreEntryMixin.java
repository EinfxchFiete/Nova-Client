package dev.novaclient.mixin;

import dev.novaclient.module.visual.ScoreboardNumbersModule;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScoreEntry.class)
public abstract class PlayerScoreEntryMixin {
	/** Used by the scoreboard sidebar (and tab list scores): empty text = no number and no extra space. */
	@Inject(method = "formatValue", at = @At("HEAD"), cancellable = true)
	private void novaclient$hideNumber(NumberFormat format, CallbackInfoReturnable<MutableComponent> cir) {
		if (ScoreboardNumbersModule.active()) {
			cir.setReturnValue(Component.empty());
		}
	}
}
