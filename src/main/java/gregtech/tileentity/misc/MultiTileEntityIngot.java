/**
 * Copyright (c) 2018 Gregorius Techneticies
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech.tileentity.misc;

import static gregapi.data.CS.*;

import gregapi.block.multitileentity.IMultiTileEntity.IMTE_CanEntityDestroy;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetBlockHardness;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetCollisionBoundingBoxFromPool;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetExplosionResistance;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetLightOpacity;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetSelectedBoundingBoxFromPool;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_IsSideSolid;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_SetBlockBoundsBasedOnState;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_SyncDataByteArray;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.BlockTextureDefault;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityQuickObstructionCheck;
import gregapi.tileentity.notick.TileEntityBase03MultiTileEntities;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityIngot extends TileEntityBase03MultiTileEntities implements IMTE_SyncDataByteArray, IMTE_CanEntityDestroy, IMTE_GetBlockHardness, IMTE_IsSideSolid, IMTE_GetLightOpacity, IMTE_GetExplosionResistance, ITileEntityQuickObstructionCheck, IMTE_GetCollisionBoundingBoxFromPool, IMTE_GetSelectedBoundingBoxFromPool, IMTE_SetBlockBoundsBasedOnState {
	public ItemStack mIngot;
	public ITexture mTexture;
	public OreDictMaterial mMaterial = MT.Empty;
	byte mSize = 1;
	
	@Override
	public void readFromNBT2(NBTTagCompound aNBT) {
		mIngot = ST.load(aNBT, NBT_VALUE);
		if (ST.valid(mIngot)) {
			mSize = UT.Code.bindStack(ST.size(mIngot));
			OreDictItemData tData = OM.anydata(mIngot);
			if (tData != null && tData.hasValidMaterialData() && tData.mMaterial.mMaterial.mID > 0) mMaterial = tData.mMaterial.mMaterial;
		}
		super.readFromNBT2(aNBT);
	}
	
	@Override
	public void writeToNBT2(NBTTagCompound aNBT) {
		super.writeToNBT2(aNBT);
		ST.save(aNBT, NBT_VALUE, mIngot);
	}
	
	@Override
	public ArrayListNoNulls<ItemStack> getDrops(int aFortune, boolean aSilkTouch) {
		return new ArrayListNoNulls<>(F, mIngot);
	}
	
	@Override
	public boolean onBlockActivated2(EntityPlayer aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return T;
		ItemStack aStack = aPlayer.getCurrentEquippedItem();
		if (ST.invalid(mIngot) || mIngot.stackSize <= 0) return setToAir();
		if (ST.equal(aStack, mIngot)) {
			if (mIngot.stackSize >= 64) return T;
			if (mIngot.stackSize + aStack.stackSize > 64) {
				aStack.stackSize -= (64-mIngot.stackSize);
				mIngot.stackSize = 64;
				mSize = ST.size(mIngot);
				updateClientData();
				playCollect();
				return T;
			}
			mIngot.stackSize += aStack.stackSize;
			mSize = ST.size(mIngot);
			updateClientData();
			aStack.stackSize = 0;
			playCollect();
			return T;
		}
		if (UT.Inventories.addStackToPlayerInventoryOrDrop(aPlayer, ST.amount(1, mIngot), T, worldObj, xCoord+0.5, yCoord+0.5, zCoord+0.5)) {
			playCollect();
			if (mIngot.stackSize-- <= 0) return setToAir();
			mSize = ST.size(mIngot);
			updateClientData();
		};
		return T;
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		return getClientDataPacketByteArray(aSendAll, UT.Code.toByteS(mMaterial.mID, 0), UT.Code.toByteS(mMaterial.mID, 1), mSize);
	}
	
	@Override
	public boolean receiveDataByteArray(byte[] aData, INetworkHandler aNetworkHandler) {
		mMaterial = OreDictMaterial.MATERIAL_ARRAY[UT.Code.bind15(UT.Code.combine(aData[0], aData[1]))];
		mSize = aData[2];
		return T;
	}
	
	@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return mTexture;}
	
	@Override
	public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {
		mTexture = BlockTextureDefault.get(mMaterial, OP.blockSolid);
		return mSize;
	}
	
	@Override
	public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		switch (aRenderPass) {
		case  0: return box(aBlock, 0.03125, PX_P[ 0], 0.03125, 0.21875, PX_P[ 2], 0.46875);
		case  1: return box(aBlock, 0.28125, PX_P[ 0], 0.03125, 0.46875, PX_P[ 2], 0.46875);
		case  2: return box(aBlock, 0.53125, PX_P[ 0], 0.03125, 0.71875, PX_P[ 2], 0.46875);
		case  3: return box(aBlock, 0.78125, PX_P[ 0], 0.03125, 0.96875, PX_P[ 2], 0.46875);
		case  4: return box(aBlock, 0.03125, PX_P[ 0], 0.53125, 0.21875, PX_P[ 2], 0.96875);
		case  5: return box(aBlock, 0.28125, PX_P[ 0], 0.53125, 0.46875, PX_P[ 2], 0.96875);
		case  6: return box(aBlock, 0.53125, PX_P[ 0], 0.53125, 0.71875, PX_P[ 2], 0.96875);
		case  7: return box(aBlock, 0.78125, PX_P[ 0], 0.53125, 0.96875, PX_P[ 2], 0.96875);
		
		case  8: return box(aBlock, 0.03125, PX_P[ 2], 0.03125, 0.46875, PX_P[ 4], 0.21875);
		case  9: return box(aBlock, 0.03125, PX_P[ 2], 0.28125, 0.46875, PX_P[ 4], 0.46875);
		case 10: return box(aBlock, 0.03125, PX_P[ 2], 0.53125, 0.46875, PX_P[ 4], 0.71875);
		case 11: return box(aBlock, 0.03125, PX_P[ 2], 0.78125, 0.46875, PX_P[ 4], 0.96875);
		case 12: return box(aBlock, 0.53125, PX_P[ 2], 0.03125, 0.96875, PX_P[ 4], 0.21875);
		case 13: return box(aBlock, 0.53125, PX_P[ 2], 0.28125, 0.96875, PX_P[ 4], 0.46875);
		case 14: return box(aBlock, 0.53125, PX_P[ 2], 0.53125, 0.96875, PX_P[ 4], 0.71875);
		case 15: return box(aBlock, 0.53125, PX_P[ 2], 0.78125, 0.96875, PX_P[ 4], 0.96875);
		
		case 16: return box(aBlock, 0.03125, PX_P[ 4], 0.03125, 0.21875, PX_P[ 6], 0.46875);
		case 17: return box(aBlock, 0.28125, PX_P[ 4], 0.03125, 0.46875, PX_P[ 6], 0.46875);
		case 18: return box(aBlock, 0.53125, PX_P[ 4], 0.03125, 0.71875, PX_P[ 6], 0.46875);
		case 19: return box(aBlock, 0.78125, PX_P[ 4], 0.03125, 0.96875, PX_P[ 6], 0.46875);
		case 20: return box(aBlock, 0.03125, PX_P[ 4], 0.53125, 0.21875, PX_P[ 6], 0.96875);
		case 21: return box(aBlock, 0.28125, PX_P[ 4], 0.53125, 0.46875, PX_P[ 6], 0.96875);
		case 22: return box(aBlock, 0.53125, PX_P[ 4], 0.53125, 0.71875, PX_P[ 6], 0.96875);
		case 23: return box(aBlock, 0.78125, PX_P[ 4], 0.53125, 0.96875, PX_P[ 6], 0.96875);
		
		case 24: return box(aBlock, 0.03125, PX_P[ 6], 0.03125, 0.46875, PX_P[ 8], 0.21875);
		case 25: return box(aBlock, 0.03125, PX_P[ 6], 0.28125, 0.46875, PX_P[ 8], 0.46875);
		case 26: return box(aBlock, 0.03125, PX_P[ 6], 0.53125, 0.46875, PX_P[ 8], 0.71875);
		case 27: return box(aBlock, 0.03125, PX_P[ 6], 0.78125, 0.46875, PX_P[ 8], 0.96875);
		case 28: return box(aBlock, 0.53125, PX_P[ 6], 0.03125, 0.96875, PX_P[ 8], 0.21875);
		case 29: return box(aBlock, 0.53125, PX_P[ 6], 0.28125, 0.96875, PX_P[ 8], 0.46875);
		case 30: return box(aBlock, 0.53125, PX_P[ 6], 0.53125, 0.96875, PX_P[ 8], 0.71875);
		case 31: return box(aBlock, 0.53125, PX_P[ 6], 0.78125, 0.96875, PX_P[ 8], 0.96875);
		
		case 32: return box(aBlock, 0.03125, PX_P[ 8], 0.03125, 0.21875, PX_P[10], 0.46875);
		case 33: return box(aBlock, 0.28125, PX_P[ 8], 0.03125, 0.46875, PX_P[10], 0.46875);
		case 34: return box(aBlock, 0.53125, PX_P[ 8], 0.03125, 0.71875, PX_P[10], 0.46875);
		case 35: return box(aBlock, 0.78125, PX_P[ 8], 0.03125, 0.96875, PX_P[10], 0.46875);
		case 36: return box(aBlock, 0.03125, PX_P[ 8], 0.53125, 0.21875, PX_P[10], 0.96875);
		case 37: return box(aBlock, 0.28125, PX_P[ 8], 0.53125, 0.46875, PX_P[10], 0.96875);
		case 38: return box(aBlock, 0.53125, PX_P[ 8], 0.53125, 0.71875, PX_P[10], 0.96875);
		case 39: return box(aBlock, 0.78125, PX_P[ 8], 0.53125, 0.96875, PX_P[10], 0.96875);
		
		case 40: return box(aBlock, 0.03125, PX_P[10], 0.03125, 0.46875, PX_P[12], 0.21875);
		case 41: return box(aBlock, 0.03125, PX_P[10], 0.28125, 0.46875, PX_P[12], 0.46875);
		case 42: return box(aBlock, 0.03125, PX_P[10], 0.53125, 0.46875, PX_P[12], 0.71875);
		case 43: return box(aBlock, 0.03125, PX_P[10], 0.78125, 0.46875, PX_P[12], 0.96875);
		case 44: return box(aBlock, 0.53125, PX_P[10], 0.03125, 0.96875, PX_P[12], 0.21875);
		case 45: return box(aBlock, 0.53125, PX_P[10], 0.28125, 0.96875, PX_P[12], 0.46875);
		case 46: return box(aBlock, 0.53125, PX_P[10], 0.53125, 0.96875, PX_P[12], 0.71875);
		case 47: return box(aBlock, 0.53125, PX_P[10], 0.78125, 0.96875, PX_P[12], 0.96875);
		
		case 48: return box(aBlock, 0.03125, PX_P[12], 0.03125, 0.21875, PX_P[14], 0.46875);
		case 49: return box(aBlock, 0.28125, PX_P[12], 0.03125, 0.46875, PX_P[14], 0.46875);
		case 50: return box(aBlock, 0.53125, PX_P[12], 0.03125, 0.71875, PX_P[14], 0.46875);
		case 51: return box(aBlock, 0.78125, PX_P[12], 0.03125, 0.96875, PX_P[14], 0.46875);
		case 52: return box(aBlock, 0.03125, PX_P[12], 0.53125, 0.21875, PX_P[14], 0.96875);
		case 53: return box(aBlock, 0.28125, PX_P[12], 0.53125, 0.46875, PX_P[14], 0.96875);
		case 54: return box(aBlock, 0.53125, PX_P[12], 0.53125, 0.71875, PX_P[14], 0.96875);
		case 55: return box(aBlock, 0.78125, PX_P[12], 0.53125, 0.96875, PX_P[14], 0.96875);
		
		case 56: return box(aBlock, 0.03125, PX_P[14], 0.03125, 0.46875, PX_P[16], 0.21875);
		case 57: return box(aBlock, 0.03125, PX_P[14], 0.28125, 0.46875, PX_P[16], 0.46875);
		case 58: return box(aBlock, 0.03125, PX_P[14], 0.53125, 0.46875, PX_P[16], 0.71875);
		case 59: return box(aBlock, 0.03125, PX_P[14], 0.78125, 0.46875, PX_P[16], 0.96875);
		case 60: return box(aBlock, 0.53125, PX_P[14], 0.03125, 0.96875, PX_P[16], 0.21875);
		case 61: return box(aBlock, 0.53125, PX_P[14], 0.28125, 0.96875, PX_P[16], 0.46875);
		case 62: return box(aBlock, 0.53125, PX_P[14], 0.53125, 0.96875, PX_P[16], 0.71875);
		case 63: return box(aBlock, 0.53125, PX_P[14], 0.78125, 0.96875, PX_P[16], 0.96875);
		}
		return T;
	}
	
	@Override public void setBlockBoundsBasedOnState(Block aBlock) {box(aBlock, 0, 0, 0, 1, UT.Code.divup(mSize, 8) / 8.0F, 1);}
	@Override public AxisAlignedBB getSelectedBoundingBoxFromPool () {return box(0, 0, 0, 1, UT.Code.divup(mSize, 8) / 8.0F, 1);}
	@Override public AxisAlignedBB getCollisionBoundingBoxFromPool() {return mSize < 8 ? null : box(0, 0, 0, 1, (mSize / 8) / 8.0F, 1);}
	
	@Override public boolean isSurfaceSolid         (byte aSide) {return F;}
	@Override public boolean isSurfaceOpaque        (byte aSide) {return F;}
	@Override public boolean isSideSolid            (byte aSide) {return F;}
	@Override public boolean isObstructingBlockAt   (byte aSide) {return F;}
	@Override public boolean checkObstruction(EntityPlayer aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	@Override public boolean canEntityDestroy(Entity aEntity) {return !(aEntity instanceof EntityDragon);}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	@Override public float getExplosionResistance2() {return 0;}
	@Override public float getBlockHardness() {return 0.25F;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.ingot";}
}
