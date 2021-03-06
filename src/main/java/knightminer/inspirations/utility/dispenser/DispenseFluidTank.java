package knightminer.inspirations.utility.dispenser;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class DispenseFluidTank extends BehaviorDefaultDispenseItem {
	private static final BehaviorDefaultDispenseItem DEFAULT = new BehaviorDefaultDispenseItem();
	private IBehaviorDispenseItem fallback;
	public DispenseFluidTank(IBehaviorDispenseItem fallback) {
		this.fallback = fallback;
	}

	@Override
	protected ItemStack dispenseStack(IBlockSource source, ItemStack stack){
		EnumFacing side = source.getBlockState().getValue(BlockDispenser.FACING);
		BlockPos pos = source.getBlockPos().offset(side);
		World world = source.getWorld();
		IFluidHandler handler = FluidUtil.getFluidHandler(world, pos, side.getOpposite());
		if(handler != null) {
			FluidActionResult result;
			FluidStack fluid = FluidUtil.getFluidContained(stack);
			if(fluid != null) {
				result = FluidUtil.tryEmptyContainer(stack, handler, Integer.MAX_VALUE, null, true);
			} else {
				result = FluidUtil.tryFillContainer(stack, handler, Integer.MAX_VALUE, null, true);
			}

			if(result.isSuccess()) {
				ItemStack resultStack = result.getResult();
				// play sound
				SoundEvent sound;
				if(fluid != null) {
					sound = fluid.getFluid().getEmptySound(fluid);
				} else {
					FluidStack resultFluid = FluidUtil.getFluidContained(resultStack);
					sound = resultFluid.getFluid().getFillSound(resultFluid);
				}
				world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);

				if(stack.getCount() == 1) {
					return resultStack;
				}

				if(!resultStack.isEmpty() && ((TileEntityDispenser)source.getBlockTileEntity()).addItemStack(resultStack) < 0) {
					DEFAULT.dispense(source, resultStack);
				}

				ItemStack shrink = stack.copy();
				shrink.shrink(1);
				return shrink;
			}
			// TODO: fallback?
			return DEFAULT.dispense(source, stack);
		}

		return fallback.dispense(source, stack);
	}
}
