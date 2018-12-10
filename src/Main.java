import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import simulator.PIC;

import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;

public class Main {
	protected Shell shell;
	private PIC PIC;
	private boolean Parsed;
	private boolean Running;
	private boolean Sleeping;
	private boolean Reset_Requested;
	private boolean Modified;
	private boolean Values_Binary;
	private boolean Display_EEPROM;
	private int Displayed_Lines;
	private boolean WDE;
	private Path filePath;
	private StyledText text;
	private Text textPc_Content;
	private Text textW_Content;
	private Text textRuntime_Content;
	private Text textCarry_Content;
	private Text textDC_Content;
	private Text textZero_Content;
	private MenuItem mntmParse;
	private MenuItem mntmRun;
	private MenuItem mntmStep;
	private MenuItem mntmReset;
	private ToolItem tltmRun;
	private ToolItem tltmStep;
	private ToolItem tltmReset;
	private Composite composite_sfr;
	private Composite composite_gpr;
	private Composite composite_eeprom;
	private ScrolledComposite scrolledComposite_gpr;
	private Group grp_gpr;
	private Group grp_eeprom;
	private Group grp_ports;

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
		PIC.SetPCListener((pc) -> shell.getDisplay().syncExec(() -> PC_Changed(pc)));
		PIC.SetWListener((w) -> shell.getDisplay().syncExec(() -> W_Changed(w)));
		PIC.SetRuntimeListener((runtime) -> shell.getDisplay().syncExec(() -> Runtime_Changed(runtime)));
		PIC.SetRegListener((address, value) -> shell.getDisplay().syncExec(() -> Register_Changed(address, value)));
		PIC.SetEEPROMListener((address, value) -> shell.getDisplay().syncExec(() -> EEPROM_Changed(address, value)));

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
		shell = new Shell(SWT.CLOSE | SWT.TITLE | SWT.MIN);
		shell.setImage(new Image(shell.getDisplay(), Main.class.getClassLoader().getResourceAsStream("micro.ico")));
		Rectangle rect = shell.getDisplay().getClientArea();
		shell.setSize((int) (rect.width * 0.5382), (int) (rect.width * 0.369));
		shell.setText("PIC Simulator - Unbenannt");

		rect = shell.getClientArea();

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_file = new Menu(mntmFile);
		mntmFile.setMenu(menu_file);

		MenuItem mntmNew = new MenuItem(menu_file, SWT.NONE);
		mntmNew.setText("New");
		mntmNew.addListener(SWT.Selection, (e) -> {
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
			Modified = false;
		});

		MenuItem mntmOpen = new MenuItem(menu_file, SWT.NONE);
		mntmOpen.setText("Open...");
		mntmOpen.addListener(SWT.Selection, (e) -> {
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
					SetParsed(false);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		MenuItem mntmSave = new MenuItem(menu_file, SWT.NONE);
		mntmSave.setText("Save");
		mntmSave.addListener(SWT.Selection, (e) -> {
			try {
				SaveFile(filePath == null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		MenuItem mntmSaveAs = new MenuItem(menu_file, SWT.NONE);
		mntmSaveAs.setText("Save As...");
		mntmSaveAs.addListener(SWT.Selection, (e) -> {
			try {
				SaveFile(true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		new MenuItem(menu_file, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_file, SWT.NONE);
		mntmExit.addListener(SWT.Selection, (e) -> shell.close());
		mntmExit.setText("Exit");

		MenuItem mntmSimulator = new MenuItem(menu, SWT.CASCADE);
		mntmSimulator.setText("Simulator");

		Menu menu_simulator = new Menu(mntmSimulator);
		mntmSimulator.setMenu(menu_simulator);

		mntmParse = new MenuItem(menu_simulator, SWT.NONE);
		mntmParse.setText("Parse");
		mntmParse.setEnabled(false);
		mntmParse.addListener(SWT.Selection, (e) -> {
			PIC.ParseLST(text.getText());
			PC_Changed(0);
			SetParsed(true);
		});

		new MenuItem(menu_simulator, SWT.SEPARATOR);

		mntmRun = new MenuItem(menu_simulator, SWT.NONE);
		mntmRun.addListener(SWT.Selection, (e) -> RunClick());
		mntmRun.setText("Run");
		mntmRun.setEnabled(false);

		mntmStep = new MenuItem(menu_simulator, SWT.NONE);
		mntmStep.addListener(SWT.Selection, (e) -> StepClick());
		mntmStep.setText("Step");
		mntmStep.setEnabled(false);

		new MenuItem(menu_simulator, SWT.SEPARATOR);

		mntmReset = new MenuItem(menu_simulator, SWT.NONE);
		mntmReset.addListener(SWT.Selection, (e) -> ResetClick());
		mntmReset.setText("Reset");
		mntmReset.setEnabled(false);

		MenuItem mntmWindow = new MenuItem(menu, SWT.CASCADE);
		mntmWindow.setText("Window");

		Menu menu_window = new Menu(mntmWindow);
		mntmWindow.setMenu(menu_window);

		MenuItem mntmValuesBinary = new MenuItem(menu_window, SWT.CHECK);
		mntmValuesBinary.setText("Display Binary Values");
		mntmValuesBinary.addListener(SWT.Selection, (e) -> SetValuesBinary(!Values_Binary));

		MenuItem mntmDisplayEEPROM = new MenuItem(menu_window, SWT.CHECK);
		mntmDisplayEEPROM.setText("Display EEPROM");
		mntmDisplayEEPROM.addListener(SWT.Selection, (e) -> SetDisplayEEPROM(!Display_EEPROM));

		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText("Help");

		Menu menu_4 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_4);

		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");

		ToolBar toolBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		toolBar.setBounds(10, 0, rect.width, 33);

		tltmRun = new ToolItem(toolBar, SWT.NONE);
		tltmRun.addListener(SWT.Selection, (e) -> RunClick());
		tltmRun.setText("Run");
		tltmRun.setEnabled(false);

		tltmStep = new ToolItem(toolBar, SWT.NONE);
		tltmStep.addListener(SWT.Selection, (e) -> StepClick());
		tltmStep.setText("Step");
		tltmStep.setEnabled(false);

		tltmReset = new ToolItem(toolBar, SWT.NONE);
		tltmReset.addListener(SWT.Selection, (e) -> ResetClick());
		tltmReset.setText("Reset");
		tltmReset.setEnabled(false);

		StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, 25);
		style.foreground = shell.getDisplay().getSystemColor(SWT.COLOR_RED);
		Bullet bullet_point = new Bullet(ST.BULLET_DOT, style);

		style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, 25);
		style.foreground = shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT);
		Bullet bullet_empty = new Bullet(ST.BULLET_CUSTOM, style);

		text = new StyledText(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setFont(new Font(shell.getDisplay(), new FontData("Consolas", 9, SWT.NORMAL)));
		text.setBounds(10, 45, rect.width - 512, (int) (rect.width * 0.3769));
		Displayed_Lines = (text.getClientArea().height / text.getLineHeight()) - 1;
		text.addListener(SWT.Modify, (e) -> {
			for (int i = 0; i < text.getLineCount(); i++) {
				if (text.getLineBullet(i) == null) {
					text.setLineBullet(i, 1, bullet_empty);
				}
			}

			// text.redraw();

			mntmParse.setEnabled(text.getText().length() != 0);
			Modified = true;

			SetRunning(false);
			SetParsed(false);
		});
		text.addListener(SWT.MouseUp, (e) -> {
			if (e.x > 25) {
				return;
			}

			int line = text.getLineAtOffset(text.getCaretOffset());
			String lineString = text.getLine(line);

			if (lineString.length() == 0 || lineString.charAt(0) == 32) {
				return;
			}

			boolean empty = text.getLineBullet(line).style.foreground.getAlpha() == 0;
			text.setLineBullet(line, 1, null);
			text.setLineBullet(line, 1, empty ? bullet_point : bullet_empty);
		});

		Group grp_sfr = new Group(shell, SWT.NONE);
		grp_sfr.setText("Special Function Registers");
		grp_sfr.setBounds(rect.width - 476, 45, 452, (int) (rect.height * 0.4173));

		ScrolledComposite scrolledComposite_sfr = new ScrolledComposite(grp_sfr,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_sfr.setBounds(10, 40, 432, (int) (rect.height * 0.4173) - 50);
		scrolledComposite_sfr.setExpandHorizontal(true);
		scrolledComposite_sfr.setExpandVertical(true);

		composite_sfr = new Composite(scrolledComposite_sfr, SWT.NONE);
		composite_sfr.setLayout(new GridLayout(2, false));

		grp_gpr = new Group(shell, SWT.NONE);
		grp_gpr.setText("General Purpose Registers");
		grp_gpr.setBounds(rect.width - 476, (int) (rect.height * 0.4173) + 51, 452, (int) (rect.height * 0.514));

		scrolledComposite_gpr = new ScrolledComposite(grp_gpr, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_gpr.setExpandVertical(true);
		scrolledComposite_gpr.setExpandHorizontal(true);
		scrolledComposite_gpr.setBounds(10, 40, 432, (int) (rect.height * 0.514) - 50);
		scrolledComposite_gpr.setMinSize(new Point(10, 10));

		composite_gpr = new Composite(scrolledComposite_gpr, SWT.NONE);
		composite_gpr.setLayout(new GridLayout(2, false));

		grp_eeprom = new Group(shell, SWT.NONE);
		grp_eeprom.setText("EEPROM");
		grp_eeprom.setBounds(rect.width - 476, (int) (rect.height * 0.6973) + 57, 452, (int) (rect.height * 0.23));
		grp_eeprom.setVisible(false);

		ScrolledComposite scrolledComposite_eeprom = new ScrolledComposite(grp_eeprom,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_eeprom.setExpandVertical(true);
		scrolledComposite_eeprom.setExpandHorizontal(true);
		scrolledComposite_eeprom.setBounds(10, 40, 432, (int) (rect.height * 0.23) - 50);
		scrolledComposite_eeprom.setMinSize(new Point(10, 10));

		composite_eeprom = new Composite(scrolledComposite_eeprom, SWT.NONE);
		composite_eeprom.setLayout(new GridLayout(2, false));

		String[] SFRs = new String[] { "00h - Indirect addr.", "01h - TMR0", "02h - PCL", "03h - STATUS", "04h - FSR",
				"05h - PORTA", "06h - PORTB", "07h - ", "08h - EEDATA", "09h - EEADR", "0Ah - PCLATH", "0Bh - INTCON",
				"80h - Indirect addr.", "81h - OPTION", "82h - PCL", "83h - STATUS", "84h - FSR", "85h - TRISA",
				"86h - TRISB", "87h - ", "88h - EECON1", "89h - EECON2", "8Ah - PCLATH", "8Bh - INTCON" };

		for (int i = 0; i < SFRs.length; i++) {
			Text text_1 = new Text(composite_sfr, SWT.BORDER | SWT.READ_ONLY);
			GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			gd_text_1.widthHint = 180;
			text_1.setLayoutData(gd_text_1);
			text_1.setText(SFRs[i]);

			Text text_2 = new Text(composite_sfr, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			text_2.setText("00");
		}

		for (int i = 0x0C; i <= 0x2F; i++) {
			Text text_1 = new Text(composite_gpr, SWT.BORDER | SWT.READ_ONLY);
			GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			gd_text_1.widthHint = 180;
			text_1.setLayoutData(gd_text_1);
			text_1.setText(String.format("%02X", i));

			Text text_2 = new Text(composite_gpr, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			text_2.setText("00");
		}

		for (int i = 0; i <= 0x3F; i++) {
			Text text_1 = new Text(composite_eeprom, SWT.BORDER | SWT.READ_ONLY);
			GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			gd_text_1.widthHint = 180;
			text_1.setLayoutData(gd_text_1);
			text_1.setText(String.format("%02X", i));

			Text text_2 = new Text(composite_eeprom, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			text_2.setText("00");
		}

		scrolledComposite_sfr.setContent(composite_sfr);
		scrolledComposite_sfr.setMinSize(composite_sfr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		scrolledComposite_gpr.setContent(composite_gpr);
		scrolledComposite_gpr.setMinSize(composite_gpr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		scrolledComposite_eeprom.setContent(composite_eeprom);
		scrolledComposite_eeprom.setMinSize(composite_eeprom.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Group grpStatus = new Group(shell, SWT.NONE);
		grpStatus.setText("Status");
		grpStatus.setBounds(10, (int) (rect.width * 0.3769) + 58, 446, 165);

		Text textPc = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textPc.setBounds(10, 38, 120, 35);
		textPc.setText("PC");

		textPc_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textPc_Content.setBounds(136, 38, 106, 35);
		textPc_Content.setText("00");

		Text textWRegister = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textWRegister.setText("W Register");
		textWRegister.setBounds(10, 79, 120, 35);

		textW_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textW_Content.setBounds(136, 79, 106, 35);
		textW_Content.setText("FF");

		Text textRuntime = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textRuntime.setText("Runtime");
		textRuntime.setBounds(10, 120, 120, 35);

		textRuntime_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textRuntime_Content.setBounds(136, 120, 106, 35);
		textRuntime_Content.setText("0");

		Text textCarry = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textCarry.setBounds(260, 38, 120, 35);
		textCarry.setText("Carry Flag");

		textCarry_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textCarry_Content.setBounds(386, 38, 50, 35);

		Text textDC = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textDC.setBounds(260, 79, 120, 35);
		textDC.setText("DC Flag");

		textDC_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textDC_Content.setBounds(386, 79, 50, 35);

		Text textZero = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textZero.setBounds(260, 120, 120, 35);
		textZero.setText("Zero Flag");

		textZero_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textZero_Content.setBounds(386, 120, 50, 35);

		grp_ports = new Group(shell, SWT.NONE);
		grp_ports.setBounds(10, (int) (rect.width * 0.4892), 902, 124);
		grp_ports.setText("I/O Ports");

		for (int i = 0; i < 8; i++) {
			final int _i = Integer.valueOf(i);

			Button btn = new Button(grp_ports, SWT.NONE);
			btn.setText("RA" + i);
			btn.setBounds(10 + i * 111, 38, 105, 35);
			btn.setEnabled(false);
			btn.addListener(SWT.Selection, (e) -> PIC.RA_Invoked(_i));

			Text text_p = new Text(grp_ports, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
			text_p.setText("RA" + i);
			text_p.setBounds(10 + i * 111, 38, 105, 35);
			text_p.setEnabled(false);
			text_p.setVisible(false);

			btn = new Button(grp_ports, SWT.NONE);
			btn.setText("RB" + i);
			btn.setBounds(10 + i * 111, 79, 105, 35);
			btn.setEnabled(false);
			btn.addListener(SWT.Selection, (e) -> PIC.RB_Invoked(_i));

			text_p = new Text(grp_ports, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
			text_p.setText("RB" + i);
			text_p.setBounds(10 + i * 111, 79, 105, 35);
			text_p.setEnabled(false);
			text_p.setVisible(false);
		}

		Button btnWde = new Button(shell, SWT.NONE);
		btnWde.setBounds(rect.width - 607, 834, 105, 35);
		btnWde.setText("WDE");
		btnWde.addListener(SWT.Selection, (e) -> {
			WDE = !WDE;
			btnWde.setBackground(WDE ? shell.getDisplay().getSystemColor(SWT.COLOR_GREEN) : null);
			PIC.SetWDE(WDE);
		});
		btnWde.notifyListeners(SWT.Selection, new Event());

		Combo combo = new Combo(shell, SWT.READ_ONLY);
		combo.setBounds(rect.width - 769, 834, 156, 33);
		combo.setItems("4MHz Quartz", "1MHz Quartz");
		combo.select(0);
		combo.addListener(SWT.Selection, (e) -> PIC.SetQuartz(combo.getSelectionIndex() == 0 ? 4 : 1));

		PIC.Reset();
	}

	private void RunClick() {
		SetRunning(!Running);

		if (Running) {
			new Thread(() -> {
				long n = System.currentTimeMillis();

				while (PIC.Step() != -1) {
					if (!Running || Reset_Requested) {
						break;
					}
				}

				System.out.println(System.currentTimeMillis() - n);

				shell.getDisplay().syncExec(() -> SetRunning(false));

				if (Reset_Requested) {
					shell.getDisplay().syncExec(() -> PIC.Reset());
					Reset_Requested = false;
				}
			}).start();
		}
	}

	private void StepClick() {
		new Thread(() -> {
			int step = PIC.Step();

			if (step == 1) {
				Sleeping = true;

				while (PIC.Step() == 1) {
					if (Reset_Requested) {
						shell.getDisplay().syncExec(() -> PIC.Reset());
						Reset_Requested = false;
						break;
					}
				}

				Sleeping = false;
			}
		}).start();
	}

	private void ResetClick() {
		if (Running || Sleeping) {
			Reset_Requested = true;
		} else {
			PIC.Reset();
		}
	}

	private void SetParsed(boolean parsed) {
		if (Parsed == parsed) {
			return;
		}

		mntmParse.setEnabled(!parsed);
		mntmRun.setEnabled(parsed);
		mntmStep.setEnabled(parsed);
		mntmReset.setEnabled(parsed);
		tltmRun.setEnabled(parsed);
		tltmStep.setEnabled(parsed);
		tltmReset.setEnabled(parsed);

		Control[] ports = grp_ports.getChildren();

		for (int i = 0; i < ports.length; i++) {
			ports[i].setEnabled(parsed);
		}

		Parsed = parsed;
	}

	private void SetRunning(boolean running) {
		if (Running == running) {
			return;
		}

		String str = running ? "Pause" : "Run";

		mntmRun.setText(str);
		tltmRun.setText(str);
		mntmStep.setEnabled(!running);
		tltmStep.setEnabled(!running);

		Running = running;
	}

	private void SetValuesBinary(boolean binary) {
		Values_Binary = binary;

		Control[] texts = composite_sfr.getChildren();

		for (int i = 1; i < texts.length; i += 2) {
			Text val_Text = (Text) texts[i];
			int val = Integer.parseInt(val_Text.getText(), binary ? 16 : 2);
			val_Text.setText(IntToString(val));
			GridData gr = (GridData) val_Text.getLayoutData();
			gr.widthHint = 50;
			val_Text.setLayoutData(gr);
		}

		texts = composite_gpr.getChildren();

		for (int i = 1; i < texts.length; i += 2) {
			Text val_Text = (Text) texts[i];
			int val = Integer.parseInt(val_Text.getText(), binary ? 16 : 2);
			val_Text.setText(IntToString(val));
		}

		int val = Integer.parseInt(textPc_Content.getText(), binary ? 16 : 2);
		textPc_Content.setText(IntToString(val));

		val = Integer.parseInt(textW_Content.getText(), binary ? 16 : 2);
		textW_Content.setText(IntToString(val));
	}

	private void SetDisplayEEPROM(boolean display) {
		Display_EEPROM = display;

		int gpr_height = (int) (shell.getClientArea().height * (display ? 0.28 : 0.514));

		grp_gpr.setSize(452, gpr_height);
		scrolledComposite_gpr.setSize(432, gpr_height - 50);
		grp_eeprom.setVisible(display);
	}

	private void PC_Changed(int pc) {
		textPc_Content.setText(IntToString(pc));

		int line = PIC.GetLSTOffset(pc);

		if (line == -1 || !Parsed) {
			return;
		}

		for (int i = 0; i < text.getLineCount(); i++) {
			if (text.getLineBackground(i) != null) {
				text.setLineBackground(i, 1, null);
			}
		}

		if (text.getLineBullet(line).style.foreground.getAlpha() != 0) {
			SetRunning(false);
			text.setLineBackground(line, 1, shell.getDisplay().getSystemColor(SWT.COLOR_YELLOW));
		} else {
			text.setLineBackground(line, 1, shell.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		}

		int idx = line - text.getTopIndex();

		if (idx <= 0 || idx >= Displayed_Lines) {
			text.setTopIndex(line);
		}
	}

	private void W_Changed(int w) {
		textW_Content.setText(IntToString(w));
	}

	private void Runtime_Changed(int runtime) {
		textRuntime_Content.setText(runtime + "µs");
	}

	private void Register_Changed(int address, int value) {
		Text hex_text;

		if (address >= 0x0C && address <= 0x2F) {
			hex_text = (Text) composite_gpr.getChildren()[((address - 0x0C) * 2) + 1];
		} else {
			Control[] texts = composite_sfr.getChildren();

			if (address <= 0x0B) {
				hex_text = (Text) texts[(address * 2) + 1];
			} else {
				hex_text = (Text) texts[((address - 0x80 + 0x0C) * 2) + 1];
			}

			if (address == 0x05 || address == 0x06 || address == 0x85 || address == 0x86) {
				int offset = (address & 0x05) == 0x05 ? 0 : 2;
				Control[] ports = grp_ports.getChildren();

				if (address == 0x05 || address == 0x06) {
					Color green = shell.getDisplay().getSystemColor(SWT.COLOR_GREEN);

					for (int i = 0; i < 8; i++) {
						Color bg = (value & (1 << i)) == 0 ? null : green;

						if (!ports[i * 4 + offset].getBackground().equals(bg)) {
							ports[i * 4 + offset].setBackground(bg);
						}

						if (!ports[i * 4 + offset + 1].getBackground().equals(bg)) {
							ports[i * 4 + offset + 1].setBackground(bg);
						}
					}
				} else {
					for (int i = 0; i < 8; i++) {
						boolean output = (value & (1 << i)) == 0;

						if (ports[i * 4 + offset].getVisible() == output) {
							ports[i * 4 + offset].setVisible(!output);
						}

						if (ports[i * 4 + offset + 1].getVisible() != output) {
							ports[i * 4 + offset + 1].setVisible(output);
						}
					}
				}
			} else if (address == 0x03) {
				String c_str = (value & 0x01) == 0x01 ? "1" : "0";
				String dc_str = (value & 0x02) == 0x02 ? "1" : "0";
				String z_str = (value & 0x04) == 0x04 ? "1" : "0";

				if (!textCarry_Content.getText().equals(c_str)) {
					textCarry_Content.setText(c_str);
				}

				if (!textDC_Content.getText().equals(dc_str)) {
					textDC_Content.setText(dc_str);
				}

				if (!textZero_Content.getText().equals(z_str)) {
					textZero_Content.setText(z_str);
				}
			}
		}

		String str = IntToString(value);

		if (hex_text.getText() != str) {
			hex_text.setText(str);
		}
	}

	private void EEPROM_Changed(int address, int value) {
		Text hex_text = (Text) composite_eeprom.getChildren()[(address * 2) + 1];
		String str = IntToString(value);

		if (hex_text.getText() != str) {
			hex_text.setText(str);
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

	private String IntToString(int val) {
		if (Values_Binary) {
			String str = Integer.toBinaryString(val);

			while (str.length() < 8) {
				str = "0" + str;
			}

			return str;
		} else {
			return String.format("%02X", val);
		}
	}
}
