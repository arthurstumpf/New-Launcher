package net.minecraft.launcher.ui.tabs.website;

import java.awt.Component;
import java.awt.Dimension;

public abstract interface Browser
{
  public abstract void loadUrl(String paramString);
  
  public abstract Component getComponent();
  
  public abstract void resize(Dimension paramDimension);
}