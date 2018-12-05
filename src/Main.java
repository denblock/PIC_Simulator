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
	private MenuItem mntmRun;
	private MenuItem mntmStep;
	private MenuItem mntmReset;
	private ToolItem tltmRun;
	private ToolItem tltmStep;
	private ToolItem tltmReset;
	private Composite composite_sfr;
	private Composite composite_gpr;
	private Button[] btns_RA;
	private Button[] btns_RB;
	private Text[] texts_RA;
	private Text[] texts_RB;

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
		shell.setSize((int) (rect.width * 0.5382), (int) (rect.height * 0.6747));
		shell.setText("PIC Simulator - Unbenannt");

		rect = shell.getClientArea();

		btns_RA = new Button[8];
		btns_RB = new Button[8];
		texts_RA = new Text[8];
		texts_RB = new Text[8];

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_1 = new Menu(mntmFile);
		mntmFile.setMenu(menu_1);

		MenuItem mntmNew = new MenuItem(menu_1, SWT.NONE);
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
		});

		MenuItem mntmOpen = new MenuItem(menu_1, SWT.NONE);
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

		MenuItem mntmSave = new MenuItem(menu_1, SWT.NONE);
		mntmSave.setText("Save");
		mntmSave.addListener(SWT.Selection, (e) -> {
			try {
				SaveFile(filePath == null);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		MenuItem mntmSaveAs = new MenuItem(menu_1, SWT.NONE);
		mntmSaveAs.setText("Save As...");
		mntmSaveAs.addListener(SWT.Selection, (e) -> {
			try {
				SaveFile(true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		new MenuItem(menu_1, SWT.SEPARATOR);

		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.addListener(SWT.Selection, (e) -> shell.close());
		mntmExit.setText("Exit");

		MenuItem mntmSimulator = new MenuItem(menu, SWT.CASCADE);
		mntmSimulator.setText("Simulator");

		Menu menu_2 = new Menu(mntmSimulator);
		mntmSimulator.setMenu(menu_2);

		MenuItem mntmParse = new MenuItem(menu_2, SWT.NONE);
		mntmParse.setText("Parse");
		mntmParse.setEnabled(false);
		mntmParse.addListener(SWT.Selection, (e) -> {
			PIC.ParseLST(text.getText());
			SetParsed(true);
		});

		new MenuItem(menu_2, SWT.SEPARATOR);

		mntmRun = new MenuItem(menu_2, SWT.NONE);
		mntmRun.addListener(SWT.Selection, (e) -> runClick());
		mntmRun.setText("Run");
		mntmRun.setEnabled(false);

		mntmStep = new MenuItem(menu_2, SWT.NONE);
		mntmStep.addListener(SWT.Selection, (e) -> stepClick());
		mntmStep.setText("Step");
		mntmStep.setEnabled(false);

		new MenuItem(menu_2, SWT.SEPARATOR);

		mntmReset = new MenuItem(menu_2, SWT.NONE);
		mntmReset.addListener(SWT.Selection, (e) -> resetClick());
		mntmReset.setText("Reset");
		mntmReset.setEnabled(false);

		MenuItem mntmWindow = new MenuItem(menu, SWT.CASCADE);
		mntmWindow.setText("Window");

		Menu menu_3 = new Menu(mntmWindow);
		mntmWindow.setMenu(menu_3);

		MenuItem mntmValuesBinary = new MenuItem(menu_3, SWT.CHECK);
		mntmValuesBinary.setText("Display Binary Values");
		mntmValuesBinary.addListener(SWT.Selection, (e) -> SetValuesBinary(!Values_Binary));

		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText("Help");

		Menu menu_4 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_4);

		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");

		ToolBar toolBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		toolBar.setBounds(10, 0, rect.width, 33);

		tltmRun = new ToolItem(toolBar, SWT.NONE);
		tltmRun.addListener(SWT.Selection, (e) -> runClick());
		tltmRun.setText("Run");
		tltmRun.setEnabled(false);

		tltmStep = new ToolItem(toolBar, SWT.NONE);
		tltmStep.addListener(SWT.Selection, (e) -> stepClick());
		tltmStep.setText("Step");
		tltmStep.setEnabled(false);

		tltmReset = new ToolItem(toolBar, SWT.NONE);
		tltmReset.addListener(SWT.Selection, (e) -> resetClick());
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

			text.redraw();

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

		Group grpSpecialFunctionRegisters = new Group(shell, SWT.NONE);
		grpSpecialFunctionRegisters.setText("Special Function Registers");
		grpSpecialFunctionRegisters.setBounds(rect.width - 476, 45, 452, (int) (rect.height * 0.4173));

		ScrolledComposite scrolledComposite = new ScrolledComposite(grpSpecialFunctionRegisters,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(10, 40, 432, (int) (rect.height * 0.4173) - 50);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		composite_sfr = new Composite(scrolledComposite, SWT.NONE);
		composite_sfr.setLayout(new GridLayout(2, false));

		Group grpGeneralPurposeRegisters = new Group(shell, SWT.NONE);
		grpGeneralPurposeRegisters.setText("General Purpose Registers");
		grpGeneralPurposeRegisters.setBounds(rect.width - 476, (int) (rect.height * 0.4173) + 51, 452,
				(int) (rect.height * 0.485));

		ScrolledComposite scrolledComposite_1 = new ScrolledComposite(grpGeneralPurposeRegisters,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_1.setExpandVertical(true);
		scrolledComposite_1.setExpandHorizontal(true);
		scrolledComposite_1.setBounds(10, 40, 432, (int) (rect.height * 0.485) - 50);
		scrolledComposite_1.setMinSize(new Point(10, 10));

		composite_gpr = new Composite(scrolledComposite_1, SWT.NONE);
		composite_gpr.setLayout(new GridLayout(2, false));

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

		scrolledComposite.setContent(composite_sfr);
		scrolledComposite.setMinSize(composite_sfr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		scrolledComposite_1.setContent(composite_gpr);
		scrolledComposite_1.setMinSize(composite_gpr.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Group grpStatus = new Group(shell, SWT.NONE);
		grpStatus.setText("Status");
		grpStatus.setBounds(10, (int) (rect.width * 0.3769) + 58, 446, 165);

		Text textPc = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textPc.setBounds(10, 38, 120, 35);
		textPc.setText("PC");

		textPc_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textPc_Content.setBounds(136, 38, 106, 35);

		Text textWRegister = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY);
		textWRegister.setText("W Register");
		textWRegister.setBounds(10, 79, 120, 35);

		textW_Content = new Text(grpStatus, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
		textW_Content.setBounds(136, 79, 106, 35);

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

		for (int i = 0; i < 8; i++) {
			final int _i = Integer.valueOf(i);

			btns_RA[i] = new Button(shell, SWT.NONE);
			btns_RA[i].setText("RA" + i);
			btns_RA[i].setBounds(10 + i * 111, (int) (rect.width * 0.3769) + 241, 105, 35);
			btns_RA[i].setEnabled(false);
			btns_RA[i].addListener(SWT.Selection, (e) -> PIC.RA_Invoked(_i));

			btns_RB[i] = new Button(shell, SWT.NONE);
			btns_RB[i].setText("RB" + i);
			btns_RB[i].setBounds(10 + i * 111, (int) (rect.width * 0.3769) + 282, 105, 35);
			btns_RB[i].setEnabled(false);
			btns_RB[i].addListener(SWT.Selection, (e) -> PIC.RB_Invoked(_i));

			texts_RA[i] = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
			texts_RA[i].setText("RA" + i);
			texts_RA[i].setBounds(10 + i * 111, (int) (rect.width * 0.3769) + 241, 105, 35);
			texts_RA[i].setEnabled(false);
			texts_RA[i].setVisible(false);

			texts_RB[i] = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
			texts_RB[i].setText("RB" + i);
			texts_RB[i].setBounds(10 + i * 111, (int) (rect.width * 0.3769) + 282, 105, 35);
			texts_RB[i].setEnabled(false);
			texts_RB[i].setVisible(false);
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

	private void SetParsed(boolean parsed) {
		if (Parsed == parsed) {
			return;
		}

		mntmRun.setEnabled(parsed);
		mntmStep.setEnabled(parsed);
		mntmReset.setEnabled(parsed);
		tltmRun.setEnabled(parsed);
		tltmStep.setEnabled(parsed);
		tltmReset.setEnabled(parsed);

		for (int i = 0; i < 8; i++) {
			btns_RA[i].setEnabled(parsed);
			btns_RB[i].setEnabled(parsed);
			texts_RA[i].setEnabled(parsed);
			texts_RB[i].setEnabled(parsed);
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
		if (Values_Binary == binary) {
			return;
		}

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

	private void Reset_PIC() {
		for (int i = 0; i <= 0x8B; i++) {
			Register_Changed(i, 0);
			
			if(i == 0x2F) {
				i = 0x80;
			}
		}

		PIC.Reset();
	}

	private void PC_Changed(int pc) {
		textPc_Content.setText(IntToString(pc));

		int line = PIC.GetLSTOffset(pc);

		if (line == -1) {
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
			text.setLineBackground(line, 1, shell.getDisplay().getSystemColor(SWT.COLOR_GREEN));
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
				boolean portA = (address & 0x05) == 0x05;

				Color green = shell.getDisplay().getSystemColor(SWT.COLOR_GREEN);
				Button[] btns = portA ? btns_RA : btns_RB;
				Text[] txts = portA ? texts_RA : texts_RB;

				if (address == 0x05 || address == 0x06) {
					for (int i = 0; i < 8; i++) {
						Color bg = (value & (1 << i)) == 0 ? null : green;

						if (!btns[i].getBackground().equals(bg)) {
							btns[i].setBackground(bg);
						}

						if (!txts[i].getBackground().equals(bg)) {
							txts[i].setBackground(bg);
						}

					}
				} else {
					for (int i = 0; i < 8; i++) {
						boolean input = (value & (1 << i)) == 0;

						if (btns[i].getVisible() == input) {
							btns[i].setVisible(!input);
						}

						if (txts[i].getVisible() != input) {
							txts[i].setVisible(input);
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

	private void runClick() {
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
					shell.getDisplay().syncExec(() -> Reset_PIC());
					Reset_Requested = false;
				}
			}).start();
		}
	}

	private void stepClick() {
		new Thread(() -> {
			int step = PIC.Step();

			if (step == 1) {
				Sleeping = true;

				while (PIC.Step() == 1) {
					if (Reset_Requested) {
						shell.getDisplay().syncExec(() -> Reset_PIC());
						Reset_Requested = false;
						break;
					}
				}

				Sleeping = false;
			}
		}).start();
	}

	private void resetClick() {
		if (Running || Sleeping) {
			Reset_Requested = true;
		} else {
			Reset_PIC();
		}
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
