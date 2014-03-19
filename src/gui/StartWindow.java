package gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import utils.ChunkManager;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import core.Message;
import core.Processor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;

public class StartWindow {

	private final int DEFAULT_MAX_SIZE = 50;
	private static final String FROM_PATH = "./", TO_PATH = "./";
	
	private Processor core;
	private JFrame frmDistributedBackupSystem;
	private JTextArea logs;
	private JComboBox<String> files;
	private final JFileChooser fc = new JFileChooser();
	private File selectedFile;

	static void test() {
		System.out.println(Message.bytesToHex(ChunkManager.fileToSHA256("forms-1.3.0.jar")));
		byte[] sha = null;
		final String FILENAME = "DatabaseManagementSystems.pdf";
		
		try {
			System.out.println(ChunkManager.createChunks(FROM_PATH + FILENAME, TO_PATH, sha));
			ChunkManager.mergeChunks(FROM_PATH, FILENAME, TO_PATH);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {

		final Processor p = new Processor(args);
		//test();
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					StartWindow window = new StartWindow(p, args);
					window.frmDistributedBackupSystem.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		p.process();

	}

	/**
	 * Create the application.
	 */
	public StartWindow(Processor core, String[] args) {
		this.core = core;
		initialize(args);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(String[] args) {
		frmDistributedBackupSystem = new JFrame();
		frmDistributedBackupSystem.setTitle("Distributed Backup System");
		frmDistributedBackupSystem.setBounds(100, 100, 588, 462);
		frmDistributedBackupSystem.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

		final JLabel lblMaxsize = new JLabel("MaxSize ("+DEFAULT_MAX_SIZE+" MB)");
		frmDistributedBackupSystem.getContentPane().add(lblMaxsize, "4, 10");

		final JSlider slider = new JSlider();
		slider.setValue(DEFAULT_MAX_SIZE);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblMaxsize.setText("MaxSize (" + slider.getValue() + " MB)");
				
				core.setSpaceLimit(slider.getValue());
			}
		});
		frmDistributedBackupSystem.getContentPane().add(slider, "6, 10, 5, 1");

		JLabel lblCurrentsize = new JLabel("Current Size 0 MB");
		frmDistributedBackupSystem.getContentPane().add(lblCurrentsize, "4, 12");

		final JLabel filePathLabel = new JLabel("New File Path:");
		frmDistributedBackupSystem.getContentPane().add(filePathLabel, "4, 16");

		JButton browseFileButton = new JButton("Browse File...");
		frmDistributedBackupSystem.getContentPane().add(browseFileButton, "6, 16");

		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		frmDistributedBackupSystem.getContentPane().add(spinner, "8, 16");

		JButton btnBackup = new JButton("Backup");
		btnBackup.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				core.addFile(selectedFile.getName(), (int) spinner.getValue());
			}
		});
		frmDistributedBackupSystem.getContentPane().add(btnBackup, "10, 16");

		files = new JComboBox<String>();
		frmDistributedBackupSystem.getContentPane().add(files, "4, 20, 7, 1, fill, default");

		JButton btnRestore = new JButton("Restore...");
		btnRestore.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				core.restoreFile((String) files.getSelectedItem(), TO_PATH);
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
		
		logs = new JTextArea();
		scrollPane.setViewportView(logs);

		browseFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(frmDistributedBackupSystem.getContentPane());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					selectedFile = fc.getSelectedFile();

					if(selectedFile != null) {
						filePathLabel.setText("New file path: " + selectedFile.getName());
					}
				}
			}
		});
		
		replaceFileList(new String[0]);
	}

	public void log(String text) {
		logs.append(text + "\n");
	}
	
	public void replaceFileList(String[] fileNames) {
		files.removeAllItems();
		
		for(String filename: fileNames)
			files.addItem(filename);
	}
}
