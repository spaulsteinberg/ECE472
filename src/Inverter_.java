
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;

public class Inverter_ implements PlugInFilter{

    public int setup(String arg, ImagePlus imp){
        if (arg.equals("about")){
            showAbout(); return DONE;
        }
        return DOES_8G+DOES_STACKS+SUPPORTS_MASKING;
    }
    public void run(ImageProcessor ip){
        byte[] pixels = (byte[])ip.getPixels();
        int width = ip.getWidth();
        Rectangle r = ip.getRoi();
        int offset, i;
        for (int y=r.y; y<(r.y+r.height); y++){
            offset = y*width;
            for (int x=r.x; x<(r.x+r.width); x++){
                i = offset + x;
                pixels[i] = (byte)(255-pixels[i]);
            }
        }
    }
    void showAbout(){
        IJ.showMessage("About Inverter_...",
                "This sample plugin filter inverts 8-bit images. Look\n"
                        +"at the 'Inverter_.java' source file to see how easy it is\n"
                        +"in ImageJ to process non-rectangular ROIs, to process\n"
                        +"all the slices in a stack, and to display an About box.");
    }
}
