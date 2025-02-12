package src.EscalarModel;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import src.ArquiteturaModel.Contexto;
import src.ArquiteturaModel.Instruction;

public class Escalar {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public Contexto[] contextos = new Contexto[3];
    public ArrayList<Instruction> imtPipeline = new ArrayList<>();
    public ArrayList<Instruction> bmtPipeline = new ArrayList<>();
    public ArrayList<Instruction> refPipeline = new ArrayList<>(); // Pipeline de Referência
    public int totalInstructionsIMT = 0;
    public int totalInstructionsBMT = 0;
    public int totalInstructionsREF = 0;
    public int blockSize = 3;

    public Escalar() {
        String instruction;
        ArrayList<Instruction> instructions = new ArrayList<>();
        try {
            for (int i = 0; i < contextos.length; i++) {
                RandomAccessFile randomAccessFile = new RandomAccessFile("thread" + i + ".txt", "r");
                while ((instruction = randomAccessFile.readLine()) != null) {
                    String[] operands = instruction.split(" ");
                    if (operands.length < 4) {
                        System.out.println("Formato de instrução inválido na linha: " + instruction);
                        continue;
                    }
                    instructions.add(
                            new Instruction(operands[0], operands[1], operands[2], operands[3], i));
                }

                contextos[i] = new Contexto(i, instructions);
                instructions.clear();

                randomAccessFile.close();
            }

        } catch (IOException e) {
            System.out.println("Exceção ao ler arquivos de thread.");
            e.printStackTrace();
        }
    }

    public void createIMTPipeline() {
        imtPipeline.clear();
        totalInstructionsIMT = 0;
        for (Contexto contexto : contextos) {
            totalInstructionsIMT += contexto.qtdInstrucoes;
        }
        int[] ponteiros = new int[contextos.length];
        for (int i = 0; i < ponteiros.length; i++) {
            ponteiros[i] = 0;
        }
        for (int i = 0; i < totalInstructionsIMT; i++) {
            int contextoAtual = i % contextos.length;
            if (ponteiros[contextoAtual] < contextos[contextoAtual].qtdInstrucoes) {
                imtPipeline.add(contextos[contextoAtual].instructions.get(ponteiros[contextoAtual]));
                ponteiros[contextoAtual]++;
            }
        }
        System.out.print("IMT Pipeline: ");
        for (var i : imtPipeline) {
            System.out.print(i.inst + " ");
        }
        System.out.println();
    }

    public void createBMTPipeline() {
        bmtPipeline.clear();
        totalInstructionsBMT = 0;
        for (Contexto contexto : contextos) {
            totalInstructionsBMT += contexto.qtdInstrucoes;
        }
        int[] ponteiros = new int[contextos.length];
        for (int i = 0; i < ponteiros.length; i++) {
            ponteiros[i] = 0;
        }
        for (int i = 0; i < totalInstructionsBMT; i++) {
            int contextoAtual = i % contextos.length;
            for (int k = 0; k < blockSize; k++) {
                if (ponteiros[contextoAtual] < contextos[contextoAtual].qtdInstrucoes) {
                    bmtPipeline.add(contextos[contextoAtual].instructions.get(ponteiros[contextoAtual]));
                    ponteiros[contextoAtual]++;
                }
            }
        }
        encontraBolha();
    }

    public void createREFPipeline() {
        refPipeline.clear();
        totalInstructionsREF = 0;
        for (Contexto contexto : contextos) {
            refPipeline.addAll(contexto.instructions);
        }
        totalInstructionsREF = refPipeline.size();
        System.out.print("REF Pipeline: ");
        for (var i : refPipeline) {
            System.out.print(i.inst + " ");
        }
        System.out.println();
    }

    public void encontraBolha() {
        for (int i = 0; i < bmtPipeline.size(); i++) {
            if (bmtPipeline.get(i).inst.equals("LDW")||bmtPipeline.get(i).inst.equals("SDW")||bmtPipeline.get(i).inst.equals("JMP")||bmtPipeline.get(i).inst.equals("BEQ")||bmtPipeline.get(i).inst.equals("BNQ")) {
                if (i + 1 < bmtPipeline.size()) {
                    if (ehBolha(i)) {
                        Instruction bubble = new Instruction("BUB", "0", "0", "0", bmtPipeline.get(i).contexto);
                        bmtPipeline.add(i + 1, bubble);
                        i += 1;
                    }
                }
            }
        }
        System.out.print("BMT Pipeline: ");
        for (var instruction : bmtPipeline) {
            System.out.print(instruction.inst + " ");
        }
        System.out.println();
    }

    public boolean ehBolha(int i) {
        if (i + 1 >= bmtPipeline.size()) return false;
        Instruction current = bmtPipeline.get(i);
        Instruction next = bmtPipeline.get(i + 1);
        return next.contexto == current.contexto &&
               (next.op1.equals(current.dest) || next.op2.equals(current.dest));
    }

    public void printPipeline(int multithread) {
        ArrayList<Instruction> pipeline;
        if (multithread == 0) {
            pipeline = imtPipeline;
        } else if (multithread == 1) {
            pipeline = bmtPipeline;
        } else {
            pipeline = refPipeline;
        }
        int pointer = -1;
        if (pipeline.size() == 0) {
            System.out.println("PIPELINE VAZIO");
            return;
        }
        for (int i = 0; i < pipeline.size() + 5; i++) {
            pointer = i;
            for (int j = 0; j < 5; j++) {
                int currentIndex = pointer - j;
                if (currentIndex >= 0 && currentIndex < pipeline.size()) {
                    Instruction instr = pipeline.get(currentIndex);
                    String color = (instr.contexto == 0) ? ANSI_RED : ANSI_GREEN;
                    System.out.print(color + "| " + instr.inst + " |" + ANSI_RESET);
                } else {
                    System.out.print("| NOP |");
                }
            }
            System.out.println();
        }
    }

}