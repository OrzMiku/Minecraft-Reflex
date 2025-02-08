package com.example.mixin.client;

import com.example.CpuTimeCollector;
import com.example.GpuTimeCollector;
import com.example.ReflexMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.example.Reflex.MOD_ID;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Unique
    private final CpuTimeCollector cpuTimeCollect = new CpuTimeCollector();
    @Unique
    private final List<GpuTimeCollector> gpuTimeCollectorList = new ArrayList<>();

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // runTick里面可能包着runTick，导致意外运行顺序，需要注意
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setErrorSection(Ljava/lang/String;)V", ordinal = 0))
    private void beforeRunTick(boolean bl, CallbackInfo ci) {
        ReflexMod.getScheduler().Wait();

        cpuTimeCollect.startCollect();

        ReflexMod.getScheduler().renderQueueAdd();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V", shift = At.Shift.AFTER))
    private void afterFlush(CallbackInfo ci) {
        ReflexMod.getScheduler().renderQueueEndInsert();

        cpuTimeCollect.endCollect();
        Long cpuTime = cpuTimeCollect.getCpuTime();
        cpuTimeCollect.reset();
        if (cpuTime != null) {
            // 更新 CPU 预测时间
            ReflexMod.getScheduler().updateCpuTime(cpuTime);
        }

        ReflexMod.getScheduler().renderQueueStartWait();
    }
}