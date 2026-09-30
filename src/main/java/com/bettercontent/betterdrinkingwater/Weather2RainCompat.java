package com.bettercontent.betterdrinkingwater;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import weather2.ServerTickHandler;
import weather2.weathersystem.WeatherManagerServer;

/** Optional server-side bridge to Weather2's localized precipitation footprint. */
public final class Weather2RainCompat {
    private Weather2RainCompat() {
    }

    public static boolean isPrecipitatingAt(final ServerLevel level, final BlockPos pos) {
        if (!ModList.get().isLoaded("weather2")) {
            return false;
        }
        return LoadedWeather2Api.isPrecipitatingAt(level, pos);
    }

    /** Kept nested so Weather2 classes are not resolved when the optional mod is absent. */
    private static final class LoadedWeather2Api {
        private LoadedWeather2Api() {
        }

        private static boolean isPrecipitatingAt(final ServerLevel level, final BlockPos pos) {
            final WeatherManagerServer manager = ServerTickHandler.getWeatherManagerFor(level);
            return manager != null && manager.isPrecipitatingAt(pos);
        }
    }
}
