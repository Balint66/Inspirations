package knightminer.inspirations.library.recipe.cauldron;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.IStringSerializable;

public interface ICauldronRecipe {

	/**
	 * Checks if the recipe matches the given input
	 * @param stack    Input stack
	 * @param boiling  Whether the cauldron is above fire
	 * @param level    Input level
	 * @param state    Input cauldron state
	 * @return True if the recipe matches
	 */
	boolean matches(ItemStack stack, boolean boiling, int level, CauldronState state);

	/**
	 * Gets the result stack for this recipe
	 * @param stack    Input stack
	 * @param boiling  Whether the cauldron is above fire
	 * @param level    Input level
	 * @param state    Input cauldron state
	 * @return ItemStack result
	 */
	default ItemStack getResult(ItemStack stack, boolean boiling, int level, CauldronState state) {
		return ItemStack.EMPTY;
	}

	/**
	 * Gets the result stack for this recipe with a state of water
	 * @param stack    Input stack
	 * @param boiling  Whether the cauldron is above fire
	 * @param level    Input level
	 * @return ItemStack result
	 */
	default ItemStack getResult(ItemStack stack, boolean boiling, int level) {
		return getResult(stack, boiling, level, CauldronState.WATER);
	}

	/**
	 * Gets the result change in cauldron level as a result of this recipe
	 */
	default int getLevel(int level) {
		return level;
	}

	/**
	 * Gets the resulting cauldron state for this recipe
	 * @param stack    Input stack
	 * @param boiling  Whether the cauldron is above fire
	 * @param level    Input level
	 * @param state    Input cauldron state
	 * @return New cauldron state
	 */
	default CauldronState getState(ItemStack stack, boolean boiling, int level, CauldronState state) {
		return state;
	}


	/**
	 * Current contents type for a cauldron state
	 */
	public enum CauldronContents implements IStringSerializable {
		WATER,
		DYE,
		POTION;

		private int meta;
		CauldronContents() {
			this.meta = ordinal();
		}

		@Override
		public String getName() {
			return name().toLowerCase(Locale.US);
		}

		public int getMeta() {
			return meta;
		}

		public static CauldronContents fromMeta(int meta) {
			if(meta > values().length) {
				meta = 0;
			}

			return values()[meta];
		}
	}

	/**
	 * Current cauldron state
	 */
	public class CauldronState {
		private CauldronContents type;
		private int color;
		private Potion potion;

		public static final CauldronState WATER = new CauldronState(CauldronContents.WATER);

		/**
		 * Creates a new water cauldron state
		 */
		private CauldronState(CauldronContents type) {
			this.type = type;
			this.color = -1;
			this.potion = null;
		}


		/* Constructors */

		/**
		 * Creates a new cauldron color state
		 * @param color  Color input
		 */
		public static CauldronState dye(int color) {
			CauldronState state = new CauldronState(CauldronContents.DYE);
			state.color = color;
			return state;
		}

		/**
		 * Creates a new potion cauldron state
		 * @param potion  Potion input
		 */
		public static CauldronState potion(Potion potion) {
			CauldronState state = new CauldronState(CauldronContents.POTION);
			state.potion = potion;
			return state;
		}

		/**
		 * Creates a new cauldron state from NBT
		 * @param tags  NBT tags
		 */
		public static CauldronState fromNBT(NBTTagCompound tags) {
			CauldronContents type = ICauldronRecipe.CauldronContents.fromMeta(tags.getInteger(TAG_TYPE));
			if(type == CauldronContents.WATER) {
				return WATER;
			}

			CauldronState state = new CauldronState(type);
			switch(type) {
				case DYE:
					state.color = tags.getInteger(TAG_COLOR);
					break;
				case POTION:
					if(tags.hasKey(TAG_POTION)) {
						state.potion = Potion.getPotionFromResourceLocation(tags.getString(TAG_POTION));
					}
					break;
			}

			return state;
		}


		/* Getters */

		/**
		 * Getst the type of this state
		 * @return  contents type
		 */
		public CauldronContents getType() {
			return type;
		}

		/**
		 * Gets the color for this state
		 * @return  color for the state, or -1 if the type is not DYE
		 */
		public int getColor() {
			return color;
		}

		/**
		 * Gets the potion for this state
		 * @return  potion for the state, or null if the type is not potion
		 */
		@Nullable
		public Potion getPotion() {
			return potion;
		}

		@Override
		public boolean equals(Object other) {
			// basic checks
			if(this == other) {
				return true;
			}
			if(other == null) {
				return false;
			}
			if(this.getClass() != other.getClass()) {
				return false;
			}

			// state check
			CauldronState state = (CauldronState) other;
			if(state.type != this.type) {
				return false;
			}

			// value checks
			if(state.color != this.color) {
				return false;
			}
			if(state.potion == null || !state.potion.equals(this.potion)) {
				return false;
			}

			return true;
		}

		/* NBT */
		public static final String TAG_TYPE = "type";
		public static final String TAG_COLOR = "color";
		public static final String TAG_POTION = "potion";

		/**
		 * Writes this state to NBT
		 * @return  NBTTagCompound of the state
		 */
		@Nonnull
		public NBTTagCompound writeToNBT() {
			NBTTagCompound tags = new NBTTagCompound();
			tags.setInteger(TAG_TYPE, type.getMeta());
			switch(type) {
				case DYE:
					tags.setInteger(TAG_COLOR, color);
					break;
				case POTION:
					if(potion != null) {
						tags.setString(TAG_POTION, potion.getName());
					}
					break;
			}

			return tags;
		}
	}
}
