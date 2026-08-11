package com.github.ovaware.dealmaker.kob;

import com.github.ovaware.dealmaker.api.DealmakerIntegrations;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DealmakerKobMod.MOD_ID)
public final class DealmakerKobMod {
    public static final String MOD_ID = "dealmaker_kob";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DealmakerKobMod() {
        DealmakerIntegrations.register(new KobDealIntegration());
        MinecraftForge.EVENT_BUS.addListener(DealmakerKobMod::registerCommands);
        MinecraftForge.EVENT_BUS.register(KobDealmakerRitual.class);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        KobCommands.register(event);
    }
}
