package jfcraft.client;

/**
 *
 * @author pquiring
 *
 * Created : Mar 24, 2014
 */

import java.net.*;

import javaforce.*;
import javaforce.gl.*;

import jfcraft.server.*;
import jfcraft.opengl.*;
import jfcraft.data.*;

public class MultiPlayerMenu extends RenderScreen {
  private Texture t_menu;
  private RenderBuffers o_menu;
  private TextField serverAddress;

  private static String host = "127.0.0.1";

  public MultiPlayerMenu() {
    id = Client.MULTI;
  }

  public void init(GL gl) {
    super.init(gl);
    serverAddress = addTextField(gl, host, 5, 32, 512-10, true, 64, false, 1);
    addButton(gl, "Start", 20, 390, 226, new Runnable() {public void run() {
      joinWorld();
    }});
    addButton(gl, "Cancel", 266, 390, 226, new Runnable() {public void run() {
      Static.video.setScreen(Static.screens.screens[Client.MAIN]);
    }});
    setFocus(serverAddress);
  }

  public void render(GL gl, int width, int height) {
    setMenuSize(512, 512);
    reset();

    if (t_menu == null) {
      t_menu = Textures.getTexture(gl, "jfcraft/multimenu");
    }

    if (o_menu == null) {
      o_menu = createMenu(gl);
    }

    //now render stuff
    gl.glViewport(0, 0, width, height);
    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

    setOrtho(gl);

    gl.glUniformMatrix4fv(Static.uniformMatrixView, 1, GL.GL_FALSE, identity.m);  //view matrix
    gl.glUniformMatrix4fv(Static.uniformMatrixModel, 1, GL.GL_FALSE, identity.m);  //model matrix

    t_menu.bind(gl);
    o_menu.bindBuffers(gl);
    o_menu.render(gl);

    renderButtons(gl);
    renderFields(gl);
    renderText(gl);
  }

  public void resize(GL gl, int width, int height) {
    super.resize(gl, width, height);
  }

  public void mousePressed(int x, int y, int button) {
    super.mousePressed(x, y, button);
  }

  public void mouseReleased(int x, int y, int button) {
  }

  public void mouseMoved(int x, int y, int button) {
  }

  public void mouseWheel(int delta) {
  }

  private void joinWorld() {
    host = serverAddress.getText();
    if (host.length() == 0) return;
    NetworkClientTransport clientTransport = new NetworkClientTransport();
    Client client = new Client(clientTransport);
    client.isLocal = false;
    client.name = Settings.current.player;
    client.pass = Settings.current.pass;
    Socket socket = null;
    try {
      socket = new Socket(host, Settings.current.tcpPort);
    } catch (Exception e) {
      Static.log(e);
      MessageMenu msg = (MessageMenu)Static.screens.screens[Client.MESSAGE];
      msg.setup("Connection Failed", e.toString(), this);
      Static.video.setScreen(msg);
      return;
    }
    clientTransport.init(socket, client);
    if (Settings.current.client_voip) {
      client.startVoIP(host);
    }
    Login login = (Login)Static.screens.screens[Client.LOGIN];
    login.setup(client);
    Static.video.setScreen(login);
  }
}
