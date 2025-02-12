package src.ArquiteturaModel;
import java.util.ArrayList;

public class Contexto {

    public int id;
    public ArrayList<Instruction> instructions = new ArrayList<>();
    public int qtdInstrucoes;

    public Contexto(int id, ArrayList<Instruction> instructions) {
        this.id = id;
        this.instructions = new ArrayList<>(instructions);
        this.qtdInstrucoes = instructions.size();
    }

}