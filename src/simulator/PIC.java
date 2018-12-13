package simulator;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PIC {
	private int W;
	private int[] Memory;
	private Register Reg;
	EEPROM EEPROM;
	private Stack Stack;
	private int Runtime;
	int CyclesLeft;
	private int PC;
	private boolean WDE;
	int Quartz = 4;
	private Consumer<Integer> PC_Listener;
	private Consumer<Integer> W_Listener;
	private Consumer<Integer> Runtime_Listener;
	BiConsumer<Integer, Integer> Reg_Listener;
	BiConsumer<Integer, Integer> EEPROM_Listener;

	private int[] LST_Offset;

	public PIC() {
		Reg = new Register(this);
		EEPROM = new EEPROM(this);
	}

	public void Reset() {
		SetW(0xFF);
		Reg.Reset();
		EEPROM.Reset();
		Stack = new Stack();
		SetRuntime(0);
		CyclesLeft = Reg.GetPrescale();
		SetPC(0);
	}

	public void ParseLST(String lst) {
		LST_Offset = new int[1024];
		Memory = new int[1024];
		Arrays.fill(Memory, 0xFFFF);
		String[] data = lst.split(System.lineSeparator());

		for (int i = 0; i < data.length; i++) {
			if (data[i].charAt(0) == ' ') {
				continue;
			}

			String[] byteData = data[i].split(" ");

			Integer address = Integer.parseInt(byteData[0], 16);
			Integer command = Integer.parseInt(byteData[1], 16);

			Memory[address] = command;
			LST_Offset[address] = i;
		}

		Reset();
	}

	public int Step() {
		Instruction instruction;
		boolean incrementPC = true;

		if (!Reg.GetPD() || Reg.GetTO()) {
			instruction = new Instruction(Memory[PC]);
		} else {
			instruction = new Instruction(0);
			incrementPC = false;
		}

		String instName = instruction.GetName();
		int[] instArgs = instruction.GetArgs();
		int instCycles = instruction.GetCycles();

		switch (instName) {
		case "addlw":
		case "andlw":
		case "iorlw":
		case "movlw":
		case "sublw":
		case "xorlw":
			SetW(Calculate(instName, instArgs[0]));
			break;
		case "addwf":
		case "andwf":
		case "comf":
		case "decf":
		case "decfsz":
		case "incf":
		case "incfsz":
		case "iorwf":
		case "movf":
		case "rlf":
		case "rrf":
		case "subwf":
		case "swapf":
		case "xorwf":
			int result = Calculate(instName, Reg.Read(instArgs[0]));

			if (instArgs[1] == 1) {
				Reg.Write(instArgs[0], result);
			} else {
				SetW(result);
			}

			if ((instName == "decfsz" || instName == "incfsz") && result == 0) {
				instCycles++;
			}
			break;
		case "bcf":
		case "bsf":
			Reg.Write(instArgs[0], Calculate(instName, Reg.Read(instArgs[0]), instArgs[1]));
			break;
		case "btfsc":
		case "btfss":
			int bit = Reg.Read(instArgs[0]) & (1 << instArgs[1]);

			if ((instName == "btfsc" && bit == 0) || (instName == "btfss" && bit != 0)) {
				instCycles++;
			}
			break;
		case "clrw":
			SetW(0);
			Reg.SetZ(true);
			break;
		case "clrf":
			Reg.Write(instArgs[0], 0);
			Reg.SetZ(true);
			break;
		case "movwf":
			Reg.Write(instArgs[0], W);
			break;
		case "goto":
			SetPC(instArgs[0] | ((Reg.Read(0x0A) & 0x18) << 8));
			incrementPC = false;
			break;
		case "call":
			Stack.Push(PC + 1);
			SetPC(instArgs[0] | ((Reg.Read(0x0A) & 0x18) << 8));
			incrementPC = false;
			break;
		case "return":
			SetPC(Stack.Pop());
			incrementPC = false;
			break;
		case "retlw":
			SetW(instArgs[0]);
			SetPC(Stack.Pop());
			incrementPC = false;
			break;
		case "retfie":
			Reg.SetGIE(true);
			SetPC(Stack.Pop());
			incrementPC = false;
			break;
		case "sleep":
			Reg.SetPD(true);
			Reg.SetTO(false);
			ClearWDT();
			break;
		case "clrwdt":
			Reg.SetPD(false);
			Reg.SetTO(false);
			ClearWDT();
			break;
		default:
			break;
		}

		if (!Reg.GetClockSource()) {
			CyclesLeft -= instCycles;
		}

		if (CyclesLeft <= 0) {
			Reg.IncrementTMR0();

			CyclesLeft += Reg.GetPrescale();
		}

		SetRuntime(Runtime + instCycles * (4 / Quartz));

		if (WDE) {
			int time = 18 * 1000 * (Reg.GetPSA() ? Reg.GetPrescale() : 1);

			if (Runtime >= time) {
				SetRuntime(Runtime - time);

				if (Reg.GetPD() && !Reg.GetTO()) {
					Reg.SetTO(true);
				} else {
					Reset();
					Reg.SetWRERR(true);
					incrementPC = false;
				}
			}
		}

		if (Reg.EEPROM_Time > 0) {
			Reg.EEPROM_Time-= 4/Quartz;

			if (Reg.EEPROM_Time <= 0) {
				Reg.SetEEIF(true);
				Reg.EEPROM_Time += 1000;
			}
		}

		if (incrementPC) {
			SetPC(PC + instCycles);
		}

		return Reg.GetPD() && !Reg.GetTO() ? 1 : 0;
	}

	int Calculate(String instructionName, int a, int b) {
		int result;
		int[] statusAffected = new int[] { 0, 0, 0 }; // { C, DC, Z }

		switch (instructionName) {
		case "movlw":
		case "movf":
			result = a;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "addwf":
		case "addlw":
			result = a + b;
			statusAffected = new int[] { 1, 1, 1 };
			break;
		case "subwf":
		case "sublw":
			result = a - b;
			statusAffected = new int[] { 1, 1, 1 };
			break;
		case "andwf":
		case "andlw":
			result = a & b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "iorwf":
		case "iorlw":
			result = a | b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "xorwf":
		case "xorlw":
			result = a ^ b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "comf":
			result = ~a;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "decf":
			result = a - 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "decfsz":
			result = a - 1;
			break;
		case "incf":
			result = a + 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "incfsz":
			result = a + 1;
			break;
		case "rlf":
			result = (a << 1) | (Reg.GetC() ? 1 : 0);
			Reg.SetC((a & 0x80) == 0x80);
			break;
		case "rrf":
			result = (a >> 1) | ((Reg.GetC() ? 1 : 0) << 7);
			Reg.SetC((a & 0x01) == 0x01);
			break;
		case "bcf":
			result = a & ~(1 << b);
			break;
		case "bsf":
			result = a | (1 << b);
			break;
		case "swapf":
			result = (a & 0x0F) << 4 | (a & 0xF0) >> 4;
			break;
		default:
			result = 0;
		}

		result = result & 0xFF;

		if (statusAffected[0] == 1) {
			Reg.SetC(result < a);
		}

		if (statusAffected[1] == 1) {
			Reg.SetDC((result & 0x0F) < (a & 0x0F));
		}

		if (statusAffected[2] == 1) {
			Reg.SetZ(result == 0);
		}

		return result;
	}

	int Calculate(String instructionName, int a) {
		return Calculate(instructionName, a, W);
	}

	private void ClearWDT() {
		SetRuntime(0);

		if (Reg.GetPSA()) {
			Reg.ClearPrescale();
		}
	}

	public void RA_Invoked(int port) {
		if (port == 4 && Reg.GetClockSource() && Reg.GetINTEDG() != Reg.GetPortA(4)) {
			CyclesLeft--;
		}

		Reg.SetPortA(port, !Reg.GetPortA(port));
	}

	public void RB_Invoked(int port) {
		if (port == 0 && Reg.GetINTE() && Reg.GetINTEDG() != Reg.GetPortB(0)) {
			Reg.SetINTF(true);
			Interrupt();
		}

		Reg.SetPortB(port, !Reg.GetPortB(port));

		if (port >= 4 && port <= 7 && Reg.GetRBIE()) {
			Reg.SetRBIF(true);
			Interrupt();
		}
	}

	void Interrupt() {
		if (Reg.GetPD() && !Reg.GetTO()) {
			Reg.SetPD(false);
			Step();
		}

		if (Reg.GetGIE()) {
			Reg.SetGIE(false);
			Stack.Push(PC);
			SetPC(4);
		}
	}

	public int GetLSTOffset(int idx) {
		if (LST_Offset == null) {
			return -1;
		}

		return LST_Offset[idx];
	}

	public void SetWDE(boolean wde) {
		WDE = wde;
	}

	public void SetQuartz(int quartz) {
		Quartz = quartz;
	}

	public void SetPCListener(Consumer<Integer> f) {
		PC_Listener = f;
	}

	public void SetWListener(Consumer<Integer> f) {
		W_Listener = f;
	}

	public void SetRuntimeListener(Consumer<Integer> f) {
		Runtime_Listener = f;
	}

	public void SetRegListener(BiConsumer<Integer, Integer> f) {
		Reg_Listener = f;
	}

	public void SetEEPROMListener(BiConsumer<Integer, Integer> f) {
		EEPROM_Listener = f;
	}

	private void SetW(int w) {
		if (W == w) {
			return;
		}

		W = w;

		if (W_Listener != null) {
			W_Listener.accept(w);
		}
	}

	void SetPC(int pc) {
		if (PC == pc) {
			return;
		}

		PC = pc;
		Reg.DirectWrite(0x02, pc & 0xFF);

		if (PC_Listener != null) {
			PC_Listener.accept(pc);
		}
	}

	private void SetRuntime(int runtime) {
		if (Runtime == runtime) {
			return;
		}

		Runtime = runtime;

		if (Runtime_Listener != null) {
			Runtime_Listener.accept(runtime);
		}
	}
}