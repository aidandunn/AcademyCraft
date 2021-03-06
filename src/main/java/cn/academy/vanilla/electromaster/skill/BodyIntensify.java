/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.electromaster.client.effect.CurrentChargingHUD;
import cn.academy.vanilla.electromaster.entity.EntityIntensifyEffect;
import cn.lambdalib.util.client.auxgui.AuxGuiHandler;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * Body Intensify/生物电强化
 * @author WeAthFolD
 */
public class BodyIntensify extends Skill {
    
    static final int MIN_TIME = 10, MAX_TIME = 40, MAX_TOLERANT_TIME = 100;
    static final String LOOP_SOUND = "em.intensify_loop", ACTIVATE_SOUND = "em.intensify_activate";
    
    static final List<PotionEffect> effects = new ArrayList<>();
    static {
        effects.add(new PotionEffect(Potion.moveSpeed.id, 0, 3));
        effects.add(new PotionEffect(Potion.jump.id, 0, 1));
        effects.add(new PotionEffect(Potion.regeneration.id, 0, 1));
        effects.add(new PotionEffect(Potion.damageBoost.id, 0, 1));
        effects.add(new PotionEffect(Potion.resistance.id, 0, 1));
    }
    
    public static final BodyIntensify instance = new BodyIntensify();
    
    private static PotionEffect createEffect(PotionEffect effect, int level, int duration) {
        return new PotionEffect(effect.getPotionID(), duration, Math.min(level, effect.getAmplifier()), effect.getIsAmbient());
    }

    private BodyIntensify() {
        super("body_intensify", 3);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new IntensifyAction());
    }
    
    public static class IntensifyAction extends SkillSyncAction<BodyIntensify> {
        
        int tick;
        float exp;
        float consumption;

        public IntensifyAction() {
            super(instance);
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = ctx().getSkillExp();

            consumption = lerpf(20, 15, exp);

            float overload = lerpf(200, 120, exp);
            ctx().consume(overload, 0);
            
            if(isRemote) {
                startEffect();
            }
        }
        
        @Override
        public void onTick() {
            tick++;
            if(!isRemote && 
               ((tick <= MAX_TIME && !ctx().consume(0, consumption)) ||
                 tick >= MAX_TOLERANT_TIME) ) {
                ActionManager.endAction(this);
            }
            
            if(isRemote)
                updateEffect();
        }
        
        // Synchronize tick to prevent corrupted logic.
        @Override
        public void readNBTFinal(NBTTagCompound tag) { 
            tick = tag.getInteger("t");
        }
        
        @Override
        public void writeNBTFinal(NBTTagCompound tag) { 
            tag.setInteger("t", tick);
        }
        
        @Override
        public void onEnd() {
            if(tick >= MIN_TIME) {
                if(tick >= MAX_TIME) tick = MAX_TIME;
                
                if(!isRemote) {
                    Collections.shuffle(effects);
                    
                    double p = getProbability(tick);
                    int i = 0;
                    int time = getBuffTime(tick);
                    
                    while(p > 0) {
                        double a = RandUtils.ranged(0, 1);
                        if(a < p) {
                            // Spawn a new buff
                            int level = getBuffLevel(tick);
                            player.addPotionEffect(createEffect(effects.get(i++), level, time));
                        }
                        
                        p -= 1.0;
                    }
                    
                    // Also give him a hunger buff
                    player.addPotionEffect(new PotionEffect(Potion.hunger.id, getHungerBuffTime(tick), 2));
                    instance.triggerAchievement(player);
                }

                ctx().addSkillExp(0.01f);

                int cooldown = (int) lerpf(45, 30, exp);
                ctx().setCooldown(cooldown);
                
                if(isRemote) 
                    endEffect(true);
                
            } else {
                if(isRemote) 
                    endEffect(false);
            }
        }
        
        @Override
        public void onAbort() {
            if(isRemote) 
                endEffect(false);
        }
        
        // CLIENT
        
        @SideOnly(Side.CLIENT)
        FollowEntitySound loopSound;
        
        @SideOnly(Side.CLIENT)
        CurrentChargingHUD hud;
        
        @SideOnly(Side.CLIENT)
        public void startEffect() {
            if(isLocal()) {
                ACSounds.playClient(loopSound = new FollowEntitySound(player, LOOP_SOUND).setLoop());
                
                AuxGuiHandler.register(hud = new CurrentChargingHUD());
            }
        }
        
        @SideOnly(Side.CLIENT)
        public void updateEffect() {
            // N/A
        }
        
        @SideOnly(Side.CLIENT)
        public void endEffect(boolean performed) {
            if(isLocal()) {
                if(loopSound != null)
                    loopSound.stop();
                if(hud != null)
                    hud.startBlend(performed);
            }
            
            if(performed) {
                ACSounds.playClient(player, ACTIVATE_SOUND, 0.5f);
                player.worldObj.spawnEntityInWorld(new EntityIntensifyEffect(player));
            }
        }

        // DATA

        // CT: ChargeTime

        private double getProbability(int ct) {
            return (ct - 10.0) / 18.0;
        }

        private int getBuffTime(int ct) {
            return (int) (4 * RandUtils.ranged(1, 2) * ct *
                    MathUtils.lerp(1.5, 2.5, ctx().getSkillExp()));
        }

        private int getHungerBuffTime(int ct) {
            return (int) (1.25f * ct);
        }

        private int getBuffLevel(int ct) {
            return (int) (MathUtils.lerp(0.5, 1, ctx().getSkillExp()) * (ct / 18.0));
        }
        
    }
    
}
