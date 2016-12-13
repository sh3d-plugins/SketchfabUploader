package sh3d_plugin;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
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
    // Export all selected items in home
    // for (Selectable item : getHome().getSelectedItems()) {
    // if (item instanceof HomePieceOfFurniture) {
    // HomePieceOfFurniture piece = (HomePieceOfFurniture) item;
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      if (piece.isMovable()) {
        exportFurniture(focusOwner, resource, piece);
      }
    }
  }
  
  private void exportFurniture(Component focusOwner, ResourceBundle resource, HomePieceOfFurniture piece) throws IOException {
    exportFurniture(focusOwner, resource, piece, piece.getName() + ".zip");
  }
  
  private void exportFurniture(Component focusOwner, ResourceBundle resource, HomePieceOfFurniture piece, String filename) throws IOException {
    DefaultFurnitureOutputStream furnitureOut = null;
    File baseDirectory = new File(getTempDir());
    File testDirectory = new File(baseDirectory, "test");
    File furniturePackageFile = new File(testDirectory, filename);
    try {
      Debugger.log(String.format("Try to export furniture, name: %s, info: %s", piece.getName(),
          piece.getInformation()));
      furnitureOut = new DefaultFurnitureOutputStream(new FileOutputStream(furniturePackageFile));
      // Write home with HomeOuputStream
      furnitureOut.writeFurniture(piece);
      
      // Upload to sketchfab
      Debugger.log(String.format("Start uploading..."));
      
      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost uploadFile = new HttpPost("https://sketchfab.com/v2/models");
      
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("source", "sweethome3d-uploader", ContentType.TEXT_PLAIN);
      builder.addTextBody("name", "A test model", ContentType.TEXT_PLAIN);
      builder.addTextBody("description", "This is a test model I made with love and passion", ContentType.TEXT_PLAIN);
      builder.addTextBody("password", "", ContentType.TEXT_PLAIN);
      builder.addTextBody("private", "0", ContentType.TEXT_PLAIN);
      builder.addTextBody("tags", "sweethome3d-uploader sweethome3d", ContentType.TEXT_PLAIN);
      builder.addTextBody("token", "", ContentType.TEXT_PLAIN);
      // This attaches the file to the POST:
      builder.addBinaryBody(
          "modelFile",
          new FileInputStream(furniturePackageFile),
          ContentType.APPLICATION_OCTET_STREAM,
          filename
      );
      HttpEntity multipart = builder.build();
      uploadFile.setEntity(multipart);
      
      CloseableHttpResponse response = httpClient.execute(uploadFile);
      HttpEntity responseEntity = response.getEntity();
      String responseString = EntityUtils.toString(responseEntity, "UTF-8");
      System.out.println(responseString);
      Debugger.log(String.format("HTTP Response: %s", responseString));
      
    } catch (FileNotFoundException e) {
      showExportError(focusOwner, resource, e);
    } catch (IOException e) {
      showExportError(focusOwner, resource, e);
    } finally {
      if (furnitureOut != null) {
        furnitureOut.close();
      }
    }
  }
  
  /**
   * Get system temp dir
   * 
   * - /tmp/
   * - %WinDir%\Temp\
   * - %USERPROFILE%\Local Settings\Temp\
   * - %USERPROFILE%\AppData\Local\Temp\
   * 
   * @return String
   */
  private String getTempDir() {
    String property = "java.io.tmpdir";
    return System.getProperty(property);
  }

  /**
   * Shows a message error.
   */
  private void showExportError(Component parent, ResourceBundle resource, IOException ex) {
    String messageFormat = resource.getString("exportError.message");
    JOptionPane.showMessageDialog(parent, String.format(messageFormat, ex.getMessage()),
        resource.getString("exportError.title"), JOptionPane.ERROR_MESSAGE);
  }
}
