
import ij.*;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.plugin.OverlayCommands;
import ij.plugin.OverlayLabels;
import ij.plugin.filter.*;
import ij.plugin.tool.OverlayBrushTool;
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
  TreeMap<Integer,Integer> setLabel = new TreeMap<Integer,Integer>();

  @Override
  public int setup(String arg, ImagePlus imp) {
    return flags;
  }

  @Override
  public void run(ImageProcessor ip) {


    if (labelOption.equals(outputLabelText[0])){
      // Binarize image using default method
      ip.autoThreshold();
      assignLabels(ip);
      labelDiscards(ip);
    }
    /* BBOX option...get labels but use a temp image. If contrast stretch selected perform here. */
    else{
      ImageProcessor I0 = ip.duplicate();
      I0.autoThreshold();
      assignLabels(I0);
      if (contrast_stretch) contrast_stretch(ip);
      overlayBBOX(ip);
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

  /* Object discards for "Labels" option. Labels added to arraylist dont fir criteria and are whitened out */
  private void labelDiscards(ImageProcessor ip){
    int min_u, max_u, min_v, max_v, size_sum;
    ArrayList<Integer> hs = new ArrayList<Integer>();
    for (int i = 1; i <= Nobjects; i++){
        min_u = Integer.MAX_VALUE; max_u = -1;
        min_v = Integer.MAX_VALUE; max_v = -1;
        size_sum = 0;
        for (int v = 0; v < ip.getHeight(); v++) {
          for (int u = 0; u < ip.getWidth(); u++) {
            if (setLabel.get(findLabel(label[u][v])) == i) {
              size_sum++;
              if (u < min_u) min_u = u;
              if (u > max_u) max_u = u;
              if (v < min_v) min_v = v;
              if (v > max_v) max_v = v;
            }
          }
        }
      processOptions(min_u, max_u, min_v, max_v, size_sum, ip,hs, i);

    }
    for (int j: hs){
      for (int v = 0; v < ip.getHeight(); v++) {
        for (int u = 0; u < ip.getWidth(); u++) {
          if (setLabel.get(findLabel(label[u][v])) == j) {
            ip.set(u, v, 0);
          }
        }
      }
    }
  }
  /* Criteria to be erased... */
  private void processOptions(int min_u, int max_u, int min_v, int max_v, int size_sum, ImageProcessor ip, ArrayList<Integer> hs, int i){
    //If border option is on, look for min coords that are out of bounds
    if (border) {
      if (min_u <= 0 || max_u >= (ip.getWidth() - 1) || min_v <= 0 || max_v >= (ip.getHeight()-1)) {
        hs.add(i);
      }
    }
    // If NOT between min and max do not apply a box, continue to next iteration.
    if ( !(min <= size_sum && max >= size_sum)){
      hs.add(i);
    }
    if (noncircular) {
      if (circleCheck((max_u - min_u), (max_v - min_v))) {
        hs.add(i);
      }
    }
  }
  /* Check for circular vs non circular objs */
  private boolean circleCheck(int width, int height){
    if ( (Math.abs(width - height) > 0.2 * width) || Math.abs(width - height) > 0.2 * height){
      return true;
    }
    double a_circle = Math.PI * 1 / 4 * width * height;
    double a_square = width * height;
    if ((Math.abs(a_square - a_circle) < 0.2 * a_circle) || (Math.abs(a_square - a_circle) < 0.2 * a_square)){
      return true;
    }
    return false;
  }

  /*Overlaying BBOX option */
  private void overlayBBOX(ImageProcessor ip){
    ip.setColor(Color.white);
    int min_u, max_u, min_v, max_v, size_sum, width, height;
  /* go through each label and find bounding area */
    for (int i = 1; i <= Nobjects; i++){
        min_u = Integer.MAX_VALUE; max_u = -1;
        min_v = Integer.MAX_VALUE; max_v = -1;
        size_sum = 0;
        for (int v = 0; v < ip.getHeight(); v++) {
          for (int u = 0; u < ip.getWidth(); u++) {
            if (setLabel.get(findLabel(label[u][v])) == i) {
              size_sum++;
              if (u < min_u) min_u = u;
              if (u > max_u) max_u = u;
              if (v < min_v) min_v = v;
              if (v > max_v) max_v = v;
            }
          }
        }
        //If border option is on, look for min coords that are out of bounds
        if (border) {
          if (min_u <= 0 || max_u >= (ip.getWidth() - 1) || min_v <= 0 || max_v >= (ip.getHeight()-1)) {
            continue;
          }
        }
        // If NOT between min and max do not apply a box, continue to next iteration.
        if ( !(min < size_sum && max > size_sum)){
          continue;
        }

        /* Formulas for non-circular objects */
        if (noncircular) {
          if (circleCheck((max_u - min_u), (max_v - min_v))) {
            continue;
          }
        }
        ip.drawRect(min_u, min_v, (max_u-min_u), (max_v-min_v));
    }

  }
  /* Used contrast stretch plugin from earlier this semester */
  private void contrast_stretch(ImageProcessor ip){

      int[] H = ip.getHistogram(256);

      int _sum = 0;
      for (int r=1; r<H.length; r++){
        _sum += H[r];
      }
      int clamp = (int)(Math.floor(_sum*0.02));
      int c_sum = 0, r1 = 0, r2 = 0;
      for (int r = 0; r < 256; r++){
        c_sum += H[r];
        if (c_sum >= clamp){
          r1 = r;
          break;
        }
      }
      c_sum = 0;
      for (int r = 255; r >= 0; r--){
        c_sum += H[r];
        if (c_sum >= clamp){
          r2 = r;
          break;
        }
      }
      int[] I = new int[256];
      for (int r = 0; r < 256; r++){
        if (r < r1){
          I[r] = 0;
        }
        else if (r2 <= r){
          I[r] = 255;
        }
        else{
          double ex1 = (double)(r-r1);
          double ex2 = (double)(r2-r1);
          I[r] = (int)Math.floor(255*ex1/ex2);
        }
      }
      ip.applyTable(I);
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
