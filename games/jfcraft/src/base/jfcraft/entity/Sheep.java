package jfcraft.entity;

/** Sheep entity
 *
 * @author pquiring
 *
 * Created : Aug 10, 2014
 */

import java.util.*;

import javaforce.*;
import javaforce.gl.*;
import jfcraft.client.Client;

import jfcraft.audio.*;
import jfcraft.data.*;
import jfcraft.item.*;
import jfcraft.opengl.*;

public class Sheep extends CreatureBase {
  public boolean hasFur;

  private transient float walkAngle;  //angle of legs/arms as walking
  private transient float walkAngleDelta;

  //render assets
  private static RenderDest dest;
  private static String textureName, furName;
  private static Texture texture, furTexture;

  public static int initHealth = 10;

  public Sheep() {
    super();
    id = Entities.SHEEP;
  }

  public RenderDest getDest() {
    return dest;
  }

  public String getName() {
    return "Sheep";
  }

  public void init() {
    super.init();
    isStatic = true;
    width = 0.6f;
    width2 = width/2;
    height = 1.0f;
    height2 = height/2;
    depth = 1.8f;
    depth2 = depth/2;
    walkAngleDelta = 1.0f;
    if (Static.isServer()) {
      eyeHeight = 0.5f;
      jumpVelocity = 0.58f;  //results in jump of 1.42
      //speeds are blocks per second
      walkSpeed = 2.3f;
      runSpeed = 3.9f;
      sneakSpeed = 1.3f;
      swimSpeed = (walkSpeed / 2.0f);
    }
  }

  public void initStatic() {
    textureName = "entity/sheep/sheep";
    furName = "entity/sheep/sheep_fur";
  }

  public void initStatic(GL gl) {
    texture = Textures.getTexture(gl, textureName);
    dest = new RenderDest(parts.length);
  }

  private static String parts[] = {
    "HEAD", "BODY", "L_ARM", "R_ARM", "L_LEG", "R_LEG",
    "HEAD_FUR", "BODY_FUR", "L_ARM_FUR", "R_ARM_FUR", "L_LEG_FUR", "R_LEG_FUR"
  };

  public void buildBuffers(RenderDest dest, RenderData data) {
    GLModel mod = loadModel("sheep");
    //transfer data into dest
    for(int a=0;a<parts.length;a++) {
      RenderBuffers buf = dest.getBuffers(a);
      GLObject obj = mod.getObject(parts[a]);
      buf.addVertex(obj.vpl.toArray());
      buf.addPoly(obj.vil.toArray());
      int cnt = obj.vpl.size();
      for(int b=0;b<cnt;b++) {
        buf.addDefault();
      }
      if (obj.maps.size() == 1) {
        GLUVMap map = obj.maps.get(0);
        buf.addTextureCoords(map.uvl.toArray());
      } else {
        GLUVMap map1 = obj.maps.get(0);
        GLUVMap map2 = obj.maps.get(1);
        buf.addTextureCoords(map1.uvl.toArray(), map2.uvl.toArray());
      }
      buf.org = obj.org;
      buf.type = obj.type;
    }
  }

  public void copyBuffers(GL gl) {
    dest.copyBuffers(gl);
    furTexture = Textures.getTexture(gl, furName);
  }

  public void bindTexture(GL gl) {
    texture.bind(gl);
  }

  private void setMatrixModel(GL gl, int bodyPart, RenderBuffers buf) {
    mat.setIdentity();
    mat.addRotate(-ang.y, 0, 1, 0);
    switch (bodyPart) {
      case HEAD:
        mat.addTranslate2(0, buf.org.y, 0);
        mat.addRotate2(-ang.x, 1, 0, 0);
        mat.addTranslate2(0, -buf.org.y, 0);
        break;
      case BODY:
        break;
      case L_ARM:
      case R_LEG:
        mat.addTranslate2(buf.org.x, buf.org.y, buf.org.z);
        mat.addRotate2(walkAngle, 1, 0, 0);
        mat.addTranslate2(-buf.org.x, -buf.org.y, -buf.org.z);
        break;
      case R_ARM:
      case L_LEG:
        mat.addTranslate2(buf.org.x, buf.org.y, buf.org.z);
        mat.addRotate2(-walkAngle, 1, 0, 0);
        mat.addTranslate2(-buf.org.x, -buf.org.y, -buf.org.z);
        break;
    }
    mat.addTranslate(pos.x, pos.y, pos.z);
    gl.glUniformMatrix4fv(Static.uniformMatrixModel, 1, GL.GL_FALSE, mat.m);  //model matrix
    if (moving) {
      walkAngle += walkAngleDelta;
      if ((walkAngle < -45.0) || (walkAngle > 45.0)) {
        walkAngleDelta *= -1;
      }
    } else {
      walkAngle = 0.0f;
    }
  }

  public void render(GL gl) {
    for(int a=0;a<6;a++) {
      RenderBuffers buf = dest.getBuffers(a);
      setMatrixModel(gl, a, buf);
      dest.getBuffers(a).bindBuffers(gl);
      dest.getBuffers(a).render(gl);
    }
    if (hasFur) {
      furTexture.bind(gl);
      for(int a=6;a<12;a++) {
        RenderBuffers buf = dest.getBuffers(a);
        setMatrixModel(gl, a-6, buf);
        dest.getBuffers(a).bindBuffers(gl);
        dest.getBuffers(a).render(gl);
      }
    }
  }

  public transient boolean walking;
  public transient int walkLength;

  public void tick() {
    super.tick();
    //do AI
    updateFlags();
    boolean fell;
    if (inWater && !jumping) {
      fell = gravity(0.5f + (float)Math.sin(floatRad) * 0.25f);
      floatRad += 0.314f;
      if (floatRad > Static.PIx2) floatRad = 0f;
    } else {
      fell = gravity(0);
    }
    boolean wasMoving = moving;
    //random walking
    if (Static.debugRotate) {
      //test rotate in a spot
      ang.y += 1.0f;
      if (ang.y > 180f) { ang.y = -180f; }
      ang.x += 1.0f;
      if (ang.x > 45.0f) { ang.x = -45.0f; }
      moving = true;
    } else {
      randomWalking();
      if (moving) {
        moveEntity();
      } else {
        vel.x = 0;
        vel.z = 0;
      }
    }
    if (fell || moving || wasMoving) Static.server.broadcastEntityMove(this);
  }

  private static Random r = new Random();
  public EntityBase spawn(Chunk chunk) {
    World world = Static.world();
    float px = r.nextInt(16) + chunk.cx * 16.0f;
    float pz = r.nextInt(16) + chunk.cz * 16.0f;
    for(float gy = 255;gy>0;gy--) {
      float py = gy;
      if (world.isEmpty(chunk.dim,px,py,pz)
        && world.isEmpty(chunk.dim,px,py-1,pz)
        && world.canSpawnOn(chunk.dim,px,py-2,pz))
      {
        py -= 1;
        Sheep e = new Sheep();
        e.init();
        e.health = initHealth;
        e.hasFur = true;
        e.pos.x = px;
        e.pos.y = py;
        e.pos.z = pz;
        return e;
      }
    }
    return null;
  }
  public float getSpawnRate() {
    return 2.0f;
  }

  public Item[] drop() {
    Random r = new Random();
    Item items[] = new Item[1];
    items[0] = new Item(Blocks.WOOL, 0, r.nextInt(2)+1);
    return items;
  }
  public boolean useTool(Client client, Coords c) {
    Static.log("Sheep:useTool");
    synchronized(client.lock) {
      char toolid = client.player.items[client.activeSlot].id;
      ItemBase tool = Static.items.items[toolid];
      if (tool.isTool && tool.tool == Items.TOOL_SHEARS) {
        if (hasFur) {
          hasFur = false;
          Static.server.broadcastSheepSheared(this);
          Random r = new Random();
          int cnt = r.nextInt(2) + 1;
          for(int a=0;a<cnt;a++) {
            Item item = new Item(Blocks.WOOL, 0, 1);
            if (item.id == Blocks.AIR) continue;
            WorldItem e = new WorldItem();
            e.setItem(item);
            e.init();
            e.dim = dim;
            e.uid = Static.world().generateUID();
            e.pos.x = pos.x + 0.5f;
            e.pos.y = pos.y + e.height2;
            e.pos.z = pos.z + 0.5f;
            e.vel.x = r.nextFloat() / 10.0f;
            e.vel.y = Math.abs(r.nextFloat() / 5.0f);
            e.vel.z = r.nextFloat() / 10.0f;
            e.age = 3 * 20;  //can pick up right away
            getChunk().addEntity(e);
            Static.server.world.addEntity(e);
            Static.server.broadcastEntitySpawn(e);
          }
        }
        return true;
      }
    }
    return false;
  }
  public void hit() {
    super.hit();
    if (sound == 0) {
      sound = 2 * 20;
      Static.server.broadcastSound(dim, pos.x, pos.y, pos.z, Sounds.SOUND_SHEEP, 1);
    }
  }

  private static final byte ver = 0;

  @Override
  public boolean write(SerialBuffer buffer, boolean file) {
    super.write(buffer, file);
    buffer.writeByte(ver);
    buffer.writeBoolean(hasFur);
    return true;
  }

  @Override
  public boolean read(SerialBuffer buffer, boolean file) {
    super.read(buffer, file);
    byte ver = buffer.readByte();
    hasFur = buffer.readBoolean();
    return true;
  }
  public int[] getGenerateDims() {
    return new int[] {Dims.EARTH};
  }
}
