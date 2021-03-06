/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;

/**
 *
 * @author thor
 */
public class MianFrame extends javax.swing.JFrame implements PropertyChangeListener {

  private String welcomeDiscrption = "<html><center>This installer will install any<br/>"
          + "required programs and configure your<br/>"
          + "environment so it’s ready to use. Enjoy 😃</center></html>";

  String username = System.getProperty("user.name");
  String password;
  String companyName = "";
  java.awt.CardLayout cl;
  JList<CheckboxListItem> componentList;
  CheckboxListItem currentScript;
  static String basePath;

  List<Path> logoList = null;
  JList<CheckboxListItem> scriptList;
  ExecuteScriptTask task;
  boolean isDone = false;
  private int scriptCompleated;

  private void setIcon() {

    try {
      URL resource = this.getClass().getResource("iconBlue.png");
      BufferedImage image = ImageIO.read(resource);
      this.setIconImage(image);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  class WelcomeDocumentListener implements DocumentListener {

    public void changedUpdate(DocumentEvent e) {
      changed();
    }

    public void removeUpdate(DocumentEvent e) {
      changed();
    }

    public void insertUpdate(DocumentEvent e) {
      changed();
    }

    public void changed() {
      if (welcomeNameTexField.getText().isEmpty() || String.valueOf(welcomePasswordField.getPassword()).isEmpty()) {
        welcomeButton.setEnabled(false);
      } else {
        welcomeButton.setEnabled(true);
        username = welcomeNameTexField.getText();
        password = String.valueOf(welcomePasswordField.getPassword());
      }

    }
  };

  /**
   * Invoked when task's progress property changes.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress" == evt.getPropertyName()) {
      int progress = (Integer) evt.getNewValue();
      progressBar.setValue(progress);
//      "("+ scriptCompleated + "/" + selectedScriptsCount+  ") Starting task ~> " + currentScript.toString()
      if (task.isCancelled()) {

      } else {
//        taskOutput.append(String.format(
//                "\n(%02d/%02d) Starting task ~> %s.\n\n",
//                scriptCompleated,
//                selectedScriptsCount,
//                currentScript.toString()));
      }
    }
  }

  class ExecuteScriptTask extends SwingWorker<Void, Void> {

    MianFrame mother;
    boolean gotCanceled = false;
    Process scriptProcess;

    public ExecuteScriptTask(MianFrame mother) {
      this.mother = mother;
    }

////    public b cancel
//    public void mycancel(boolean mayInterruptIfRunning) {
//      gotCanceled = true;
//      cancel(mayInterruptIfRunning);
//    }

    /*
         * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {
      double taskPercent = 100 / selectedScriptsCount;
      int progressPercent = 0;
      scriptCompleated = 0;
      setProgress(progressPercent);
      for (int i = 0; i < componentList.getModel().getSize(); i++) {
        try {
          if (!componentList.getModel().getElementAt(i).isSelected) {
            continue;
          }
          ProcessBuilder builder = new ProcessBuilder();
          currentScript = componentList.getModel().getElementAt(i);
          builder.command(basePath + "resources/worker_scripts/script_entry_point", currentScript.getRunablePath(), username, password);
          scriptProcess = builder.start();
          taskOutput.append(String.format(
                  "\n(%02d/%02d) Starting task ~> %s.\n\n",
                  ++scriptCompleated,
                  selectedScriptsCount,
                  currentScript.toString()));
          StreamGobbler streamGobbler = new StreamGobbler(scriptProcess.getInputStream(), (x) -> taskOutput.append(x + "\n"));
          Executors.newSingleThreadExecutor().submit(streamGobbler);
          setProgress(progressPercent);
          int exitCode = scriptProcess.waitFor();
          taskOutput.append("\nExit code: " + exitCode + "\n");
          scriptProcess.destroy();
          progressPercent += Math.round(taskPercent);
          setProgress(progressPercent);
        } catch (IOException | InterruptedException ex) {
          System.out.println("Failed in script execution");
          return null;
        }
      }
      setProgress(100);

      return null;
    }

    /*
         * Executed in event dispatching thread
     */
    @Override
    public void done() {
      isDone = true;
      cancelBackBut.setText("Back");
      if (isCancelled()) {
        taskOutput.append("CANCEL!");
        scriptProcess.destroy();
        return;
      }
      Toolkit.getDefaultToolkit().beep();
      setCursor(null); //turn off the wait cursor
      taskOutput.append("\nDone!\n");
      Object[] options = {"Wait! I missed something",
        "Nice! I'm done here"};
      int n = JOptionPane.showOptionDialog(mother,
              "<html><font size=\"5\"><b>Grats! Your system is Good To Go.</b></font></html>",
              "Setup complete",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              new ImageIcon(basePath + "resources/defaults/cheersIconSmall.png"),
              options,
              options[1]);
      if (n != 0) {
        File downloadDir = new File(basePath + "resources/downloads/");
        for (File file : downloadDir.listFiles()) {
          if (!"do_not_delete".equals(file.getName())) {
            System.out.println("Deliting file: " + file.getName());
            file.delete();
          }
        }
        mother.dispatchEvent(new WindowEvent(mother, WindowEvent.WINDOW_CLOSING));
      }
    }
  }

  class ScriptReadHelpTask extends SwingWorker<Void, Void> {

    public ScriptReadHelpTask() {
    }

////    public b cancel
//    public void mycancel(boolean mayInterruptIfRunning) {
//      gotCanceled = true;
//      cancel(mayInterruptIfRunning);
//    }
    boolean readDescription(CheckboxListItem scriptToCheck) {

      Process scriptProcess;
      try {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(scriptToCheck.getRunablePath(), "-h");
        scriptProcess = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(scriptProcess.getInputStream(), (x) -> scriptToCheck.setToolTip(x));
        Executors.newSingleThreadExecutor().submit(streamGobbler);
//        scriptProcess.
        Thread.sleep(1);
        int exitCode = scriptProcess.waitFor();
        scriptProcess.destroy();
      } catch (IOException | InterruptedException ex) {
        System.out.println("Failed to read -h from script");
        return false;
      }
      return true;
    }

    boolean hasDescritopn(CheckboxListItem scriptToCheck) {
      File file = new File(scriptToCheck.getRunablePath());

      try {
        Scanner scanner = new Scanner(file);
        if (!scanner.hasNextLine()) {
          return false;
        }
        scanner.nextLine();
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          if (line.isEmpty() || line.startsWith("#")) {
            continue;
          }
          if (line.contains("if [ \"$1\" == \"-h\" ]")) {
            return true;
          } else {
            return false;
          }
        }
      } catch (FileNotFoundException e) {

      }

      return false;
    }

    /*
         * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {

      for (int i = 0; i < componentList.getModel().getSize(); i++) {
        CheckboxListItem scriptToCheck = componentList.getModel().getElementAt(i);
        if (hasDescritopn(scriptToCheck)) {
          readDescription(scriptToCheck);
        } else {
          scriptToCheck.setToolTip("No description");
        }
      }

      return null;
    }

    /*
         * Executed in event dispatching thread
     */
    @Override
    public void done() {

    }
  }

  /**
   * Creates new form mianFrame
   */
  public MianFrame() {
    initComponents();
    welcomeDiscriotnLabel.setText(welcomeDiscrption);
    welcomeDiscriotnLabel.setHorizontalTextPosition(SwingConstants.CENTER);
    welcomeNameTexField.setText(username);
    welcomeNameTexField.getDocument().addDocumentListener(new WelcomeDocumentListener());
    welcomePasswordField.getDocument().addDocumentListener(new WelcomeDocumentListener());
    componentList = getCheckboxScriptList();
    ScriptReadHelpTask setTooltipTask = new ScriptReadHelpTask();
    setTooltipTask.execute();
    jScrollPane1.setViewportView(componentList);
    cl = (java.awt.CardLayout) jPanelCard.getLayout();
    cl.next(jPanelCard);

    setLogoAndCompanyName();
    welcomeTextLabel.setText("<html><center>Welcome to the" + companyName + "<br/>GTG Linux installer</center></html>");
    getRootPane().setDefaultButton(welcomeButton);
    setIcon();
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    taskOutput.setEditable(false);
    DefaultCaret caret = (DefaultCaret) taskOutput.getCaret();
    caret.setUpdatePolicy(ALWAYS_UPDATE);

  }

  JList getCheckboxScriptList() {

    CheckboxListItem[] scripts = null;
    try {
      List<Path> scriptPathList = null;
      scriptPathList = Files.walk(Paths.get(basePath + "scripts/"))
              .filter(Files::isRegularFile).
              collect(Collectors.toList());
      scripts = new CheckboxListItem[scriptPathList.size()];
      int i = 0;
      CheckboxListItem itemTemp;
      for (Path s : scriptPathList) {
        CheckboxListItem item = new CheckboxListItem(s);
        if (item.toString().equalsIgnoreCase("update")) {
          itemTemp = scripts[0];
          scripts[0] = item;
          scripts[i++] = itemTemp;
        } else if (item.toString().equalsIgnoreCase("Dev essentials")) {
          itemTemp = scripts[0];
          scripts[0] = item;
          scripts[i++] = itemTemp;
        } else {
          scripts[i++] = item;
        }
      }
    } catch (IOException ex) {
    }
    scriptList = new JList<CheckboxListItem>(scripts) {
      // This method is called as the cursor moves within the list.
      public String getToolTipText(MouseEvent evt) {
        // Get item index
        int index = locationToIndex(evt.getPoint());

        // Get item
        CheckboxListItem item = getModel().getElementAt(index);

        // Return the tool tip text
        return item.getToolTip();
      }
    };

//    logoLabel.setIcon(new ImageIcon("resources/defaults/default.png"));
    // Use a CheckboxListRenderer (see below)
    // to renderer list cells
    scriptList.setCellRenderer(new CheckboxListRenderer());
    scriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Add a mouse listener to handle changing selection
    scriptList.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent event) {
        JList<CheckboxListItem> list
                = (JList<CheckboxListItem>) event.getSource();

        // Get index of item clicked
        int index = list.locationToIndex(event.getPoint());
        CheckboxListItem item = (CheckboxListItem) list.getModel()
                .getElementAt(index);

        // Toggle selected state
        item.setSelected(!item.isSelected());

        // Repaint cell
        list.repaint(list.getCellBounds(index, index));
      }
    });
    return scriptList;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jProgressBar1 = new javax.swing.JProgressBar();
    logoLabel = new javax.swing.JLabel();
    jPanelCard = new javax.swing.JPanel();
    jPanelInstallSelect = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    javax.swing.JList<CheckboxListItem> jList1 = new javax.swing.JList<>();
    jLabel3 = new javax.swing.JLabel();
    jButtonSelectAll = new javax.swing.JButton();
    jButtonClearAll = new javax.swing.JButton();
    jButtonDoIt = new javax.swing.JButton();
    welcomePanel = new javax.swing.JPanel();
    welcomeTextLabel = new javax.swing.JLabel();
    welcomeDiscriotnLabel = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    welcomeNameTexField = new javax.swing.JTextField();
    jLabel1 = new javax.swing.JLabel();
    jPanel2 = new javax.swing.JPanel();
    welcomePasswordField = new javax.swing.JPasswordField();
    jLabel2 = new javax.swing.JLabel();
    jPanel3 = new javax.swing.JPanel();
    welcomeButton = new javax.swing.JButton();
    progress = new javax.swing.JPanel();
    jLabel4 = new javax.swing.JLabel();
    progressBar = new javax.swing.JProgressBar();
    jScrollPane2 = new javax.swing.JScrollPane();
    taskOutput = new javax.swing.JTextArea();
    cancelBackBut = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

    logoLabel.setIcon(new javax.swing.ImageIcon("/home/thor/install_script/resources/company/Rovsing.png")); // NOI18N

    jPanelCard.setLayout(new java.awt.CardLayout());

    jScrollPane1.setViewportView(jList1);

    jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel3.setText("Select components to install (hover to see details)");

    jButtonSelectAll.setText("Select All");
    jButtonSelectAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButtonSelectAllActionPerformed(evt);
      }
    });

    jButtonClearAll.setText("Clear All");
    jButtonClearAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButtonClearAllActionPerformed(evt);
      }
    });

    jButtonDoIt.setText("DO IT!");
    jButtonDoIt.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButtonDoItActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanelInstallSelectLayout = new javax.swing.GroupLayout(jPanelInstallSelect);
    jPanelInstallSelect.setLayout(jPanelInstallSelectLayout);
    jPanelInstallSelectLayout.setHorizontalGroup(
      jPanelInstallSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanelInstallSelectLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanelInstallSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(jPanelInstallSelectLayout.createSequentialGroup()
            .addComponent(jScrollPane1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanelInstallSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(jButtonClearAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(jButtonSelectAll, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
              .addComponent(jButtonDoIt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
          .addComponent(jLabel3))
        .addContainerGap())
    );
    jPanelInstallSelectLayout.setVerticalGroup(
      jPanelInstallSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInstallSelectLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel3)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanelInstallSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(jPanelInstallSelectLayout.createSequentialGroup()
            .addComponent(jButtonSelectAll)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButtonClearAll)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButtonDoIt, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE))
        .addContainerGap())
    );

    jPanelCard.add(jPanelInstallSelect, "card3");

    welcomePanel.setLayout(new java.awt.GridLayout(5, 3));

    welcomeTextLabel.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
    welcomeTextLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    welcomeTextLabel.setText("Welcome to the linux installer");
    welcomeTextLabel.setName("welcomeTextLabel"); // NOI18N
    welcomePanel.add(welcomeTextLabel);

    welcomeDiscriotnLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    welcomeDiscriotnLabel.setText("sadddfsdf");
    welcomeDiscriotnLabel.setName(""); // NOI18N
    welcomePanel.add(welcomeDiscriotnLabel);

    welcomeNameTexField.setToolTipText("Type your Linux user name");

    jLabel1.setText("Linux username");

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
        .addContainerGap(19, Short.MAX_VALUE)
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(welcomeNameTexField, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(49, 49, 49))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
        .addContainerGap(20, Short.MAX_VALUE)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(welcomeNameTexField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addContainerGap())
    );

    welcomePanel.add(jPanel1);

    welcomePasswordField.setToolTipText("Type your Linux user password");

    jLabel2.setText("Linux password");

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
        .addContainerGap(20, Short.MAX_VALUE)
        .addComponent(jLabel2)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(welcomePasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(50, 50, 50))
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel2Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(welcomePasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel2))
        .addContainerGap(22, Short.MAX_VALUE))
    );

    welcomePanel.add(jPanel2);

    welcomeButton.setText("Next");
    welcomeButton.setEnabled(false);
    welcomeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        welcomeButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
    jPanel3.setLayout(jPanel3Layout);
    jPanel3Layout.setHorizontalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
        .addContainerGap(305, Short.MAX_VALUE)
        .addComponent(welcomeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(14, 14, 14))
    );
    jPanel3Layout.setVerticalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel3Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(welcomeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        .addContainerGap())
    );

    welcomePanel.add(jPanel3);

    jPanelCard.add(welcomePanel, "card2");

    jLabel4.setText("Setup progress");

    taskOutput.setColumns(20);
    taskOutput.setRows(5);
    jScrollPane2.setViewportView(taskOutput);

    cancelBackBut.setText("Cancel");
    cancelBackBut.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cancelBackButActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout progressLayout = new javax.swing.GroupLayout(progress);
    progress.setLayout(progressLayout);
    progressLayout.setHorizontalGroup(
      progressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(progressLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(progressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, progressLayout.createSequentialGroup()
            .addGroup(progressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
              .addComponent(jScrollPane2)
              .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, progressLayout.createSequentialGroup()
            .addGap(154, 154, 154)
            .addComponent(cancelBackBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(158, 158, 158))
          .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
    );
    progressLayout.setVerticalGroup(
      progressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(progressLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel4)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cancelBackBut)
        .addContainerGap())
    );

    jPanelCard.add(progress, "card4");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(logoLabel)
        .addContainerGap(175, Short.MAX_VALUE))
      .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
          .addContainerGap()
          .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addContainerGap()))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(logoLabel)
        .addContainerGap(253, Short.MAX_VALUE))
      .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
          .addGap(56, 56, 56)
          .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addContainerGap()))
    );

    setBounds(0, 0, 417, 337);
  }// </editor-fold>//GEN-END:initComponents


  private void welcomeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_welcomeButtonActionPerformed
    cl.next(jPanelCard);
    cl.next(jPanelCard);

    getRootPane().setDefaultButton(jButtonDoIt);

  }//GEN-LAST:event_welcomeButtonActionPerformed

  private void jButtonClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearAllActionPerformed
    for (int i = 0; i < componentList.getModel().getSize(); i++) {
      componentList.getModel().getElementAt(i).setSelected(false);
      componentList.repaint(componentList.getCellBounds(i, i));
    }
  }//GEN-LAST:event_jButtonClearAllActionPerformed

  private void jButtonSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSelectAllActionPerformed
    for (int i = 0; i < componentList.getModel().getSize(); i++) {
      componentList.getModel().getElementAt(i).setSelected(true);
      componentList.repaint(componentList.getCellBounds(i, i));
    }
  }//GEN-LAST:event_jButtonSelectAllActionPerformed


  private void jButtonDoItActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDoItActionPerformed
    progressBar.setValue(0);
    task = new ExecuteScriptTask(this);
    task.addPropertyChangeListener(this);
    task.execute();
    cl.next(jPanelCard);
    cl.next(jPanelCard);
    isDone = false;
    cancelBackBut.setText("Cancel");
  }//GEN-LAST:event_jButtonDoItActionPerformed

  private void cancelBackButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBackButActionPerformed
    if (isDone) {
      cl.next(jPanelCard);
    } else {
      task.cancel(true);
    }

  }//GEN-LAST:event_cancelBackButActionPerformed

  private void setLogoAndCompanyName() {
    try {
      logoList = Files.walk(Paths.get(basePath + "resources/company/"))
              .filter(Files::isRegularFile).
              collect(Collectors.toList());
      for (Path companyFiles : logoList) {
        if ("png".equals(companyFiles.getFileName().toString().split("\\.")[1])) {
          companyName = " " + companyFiles.getFileName().toString().split("\\.")[0];
          logoLabel.setIcon(new ImageIcon(basePath + "resources/company/" + companyFiles.getFileName().toString()));
          return;
        }
      }
    } catch (IOException ex) {
    }
    logoLabel.setIcon(new ImageIcon(basePath + "resources/defaults/default.png"));

  }

  private static class StreamGobbler implements Runnable {

    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines()
              .forEach(consumer);
    }
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) throws UnsupportedEncodingException {

//    basePath = "../"; // To run from IDE 
    basePath = new File("").getAbsolutePath() + "/"; // To run from "run" script

//     System.out.println(basePath);
////    basePath = "/";
//
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
     */
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(MianFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(MianFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(MianFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(MianFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new MianFrame().setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton cancelBackBut;
  private javax.swing.JButton jButtonClearAll;
  private javax.swing.JButton jButtonDoIt;
  private javax.swing.JButton jButtonSelectAll;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanelCard;
  private javax.swing.JPanel jPanelInstallSelect;
  private javax.swing.JProgressBar jProgressBar1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JLabel logoLabel;
  private javax.swing.JPanel progress;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JTextArea taskOutput;
  private javax.swing.JButton welcomeButton;
  private javax.swing.JLabel welcomeDiscriotnLabel;
  private javax.swing.JTextField welcomeNameTexField;
  private javax.swing.JPanel welcomePanel;
  private javax.swing.JPasswordField welcomePasswordField;
  private javax.swing.JLabel welcomeTextLabel;
  // End of variables declaration//GEN-END:variables

  int selectedScriptsCount = 0;
// Represents items in the list that can be selected

  class CheckboxListItem {

    private boolean isSelected = false;
    private Path script;
    private String description;

//    public static int selectedCount = 0;
    public CheckboxListItem(Path path) {
      this.script = path;
      setSelected(true);

//      try {
//        Process scriptProcess;
//        ProcessBuilder builder = new ProcessBuilder();
//        builder.command(path.toString(), "-h");
//        scriptProcess = builder.start();
//        StreamGobbler streamGobbler = new StreamGobbler(scriptProcess.getInputStream(), (x) -> description = x);
//        Executors.newSingleThreadExecutor().submit(streamGobbler);
//        int exitCode = scriptProcess.waitFor();
//        scriptProcess.destroy();
//      } catch (IOException | InterruptedException ex) {
//        System.out.println("Failed to get script description");
//      }
    }

    public boolean isSelected() {
      return isSelected;
    }

    public void setSelected(boolean isSelected) {
      this.isSelected = isSelected;
      selectedScriptsCount += isSelected ? 1 : - 1;
    }

    public String getToolTip() {
      return description;
    }

    public void setToolTip(String tt) {
//      System.out.println("Setting tt --> " + tt);
      description = tt;
    }

    public String getRunablePath() {
      return script.toString();//.replace(" ", "\\ ");
    }

    public String toString() {
      String name;
      name = script.getFileName().toString();
      try {
        name = name.split("\\.")[0];
      } catch (Exception e) {
      }
      return name;
    }
  }

// Handles rendering cells in the list using a check box
  class CheckboxListRenderer extends JCheckBox implements
          ListCellRenderer<CheckboxListItem> {

    public Component getListCellRendererComponent(
            JList<? extends CheckboxListItem> list, CheckboxListItem value,
            int index, boolean isSelected, boolean cellHasFocus) {
      setEnabled(list.isEnabled());
      setSelected(value.isSelected());
      setFont(list.getFont());
      setBackground(list.getBackground());
      setForeground(list.getForeground());
      setText(value.toString());
      return this;
    }
  }

}

//public class gui {
//    private JPanel mainPanel;
//    private JLabel logoLabel;
//
//
//    public gui() {
//
//        logoLabel.setIcon(new ImageIcon("resources/company/logo.png"));
//
//
//    }
//
//
//
//    public static void main(String[] args) {
//        JFrame mainFrame = new JFrame("Linux Installer");
//        mainFrame.setContentPane(new gui().mainPanel);
//        mainFrame.setDefaultCloseOperation(mainFrame.EXIT_ON_CLOSE);
//        mainFrame.pack();
//        mainFrame.setVisible(true);
//
////        File curDir = new File("");
////        getAllFiles(curDir);
//    }
//
//
//    private static void getAllFiles(File curDir) {
//
//        File[] filesList = curDir.listFiles();
//        for(File f : filesList){
//            if(f.isDirectory())
//                System.out.println(f.getName());
//            if(f.isFile()){
//                System.out.println(f.getName());
//            }
//        }
//
//    }
//}
