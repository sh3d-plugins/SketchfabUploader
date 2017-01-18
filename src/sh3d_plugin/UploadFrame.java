package sh3d_plugin;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;

public class UploadFrame extends JFrame implements ActionListener {
  private Home home;
  private ResourceBundle resource;
  private Component focusOwner;
  
  private JTextField nameField;
  private JTextField descriptionField;
  private JTextField tagsField;
  private JTextField apiTokenField;
  
  UploadFrame(Home home, ResourceBundle resource, Component focusOwner) {
    this.home = home;
    this.resource = resource;
    this.focusOwner = focusOwner;
    
    Debugger.log("Created GUI on EDT? " + SwingUtilities.isEventDispatchThread());
    
    String[] labels = {"Model name: ", "Model description: ", "Model tags: ", "API token: "};
    int numPairs = labels.length;

    //Create and populate the panel.
    JPanel uploadPanel = new JPanel(new SpringLayout());
    for (int i = 0; i < numPairs; i++) {
        JLabel l = new JLabel(labels[i], JLabel.TRAILING);
        uploadPanel.add(l);
        JTextField textField = new JTextField(10);
        l.setLabelFor(textField);
        uploadPanel.add(textField);
    }
    
    JButton uploadButton = new JButton("Upload");
    uploadButton.addActionListener(this);
    uploadButton.setActionCommand("uploadAction");
    uploadPanel.add(uploadButton);
    //numPairs++;
    
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(this);
    cancelButton.setActionCommand("uploadAction");
    uploadPanel.add(cancelButton);
    numPairs++;

    //Lay out the panel.
    SpringUtilities.makeCompactGrid(uploadPanel,
                                    numPairs, 2, //rows, cols
                                    6, 6,        //initX, initY
                                    6, 6);       //xPad, yPad

    //Create and set up the window.
    //JFrame frame = new JFrame("UploadFrame");
    this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    //Set up the content pane.
    uploadPanel.setOpaque(true);  //content panes must be opaque
    this.setContentPane(uploadPanel);

    //Display the window.
    this.pack();
    this.setVisible(true);
  }

  // Operation performed when the uploadButton is clicked:
  public void actionPerformed(ActionEvent e) {
    System.out.println("Inside UploadFrame ---> actionPerformed()");
    if ("uploadAction".equals(e.getActionCommand())) {
      System.out.println("uploadButton clcked !!!");

      // Export all selected items in home
      // for (Selectable item : getHome().getSelectedItems()) {
      // if (item instanceof HomePieceOfFurniture) {
      // HomePieceOfFurniture piece = (HomePieceOfFurniture) item;
      for (HomePieceOfFurniture piece : home.getFurniture()) {
        if (piece.isMovable()) {
          try {
            exportFurniture(focusOwner, resource, piece);
          } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        }
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
      builder.addTextBody("name", this.nameField.getText(), ContentType.TEXT_PLAIN);
      builder.addTextBody("description", this.descriptionField.getText(), ContentType.TEXT_PLAIN);
      builder.addTextBody("password", "", ContentType.TEXT_PLAIN);
      builder.addTextBody("private", "0", ContentType.TEXT_PLAIN);
      builder.addTextBody("tags", this.tagsField.getText(), ContentType.TEXT_PLAIN);
      builder.addTextBody("token", this.apiTokenField.getText(), ContentType.TEXT_PLAIN);
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