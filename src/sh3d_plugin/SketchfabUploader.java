package sh3d_plugin;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

public class SketchfabUploader extends Plugin {

  @Override
  public PluginAction[] getActions() {
    return new PluginAction[] {new UploadAction()};
  }

  public class UploadAction extends PluginAction {

    public UploadAction() {
      putPropertyValue(Property.NAME, "Sketchfab uploader");
      putPropertyValue(Property.MENU, "Tools");
      // Enables the action by default
      setEnabled(true);
    }

    @Override
    public void execute() {
      try {
        exportFurnitures();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void exportFurnitures() throws IOException {
    ResourceBundle resource = ResourceBundle.getBundle("sh3d_plugin.ApplicationPlugin",
        Locale.getDefault(), getPluginClassLoader());
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (getHome().getSelectedItems().isEmpty()) {
      // JOptionPane.showMessageDialog(null, "Please select a furniture first.");
      // return;
    }
    
    Home home = getHome();
    
    UploadFrame uploadFrame = new UploadFrame(home, resource, focusOwner);
    uploadFrame.setVisible(true);
  }
  
}
