package gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import core.Processor;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.SpinnerNumberModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JSlider;
import javax.swing.JTextArea;

public class StartWindow {

	private Processor core;
	private JFrame frmDistributedBackupSystem;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		final Processor p = new Processor(args);
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					StartWindow window = new StartWindow(p);
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
	public StartWindow(Processor core) {
		this.core = core;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmDistributedBackupSystem = new JFrame();
		frmDistributedBackupSystem.setTitle("Distributed Backup System");
		frmDistributedBackupSystem.setBounds(100, 100, 563, 464);
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
		
		JLabel mcAdressLabel = new JLabel("Multicast Address:");
		frmDistributedBackupSystem.getContentPane().add(mcAdressLabel, "4, 4");
		
		JLabel mdrAddressLabel = new JLabel("Multicast Restore Adress:");
		frmDistributedBackupSystem.getContentPane().add(mdrAddressLabel, "4, 6");
		
		JLabel mdbAddressLabel = new JLabel("Multicast Backup Address");
		frmDistributedBackupSystem.getContentPane().add(mdbAddressLabel, "4, 8");
		
		JLabel lblMaxsize = new JLabel("MaxSize (0 MB)");
		frmDistributedBackupSystem.getContentPane().add(lblMaxsize, "4, 10");
		
		JSlider slider = new JSlider();
		frmDistributedBackupSystem.getContentPane().add(slider, "6, 10, 5, 1");
		
		JLabel lblCurrentsize = new JLabel("Current Size 0 MB");
		frmDistributedBackupSystem.getContentPane().add(lblCurrentsize, "4, 12");
		
		JLabel filePathLabel = new JLabel("New File Path:");
		frmDistributedBackupSystem.getContentPane().add(filePathLabel, "4, 16");
		
		JButton browseFileButton = new JButton("Browse File...");
		frmDistributedBackupSystem.getContentPane().add(browseFileButton, "6, 16");
		
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		frmDistributedBackupSystem.getContentPane().add(spinner, "8, 16");
		
		JButton btnBackup = new JButton("Backup");
		btnBackup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		frmDistributedBackupSystem.getContentPane().add(btnBackup, "10, 16");
		
		JComboBox comboBox = new JComboBox();
		frmDistributedBackupSystem.getContentPane().add(comboBox, "4, 20, 7, 1, fill, default");
		
		JButton btnRestore = new JButton("Restore...");
		btnRestore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		frmDistributedBackupSystem.getContentPane().add(btnRestore, "8, 22");
		
		JButton btnRemove = new JButton("Remove");
		frmDistributedBackupSystem.getContentPane().add(btnRemove, "10, 22");
		
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		frmDistributedBackupSystem.getContentPane().add(textArea, "4, 26, 7, 1, fill, fill");
	}

}
