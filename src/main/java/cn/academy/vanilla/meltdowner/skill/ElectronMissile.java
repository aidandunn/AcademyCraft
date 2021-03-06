/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static cn.lambdalib.util.generic.RandUtils.*;
import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class ElectronMissile extends Skill {
    
    public static final ElectronMissile instance = new ElectronMissile();
    private static final Object delegate = NetworkMessage.staticCaller(ElectronMissile.class);
    
    static int MAX_HOLD = 5;

    private ElectronMissile() {
        super("electron_missile", 5);
    }

    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new EMAction());
    }
    
    public static class EMAction extends SkillSyncAction {
        
        LinkedList<EntityMdBall> active;
        
        int ticks;

        float exp;
        float overload, consumption;
        float overload_attacked, consumption_attacked;

        public EMAction() {
            super(instance);
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = ctx().getSkillExp();
            overload = lerpf(2, 1.5f, exp);
            consumption = lerpf(20, 15, exp);

            overload_attacked = lerpf(61, 32, exp);
            consumption_attacked = lerpf(270, 405, exp);
            
            if(!isRemote) {
                active = new LinkedList<>();
            }
        }
        
        @Override
        public void onTick() {
            if(!ctx().consume(overload, consumption) && !isRemote) {
                ActionManager.abortAction(this);
            } else {
                if(!isRemote) {
                    int timeLimit = (int) lerpf(200, 400, exp);
                    if (ticks <= timeLimit) {
                        if(ticks % 10 == 0) {
                            if(active.size() < MAX_HOLD) {
                                EntityMdBall ball = new EntityMdBall(player);
                                player.worldObj.spawnEntityInWorld(ball);
                                active.add(ball);
                            }
                        }
                        if(ticks != 0 && ticks % 8 == 0) {
                            float range = lerpf(7, 12, exp);

                            List<Entity> list = WorldUtils.getEntities(player, range,
                                    EntitySelectors.exclude(player).and(EntitySelectors.living()));
                            if(!active.isEmpty() && !list.isEmpty() && ctx().consume(
                                    overload_attacked,
                                    consumption_attacked)) {
                                double min = Double.MAX_VALUE;
                                Entity result = null;
                                for(Entity e : list) {
                                    double dist = e.getDistanceToEntity(player);
                                    if(dist < min) {
                                        min = dist;
                                        result = e;
                                    }
                                }

                                // Find a random ball and destroy it
                                int index = 1 + nextInt(active.size());
                                Iterator<EntityMdBall> iter = active.iterator();
                                EntityMdBall ball = null;
                                while(index --> 0)
                                    ball = iter.next();
                                iter.remove();

                                // client action
                                NetworkMessage.sendToAllAround(TargetPoints.convert(player, 15), delegate,
                                        "spawn_ray", player.worldObj,
                                        VecUtils.entityPos(ball),
                                        VecUtils.add(VecUtils.entityPos(result), VecUtils.vec(0, result.getEyeHeight(), 0)));

                                // server action
                                result.hurtResistantTime = -1;

                                float damage = lerpf(14, 27, exp);
                                MDDamageHelper.attack(ctx(), result, damage);
                                ctx().addSkillExp(0.001f);
                                ball.setDead();
                            }
                        }
                    } else { // ticks > timeLimit
                        ActionManager.abortAction(this);
                    }
                } else { // isRemote
                    updateEffect();
                }

                ++ticks;
            }
        }
        
        @Override
        public void onEnd() {
            int cooldown = MathUtils.clampi(100, 300, ticks);
            ctx().setCooldown(cooldown);
        }
        
        @Override
        public void onFinalize() {
            if(!isRemote) {
                for(EntityMdBall ball : active) {
                    ball.setDead();
                }
            }
        }
        
        // CLIENT
        @SideOnly(Side.CLIENT)
        void updateEffect() {
            int count = rangei(1, 3);
            while(count --> 0) {
                double r = ranged(0.5, 1);
                double theta = ranged(0, Math.PI * 2);
                double h = ranged(-1.2, 0);
                Vec3 pos = VecUtils.add(VecUtils.vec(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ),
                        VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)));
                Vec3 vel = VecUtils.vec(ranged(-.02, .02), ranged(.01, .05), ranged(-.02, .02));
                player.worldObj.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(player.worldObj, pos, vel));
            }
        }
        
    }
    
    @SideOnly(Side.CLIENT)
    @Listener(channel="spawn_ray", side=Side.CLIENT)
    private static void hSpawnRay(World world, Vec3 from, Vec3 to) {
        EntityMdRaySmall ray = new EntityMdRaySmall(world);
        ray.setFromTo(from, to);
        world.spawnEntityInWorld(ray);
    }
    
}
