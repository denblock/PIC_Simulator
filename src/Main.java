import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import simulator.PIC;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Point;

public class Main {

	protected Shell shell;
	private PIC PIC;
	private String[] SFRs;
	private boolean Parsed;
	private boolean Reset;
	private boolean Modified;
	private Path filePath;
	private StyledText text;
	private Text textPc_Content;
	private Text textW_Content;
	private MenuItem mntmRun;
	private MenuItem mntmStep;
	private MenuItem mntmReset;
	private ToolItem tltmRun;
	private ToolItem tltmStep;
	private ToolItem tltmReset;
	private Composite composite_sfr;
	private Composite composite_gpr;
	private Button btnRA4;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Main window = new Main();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
		PIC = new PIC();
		SFRs = new String[] { "00h - Indirect addr.", "01h - TMR0", "02h - PCL", "03h - STATUS", "04h - FSR",
				"05h - PORTA", "06h - PORTB", "07h - ", "08h - EEDATA", "09h - EEADR", "0Ah - PCLATH", "0Bh - INTCON",
				"80h - Indirect addr.", "81h - OPTION", "82h - PCL", "83h - STATUS", "84h - FSR", "85h - TRISA",
				"86h - TRISB", "87h - ", "88h - EECON1", "89h - EECON2", "8Ah - PCLATH", "8Bh - INTCON" };

		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 * 
	 * @throws IOException
	 */
	protected void createContents() throws IOException {
		shell = new Shell();
		shell.setSize(1592, 1091);
		shell.setText("PIC Simulator - Unbenannt");

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_1 = new Menu(mntmFile);
		mntmFile.setMenu(menu_1);

		MenuItem mntmNew = new MenuItem(menu_1, SWT.NONE);
		mntmNew.addSelectionListener(new newSelectionListener());
		mntmNew.setText("New");

		MenuItem mntmOpen = new MenuItem(menu_1, SWT.NONE);
		mntmOpen.addSelectionListener(new openSelectionListener());
		mntmOpen.setText("Open...");

		MenuItem mntmSave = new MenuItem(menu_1, SWT.NONE);
		mntmSave.addSelectionListener(new saveSelectionListener());
		mntmSave.setText("Save");

		MenuItem mntmSaveAs = new MenuItem(menu_1, SWT.NONE);
		mntmSaveAs.addSelectionListener(new saveAsSelectionListener());
		mntmSaveAs.setText("Save As...");

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.addListener(SWT.Selection, (e) -> shell.close());
		mntmExit.setText("Exit");

		MenuItem mntmSimulator = new MenuItem(menu, SWT.CASCADE);
		mntmSimulator.setText("Simulator");

		Menu menu_2 = new Menu(mntmSimulator);
		mntmSimulator.setMenu(menu_2);

		MenuItem mntmParse = new MenuItem(menu_2, SWT.NONE);
		mntmParse.addSelectionListener(new parseSelectionListener());
		mntmParse.setText("Parse");

		new MenuItem(menu_2, SWT.SEPARATOR);

		mntmRun = new MenuItem(menu_2, SWT.NONE);
		mntmRun.addSelectionListener(new runSelectionListener());
		mntmRun.setText("Run");

		mntmStep = new MenuItem(menu_2, SWT.NONE);
		mntmStep.addSelectionListener(new stepSelectionListener());
		mntmStep.setText("Step");

		new MenuItem(menu_2, SWT.SEPARATOR);

		mntmReset = new MenuItem(menu_2, SWT.NONE);
		mntmReset.addSelectionListener(new resetSelectionListener());
		mntmReset.setText("Reset");

		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText("Help");

		Menu menu_3 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_3);

		MenuItem mntmAbout = new MenuItem(menu_3, SWT.NONE);
		mntmAbout.setText("About");

		ToolBar toolBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		toolBar.setBounds(10, 0, 141, 33);

		tltmRun = new ToolItem(toolBar, SWT.NONE);
		tltmRun.addSelectionListener(new runSelectionListener());
		tltmRun.setText("Run");

		tltmStep = new ToolItem(toolBar, SWT.NONE);
		tltmStep.addSelectionListener(new stepSelectionListener());
		tltmStep.setText("Step");

		tltmReset = new ToolItem(toolBar, SWT.NONE);
		tltmReset.addSelectionListener(new resetSelectionListener());
		tltmReset.setText("Reset");

		text = new StyledText(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setFont(SWTResourceManager.getFont("Consolas", 9, SWT.NORMAL));
		text.setBounds(10, 45, 1056, 591);
		text.addListener(SWT.Modify, (e) -> {
			Modified = true;

			if (Parsed) {
				Parsed = false;
				Redraw();
			}
		});

		Group grpSpecialFunctionRegisters = new Group(shell, SWT.NONE);
		grpSpecialFunctionRegisters.setText("Special Function Registers");
		grpSpecialFunctionRegisters.setBounds(1092, 45, 452, 419);

		ScrolledComposite scrolledComposite = new ScrolledComposite(grpSpecialFunctionRegisters,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(10, 40, 432, 369);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		composite_sfr = new Composite(scrolledComposite, SWT.NONE);
		composite_sfr.setLayout(new GridLayout(2, false));

		Group grpGeneralPurposeRegisters = new Group(shell, SWT.NONE);
		grpGeneralPurposeRegisters.setText("General Purpose Registers");
		grpGeneralPurposeRegisters.setBounds(1092, 470, 452, 514);

		ScrolledComposite scrolledComposite_1 = new ScrolledComposite(grpGeneralPurposeRegisters,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_1.setExpandVertical(true);
		scrolledComposite_1.setExpandHorizontal(true);
		scrolledComposite_1.setBounds(10, 40, 432, 464);
		scrolledComposite_1.setMinSize(new Point(10, 10));

		composite_gpr = new Composite(scrolledComposite_1, SWT.NONE);
		composite_gpr.setLayout(new GridLayout(2, false));

		for (int i = 0; i < SFRs.length; i++) {
			Text text_1 = new Text(composite_sfr, SWT.BORDER | SWT.READ_ONLY);
			GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			gd_text_1.widthHint = 180;
			text_1.setLayoutData(gd_text_1);
			text_1.setText(SFRs[i]);

			Text text_2 = new Text(composite_sfr, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}

		for (int i = 0x0C; i < 0x2F; i++) {
			Text text_1 = new Text(composite_gpr, SWT.BORDER | SWT.READ_ONLY);
			GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			gd_text_1.widthHint = 180;
			text_1.setLayoutData(gd_text_1);
			text_1.setText(String.format("%02X", i));

			Text text_2 = new Text(composite_gpr, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}

		scrolledComposite.setContent(composite_sfr);
		scrolledComposite.setMinSize(composite_sfr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		scrolledComposite_1.setContent(composite_gpr);
		scrolledComposite_1.setMinSize(composite_gpr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Text txtPc = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		txtPc.setText("PC");
		txtPc.setBounds(10, 649, 120, 31);

		Text txtWRegister = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		txtWRegister.setText("W Register");
		txtWRegister.setBounds(10, 686, 120, 31);

		textPc_Content = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textPc_Content.setBounds(136, 649, 80, 31);

		textW_Content = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textW_Content.setBounds(136, 686, 80, 31);

		btnRA4 = new Button(shell, SWT.TOGGLE);
		btnRA4.setBounds(284, 649, 105, 35);
		btnRA4.setText("RA4");
		btnRA4.addListener(SWT.Selection, (e) -> PIC.RA4_Invoked(btnRA4.getSelection()));

		Redraw();
	}

	private void Redraw() {
		mntmRun.setEnabled(Parsed);
		mntmStep.setEnabled(Parsed);
		mntmReset.setEnabled(Parsed);
		tltmRun.setEnabled(Parsed);
		tltmStep.setEnabled(Parsed);
		tltmReset.setEnabled(Parsed);
		btnRA4.setEnabled(Parsed);

		for (int i = 0; i < text.getLineCount(); i++) {
			text.setLineBackground(i, 1, null);
		}

		if (Parsed) {
			text.setLineBackground(PIC.GetLSTOffset(PIC.GetPC()), 1,
					shell.getDisplay().getSystemColor(SWT.COLOR_GREEN));
			text.setSelection(text.getOffsetAtLine(PIC.GetLSTOffset(PIC.GetPC())));
		}

		textPc_Content.setText(String.format("%02X", PIC.GetPC()));
		textW_Content.setText(String.format("%02X", PIC.GetW()));

		Object[] texts = CombineArrays(composite_sfr.getChildren(), composite_gpr.getChildren());

		for (int i = 0; i < texts.length / 2; i++) {
			Text text_1 = (Text) texts[2 * i];
			Text text_2 = (Text) texts[(2 * i) + 1];
			int pos = Integer.parseInt(text_1.getText().substring(0, 2), 16);
			text_2.setText(String.format("%02X", PIC.GetRegister()[pos]));
		}
	}

	private void SaveFile(boolean searchPath) throws IOException {
		if (searchPath) {
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setFilterExtensions(new String[] { "*.lst" });
			String selected = fd.open();

			if (selected != null) {
				filePath = Paths.get(selected);
			} else {
				return;
			}
		}

		Files.write(filePath, text.getText().getBytes(StandardCharsets.UTF_8));
		shell.setText("PIC Simulator - " + filePath.getFileName());
		Modified = false;
	}

	private class newSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (Modified) {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage(
						"Möchten Sie Ihre Änderungen an \"" + shell.getText().substring(16) + "\" speichern?");
				messageBox.setText("PIC Simulator");

				if (messageBox.open() == SWT.YES) {
					try {
						SaveFile(filePath == null);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}

			filePath = null;
			text.setText("");
			shell.setText("PIC Simulator - Unbenannt");
		}
	}

	private class openSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			FileDialog fd = new FileDialog(shell, SWT.OPEN);
			fd.setFilterExtensions(new String[] { "*.lst" });
			String selected = fd.open();

			if (selected != null) {
				try {
					filePath = Paths.get(selected);
					List<String> arr = Files.readAllLines(filePath, StandardCharsets.UTF_8);
					String data = String.join(System.lineSeparator(), arr);
					text.setText(data);
					shell.setText("PIC Simulator - " + filePath.getFileName());

					Modified = false;
					Parsed = false;
					Redraw();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private class saveSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			try {
				SaveFile(filePath == null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class saveAsSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			try {
				SaveFile(true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class parseSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			PIC.ParseLST(text.getText());
			Parsed = true;
			Redraw();
		}
	}

	private class runSelectionListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			new Thread(() -> {
				int run = 0;
				Reset = false;

				while (!Reset && run == 0) {
					run = PIC.Step();

					shell.getDisplay().syncExec(() -> Redraw());
				}

			}).start();
		}
	}

	private class stepSelectionListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			new Thread(() -> {
				PIC.Step();
				shell.getDisplay().syncExec(() -> Redraw());
			}).start();
		}
	}

	private class resetSelectionListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			Reset = true;
			PIC.Reset();
			Redraw();
		}
	}

	private <T> Object[] CombineArrays(T[] t1, T[] t2) {
		Object[] result = new Object[t1.length + t2.length];
		System.arraycopy(t1, 0, result, 0, t1.length);
		System.arraycopy(t2, 0, result, t1.length, t2.length);
		return result;
	}
}
