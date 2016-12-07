/**
 * 
 */
package sh3d_plugin;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.tools.URLContent;

/**
 * @author Devin Chen
 *
 */
public class DefaultFurnitureOutputStream extends FilterOutputStream {

  public DefaultFurnitureOutputStream(OutputStream out) {
    super(out);
  }
  
  /**
   * Writes home in a zipped stream followed by <code>Content</code> objects 
   * it points to.
   */
  public void writeFurniture(HomePieceOfFurniture piece) throws IOException {
    // Create a zip output on out stream 
    ZipOutputStream zipOut = new ZipOutputStream(this.out);

    if (piece.getModel() instanceof URLContent && ((URLContent) piece.getModel()).isJAREntry()) {
      Debugger.log("URLContent");
      URLContent urlContent = (URLContent) piece.getModel();
      // jar:file:/.../test.sweethome3d!/1/iphone.obj
      String jarEntryName = urlContent.getJAREntryName(); // 1/iphone.obj
      // dir = 1/
      String dir = jarEntryName.substring(0, jarEntryName.lastIndexOf('/') + 1);
      // Only export data in the same dir as the obj file
      // If model is 1/iphone.obj, then export 1/iphone.mtl, 1/iphone.jpg ...
      exportEntries(zipOut, dir, urlContent);
    } else {
      Debugger.log("Not URLContent");
      exportEntry(zipOut, getEntryName(piece.getModel()), piece.getModel());
    }
    
    // Finish zip writing
    zipOut.finish();
  }
  

  /**
   * Returns the entry name of a <code>content</code>.
   */
  private String getEntryName(Content content) {
    if (content instanceof URLContent) {
      String file = ((URLContent) content).getURL().getFile();
      return file.substring(file.lastIndexOf('/') + 1);
    } else {
      JOptionPane.showMessageDialog(null, "Failed to getEntryName");
      // return "content" + this.fileContentIndex++;
      return "xxdebug";
    }
  }

  /**
   * Export all the sibling files of the zipped <code>urlContent</code>.
   * 
   * A .OBJ file references one or more .MTL files (called "material libraries"), and from there,
   * references one or more material descriptions by name.
   * 
   * @param dir export files under this directory, e.g. 1/
   */
  private void exportEntries(ZipOutputStream zipOut, String dir, URLContent urlContent) throws IOException {
    ZipInputStream zipIn = null;
    try {
      // Open zipped stream that contains urlContent
      zipIn = new ZipInputStream(urlContent.getJAREntryURL().openStream());
      // Export each entry in dir of home stream
      for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null;) {
        String zipEntryName = entry.getName();
        // Ignore 1/
        // Export 1/iphone.obj
        // Export 1/iphone.mtl
        // Export 1/iphone.jpg
        if (zipEntryName.startsWith(dir) && !zipEntryName.equals(dir)) {
          Content siblingContent =
              new URLContent(new URL("jar:" + urlContent.getJAREntryURL() + "!/" + zipEntryName));
          String fileName = getLastName(zipEntryName);
          exportEntry(zipOut, fileName, siblingContent);
        }
      }
    } finally {
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }
  
  /**
   * Get last name (AKA filename) of the given path.
   * @param path e.g. 1/iphone.obj
   * @return e.g. iphone.obj
   */
  private String getLastName(String path) {
    return new File(path).getName();
  }

  /**
   * export a new entry named <code>entryName</code> that contains a given <code>content</code>.
   */
  private void exportEntry(ZipOutputStream zipOut, String entryName, Content content) throws IOException {
    if (Debugger.isEnabled()) {
      Debugger.log("entryName: " + entryName);
    }

    byte [] buffer = new byte [8192];
    InputStream contentIn = null;
    try {
      zipOut.putNextEntry(new ZipEntry(entryName));
      contentIn = content.openStream();          
      int size; 
      while ((size = contentIn.read(buffer)) != -1) {
        zipOut.write(buffer, 0, size);
      }
      zipOut.closeEntry();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(null, e.getMessage());
      e.printStackTrace();
    } finally {
      if (contentIn != null) {
        contentIn.close();
      }
      
      if (Debugger.isEnabled()) {
        Debugger.log(String.format("Success to export entry: %s", entryName));
      }
    }
  }

}
