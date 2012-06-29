package org.powertac.tourney.services;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.stereotype.Service;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Service("upload")
public class Upload
{

  // Init
  // ---------------------------------------------------------------------------------------

  private UploadedFile uploadedFile;
  private String fileName;
  private String uploadLocation = "/export/scratch/";

  // Actions
  // ------------------------------------------------------------------------------------
  public Upload ()
  {
    System.out.println("Instantiated Upload Service");
    TournamentProperties properties = new TournamentProperties();
    setUploadLocation(properties.getProperty("fileUploadLocation"));
  }

  public String submit (String name)
  {

    // Just to demonstrate what information you can get from the uploaded file.
    System.out.println("File type: " + uploadedFile.getContentType());
    System.out.println("File name: " + name);
    System.out.println("File size: " + uploadedFile.getSize() + " bytes");

    // Prepare filename prefix and suffix for an unique filename in upload
    // folder.
    String prefix = name;
    String suffix = FilenameUtils.getExtension(uploadedFile.getName());

    // Prepare file and outputstream.
    File file = null;
    OutputStream output = null;

    try {
      // Create file with unique name in upload folder and write to it.
      file =
        File.createTempFile(prefix, "." + suffix, new File(getUploadLocation()));
      output = new FileOutputStream(file);
      IOUtils.copy(uploadedFile.getInputStream(), output);
      fileName = file.getName();

      // Show succes message.
      FacesContext.getCurrentInstance()
              .addMessage("uploadForm",
                          new FacesMessage(FacesMessage.SEVERITY_INFO,
                                           "File upload succeed!" + prefix
                                                   + "." + suffix, null));

      return fileName;
    }
    catch (IOException e) {
      // Cleanup.
      if (file != null)
        file.delete();

      // Show error message.
      FacesContext
              .getCurrentInstance()
              .addMessage("uploadForm",
                          new FacesMessage(
                                           FacesMessage.SEVERITY_ERROR,
                                           "File upload failed with I/O error.",
                                           null));

      // Always log stacktraces (with a real logger).
      e.printStackTrace();
    }
    finally {
      IOUtils.closeQuietly(output);
    }
    return null;
  }

  // Getters
  // ------------------------------------------------------------------------------------

  public UploadedFile getUploadedFile ()
  {
    return uploadedFile;
  }

  public String getFileName ()
  {
    return fileName;
  }

  // Setters
  // ------------------------------------------------------------------------------------

  public void setUploadedFile (UploadedFile uploadedFile)
  {
    this.uploadedFile = uploadedFile;
  }

  public String getUploadLocation ()
  {
    return uploadLocation;
  }

  public void setUploadLocation (String uploadLocation)
  {
    this.uploadLocation = uploadLocation;
  }

}
