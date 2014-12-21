package cn.academy.api.ctrl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import cn.academy.core.AcademyCraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class SkillStateMessage implements IMessage {
	
	private int playerEntityId;
	private String className;
	private NBTTagCompound nbt;
	
	public SkillStateMessage() {}
	
	public SkillStateMessage(SkillState state) {
		this.playerEntityId = state.player.getEntityId();
		Class<? extends SkillState> clazz = state.getClass();
		this.className = clazz.getName();
		this.nbt = new NBTTagCompound();
		state.toNBT(this.nbt);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		playerEntityId = buf.readInt();
		className = ByteBufUtils.readUTF8String(buf);
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(playerEntityId);
		ByteBufUtils.writeUTF8String(buf, className);
		ByteBufUtils.writeTag(buf, nbt);
	}

	public static class Handler implements IMessageHandler<SkillStateMessage, IMessage> {

		@Override
		public IMessage onMessage(SkillStateMessage msg, MessageContext ctx) {
			SkillState ss = null;
			try {
				Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(msg.playerEntityId);
				if (entity == null) return null;
				if (entity == Minecraft.getMinecraft().thePlayer) return null;
				
				Class<?> clazz = Class.forName(msg.className);
				//FIXME
				
				for (Constructor ctor : clazz.getDeclaredConstructors()) {
					if (ctor.getParameterTypes().length == 1) { 
						if (ctor.getParameterTypes()[0].isAssignableFrom(EntityPlayer.class)) {
							ctor.setAccessible(true);
							ss = (SkillState) ctor.newInstance(entity);
							break;
						}
					} /*else if (ctor.getParameterTypes().length == 2){
						if (ctor.getParameterTypes()[1].isAssignableFrom(EntityPlayer.class)) {
							ctor.setAccessible(true);
							ss = (SkillState) ctor.newInstance(null, (EntityPlayer) entity);
							break;
						}
					}*/
				}
			} catch (Exception e) {
				AcademyCraftMod.log.error("Cannot find constructor for SKillState. Check the implementation of your SkillState.");
				throw new RuntimeException(e);
			}
			if (ss == null)
				throw new RuntimeException("Cannot find constructor for SKillState. Check the implementation of your SkillState.");
			ss.startSkill();
			return null;
		}
		
	}
}
