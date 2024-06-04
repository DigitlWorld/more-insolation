package me.digitlworld.moreinsolation;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.core.util.recipes.machine.InsolatorRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootDataResolver;
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

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        curRecipes.addAll(generateSaplingRecipes());

        rm.replaceRecipes(curRecipes);
    }

    private List<InsolatorRecipe> generateSaplingRecipes()
    {
        // TODO: This is all by inference. It doesn't guarantee that a recipe will be correct.
        //       Do better in the future.

        var saplings = getItemsForTag("minecraft", "saplings");
        var logs = getItemsForTag("minecraft", "logs");
        var leaves = getItemsForTag("minecraft", "leaves");

        if( saplings == null || logs == null || leaves == null )
        {
            return List.of();
        }

        List<InsolatorRecipe> generatedRecipes = new LinkedList<>();

        for( var sapling : saplings.keySet() )
        {
            // Generate proposed log items
            var originalSaplingPath = sapling.getPath();
            var woodTypePrefix = originalSaplingPath.replace("_sapling", "");

            var proposedLog = new ResourceLocation(sapling.getNamespace(), woodTypePrefix + "_log");
            var proposedLeaves = new ResourceLocation(sapling.getNamespace(), woodTypePrefix + "_leaves");

            if( logs.get(proposedLog) instanceof BlockItem log )
            {
                generatedRecipes.add( generateRecipeForSapling(saplings.get(sapling), log, null, null));
            }
        }

        return generatedRecipes;
    }

    private @Nullable Map<ResourceLocation,Item> getItemsForTag(String namespace, String path )
    {
        var tagKey = ItemTags.create(new ResourceLocation(namespace, path));
        var tags = ForgeRegistries.ITEMS.tags();

        if( tags == null )
        {
            return null;
        }

        return tags.getTag(tagKey).stream().collect(Collectors.toMap(MoreInsolationMod::getResourceLocation, (i) -> i));
    }

    private static ResourceLocation getResourceLocation( Item item )
    {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    private List<InsolatorRecipe> generateFlowerRecipes()
    {
        var flowers = getItemsForTag("minecraft", "flowers");

        if( flowers == null )
        {
            return List.of();
        }

        return flowers.values().stream().map(this::generateRecipeForFlower).toList();
    }

    private InsolatorRecipe generateRecipeForFlower( Item flower )
    {
        var itemLocation = ForgeRegistries.ITEMS.getKey(flower);

        if( itemLocation != null )
        {
            var recipeName = "insolation_recipe/" + itemLocation.getNamespace() + "/" + itemLocation.getPath();

            ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, recipeName);

            ItemStack stack = new ItemStack(flower,1);
            var rarity = flower.getRarity(stack);

            Ingredient inputItem = Ingredient.of(flower);

            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            FluidIngredient waterIngredient = FluidIngredient.of(water);

            ItemStack outputItem = new ItemStack(flower, 1);
            Float outputItemChance = 2.0f;

            return new InsolatorRecipe(id, getFlowerEnergyFromRarity(rarity), 0.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputItem), List.of(outputItemChance), List.of());
        }

        return null;
    }

    private InsolatorRecipe generateRecipeForSapling(Item sapling, BlockItem log, BlockItem leaves, LootDataResolver lootData )
    {
        if(leaves != null)
        {
            // TODO: attempt to run loot generation for tree leaves, to generate apples/sticks/etc.
        }

        var itemLocation = ForgeRegistries.ITEMS.getKey(sapling);

        if( itemLocation != null )
        {
            var recipeName = "insolation_recipe/" + itemLocation.getNamespace() + "/" + itemLocation.getPath();

            ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, recipeName);

            ItemStack stack = new ItemStack(sapling,1);
            var rarity = sapling.getRarity(stack);

            Ingredient inputItem = Ingredient.of(sapling);

            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            FluidIngredient waterIngredient = FluidIngredient.of(water);

            ItemStack outputItemLog = new ItemStack(log, 1);
            Float outputItemLogChance = 6.0f;

            ItemStack outputItemSapling = new ItemStack(sapling, 1);
            Float outputItemSaplingChance = 1.1f;

            return new InsolatorRecipe(id, getSaplingEnergyFromRarity(rarity), 0.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputItemLog, outputItemSapling), List.of(outputItemLogChance, outputItemSaplingChance), List.of());
        }

        return null;
    }

    private int getFlowerEnergyFromRarity(Rarity rarity)
    {
        return switch (rarity) {
            case COMMON -> 20000;
            case UNCOMMON -> 30000;
            case RARE -> 50000;
            case EPIC -> 80000;
        };
    }

    private int getSaplingEnergyFromRarity(Rarity rarity)
    {
        return switch (rarity) {
            case COMMON -> 60000;
            case UNCOMMON -> 75000;
            case RARE -> 100000;
            case EPIC -> 150000;
        };
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
