package com.example.examplemod;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.core.util.recipes.machine.InsolatorRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

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

        var curRecipes = rm.getRecipes();
        curRecipes.addAll(generateFlowerRecipes());
        rm.replaceRecipes(curRecipes);
    }

    private List<InsolatorRecipe> generateFlowerRecipes()
    {
        var tagkey = ItemTags.create(new ResourceLocation("minecraft", "flowers"));

        var tag = ForgeRegistries.ITEMS.tags().getTag(tagkey);

        return tag.stream().map(this::generateRecipeForFlower).toList();
    }

    private InsolatorRecipe generateRecipeForFlower( Item flower )
    {
        var itemLocation = ForgeRegistries.ITEMS.getKey(flower);

        if( itemLocation != null )
        {
            var recipeName = "insolation_recipe/" + itemLocation.getNamespace() + "/" + itemLocation.getPath();

            ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, recipeName);

            Ingredient inputItem = Ingredient.of(flower);

            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            FluidIngredient waterIngredient = FluidIngredient.of(water);

            ItemStack outputItem = new ItemStack(flower, 1);
            Float outputItemChance = 2.0f;

            return new InsolatorRecipe(id, 20000, 3.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputItem), List.of(outputItemChance), List.of());
        }

        return null;
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
