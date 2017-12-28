package knightminer.inspirations.tweaks;

import java.util.List;

import knightminer.inspirations.common.Config;
import knightminer.inspirations.library.InspirationsRegistry;
import knightminer.inspirations.library.recipe.cauldron.ICauldronRecipe;
import knightminer.inspirations.shared.InspirationsShared;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;

public class TweaksEvents {

	@SubscribeEvent
	public static void unsaddlePig(EntityInteract event) {
		if(!Config.enablePigDesaddle) {
			return;
		}

		EntityPlayer player = event.getEntityPlayer();
		ItemStack stack = player.getHeldItem(event.getHand());
		// must be sneaking and holding nothing
		if(player.isSneaking() && stack.isEmpty()) {
			Entity target = event.getTarget();
			if(target instanceof EntityPig) {
				EntityPig pig = (EntityPig) target;
				if(pig.getSaddled()) {
					pig.setSaddled(false);
					pig.world.playSound(player, pig.posX, pig.posY, pig.posZ, SoundEvents.ENTITY_PIG_SADDLE, SoundCategory.NEUTRAL, 0.5F, 1.0F);
					ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(Items.SADDLE), player.inventory.currentItem);
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public static void extraBonemeal(BonemealEvent event) {
		if(!Config.enableExtraBonemeal) {
			return;
		}

		// running client side acts weird
		World world = event.getWorld();
		if(world.isRemote) {
			return;
		}

		BlockPos pos = event.getPos();
		Block block = world.getBlockState(pos).getBlock();
		boolean isMycelium = block == Blocks.MYCELIUM;
		// block must be mycelium for mushrooms or sand for dead bushes
		if(!isMycelium && block != Blocks.SAND) {
			return;
		}

		// this is mostly copied from grass block code, so its a bit weird
		BlockPos up = pos.up();
		BlockBush bush = Blocks.DEADBUSH;
		IBlockState state = bush.getDefaultState();

		// 128 chances, this affects how far blocks are spread
		for (int i = 0; i < 128; ++i) {
			BlockPos next = up;
			int j = 0;

			while (true)  {
				// the longer we go, the closer to old blocks we place the block
				if (j >= i / 16) {
					if (world.isAirBlock(next)) {
						if (world.rand.nextInt(128) == 0) {
							// mycelium randomly picks between red and brown
							if(isMycelium) {
								bush = world.rand.nextInt(2) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM;
								state = bush.getDefaultState();
							}
							// if it can be planted here, plant it
							if(bush.canBlockStay(world, next, state)) {
								world.setBlockState(next, state);
							}
						}
					}

					break;
				}

				// randomly offset the position
				next = next.add(world.rand.nextInt(3) - 1, (world.rand.nextInt(3) - 1) * world.rand.nextInt(3) / 2, world.rand.nextInt(3) - 1);

				// if the new position is invalid, this cycle is done
				if (world.getBlockState(next.down()).getBlock() != block|| world.getBlockState(next).isNormalCube()) {
					break;
				}

				++j;
			}
		}

		event.setResult(Result.ALLOW);
	}

	@SubscribeEvent
	public static void dropHeartroot(HarvestDropsEvent event) {
		if(!Config.enableHeartbeet || event.getState().getBlock() != Blocks.BEETROOTS) {
			return;
		}

		// we get a base of two chances, and each fortune level adds one more
		int rolls = event.getFortuneLevel() + 2;
		// up to fortune 4 we will keep, any higher just ignore
		if(rolls > 6) {
			rolls = 6;
		}

		List<ItemStack> drops = event.getDrops();
		// find the first beetroot from the drops
		iterator:
			for(ItemStack stack : drops) {
				// as soon as we find one, chance to replace it
				if(stack.getItem() == Items.BEETROOT) {
					// for each roll, try to get the drop once
					for(int i = 0; i < rolls; i++) {
						if(event.getWorld().rand.nextInt(100) == 0) {
							stack.shrink(1);
							if(stack.isEmpty()) {
								drops.remove(stack);
							}
							drops.add(InspirationsShared.heartbeet.copy());
							// cap at one heartroot in case we get extras, plus prevents concurrent modification
							break iterator;
						}
					}
				}
			}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void vineBreakEvent(BreakEvent event) {
		if(!Config.harvestHangingVines) {
			return;
		}

		// stop if on client or already canceled
		if(event.isCanceled()) {
			return;
		}
		World world = event.getWorld();
		if(world.isRemote) {
			return;
		}

		// check conditions: must be shearing vines and not creative
		EntityPlayer player = event.getPlayer();
		if(player.capabilities.isCreativeMode) {
			return;
		}
		Block block = event.getState().getBlock();
		if(!(block instanceof BlockVine)) {
			return;
		}
		ItemStack shears = player.getHeldItemMainhand();
		Item item = shears.getItem();
		if(!(item instanceof ItemShears || item.getToolClasses(shears).contains("shears"))) {
			return;
		}

		BlockPos pos = event.getPos().down();
		BlockVine vine = (BlockVine) block;
		IBlockState state = world.getBlockState(pos);

		// iterate down until we find either a non-vine or the vine can stay
		int count = 0;
		while(state.getBlock() == block && vine.isShearable(shears, world, pos) && !vineCanStay(vine, world, state, pos)) {
			count++;
			for(ItemStack stack : vine.onSheared(shears, world, pos, 0)) {
				Block.spawnAsEntity(world, pos, stack);
			}
			pos = pos.down();
			state = world.getBlockState(pos);
		}
		// break all the vines we dropped as items,
		// mainly for safety even though vines should break it themselves
		for(int i = 0; i < count; i++) {
			pos = pos.up();
			world.setBlockToAir(pos);
		}
	}

	private static boolean vineCanStay(BlockVine vine, World world, IBlockState state, BlockPos pos) {
		// check if any of the four sides allows the vine to stay
		for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
			if (state.getValue(BlockVine.getPropertyFor(side)) && vine.canAttachTo(world, pos, side.getOpposite())) {
				return true;
			}
		}

		return false;
	}

	@SubscribeEvent
	public static void clickCauldron(RightClickBlock event) {
		if(!Config.enableCauldronRecipes || Config.enableCauldronDyeing) {
			return;
		}

		World world = event.getWorld();
		BlockPos pos = event.getPos();
		IBlockState state = world.getBlockState(pos);
		if(state.getBlock() != Blocks.CAULDRON) {
			return;
		}
		int level = state.getValue(BlockCauldron.LEVEL);
		if(level == 0) {
			return;
		}

		ItemStack stack = event.getItemStack();
		if(stack.isEmpty()) {
			return;
		}

		boolean isBoiling = world.getBlockState(pos.down()).getBlock() instanceof BlockFire;
		ICauldronRecipe recipe = InspirationsRegistry.getCauldronResult(stack, isBoiling, level);
		if(recipe != null) {
			if(!world.isRemote) {
				int newLevel = MathHelper.clamp(recipe.getLevel(level), 0, 3);
				if(newLevel != level) {
					world.setBlockState(pos, Blocks.CAULDRON.getDefaultState().withProperty(BlockCauldron.LEVEL, newLevel));
					world.updateComparatorOutputLevel(pos, Blocks.CAULDRON);
				}
				world.playSound((EntityPlayer)null, pos, SoundEvents.ENTITY_BOBBER_SPLASH, SoundCategory.BLOCKS, 0.3F, 1.0F);

				ItemStack result = recipe.getResult(stack, isBoiling, level);
				stack.shrink(1);
				if(!result.isEmpty()) {
					EntityPlayer player = event.getEntityPlayer();
					ItemHandlerHelper.giveItemToPlayer(player, result, player.inventory.currentItem);
				}
			}

			event.setCanceled(true);
			event.setCancellationResult(EnumActionResult.SUCCESS);
		}
	}
}
