
import ij.*;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.plugin.OverlayCommands;
import ij.plugin.filter.*;
import ij.process.*;

import java.util.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.awt.*;
import java.awt.event.*;

public class connected_components implements ExtendedPlugInFilter, DialogListener {
  private PlugInFilterRunner pfr = null;
  private int flags = DOES_8G;
  private int min = 0;
  private int max = 0;
  private boolean border = false; private boolean noncircular = false; private boolean contrast_stretch = false;
  private int disjointSetLabel[];    // disjoint set label
  private int disjointSetRank[];    // disjoint set rank

  private int nextLabel = 1;        // initial object label
  private int Nobjects = 0;         // num objects created
  private String[] outputLabelText = new String[] {
          "Labels", "BBOX" };
  private String labelOption = outputLabelText[0];
  private int[][] label;

  @Override
  public int setup(String arg, ImagePlus imp) {
    return flags;
  }

  @Override
  public void run(ImageProcessor ip) {

    // Assign labels to object pixels (255)


    if (labelOption.equals(outputLabelText[0])){
      // Binarize image using default method
      ip.autoThreshold();
      assignLabels(ip);
    }
    else{
      ImageProcessor I0 = ip.duplicate();
      I0.autoThreshold();
      assignLabels(I0);
      overlayBBOX(ip);
      //ip.insert(ip.convertToByte(true), 0, 0);
    }

  }

  /* Set up GUI */
  public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
    this.pfr = pfr;

    GenericDialog gd = new GenericDialog("Connected Components");
    gd.addRadioButtonGroup("Output:", outputLabelText, 1, 2, labelOption);
    gd.addNumericField("Min size:", min, 0);
    gd.addNumericField("Max size:", max, 0);
    gd.addCheckbox("Remove Border Objects", border);
    gd.addCheckbox("Remove noncircular objects", noncircular);
    gd.addCheckbox("Contrast stretch", contrast_stretch);

    gd.addPreviewCheckbox(pfr);
    gd.addDialogListener(this);
    gd.showDialog();

    if (gd.wasCanceled())
      return DONE;
    return flags;
  }

  /* Get Dialog changes from GUI */
  public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
    labelOption = gd.getNextRadioButton();
    min = (int)gd.getNextNumber();
    max = (int)gd.getNextNumber();
    border = gd.getNextBoolean();
    noncircular = gd.getNextBoolean();
    contrast_stretch = gd.getNextBoolean();
    return true;
  }

  @Override
  public void setNPasses(int nPasses) {
    ;
  }

  private void overlayBBOX(ImageProcessor ip){
    //if overlays for labels become too much make visited list
    //make a size list for each pixel (u,v) = i
    //add condition here like if(border) dont add these else add all
    ColorProcessor ip_overlay = ip.convertToColorProcessor();
    ip_overlay.setColor(Color.yellow);
    int min_u, max_u, min_v, max_v, size_sum;
    ArrayList<Integer> objSize = new ArrayList<Integer>();

    for (int i: disjointSetLabel){
      if (i != -1){
        min_u = Integer.MAX_VALUE; max_u = -1;
        min_v = Integer.MAX_VALUE; max_v = -1;
        size_sum = 0;
        for (int v = 0; v < ip.getHeight(); v++){
          for (int u = 0; u < ip.getWidth(); u++){
            if (label[u][v] == i){
              size_sum++;
              if (u < min_u) min_u = u;
              if (u > max_u) max_u = u;
              if (v < min_v) min_v = v;
              if (v > max_v) max_v = v;
            }
          }
        }
        objSize.add(size_sum);
        /* If border option is on, look for min coords that are out of bounds */
        if (border) {
          if (min_u <= 0 || max_u >= (ip.getWidth() - 1) || min_v <= 0 || max_v >= (ip.getHeight()-1)) {
            continue;
          }
        }
        /* If NOT between min and max do not apply a box, continue to next iteration. */
        if ( !(min <= size_sum && max >= size_sum)){
          continue;
        }
        ip_overlay.drawRect(min_u, min_v, (max_u-min_u), (max_v-min_v));
    }
    }
    for (int z : disjointSetRank) IJ.log(String.format("size at index is: %d\n", z));
    ImagePlus imp_overlay = new ImagePlus("blobs.gif", ip_overlay);
    imp_overlay.show();

  }

  private void assignLabels(ImageProcessor ip) {

    nextLabel = 1; // initial object label
    Nobjects = 0;  // num objects created

    int Nrow = ip.getHeight();
    int Ncol = ip.getWidth();

    /*int[][]*/ label = new int[Ncol][Nrow];

    disjointSetLabel = new int[Nrow*Ncol];
    disjointSetRank = new int[Nrow*Ncol];

    for (int i=0; i<Nrow*Ncol; i++) {
      disjointSetLabel[i] = -1;
      disjointSetRank[i] = 0;
    }

    for (int v=0; v<Nrow; v++) {
      for (int u=0; u<Ncol; u++) {
        int Iuv = ip.get(u, v);

        label[u][v] = 0; // default label: background
        if (Iuv == 0)
          continue;

        int S = (u==0) ? 0 : findLabel(label[u-1][v]);
        int T = (v==0) ? 0 : findLabel(label[u][v-1]);

        if (S==0 && T==0) { 
          label[u][v] = nextLabel++;
          Nobjects++;
        } else 
        if (S!=0 && T==0) {
          label[u][v] = S;
        } else 
        if (S==0 && T!=0) {
          label[u][v] = T;
        } else {
          if (findLabel(S) == findLabel(T))
            label[u][v] = S;
          else {
            label[u][v] = mergeLabels(S, T);
            Nobjects -= 1;
          }
        }
      }
    }

    // Convert labels to sequence 1:N

    TreeMap<Integer,Integer> setLabel = new TreeMap<Integer,Integer>();

    setLabel.put(0, 0);

    for (int v=0; v<Nrow; v++) {
      IJ.showProgress(v, Nrow);
      for (int u=0; u<Ncol; u++) {
        int Luv = findLabel(label[u][v]);
        if (setLabel.containsKey(Luv) == false)
          setLabel.put(Luv, setLabel.size());

        ip.set(u, v, setLabel.get(Luv));
      }
    }
  }

  private int mergeLabels(int i1, int i2) {
    i1 = findLabel(i1);
    i2 = findLabel(i2);

    // union-by-rank merging of sets
    if (i1 != i2) {
      if (disjointSetRank[i1] > disjointSetRank[i2])
        disjointSetLabel[i2] = i1;
      else
      if (disjointSetRank[i1] < disjointSetRank[i1])
        disjointSetLabel[i1] = i2;
      else {
        disjointSetLabel[i2] = i1;
        disjointSetRank[i1] += 1;
      }
    }

    return findLabel(i1);
  }

  private int findLabel(int i) {
    if (disjointSetLabel[i] == -1)
      return i;
  
    // recursive path compression
    disjointSetLabel[i] = findLabel(disjointSetLabel[i]);
    return disjointSetLabel[i];
  }
}
