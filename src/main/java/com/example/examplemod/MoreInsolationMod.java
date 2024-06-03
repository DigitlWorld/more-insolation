package com.example.examplemod;

import cofh.core.init.data.providers.CoreTagsProvider;
import cofh.lib.common.fluid.FluidCoFH;
import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.core.util.managers.machine.InsolatorRecipeManager;
import cofh.thermal.core.util.recipes.machine.InsolatorRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import com.teamcofh.
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MoreInsolationMod.MODID)
public class MoreInsolationMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "moreinsolation";

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public MoreInsolationMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onLoadComplete(ServerAboutToStartEvent event) {

        var rm = event.getServer().getRecipeManager();
        ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, "test_recipe");

        Ingredient inputItem = Ingredient.of(Items.DIAMOND);

        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        FluidIngredient waterIngredient = FluidIngredient.of(water);

        ItemStack outputItem = new ItemStack(Items.DIAMOND, 1);
        Float outputItemChance = 2.0f;

        InsolatorRecipe recipe = new InsolatorRecipe(id, 3000, 3.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputItem), List.of(2.0f), List.of() );

        var curRecipes = rm.getRecipes();
        curRecipes.add(recipe);
        rm.replaceRecipes(curRecipes);

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }
    }
}
