package com.mrcrayfish.furniture.items;

import com.mrcrayfish.furniture.MrCrayfishFurnitureMod;
import com.mrcrayfish.furniture.init.FurnitureItems;
import com.mrcrayfish.furniture.tileentity.TileEntityMailBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemPackage extends Item implements IItemInventory, IAuthored
{
    public ItemPackage()
    {
        setMaxStackSize(1);
        setCreativeTab(MrCrayfishFurnitureMod.tabFurniture);
    }

    @Override
    public Item getSignedItem()
    {
        return FurnitureItems.PACKAGE_SIGNED;
    }

    @Override
    public boolean getShareTag()
    {
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        TileEntity tile_entity = worldIn.getTileEntity(pos);
        if(!worldIn.isRemote)
        {
            if(tile_entity instanceof TileEntityMailBox)
            {
                if(player.isSneaking() && !worldIn.isRemote)
                {
                    player.sendMessage(new TextComponentTranslation("cfm.message.package_sign"));
                }
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        ItemStack stack = playerIn.getHeldItem(hand);
        if(!worldIn.isRemote)
        {
            playerIn.openGui(MrCrayfishFurnitureMod.instance, 7, worldIn, 0, 0, 0);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}