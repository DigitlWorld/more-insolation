package me.digitlworld.moreinsolation;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.thermal.core.util.managers.machine.InsolatorRecipeManager;
import cofh.thermal.core.util.recipes.machine.InsolatorRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        var generatedRecipes = new LinkedList<InsolatorRecipe>();

        generatedRecipes.addAll(generateFlowerRecipes());
        generatedRecipes.addAll(generateSaplingRecipes());

        // TODO: Currently crashes server with farmersdelight:rice. Not sure why.
        //generatedRecipes.addAll(generateCropSeedRecipes());

        var curRecipes = rm.getRecipes();
        var currentInsolatorRecipes = curRecipes.stream().filter( r -> r instanceof InsolatorRecipe )
                                                         .flatMap( r -> ((InsolatorRecipe)r).getInputItems().stream() )
                                                         .map( i -> getItemNameTag( getItemFromIngredient( i ) ) )
                                                         .collect(Collectors.toSet());

        var newRecipes = generatedRecipes.stream().collect( Collectors.toMap( MoreInsolationMod::getInputItemNameFromRecipe, i -> i ) );

        var newRecipesToAdd = newRecipes.keySet().stream().filter( k -> !currentInsolatorRecipes.contains( k ) ).map(newRecipes::get).toList();
        curRecipes.addAll(newRecipesToAdd);

        rm.replaceRecipes(curRecipes);

        for( var newRecipe : newRecipesToAdd ) {
            InsolatorRecipeManager.instance().addRecipe(newRecipe);
        }
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

    private List<InsolatorRecipe> generateCropSeedRecipes()
    {
        // TODO: This is all by inference. It doesn't guarantee that a recipe will be correct.
        //       Do better in the future.

        var crops = getItemsForTag("forge", "crops");
        var seeds = getItemsForTag("forge", "seeds");

        if( seeds == null || crops == null )
        {
            return List.of();
        }

        List<InsolatorRecipe> generatedRecipes = new LinkedList<>();

        for( var crop : crops.keySet() )
        {
            var cropItem = crops.get(crop);

            var tags = getItemTags( cropItem );

            var cropString = PathMethods.matchAndReturnTSuffix( tags, "forge", "crops" );

            if( cropString != null )
            {
                var potentialSeedLocation = new ResourceLocation("forge", "seeds/" + cropString );

                var seedItem = seeds.values().stream().filter( item -> {
                    var itemTags = getItemTags(item);
                    return itemTags.filter(tk -> {
                        var tagLocation = tk.location();
                        return tagLocation.getNamespace().equals(potentialSeedLocation.getNamespace())
                                && tagLocation.getPath().equals(potentialSeedLocation.getPath());
                    }).count() == 1;
                }).findFirst();

                if( seedItem.isPresent() )
                {
                    generatedRecipes.add( generateCropWithSeedRecipe(cropItem, seedItem.get() ) );
                }
                else
                {
                    generatedRecipes.add( generateCropSelfRecipe( cropItem ) );
                }
            }
        }

        return generatedRecipes;
    }

    private InsolatorRecipe generateCropSelfRecipe( Item cropItem )
    {
        var itemLocation = ForgeRegistries.ITEMS.getKey(cropItem);

        if( itemLocation != null )
        {
            var recipeName = "insolation_recipe/" + itemLocation.getNamespace() + "/" + itemLocation.getPath();

            ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, recipeName);

            ItemStack stack = new ItemStack(cropItem,1);
            var rarity = cropItem.getRarity(stack);

            Ingredient inputItem = Ingredient.of(cropItem);

            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            FluidIngredient waterIngredient = FluidIngredient.of(water);

            ItemStack outputItem = new ItemStack(cropItem, 1);
            Float outputItemChance = 2.5f;

            return new InsolatorRecipe(id, getFlowerEnergyFromRarity(rarity), 0.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputItem), List.of(outputItemChance), List.of());
        }

        return null;
    }

    private static @Nullable String getItemNameTag( @NotNull Item item )
    {
        var itemLocation = ForgeRegistries.ITEMS.getKey(item);

        if( itemLocation == null )
        {
            return null;
        }

        return itemLocation.toString();
    }

    private static Item getItemFromIngredient( Ingredient ingredient )
    {
        var item = Arrays.stream(ingredient.getItems()).findFirst();
        return item.map(ItemStack::getItem).orElse(null);
    }

    private static String getInputItemNameFromRecipe(InsolatorRecipe recipe)
    {
        var item = recipe.getInputItems().stream().findFirst().map( MoreInsolationMod::getItemFromIngredient ).orElse(null);

        if( item != null )
        {
            return getItemNameTag(item);
        }

        return null;
    }

    private InsolatorRecipe generateCropWithSeedRecipe( Item cropItem, Item seedItem )
    {
        var itemLocation = ForgeRegistries.ITEMS.getKey(seedItem);

        if( itemLocation != null )
        {
            var recipeName = "insolation_recipe/" + itemLocation.getNamespace() + "/" + itemLocation.getPath();

            ResourceLocation id = new ResourceLocation(MoreInsolationMod.MODID, recipeName);

            ItemStack stack = new ItemStack(seedItem,1);
            var rarity = seedItem.getRarity(stack);

            Ingredient inputItem = Ingredient.of(seedItem);

            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            FluidIngredient waterIngredient = FluidIngredient.of(water);

            ItemStack outputCrop = new ItemStack(cropItem, 1);
            Float outputCropChance = 2.0f;

            ItemStack outputSeed = new ItemStack(seedItem, 1);
            Float outputSeedChance = 1.1f;

            return new InsolatorRecipe(id, getFlowerEnergyFromRarity(rarity), 0.0f, List.of(inputItem), List.of(waterIngredient), List.of(outputCrop, outputSeed), List.of(outputCropChance,outputSeedChance), List.of());
        }

        return null;
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

    private static Stream<TagKey<Item>> getItemTags(Item toGet)
    {
        return ForgeRegistries.ITEMS.tags().getReverseTag(toGet).orElseThrow().getTagKeys();
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

