package cn.academy.vanilla.vecmanip.client.effect

import cn.academy.core.Resources
import cn.academy.core.entity.LocalEntity
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{RegEntity, RegInitCallback}
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.deprecated.{Mesh, MeshUtils, SimpleMaterial}
import cn.lambdalib.util.generic.{MathUtils, RandUtils}
import cn.lambdalib.vis.curve.CubicCurve
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

import scala.collection.mutable

@SideOnly(Side.CLIENT)
@Registrant
object WaveEffect {

  @RegInitCallback
  def init() = {
    RenderingRegistry.registerEntityRenderingHandler(classOf[WaveEffect], new WaveEffectRenderer)
  }

}

@SideOnly(Side.CLIENT)
@Registrant
class WaveEffect(world: World, val rings: Int, val size: Double) extends LocalEntity(world) {

  class Ring(val life: Int, var offset: Double, val size: Double, val timeOffset: Int)

  val ringList = new mutable.MutableList[Ring]
  val life = 15

  (0 until rings).foreach(idx => {
    ringList += new Ring(
      RandUtils.rangei(8, 12),
      idx * 1.5 + RandUtils.ranged(-.3, .3),
      size * RandUtils.ranged(0.8, 1.2),
      idx * 2 + RandUtils.rangei(-1, 1))
  })

  ignoreFrustumCheck = true

  override def onUpdate() = {
    if (ticksExisted >= life) {
      setDead()
    }
  }

  override def shouldRenderInPass(pass: Int) = pass == 1
}

@SideOnly(Side.CLIENT)
class WaveEffectRenderer extends Render {
  import org.lwjgl.opengl.GL11._
  import cn.lambdalib.util.generic.MathUtils._

  val alphaCurve = new CubicCurve()
  alphaCurve.addPoint(0, 0)
  alphaCurve.addPoint(0.2, 1)
  alphaCurve.addPoint(0.5, 1)
  alphaCurve.addPoint(0.8, 1)
  alphaCurve.addPoint(1, 0)

  val texture = Resources.getTexture("effects/glow_circle")

  val mesh = new Mesh()
  val material = new SimpleMaterial(texture).setIgnoreLight()
  MeshUtils.createBillboard(mesh, -.5, -.5, 1, 1)

  val sizeCurve = new CubicCurve
  sizeCurve.addPoint(0, 0.4)
  sizeCurve.addPoint(0.2, 0.8)
  sizeCurve.addPoint(2.5, 1.5)


  override def doRender(entity: Entity, x: Double, y: Double, z: Double, v3: Float, v4: Float) = entity match {
    case effect: WaveEffect =>
      val maxAlpha = clampd(0, 1, alphaCurve.valueAt(effect.ticksExisted.toDouble / effect.life))

      glDisable(GL_CULL_FACE)
      glDisable(GL_DEPTH_TEST)
      glEnable(GL_BLEND)
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
      glPushMatrix()
      glTranslated(x, y, z)
      glRotated(-entity.rotationYaw, 0, 1, 0)
      glRotated(entity.rotationPitch, 1, 0, 0)

      val zOffset = entity.ticksExisted / 40.0

      glTranslated(0, 0, zOffset)

      effect.ringList.foreach(ring => {
        val alpha = clampd(0, 1, alphaCurve.valueAt((effect.ticksExisted - ring.timeOffset).toDouble / ring.life))
        val realAlpha = Math.min(maxAlpha, alpha)

        if (realAlpha > 0) {
          glPushMatrix()
          glTranslated(0, 0, ring.offset)

          val sizeScale = sizeCurve.valueAt(MathUtils.clampd(0, 1.62, entity.ticksExisted / 20.0))
          glScaled(ring.size * sizeScale, ring.size * sizeScale, 1)
          material.color.a =  realAlpha * 0.7
          mesh.draw(material)
          glPopMatrix()
        }
      })

      glPopMatrix()
      glEnable(GL_CULL_FACE)
      glEnable(GL_DEPTH_TEST)
  }

  override def getEntityTexture(entity: Entity) = null
}