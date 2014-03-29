package gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import core.Processor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;

public class StartWindow {

	private final int DEFAULT_MAX_SIZE = 50;
	
	private Processor core;
	private JFrame frmDistributedBackupSystem;
	private JTextArea logs;
	private JComboBox<String> files;
	private JSpinner replicationDegree;
	private JSlider maxUsedSpace;
	private JLabel lblCurrentsize;
	private JScrollBar verticalScroll;
	private final JFileChooser fc = new JFileChooser();
	private File selectedFile;

	private JLabel lblMaxsize;
/*
	static void test() {
		System.out.println(Message.bytesToHex(ChunkManager.fileToSHA256("forms-1.3.0.jar")));
		byte[] sha = null;
		final String FILENAME = "DatabaseManagementSystems.pdf";
		
		try {
			System.out.println(ChunkManager.createChunks("./" + FILENAME, "./Restored/", sha));
			ChunkManager.mergeChunks("./", FILENAME, "./Restored/");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	static void test2() {
		String[] results = new String[2];
		ChunkManager.deleteFirstChunk("./Chunks", results);
		System.out.println(results[0]);
		System.out.println(results[1]);
	}*/
	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		
		if(args.length!=7)
		{
			System.out.println("The 7th argument must be the interface to use (en0, en1, ...)");
			return;
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					StartWindow window = new StartWindow(args);
					window.frmDistributedBackupSystem.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Create the application.
	 */
	public StartWindow(String[] args) {
		core = new Processor(args,this);
		initialize(args);
		core.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(String[] args) {
		fc.setAcceptAllFileFilterUsed(false);
		frmDistributedBackupSystem = new JFrame();
		frmDistributedBackupSystem.setTitle("Distributed Backup System");
		frmDistributedBackupSystem.setBounds(100, 100, 588, 462);
		frmDistributedBackupSystem.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmDistributedBackupSystem.setPreferredSize(new Dimension(500, 500));
		frmDistributedBackupSystem.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(28dlu;default)"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(21dlu;default)"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("max(74dlu;default):grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel mcAdressLabel = new JLabel("Multicast Address: " + args[0] +
				":" + args[1]);
		frmDistributedBackupSystem.getContentPane().add(mcAdressLabel, "4, 4");

		JLabel mdrAddressLabel = new JLabel("Multicast Restore Adress: " + args[2] +
				":" + args[3]);
		frmDistributedBackupSystem.getContentPane().add(mdrAddressLabel, "4, 6");

		JLabel mdbAddressLabel = new JLabel("Multicast Backup Address "  + args[4] +
				":" + args[5]);
		frmDistributedBackupSystem.getContentPane().add(mdbAddressLabel, "4, 8");

		lblMaxsize = new JLabel("MaxSize ("+DEFAULT_MAX_SIZE+" MB)");
		frmDistributedBackupSystem.getContentPane().add(lblMaxsize, "4, 10");

		maxUsedSpace = new JSlider();
		maxUsedSpace.setValue(DEFAULT_MAX_SIZE);
		maxUsedSpace.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblMaxsize.setText("MaxSize (" + maxUsedSpace.getValue() + " MB)");
				
				core.setSpaceLimit(maxUsedSpace.getValue());
			}
		});
		frmDistributedBackupSystem.getContentPane().add(maxUsedSpace, "6, 10, 5, 1");

		lblCurrentsize = new JLabel("Current Size: 0 MB");
		frmDistributedBackupSystem.getContentPane().add(lblCurrentsize, "4, 12");

		final JLabel filePathLabel = new JLabel("File to be backed up:");
		frmDistributedBackupSystem.getContentPane().add(filePathLabel, "4, 16");

		JButton browseFileButton = new JButton("Browse File...");
		frmDistributedBackupSystem.getContentPane().add(browseFileButton, "6, 16");

		replicationDegree = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		frmDistributedBackupSystem.getContentPane().add(replicationDegree, "8, 16");

		JButton btnBackup = new JButton("Backup");
		btnBackup.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(selectedFile!= null)
					core.addFile(selectedFile.getAbsolutePath(), (int) replicationDegree.getValue());
			}
		});
		frmDistributedBackupSystem.getContentPane().add(btnBackup, "10, 16");

		files = new JComboBox<String>();
		frmDistributedBackupSystem.getContentPane().add(files, "4, 20, 7, 1, fill, default");

		JButton btnRestore = new JButton("Restore...");
		btnRestore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showOpenDialog(frmDistributedBackupSystem.getContentPane());

				if (returnVal == JFileChooser.APPROVE_OPTION && files.getSelectedItem() != null) {
					core.restoreFile((String) files.getSelectedItem(), fc.getCurrentDirectory().getAbsolutePath());
				}
			}
		});
		
		frmDistributedBackupSystem.getContentPane().add(btnRestore, "8, 22");

		JButton btnRemove = new JButton("Remove");
		btnRemove.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				core.removeFile((String) files.getSelectedItem());
			}
		});
		frmDistributedBackupSystem.getContentPane().add(btnRemove, "10, 22");
		
		JScrollPane scrollPane = new JScrollPane();
		frmDistributedBackupSystem.getContentPane().add(scrollPane, "4, 26, 7, 1, fill, fill");
		verticalScroll = scrollPane.getVerticalScrollBar();
		
		logs = new JTextArea();
		scrollPane.setViewportView(logs);

		browseFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int returnVal = fc.showOpenDialog(frmDistributedBackupSystem.getContentPane());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					selectedFile = fc.getSelectedFile();

					if(selectedFile != null) {
						filePathLabel.setText("File to be backed up: " + selectedFile.getName());
					}
				}
			}
		});
		
		replaceFileList(new String[0]);
	}

	public void log(String text) {
		logs.append(text + "\n");
		verticalScroll.setValue( verticalScroll.getMaximum() );
	}
	
	public void replaceFileList(Object[] objects) {
		files.removeAllItems();
		
		for(Object filename: objects)
			files.addItem(filename.toString());
	}
	
	public void setUsedSpace(long usedSpace) {
		lblCurrentsize.setText("Current size: " + String.format("%.4f", usedSpace/1000000.0)  + " MB");
	}
	
	public int getReplicationDegree() {
		return (int) replicationDegree.getValue();
	}
	
	public int getMaxUsedSpace() {
		return maxUsedSpace.getValue();
	}

	public void setMaxUsedSpace(int num) {
		maxUsedSpace.setValue(num);
		lblMaxsize.setText("MaxSize ("+num+" MB)");
	}
}
