package sh3d_plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.PieceOfFurniture;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

public class SketchfabUploader extends Plugin {

	@Override
	public PluginAction[] getActions() {
		return new PluginAction [] {new VolumeAction()}; 
	}
	
	public class VolumeAction extends PluginAction {

        public VolumeAction() {
            putPropertyValue(Property.NAME, "Sketchfab uploader");
            putPropertyValue(Property.MENU, "Tools");
            // Enables the action by default
            setEnabled(true);
        }
		
		@Override
		public void execute() {
            float volumeInCm3 = 0;
            // Compute the sum of the volume of the bounding box of 
            // each movable piece of furniture in home
            for (PieceOfFurniture piece : getHome().getFurniture()) {
                if (piece.isMovable()) {
                	Content furnitureModel = piece.getModel();
                	try {
						InputStream in = furnitureModel.openStream();
						File homeFile = new File("/tmp/test.obj");
						OutputStream out;
						out = new FileOutputStream(homeFile);
						
						byte [] buffer = new byte [8192];
					    int size; 
					    while ((size = in.read(buffer)) != -1) {
					        out.write(buffer, 0, size);
					    }
					    
						out.close();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            }
            
            // Display the result in a message box (\u00b3 is for 3 in supercript)
            String message = String.format(
                "The maximum volume of the movable furniture in home is %.2f m\u00b3.", 
                volumeInCm3 / 1000000);
            JOptionPane.showMessageDialog(null, message);
		}

	}

}
