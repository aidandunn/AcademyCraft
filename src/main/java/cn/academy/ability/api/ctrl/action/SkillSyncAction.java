/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.ctrl.action;

import cn.academy.ability.api.AbilityContext;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import com.google.common.base.Preconditions;
import net.minecraft.world.World;

/**
 * A simple wrapper that setup the commonly used data and sandbox methods for Skill SyncActions.
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class SkillSyncAction<TSkill extends Skill> extends SyncAction {
    
    private AbilityContext ctx;
    private final TSkill skill;
    public World world;

    public SkillSyncAction(TSkill skill) {
        super(-1);
        this.skill = skill;
    }
    
    @Override
    public void onStart() {
        ctx = AbilityContext.of(player, skill);
        world = player.worldObj;
    }

    /**
     * @return The ability context
     */
    protected AbilityContext ctx() {
        return Preconditions.checkNotNull(ctx);
    }
    
}
