import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;;

public class PIC {
	int W;
	boolean Z;
	boolean C;
	boolean DC;
	int[] Memory;
	int[] Reg;
	Stack<Integer> Stack;

	public void Simulate(String fileName) throws IOException {
		W = 0xFF;
		Z = false;
		C = false;
		DC = false;
		Stack = new Stack<Integer>();
		Memory = new int[1024];
		Reg = new int[1024];
		Arrays.fill(Memory, 0xFFFF);

		List<String> data = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);

		for (String line : data) {
			if (line.charAt(0) == ' ') {
				continue;
			}

			String[] byteData = line.split(" ");

			Integer address = Integer.parseInt(byteData[0], 16);
			Integer command = Integer.parseInt(byteData[1], 16);

			Memory[address] = command;
		}

		for (int i = 0; i < Memory.length; i++) {
			if (Memory[i] == 0xFFFF) {
				continue;
			}

			Instruction instruction = new Instruction(Memory[i]);
			String instName = instruction.GetName();
			int[] instArgs = instruction.GetArgs();

			switch (instName) {
			case "addlw":
			case "andlw":
			case "iorlw":
			case "movlw":
			case "sublw":
			case "xorlw":
				W = Calculate(instName, instArgs[0]);
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
				int result = Calculate(instName, Reg[instArgs[0]]);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}

				if ((instName == "decfsz" || instName == "incfsz") && result == 0) {
					i = i + 1;
				}
				break;
			case "bcf":
			case "bsf":
				Reg[instArgs[0]] = Calculate(instName, Reg[instArgs[0]], instArgs[1]);
				break;
			case "btfsc":
			case "btfss":
				int bit = Reg[instArgs[0]] & (1 << instArgs[1]);

				if ((instName == "btfsc" && bit == 0) || (instName == "btfss" && bit != 0)) {
					i = i + 1;
				}
				break;
			case "clrw":
				W = 0;
				Z = true;
				break;
			case "clrf":
				Reg[instArgs[0]] = 0;
				Z = true;
				break;
			case "movwf":
				Reg[instArgs[0]] = W;
				break;
			case "goto":
				i = instArgs[0] - 1;
				break;
			case "call":
				Stack.push(i + 1);
				i = instArgs[0] - 1;
				break;
			case "return":
				i = Stack.pop() - 1;
				break;
			case "retlw":
				W = instArgs[0];
				i = Stack.pop() - 1;
				break;
			default:
				break;
			}

			System.out.println(instName + ":\tW=" + Integer.toHexString(W) + ", FSR=" + Integer.toHexString(Reg[4])
					+ ", wert1=" + Integer.toHexString(Reg[0xC]) + ", wert2=" + Integer.toHexString(Reg[0xD])
					+ ", ergeb=" + Integer.toHexString(Reg[0xE]) + ", DC=" + (DC ? 1 : 0) + ", C=" + (C ? 1 : 0)
					+ ", Z=" + (Z ? 1 : 0));

		}
	}

	public int Calculate(String instructionName, int a, int b) {
		int result;
		int[] statusAffected = new int[] { 0, 0, 0 };	// { C, DC, Z }

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
		case "decfsz":
			result = a - 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "incf":
		case "incfsz":
			result = a + 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "rlf":
			result = (a << 1) | (C ? 1 : 0);
			C = ((a & 0x80) >> 7) == 1;
			break;
		case "rrf":
			result = (a >> 1) | ((C ? 1 : 0) << 7);
			C = (a & 0x01) == 1;
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
			C = result < a;
		}

		if (statusAffected[1] == 1) {
			DC = (result & 0x0F) < (a & 0x0F);
		}

		if (statusAffected[2] == 1) {
			Z = result == 0;
		}

		return result;
	}

	public int Calculate(String instructionName, int a) {
		return Calculate(instructionName, a, W);
	}
}