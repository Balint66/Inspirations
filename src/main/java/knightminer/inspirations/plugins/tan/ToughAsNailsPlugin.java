package knightminer.inspirations.plugins.tan;

import com.google.common.eventbus.Subscribe;

import knightminer.inspirations.Inspirations;
import knightminer.inspirations.common.Config;
import knightminer.inspirations.common.PulseBase;
import knightminer.inspirations.library.InspirationsRegistry;
import knightminer.inspirations.plugins.tan.recipes.FillCanteenRecipe;
import knightminer.inspirations.recipes.InspirationsRecipes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.mantle.pulsar.pulse.Pulse;

@Pulse(
		id = ToughAsNailsPlugin.pulseID,
		description = "Adds support between TAN thirst and the cauldron overrides",
		modsRequired = "toughasnails",
		pulsesRequired = InspirationsRecipes.pulseID)
public class ToughAsNailsPlugin extends PulseBase {
	public static final String pulseID = "ToughAsNailsPlugin";

	@ObjectHolder(value = "toughasnails:purified_water_bottle")
	public static final Item waterBottle = null;
	@ObjectHolder(value = "toughasnails:charcoal_filter")
	public static final Item charcoalFilter = null;
	@ObjectHolder(value = "toughasnails:fruit_juice")
	public static final Item fruitJuice = null;
	@ObjectHolder(value = "toughasnails:canteen")
	public static final Item canteen = null;

	// fluids
	public static Fluid sweetenedWater;
	public static Fluid[] juices;


	@Subscribe
	public void preInit(FMLPreInitializationEvent event) {
		if(!Config.enableExtendedCauldron) {
			return;
		}

		// juice types
		if(Config.tanJuiceInCauldron) {
			sweetenedWater = registerColoredFluid("sweetened_water", 0xFF35ACF2);//0xFFA0E2FF);
			juices = new Fluid[] {
					registerColoredFluid("apple_juice", 0xFFFBBA44),
					registerColoredFluid("beetroot_juice", 0xFFAA1226),
					registerColoredFluid("cactus_juice", 0xFF7FB33D),
					registerColoredFluid("carrot_juice", 0xFFD5632C),
					registerColoredFluid("chorus_fruit_juice", 0xFFA361B3),
					registerColoredFluid("glistering_melon_juice", 0xFFFF4747),
					registerColoredFluid("golden_apple_juice", 0xFFFF9D49),
					registerColoredFluid("golden_carrot_juice", 0xFFFF6E56),
					registerColoredFluid("melon_juice", 0xFFCD3833),
					registerColoredFluid("pumpkin_juice", 0xFFCE8431)
			};
		}
	}

	@Subscribe
	public void init(FMLInitializationEvent event) {
		// we need cauldron fluids for this to work
		if(!Config.enableExtendedCauldron) {
			return;
		}

		// allow water to be purified in the cauldron, then used to make juices
		Fluid purifiedWater = FluidRegistry.getFluid("purified_water");
		if(purifiedWater != null) {
			InspirationsRegistry.addCauldronWater(purifiedWater);
			// add recipe to fill purified bottle
			if(waterBottle != null) {
				InspirationsRegistry.addCauldronFluidItem(new ItemStack(waterBottle, 1), new ItemStack(Items.GLASS_BOTTLE), purifiedWater);
			}

			// filter water in a cauldron
			if(charcoalFilter != null) {
				InspirationsRegistry.addCauldronScaledTransformRecipe(new ItemStack(charcoalFilter), FluidRegistry.WATER, purifiedWater, null);
			}

			// fill canteen from a cauldron
			if(canteen != null) {
				for(int i = 0; i < 3; i++) {
					// normal water canteen starts at 1
					InspirationsRegistry.addCauldronRecipe(new FillCanteenRecipe(canteen, i, 1, FluidRegistry.WATER));
					// purified water starts at 2
					InspirationsRegistry.addCauldronRecipe(new FillCanteenRecipe(canteen, i, 2, purifiedWater));
				}
				InspirationsRegistry.addCauldronBlacklist(canteen, OreDictionary.WILDCARD_VALUE);
			}

			// make juice in the cauldron
			if(Config.tanJuiceInCauldron && fruitJuice != null) {
				InspirationsRegistry.addCauldronScaledTransformRecipe(new ItemStack(Items.SUGAR), purifiedWater, sweetenedWater, null);
				Item[] items = {
						Items.APPLE,
						Items.BEETROOT,
						Item.getItemFromBlock(Blocks.CACTUS),
						Items.CARROT,
						Items.CHORUS_FRUIT,
						Items.SPECKLED_MELON,
						Items.GOLDEN_APPLE,
						Items.GOLDEN_CARROT,
						Items.MELON,
						Item.getItemFromBlock(Blocks.PUMPKIN)
				};
				for(int i = 0; i < items.length; i++) {
					addJuiceRecipe(juices[i], i, items[i]);
				}
			}
		} else {
			Inspirations.log.error("Unable to find Tough as Nails purified water fluid, skipping recipes");
		}
	}

	private static void addJuiceRecipe(Fluid fluid, int meta, Item ingredient) {
		InspirationsRegistry.addCauldronScaledTransformRecipe(new ItemStack(ingredient), sweetenedWater, fluid, null);
		InspirationsRegistry.addCauldronFluidItem(new ItemStack(fruitJuice, 1, meta), new ItemStack(Items.GLASS_BOTTLE), fluid);
	}
}
