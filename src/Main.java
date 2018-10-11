
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

	public static void main(String[] args) throws IOException {
		List<String> data = Files.readAllLines(Paths.get("TPicSim1.LST"), StandardCharsets.UTF_8);

		int acc = 0xff;
		Boolean zero = false;
		Boolean carry = false;
		Boolean dc = false;
		int[] memory = new int[1024];
		Arrays.fill(memory, 0xFFFF);

		for (String line : data) {
			if (line.charAt(0) == ' ') {
				continue;
			}

			String[] byteData = line.split(" ");

			Integer address = Integer.parseInt(byteData[0], 16);
			Integer command = Integer.parseInt(byteData[1], 16);

			memory[address] = command;
		}

		for (Integer a : memory) {
			if (a == 0xFFFF) {
				continue;
			}

			Instruction instruction = new Instruction(a);
			int[] instArgs = instruction.GetArgs();

			switch (instruction.GetName()) {
			case "movlw":
				acc = instArgs[0];

				zero = acc == 0;
				break;
			case "addlw":
				acc = instArgs[0] + acc;

				carry = acc > 255;
				zero = acc == 0;

				acc = acc & 0xFF;
				break;
			case "sublw":
				acc = instArgs[0] - acc;

				carry = acc >= 0;
				zero = acc == 0;

				acc = acc & 0xFF;
				break;
			case "andlw":
				acc = instArgs[0] & acc;

				zero = acc == 0;
				break;
			case "iorlw":
				acc = instArgs[0] | acc;

				zero = acc == 0;
				break;
			case "xorlw":
				acc = instArgs[0] ^ acc;

				zero = acc == 0;
				break;
			case "movwf":
				break;
			default:
				break;
			}

			System.out.println(instruction.GetName() + ":\t" + Integer.toHexString(acc) + "\tC=" + (carry ? 1 : 0) + " DC=" + (dc ? 1 : 0) + " Z=" + (zero ? 1 : 0));
		}

	}
}
