/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.lambdalib.util.entityx.handlers.Life;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.mc.BlockSelectors;
import cn.lambdalib.util.mc.IBlockSelector;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 *
 */
public class ArcGen extends Skill {
    
    public static final ArcGen instance = new ArcGen();
    
    static IBlockSelector blockFilter = new IBlockSelector() {

        @Override
        public boolean accepts(World world, int x, int y, int z, Block block) {
            return block == Blocks.water || block == Blocks.flowing_water || 
                    BlockSelectors.filNormal.accepts(world, x, y, z, block);
        }
        
    };

    private ArcGen() {
        super("arc_gen", 1);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstanceInstant().addExecution(new ArcGenAction());
    }
    
    public static class ArcGenAction extends SyncActionInstant<ArcGen> {

        public ArcGenAction() {
            super(instance);
        }

        @Override
        public boolean validate() {
            AbilityData aData = AbilityData.get(player);
            CPData cpData = CPData.get(player);

            float overload = lerpf(36, 16, aData.getSkillExp(instance));
            float cp = lerpf(117, 135, aData.getSkillExp(instance));

            return cpData.perform(overload, cp);
        }

        @Override
        public void execute() {
            AbilityData aData = AbilityData.get(player);
            World world = player.worldObj;
            
            if(!isRemote) {
                // Perform ray trace
                MovingObjectPosition result = Raytrace.traceLiving(player, getRange(), null, blockFilter);

                if(result != null) {
                    float expincr;
                    if(result.typeOfHit == MovingObjectType.ENTITY) {
                        EMDamageHelper.attack(ctx(), result.entityHit, getDamage());
                        expincr = getExpIncr(true);
                    } else { //BLOCK
                        int hx = result.blockX, hy = result.blockY, hz = result.blockZ;
                        Block block = player.worldObj.getBlock(hx, hy, hz);
                        if(block == Blocks.water) {
                            if(RandUtils.ranged(0, 1) < getFishProb()) {
                                world.spawnEntityInWorld(new EntityItem(
                                    world,
                                    result.hitVec.xCoord,
                                    result.hitVec.yCoord,
                                    result.hitVec.zCoord,
                                    new ItemStack(Items.cooked_fished)));
                                instance.triggerAchievement(player);
                            }
                        } else {
                            if(RandUtils.ranged(0, 1) < getIgniteProb()) {
                                if(world.getBlock(hx, hy + 1, hz) == Blocks.air) {
                                    world.setBlock(hx, hy + 1, hz, Blocks.fire, 0, 0x03);
                                }
                            }
                        }
                        expincr = getExpIncr(false);
                    }
                    aData.addSkillExp(instance, expincr);
                }
            } else {
                spawnEffects();
            }
            
            ctx().setCooldown((int) lerpf(40, 15, aData.getSkillExp(instance)));
        }
        
        @SideOnly(Side.CLIENT)
        private void spawnEffects() {
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.addMotionHandler(new Life(10));
            arc.lengthFixed = false;
            arc.length = getRange();
            
            player.worldObj.spawnEntityInWorld(arc);
            ACSounds.playClient(player, "em.arc_weak", 0.5f);
        }


        private float getDamage() {
            return lerpf(9, 15, ctx().getSkillExp());
        }

        private double getIgniteProb() {
            return lerpf(0, 0.6f, ctx().getSkillExp());
        }

        private float getExpIncr(boolean effectiveHit) {
            if (effectiveHit) {
                return lerpf(0.0048f, 0.0072f, ctx().getSkillExp());
            } else {
                return lerpf(0.0018f, 0.0027f, ctx().getSkillExp());
            }
        }

        private double getFishProb() {
            return ctx().getSkillExp() > 0.5f ? 0.1 : 0;
        }

        private boolean canStunEnemy() {
            return ctx().getSkillExp() >= 1.0f;
        }

        private float getRange() {
            return lerpf(6, 15, ctx().getSkillExp());
        }
        
    }

}
