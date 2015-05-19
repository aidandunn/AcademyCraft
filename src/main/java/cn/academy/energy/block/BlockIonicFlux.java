/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.liutils.template.client.render.block.RenderEmptyBlock;

/**
 * @author WeAthFolD
 */
@Registrant
public class BlockIonicFlux extends BlockFluidClassic implements ITileEntityProvider {
	
	public static Fluid fluid = new Fluid("imagFlux");
	static { //TODO: @RegFluid
		fluid.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
		FluidRegistry.registerFluid(fluid);
	}
	
	public static Material material = new MaterialLiquid(MapColor.obsidianColor);

	public BlockIonicFlux() {
		super(fluid, Material.water);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_ionic_flux");
		setBlockTextureName("academy:black");
		
		this.setQuantaPerBlock(3);
	}    
	
	@SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass() {
        return 0;
    }

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileIonicFlux();
	}

}